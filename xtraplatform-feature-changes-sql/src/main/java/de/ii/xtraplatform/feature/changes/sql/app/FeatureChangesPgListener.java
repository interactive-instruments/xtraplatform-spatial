/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.changes.sql.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.feature.changes.sql.domain.FeatureChangesPgConfiguration;
import de.ii.xtraplatform.features.domain.FeatureChange;
import de.ii.xtraplatform.features.domain.FeatureChangeHandler;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureQueriesExtension;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.Query;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql.Dialect;
import de.ii.xtraplatform.features.sql.domain.SqlClient;
import de.ii.xtraplatform.features.sql.domain.SqlConnector;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class FeatureChangesPgListener implements FeatureQueriesExtension {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureChangesPgListener.class);

  private final ScheduledExecutorService executorService;
  private final Map<String, Map<String, Subscription>> subscriptions;

  @Inject
  public FeatureChangesPgListener() {
    this.executorService = new ScheduledThreadPoolExecutor(1);
    this.subscriptions = new ConcurrentHashMap<>();
  }

  @Override
  public boolean isSupported(FeatureProviderConnector<?, ?, ?> connector) {
    return connector instanceof SqlConnector
        && ((SqlConnector) connector).getDialect() == Dialect.PGIS;
  }

  @Override
  public void on(
      LIFECYCLE_HOOK hook, FeatureProvider2 provider, FeatureProviderConnector<?, ?, ?> connector) {
    Optional<FeatureChangesPgConfiguration> configuration = getConfiguration(provider.getData());

    if (configuration.isPresent()) {
      SqlClient sqlClient = ((SqlConnector) connector).getSqlClient();

      switch (hook) {
        case STARTED:
          subscribe(
              provider, configuration.get(), sqlClient::getConnection, sqlClient::getNotifications);
          break;
      }
    }
  }

  @Override
  public void on(
      QUERY_HOOK hook,
      FeatureProviderDataV2 data,
      FeatureProviderConnector<?, ?, ?> connector,
      Query query,
      BiConsumer<String, String> aliasResolver) {}

  private Optional<FeatureChangesPgConfiguration> getConfiguration(FeatureProviderDataV2 data) {
    return data.getExtensions().stream()
        .filter(
            extension ->
                extension.isEnabled() && extension instanceof FeatureChangesPgConfiguration)
        .map(extension -> (FeatureChangesPgConfiguration) extension)
        .findFirst();
  }

  private void subscribe(
      FeatureProvider2 provider,
      FeatureChangesPgConfiguration configuration,
      Supplier<Connection> connectionSupplier,
      Function<Connection, List<String>> notificationPoller) {
    subscriptions.put(provider.getId(), new ConcurrentHashMap<>());

    getSubscriptions(
            provider.getData().getTypes(),
            configuration.getListenForTypes(),
            connectionSupplier,
            notificationPoller)
        .forEach(subscription -> subscribe(provider.getId(), subscription));

    executorService.scheduleWithFixedDelay(
        () -> poll(provider.getId(), provider.getChangeHandler()),
        configuration.getPollingInterval().toSeconds(),
        configuration.getPollingInterval().toSeconds(),
        TimeUnit.SECONDS);
  }

  private void subscribe(String provider, Subscription unconnected) {
    try {
      Subscription connected = unconnected.connect();
      subscriptions.get(provider).put(connected.getChannel(), connected);

      Statement stmt = connected.getConnection().createStatement();
      stmt.execute(connected.getCreateFunction());
      stmt.execute(connected.getCreateTrigger());
      stmt.execute(connected.getListen());
      stmt.close();

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Subscribed to feature changes for {}", connected.getChannel());
      }
    } catch (SQLException e) {
      LogContext.error(
          LOGGER, e, "Could not subscribe to feature changes for {}", unconnected.getChannel());
    }
  }

  private void poll(String provider, FeatureChangeHandler featureChangeHandler) {
    if (subscriptions.containsKey(provider)) {
      subscriptions
          .get(provider)
          .forEach((channel, subscription) -> poll(provider, featureChangeHandler, subscription));
    }
  }

  private void poll(
      String provider, FeatureChangeHandler featureChangeHandler, Subscription subscription) {
    if (!subscription.isConnected()) {
      LOGGER.debug("Lost connection to retrieve feature changes for {}", subscription.getChannel());
      subscriptions.get(provider).remove(subscription.getChannel());
      subscribe(provider, subscription);

      return;
    }

    for (String notification : subscription.pollNotifications()) {
      // if (LOGGER.isTraceEnabled()) {
      LOGGER.debug("Feature change notification received: " + notification);
      // }
      onFeatureChange(subscription.getType(), featureChangeHandler, notification);
    }
  }

  private void onFeatureChange(
      String featureType, FeatureChangeHandler featureChangeHandler, String payload) {
    try {
      FeatureChange featureChange = Notification.from(featureType, payload).asFeatureChange();
      featureChangeHandler.handle(featureChange);
    } catch (Throwable e) {
      LogContext.errorAsInfo(
          LOGGER, e, "Could not parse feature change notification, the notification is ignored");
    }
  }

  private List<Subscription> getSubscriptions(
      Map<String, FeatureSchema> types,
      List<String> includes,
      Supplier<Connection> connectionSupplier,
      Function<Connection, List<String>> notificationPoller) {
    return types.entrySet().stream()
        .filter(entry -> includes.isEmpty() || includes.contains(entry.getKey()))
        .flatMap(
            entry ->
                entry.getValue().getEffectiveSourcePaths().stream()
                    .map(
                        sourcePath ->
                            ImmutableSubscription.builder()
                                .connectionFactory(connectionSupplier)
                                .notificationPoller(notificationPoller)
                                .type(entry.getKey())
                                .table(sourcePath.substring(1))
                                .idColumn(
                                    entry
                                        .getValue()
                                        .getIdProperty()
                                        .flatMap(FeatureSchema::getSourcePath)
                                        .orElseThrow())
                                .geometryColumn(
                                    entry
                                        .getValue()
                                        .getPrimaryGeometry()
                                        .map(s -> s.getSourcePath().orElseThrow()))
                                .intervalColumns(
                                    entry
                                        .getValue()
                                        .getPrimaryInterval()
                                        .map(
                                            t ->
                                                Tuple.of(
                                                    t.first().getSourcePath().orElseThrow(),
                                                    t.second().getSourcePath().orElseThrow())))
                                .instantColumn(
                                    entry
                                        .getValue()
                                        .getPrimaryInstant()
                                        .map(s -> s.getSourcePath().orElseThrow()))
                                .build()))
        .collect(Collectors.toList());
  }
}
