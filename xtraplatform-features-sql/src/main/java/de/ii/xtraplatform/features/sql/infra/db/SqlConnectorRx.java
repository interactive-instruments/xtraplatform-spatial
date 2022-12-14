/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.infra.db;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap.Builder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.features.domain.ConnectionInfo;
import de.ii.xtraplatform.features.domain.Tuple;
import de.ii.xtraplatform.features.sql.app.FeatureProviderSql;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql.Dialect;
import de.ii.xtraplatform.features.sql.domain.SqlClient;
import de.ii.xtraplatform.features.sql.domain.SqlConnector;
import de.ii.xtraplatform.features.sql.domain.SqlQueryOptions;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.davidmoten.rx.jdbc.Database;
import org.davidmoten.rx.jdbc.pool.DatabaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
public class SqlConnectorRx implements SqlConnector {

  public static final String CONNECTOR_TYPE = "SLICK";
  private static final SqlQueryOptions NO_OPTIONS = SqlQueryOptions.withColumnTypes(String.class);
  private static final Logger LOGGER = LoggerFactory.getLogger(SqlConnectorRx.class);

  private final SqlDataSourceFactory sqlDataSourceFactory;
  private final ConnectionInfoSql connectionInfo;
  private final String poolName;
  private final MetricRegistry metricRegistry;
  private final HealthCheckRegistry healthCheckRegistry;
  private final int maxConnections;
  private final int minConnections;
  private final int queueSize;
  private final Path dataDir;
  private final String applicationName;
  private final String providerId;
  private final AtomicInteger refCounter;

  private Database session;
  private HikariDataSource dataSource;
  private SqlClient sqlClient;
  private Throwable connectionError;

  @AssistedInject
  public SqlConnectorRx(
      SqlDataSourceFactory sqlDataSourceFactory,
      AppContext appContext,
      @Assisted MetricRegistry metricRegistry,
      @Assisted HealthCheckRegistry healthCheckRegistry,
      @Assisted String providerId,
      @Assisted ConnectionInfoSql connectionInfo) {
    this.sqlDataSourceFactory = sqlDataSourceFactory;
    this.connectionInfo = connectionInfo;
    this.poolName = String.format("db.%s", providerId);
    this.metricRegistry = metricRegistry;
    this.healthCheckRegistry = healthCheckRegistry;

    this.maxConnections = connectionInfo.getPool().getMaxConnections();
    this.minConnections =
        connectionInfo.getPool().getMinConnections() >= 0
            ? connectionInfo.getPool().getMinConnections()
            : maxConnections;

    // int capacity = maxConnections / maxQueries;
    // TODO
    this.queueSize = 1024; // Math.max(1024, maxConnections * capacity * 2);

    this.dataDir = appContext.getDataDir();
    // LOGGER.debug("QUEUE {} {} {} {} {}", connectionInfo.getDatabase(), maxQueries,
    // maxConnections, capacity, maxConnections * capacity * 2);

    this.applicationName =
        String.format("%s %s - %s", appContext.getName(), appContext.getVersion(), providerId);
    this.providerId = providerId;
    this.refCounter = new AtomicInteger(0);
  }

  @Override
  public String getType() {
    return String.format("%s/%s", FeatureProviderSql.PROVIDER_TYPE, CONNECTOR_TYPE);
  }

  @Override
  public int getMaxConnections() {
    return maxConnections;
  }

  @Override
  public int getMinConnections() {
    return minConnections;
  }

  @Override
  public int getQueueSize() {
    return queueSize;
  }

  @Override
  public Dialect getDialect() {
    return connectionInfo.getDialect();
  }

  @Override
  public void start() {
    try {
      HikariConfig hikariConfig = createHikariConfig();
      this.dataSource = new HikariDataSource(hikariConfig);
      this.session = createSession(dataSource);
      this.sqlClient = new SqlClientRx(session, connectionInfo.getDialect());
    } catch (Throwable e) {
      // TODO: handle properly, service start should fail with error message, show in manager
      // LOGGER.error("CONNECTING TO DB FAILED", e);
      this.connectionError = e;
    }
  }

  @Override
  public void stop() {
    this.sqlClient = null;
    if (Objects.nonNull(healthCheckRegistry)) {
      try {
        healthCheckRegistry.unregister(MetricRegistry.name(poolName, "pool", "ConnectivityCheck"));
      } catch (Throwable e) {
        // ignore
      }
    }
    if (Objects.nonNull(session)) {
      try {
        session.close();
      } catch (Throwable e) {
        // ignore
      }
    }
    if (Objects.nonNull(dataSource)) {
      try {
        dataSource.close();
      } catch (Throwable e) {
        // ignore
      }
    }
  }

  @Override
  public String getProviderId() {
    return providerId;
  }

  @Override
  public boolean isConnected() {
    return Objects.nonNull(sqlClient);
  }

  @Override
  public Optional<Throwable> getConnectionError() {
    return Optional.ofNullable(connectionError);
  }

  @Override
  public boolean isSameDataset(ConnectionInfo connectionInfo) {
    return Objects.equals(
        connectionInfo.getDatasetIdentifier(), this.connectionInfo.getDatasetIdentifier());
  }

  @Override
  public String getDatasetIdentifier() {
    return this.connectionInfo.getDatasetIdentifier();
  }

