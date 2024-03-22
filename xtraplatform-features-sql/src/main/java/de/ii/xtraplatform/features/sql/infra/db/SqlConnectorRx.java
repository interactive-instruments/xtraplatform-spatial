/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.infra.db;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap.Builder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import de.ii.xtraplatform.base.domain.resiliency.AbstractVolatilePolling;
import de.ii.xtraplatform.base.domain.resiliency.Volatile2.Polling;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry.ChangeHandler;
import de.ii.xtraplatform.base.domain.resiliency.VolatileUnavailableException;
import de.ii.xtraplatform.features.domain.ConnectionInfo;
import de.ii.xtraplatform.features.domain.Tuple;
import de.ii.xtraplatform.features.sql.app.FeatureProviderSql;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql.Dialect;
import de.ii.xtraplatform.features.sql.domain.SqlClient;
import de.ii.xtraplatform.features.sql.domain.SqlConnector;
import de.ii.xtraplatform.features.sql.domain.SqlQueryBatch;
import de.ii.xtraplatform.features.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.features.sql.domain.SqlRow;
import de.ii.xtraplatform.streams.domain.Reactive.Source;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
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
import org.slf4j.MDC;

/**
 * @author zahnen
 */
public class SqlConnectorRx extends AbstractVolatilePolling implements SqlConnector, Polling {

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
  private final boolean asyncStartup;

  private Database session;
  private HikariDataSource dataSource;
  private SqlClient sqlClient;
  private Throwable connectionError;
  private Connection pollConnection;

  @AssistedInject
  public SqlConnectorRx(
      SqlDataSourceFactory sqlDataSourceFactory,
      AppContext appContext,
      VolatileRegistry volatileRegistry,
      @Assisted MetricRegistry metricRegistry,
      @Assisted HealthCheckRegistry healthCheckRegistry,
      @Assisted String providerId,
      @Assisted ConnectionInfoSql connectionInfo) {
    super(volatileRegistry);
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
    this.asyncStartup = appContext.getConfiguration().getModules().isStartupAsync();

    /*RxJavaPlugins.setErrorHandler(
    e -> {
      if (e instanceof UndeliverableException) {
        LogContext.error(LOGGER, e.getCause(), "RXUD");
      } else {
        LogContext.error(LOGGER, e, "RX");
      }
    });*/
  }

