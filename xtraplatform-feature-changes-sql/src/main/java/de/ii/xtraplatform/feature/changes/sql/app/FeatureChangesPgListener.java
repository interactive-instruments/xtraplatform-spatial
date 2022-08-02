/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.changes.sql.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.util.Triple;
import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.feature.changes.sql.domain.FeatureChangesPgConfiguration;
import de.ii.xtraplatform.features.domain.FeatureChange;
import de.ii.xtraplatform.features.domain.FeatureChange.Action;
import de.ii.xtraplatform.features.domain.FeatureChangeHandler;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureQueriesExtension;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureChange;
import de.ii.xtraplatform.features.domain.Query;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql.Dialect;
import de.ii.xtraplatform.features.sql.domain.SqlClient;
import de.ii.xtraplatform.features.sql.domain.SqlConnector;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.threeten.extra.Interval;

@Singleton
@AutoBind
public class FeatureChangesPgListener implements FeatureQueriesExtension {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureChangesPgListener.class);

  private final ScheduledExecutorService executorService;
  private final Map<
          String,
          Map<
              Tuple<String, String>,
              Triple<Connection, Function<Connection, List<String>>, Supplier<Connection>>>>
      subscriptions;

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

    getChannels(provider.getData().getTypes(), configuration.getListenForTypes())
        .forEach(
            channel ->
                subscribe(provider.getId(), channel, connectionSupplier, notificationPoller));

    executorService.scheduleWithFixedDelay(
        () -> poll(provider.getId(), provider.getFeatureChangeHandler()), 15, 5, TimeUnit.SECONDS);
  }

  private void subscribe(
      String provider,
      Tuple<String, String> channel,
      Supplier<Connection> connectionSupplier,
      Function<Connection, List<String>> notificationPoller) {
    try {
      Connection connection = connectionSupplier.get();
      Statement stmt = connection.createStatement();
      stmt.execute("LISTEN " + String.format("%s_%s", channel.first(), channel.second()));
      stmt.close();
      subscriptions
          .get(provider)
          .put(channel, Triple.of(connection, notificationPoller, connectionSupplier));
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Subscribed to feature changes for {}", channel);
      }
    } catch (SQLException e) {
      LogContext.error(LOGGER, e, "Could not subscribe to feature changes for {}", channel);
    }
  }

  private void poll(String provider, FeatureChangeHandler featureChangeHandler) {
    if (subscriptions.containsKey(provider)) {
      subscriptions
          .get(provider)
          .forEach(
              (channel, connection) -> {
                poll(
                    provider,
                    featureChangeHandler,
                    channel,
                    connection.first(),
                    connection.second(),
                    connection.third());
              });
    }
  }

  private void poll(
      String provider,
      FeatureChangeHandler featureChangeHandler,
      Tuple<String, String> channel,
      Connection connection,
      Function<Connection, List<String>> notificationPoller,
      Supplier<Connection> connectionSupplier) {
    try {
      // need to poll the notification queue using a dummy query
      Statement stmt = connection.createStatement();
      ResultSet rs = stmt.executeQuery("SELECT 1");
      rs.close();
      stmt.close();

      List<String> notifications = notificationPoller.apply(connection);
      for (String notification : notifications) {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Feature change notification received: " + notification);
        }
        onFeatureChange(channel.first(), featureChangeHandler, notification);
      }
    } catch (SQLException e) {
      try {
        if (connection.isValid(1)) {
          // assume a temporary issue
          LogContext.errorAsDebug(
              LOGGER, e, "Temporary failure to retrieve feature changes for {}", channel);
          return;
        }
      } catch (SQLException ex) {
        // continue
      }

      // try to establish a new connection
      LogContext.errorAsDebug(
          LOGGER, e, "Lost connection to retrieve feature changes for {}", channel);
      subscriptions.get(provider).remove(channel);
      subscribe(provider, channel, connectionSupplier, notificationPoller);
    }
  }

  private void onFeatureChange(
      String featureType, FeatureChangeHandler featureChangeHandler, String notification) {
    try {
      FeatureChange featureChange = parseFeatureChange(featureType, notification);
      featureChangeHandler.handle(featureChange);
    } catch (Throwable e) {
      LogContext.errorAsInfo(
          LOGGER, e, "Could not parse feature change notification, the notification is ignored");
    }
  }

  private List<Tuple<String, String>> getChannels(
      Map<String, FeatureSchema> types, List<String> subscriptions) {
    return types.entrySet().stream()
        .filter(entry -> subscriptions.isEmpty() || subscriptions.contains(entry.getKey()))
        .flatMap(
            entry ->
                entry.getValue().getEffectiveSourcePaths().stream()
                    .map(
                        sourcePath ->
                            Tuple.of(entry.getKey(), sourcePath.substring(1, sourcePath.length()))))
        .collect(Collectors.toList());
  }

  private static final Splitter SPLITTER = Splitter.on(",").trimResults();

  private FeatureChange parseFeatureChange(String featureType, String notification) {
    List<String> parameters = SPLITTER.splitToList(notification);

    if (parameters.size() < 9) {
      throw new IllegalArgumentException("incomplete parameters - " + notification);
    }

    Action action = Action.fromString(parameters.get(0));

    if (action == Action.UNKNOWN) {
      throw new IllegalArgumentException("unknown action - " + parameters.get(0));
    }

    return ImmutableFeatureChange.builder()
        .action(action)
        .featureType(featureType)
        .featureIds(parseFeatureId(parameters.get(2)))
        .interval(parseInterval(parameters.subList(3, 5)))
        .boundingBox(parseBbox(parameters.subList(5, 9)))
        .build();
  }

  private static List<String> parseFeatureId(String featureId) {
    if (featureId.isEmpty() || featureId.equalsIgnoreCase("NULL")) return ImmutableList.of();

    return ImmutableList.of(featureId);
  }

  private static Optional<Interval> parseInterval(List<String> interval) {
    if (interval.get(0).isEmpty()) {
      // no instant or interval, ignore
    } else if (interval.get(1).isEmpty()) {
      // an instant
      try {
        Instant instant = parseTimestamp(interval.get(0));
        if (Objects.nonNull(instant)) return Optional.of(Interval.of(instant, instant));
      } catch (Exception e) {
        // ignore
      }
    } else {
      // an interval
      try {
        Instant begin = parseTimestamp(interval.get(0));
        Instant end = parseTimestamp(interval.get(1));
        return Optional.of(Interval.of(begin, end));
      } catch (Exception e) {
        // ignore
      }
    }
    return Optional.empty();
  }

  private static Instant parseTimestamp(String timestamp) {
    try {
      return Instant.parse(timestamp);
    } catch (Exception e) {
      return null;
    }
  }

  private static Optional<BoundingBox> parseBbox(List<String> bbox) {
    try {
      return Optional.of(
          BoundingBox.of(
              Double.parseDouble(bbox.get(0)),
              Double.parseDouble(bbox.get(1)),
              Double.parseDouble(bbox.get(2)),
              Double.parseDouble(bbox.get(3)),
              OgcCrs.CRS84));
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