  @Override
  public Tuple<Boolean, String> canBeSharedWith(
      ConnectionInfo connectionInfo, boolean checkAllParameters) {
    if (!(connectionInfo instanceof ConnectionInfoSql)) {
      return Tuple.of(false, "provider types do not match");
    }

    if (!this.connectionInfo.isShared() || !connectionInfo.isShared()) {
      return Tuple.of(false, "");
    }

    ConnectionInfoSql connectionInfoSql = (ConnectionInfoSql) connectionInfo;

    Builder<String, Boolean> matches =
        new Builder<String, Boolean>()
            .put("host", Objects.equals(this.connectionInfo.getHost(), connectionInfoSql.getHost()))
            .put(
                "database",
                Objects.equals(this.connectionInfo.getDatabase(), connectionInfoSql.getDatabase()))
            .put(
                "user", Objects.equals(this.connectionInfo.getUser(), connectionInfoSql.getUser()));

    if (checkAllParameters) {
      matches
          .put(
              "password",
              Objects.equals(this.connectionInfo.getPassword(), connectionInfoSql.getPassword()))
          .put(
              "minConnections",
              Objects.equals(
                  this.connectionInfo.getPool().getMinConnections(),
                  connectionInfoSql.getPool().getMinConnections()))
          .put(
              "maxConnections",
              Objects.equals(
                  this.connectionInfo.getPool().getMaxConnections(),
                  connectionInfoSql.getPool().getMaxConnections()))
          .put(
              "dialect",
              Objects.equals(this.connectionInfo.getDialect(), connectionInfoSql.getDialect()))
          .put(
              "schemas",
              Objects.equals(this.connectionInfo.getSchemas(), connectionInfoSql.getSchemas()))
          .put(
              "initFailFast",
              Objects.equals(
                  this.connectionInfo.getPool().getInitFailFast(),
                  connectionInfoSql.getPool().getInitFailFast()))
          .put(
              "idleTimeout",
              Objects.equals(
                  this.connectionInfo.getPool().getIdleTimeout(),
                  connectionInfoSql.getPool().getIdleTimeout()))
          .put(
              "driverOptions",
              Objects.equals(
                  this.connectionInfo.getDriverOptions(), connectionInfoSql.getDriverOptions()));
    }

    List<String> nonMatching =
        matches.build().entrySet().stream()
            .filter(entry -> !entry.getValue())
            .map(Entry::getKey)
            .collect(Collectors.toList());

    if (!nonMatching.isEmpty()) {
      return Tuple.of(
          false, String.format("parameters do not match [%s]", String.join(",", nonMatching)));
    }

    return Tuple.of(true, null);
  }

  @Override
  public SqlClient getSqlClient() {
    return sqlClient;
  }

  @Override
  public Optional<AtomicInteger> getRefCounter() {
    if (connectionInfo.isShared()) {
      return Optional.of(refCounter);
    }
    return Optional.empty();
  }

  private HikariConfig createHikariConfig() {
    HikariConfig config = new HikariConfig();

    config.setUsername(connectionInfo.getUser().orElse(""));
    config.setPassword(getPassword(connectionInfo));
    config.setMaximumPoolSize(maxConnections);
    config.setMinimumIdle(minConnections);
    config.setInitializationFailTimeout(getInitFailTimeout(connectionInfo));
    config.setIdleTimeout(parseMs(connectionInfo.getPool().getIdleTimeout()));
    config.setPoolName(poolName);
    config.setKeepaliveTime(300000);

    config.setDataSource(sqlDataSourceFactory.create(providerId, connectionInfo));
    sqlDataSourceFactory.getInitSql(connectionInfo).ifPresent(config::setConnectionInitSql);

    config.setMetricRegistry(metricRegistry);
    config.setHealthCheckRegistry(healthCheckRegistry);

    return config;
  }

  private Database createSession(HikariDataSource dataSource) {
    int maxIdleTime = minConnections == maxConnections ? 0 : 600;
    DatabaseType healthCheck =
        connectionInfo.getDialect() == Dialect.GPKG ? DatabaseType.SQLITE : DatabaseType.POSTGRES;
    int idleTimeBeforeHealthCheck = 60;
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace(
          "rxjava2-jdbc - maxIdleTime: {}, healthCheck: {},  idleTimeBeforeHealthCheck: {}",
          maxIdleTime,
          healthCheck,
          idleTimeBeforeHealthCheck);
    }

    return Database.nonBlocking()
        .connectionProvider(dataSource)
        .maxPoolSize(maxConnections)
        .maxIdleTime(
            maxIdleTime,
            TimeUnit.SECONDS) // TODO: workaround for bug in rxjava2-jdbc, remove when fixed
        .healthCheck(healthCheck)
        .idleTimeBeforeHealthCheck(idleTimeBeforeHealthCheck, TimeUnit.SECONDS)
        .build();
  }

  private static long getInitFailTimeout(ConnectionInfoSql connectionInfo) {
    if (!connectionInfo.getPool().getInitFailFast()) {
      return -1;
    }
    return parseMs(connectionInfo.getPool().getInitFailTimeout());
  }

  private static long parseMs(String duration) {
    try {
      return Long.parseLong(duration) * 1000;
    } catch (Throwable e) {
      // ignore
    }
    return Duration.parse("PT" + duration).toMillis();
  }

  private static String getPassword(ConnectionInfoSql connectionInfo) {
    String password = connectionInfo.getPassword().orElse("");
    try {
      password = new String(Base64.getDecoder().decode(password), Charsets.UTF_8);
    } catch (IllegalArgumentException e) {
      // ignore if not valid base64
    }

    return password;
  }
}