  @Override
  public String getType() {
    return String.format("%s/%s", FeatureProviderSql.PROVIDER_SUB_TYPE, CONNECTOR_TYPE);
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
      this.connectionError = e;
      setMessage(e.getMessage());
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
    if (Objects.nonNull(pollConnection)) {
      try {
        pollConnection.close();
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
              "initFailTimeout",
              Objects.equals(
                  this.connectionInfo.getPool().getInitFailTimeout(),
                  connectionInfoSql.getPool().getInitFailTimeout()))
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
    config.setMinimumIdle(asyncStartup ? 0 : minConnections);
    config.setInitializationFailTimeout(asyncStartup ? -1 : getInitFailTimeout(connectionInfo));
    config.setIdleTimeout(parseMs(connectionInfo.getPool().getIdleTimeout()));
    config.setPoolName(poolName);
    config.setKeepaliveTime(300000);

    config.setDataSource(sqlDataSourceFactory.create(providerId, connectionInfo));
    sqlDataSourceFactory.getInitSql(connectionInfo).ifPresent(config::setConnectionInitSql);

    config.setMetricRegistry(metricRegistry);
    // config.setHealthCheckRegistry(healthCheckRegistry);

    if (asyncStartup) {
      config.setConnectionTimeout(5000);
    }

    return config;
  }

  private Database createSession(HikariDataSource dataSource) {
    int maxIdleTime = 5; // minConnections == maxConnections ? 0 : 600;
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
    return parseMs(Objects.requireNonNullElse(connectionInfo.getPool().getInitFailTimeout(), "1"));
  }

  private static long parseMs(String duration) {
    try {
      return Long.parseLong(duration) * 1000;
    } catch (Throwable e) {
      // ignore
    }
    return Duration.parse("PT" + duration).toMillis();
  }

  public static String getPassword(ConnectionInfoSql connectionInfo) {
    String password = connectionInfo.getPassword().orElse("");
    try {
      password = new String(Base64.getDecoder().decode(password), Charsets.UTF_8);
    } catch (IllegalArgumentException e) {
      // ignore if not valid base64
    }

    return password;
  }

  @Override
  public int getIntervalMs() {
    return 1000;
  }

  @Override
  public Optional<String> getInstanceId() {
    return Optional.of(providerId);
  }

  public static ChangeHandler withMdc(ChangeHandler consumer) {
    Map<String, String> mdc = MDC.getCopyOfContextMap();

    if (Objects.nonNull(mdc)) {
      return (u, v) -> {
        MDC.setContextMap(mdc);
        consumer.change(u, v);
      };
    }
    return consumer;
  }

  @Override
  protected synchronized void onVolatileStart() {
    super.onVolatileStart();

    if (asyncStartup) {
      if (getState() == State.UNAVAILABLE) {
        LOGGER.warn("Could not establish connection to database: {}", getDatasetIdentifier());
      }

      onStateChange(
          withMdc(
              (from, to) -> {
                if (to == State.AVAILABLE) {
                  LOGGER.info("Re-established connection to database: {}", getDatasetIdentifier());
                } else if (to == State.UNAVAILABLE) {
                  LOGGER.warn("Lost connection to database: {}", getDatasetIdentifier());
                }
              }),
          false);
    }
  }

  @Override
  public Source<SqlRow> getSourceStream(SqlQueryBatch queryBatch, SqlQueryOptions options) {
    if (!isAvailable()) {
      throw new VolatileUnavailableException("Connector is not available");
    }
    return SqlConnector.super.getSourceStream(queryBatch, options);
  }

  @Override
  public de.ii.xtraplatform.base.domain.util.Tuple<State, String> check() {
    if (Objects.isNull(sqlClient)) {
      // TODO: retry
      if (Objects.nonNull(connectionError)) {
        return de.ii.xtraplatform.base.domain.util.Tuple.of(
            State.LIMITED, connectionError.getMessage());
      }
      return de.ii.xtraplatform.base.domain.util.Tuple.of(State.UNAVAILABLE, null);
    }

    try {
      if (Objects.nonNull(pollConnection) && !pollConnection.isValid(1)) {
        try {
          pollConnection.close();
        } catch (Throwable e) {
          // ignore
        }
        this.pollConnection = null;
      }
      if (Objects.isNull(pollConnection)) {
        this.pollConnection = dataSource.getConnection();
        // boolean valid = pollConnection.isValid(1);
        // sqlClient.getSqlDialect().getDbInfo(connection);
      }
    } catch (SQLException e) {
      if (LOGGER.isDebugEnabled(MARKER.DI)) {
        LOGGER.debug(
            "SQL {} {} {} {}", e.getClass(), e.getSQLState(), e.getErrorCode(), e.getMessage());
      }
      if (isUnavailable(e)) {
        dataSource.getHikariConfigMXBean().setMinimumIdle(0);
        dataSource.getHikariPoolMXBean().softEvictConnections();

        return de.ii.xtraplatform.base.domain.util.Tuple.of(State.UNAVAILABLE, e.getMessage());
      }

      return de.ii.xtraplatform.base.domain.util.Tuple.of(State.LIMITED, e.getMessage());
    } catch (Throwable e) {
      if (LOGGER.isDebugEnabled(MARKER.DI)) {
        LOGGER.debug("SQL OTHER {}", e.getMessage());
      }
    }

    dataSource.getHikariConfigMXBean().setMinimumIdle(minConnections);

    return de.ii.xtraplatform.base.domain.util.Tuple.of(State.AVAILABLE, null);
  }

  @Override
  public Optional<HealthCheck> asHealthCheck() {
    return Optional.empty();
  }

  // TODO: SQLite
  private static boolean isUnavailable(SQLException e) {
    // see https://www.postgresql.org/docs/current/errcodes-appendix.html
    return Objects.isNull(e.getSQLState()) // no connection
        || e.getSQLState().startsWith("08") // Connection Exception
        || e.getSQLState().startsWith("53") // Insufficient Resources
        || e.getSQLState().startsWith("57") // Operator Intervention
        || e.getSQLState().startsWith("58") // System Error
        || e.getSQLState().startsWith("XX"); // Internal Error
  }
}
