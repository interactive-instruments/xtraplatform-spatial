/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.infra.db;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import java.time.Duration;
import org.davidmoten.rx.jdbc.ConnectionProvider;
import org.davidmoten.rx.jdbc.Database;;
import org.davidmoten.rx.jdbc.exceptions.SQLRuntimeException;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap.Builder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.features.domain.AbstractFeatureProvider;
import de.ii.xtraplatform.features.domain.ConnectionInfo;
import de.ii.xtraplatform.features.domain.FeatureStorePathParser;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.features.domain.Tuple;
import de.ii.xtraplatform.features.sql.app.FeatureProviderSql;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.features.sql.domain.FeatureProviderSqlData;
import de.ii.xtraplatform.features.sql.domain.SqlClient;
import de.ii.xtraplatform.features.sql.domain.SqlConnector;
import de.ii.xtraplatform.features.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.web.domain.Dropwizard;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author zahnen */
public class SqlConnectorSlick implements SqlConnector {

  public static final String CONNECTOR_TYPE = "SLICK";
  private static final SqlQueryOptions NO_OPTIONS = SqlQueryOptions.withColumnTypes(String.class);
  private static final Logger LOGGER = LoggerFactory.getLogger(SqlConnectorSlick.class);

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

  private Database session;
  private HikariDataSource dataSource;
  private SqlClient sqlClient;
  private Throwable connectionError;

  @AssistedInject
  public SqlConnectorSlick(
      AppContext appContext, Dropwizard dropwizard, @Assisted FeatureProviderSqlData data) {
    this.connectionInfo = (ConnectionInfoSql) data.getConnectionInfo();
    this.poolName = String.format("db.%s", data.getId());
    this.metricRegistry = dropwizard.getEnvironment().metrics();
    this.healthCheckRegistry = dropwizard.getEnvironment().healthChecks();

    int maxQueries = getMaxQueries(data);
    if (connectionInfo.getPool().getMaxConnections() > 0) {
      this.maxConnections = connectionInfo.getPool().getMaxConnections();
    } else {
      this.maxConnections = maxQueries * Runtime.getRuntime().availableProcessors();
    }
    if (connectionInfo.getPool().getMinConnections() >= 0) {
      this.minConnections = connectionInfo.getPool().getMinConnections();
    } else {
      this.minConnections = maxConnections;
    }
    int capacity = maxConnections / maxQueries;
    // TODO
    this.queueSize = Math.max(1024, maxConnections * capacity * 2);

    this.dataDir = appContext.getDataDir();
    // LOGGER.debug("QUEUE {} {} {} {} {}", connectionInfo.getDatabase(), maxQueries,
    // maxConnections, capacity, maxConnections * capacity * 2);

    this.applicationName =
        String.format("%s %s - %s", appContext.getName(), appContext.getVersion(), data.getId());
    this.providerId = data.getId();
  }

  @Override
  public String getType() {
    return String.format("%s/%s", FeatureProviderSql.PROVIDER_TYPE, CONNECTOR_TYPE);
  }

  // TODO: better way to get maxQueries
  private int getMaxQueries(FeatureProviderSqlData data) {
    FeatureStorePathParser pathParser =
        FeatureProviderSql.createPathParser(data.getSourcePathDefaults(), null);
    Map<String, FeatureStoreTypeInfo> typeInfos =
        AbstractFeatureProvider.createTypeInfos(pathParser, data.getTypes());
    int maxQueries = 0;

    for (FeatureStoreTypeInfo typeInfo : typeInfos.values()) {
      int numberOfQueries =
          typeInfo.getInstanceContainers().get(0).getAllAttributesContainers().size();

      if (numberOfQueries > maxQueries) {
        maxQueries = numberOfQueries;
      }
    }
    return maxQueries <= 0 ? 1 : maxQueries;
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
  public void start() {
    try {
      HikariConfig hikariConfig =
          createHikariConfig(
              connectionInfo, poolName, maxConnections, minConnections, dataDir, applicationName);
      hikariConfig.setMetricRegistry(metricRegistry);
      hikariConfig.setHealthCheckRegistry(healthCheckRegistry);

      this.dataSource = new HikariDataSource(hikariConfig);
      this.session = Database.fromBlocking(new ConnectionProviderHikari(dataSource));
      this.sqlClient = new SqlClientSlick(session, connectionInfo.getDialect());

    } catch (Throwable e) {
      // TODO: handle properly, service start should fail with error message, show in manager
      // LOGGER.error("CONNECTING TO DB FAILED", e);
      this.connectionError = e;
    }
  }

  @Override
  public void stop() {
    this.sqlClient = null;
    if (Objects.nonNull(session)) {
      try {
        session.close();
      } catch (Throwable e) {
        // ignore
      }
      healthCheckRegistry.unregister(MetricRegistry.name(dataSource.getPoolName(), "pool",
          "ConnectivityCheck"));
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
              "schema",
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

  private static class ConnectionProviderHikari implements ConnectionProvider {
    private final HikariDataSource pool;
    private final AtomicBoolean isOpen;

    ConnectionProviderHikari(HikariDataSource pool) {
      this.isOpen = new AtomicBoolean(true);
      this.pool = pool;
    }

    @Override
    public Connection get() {
      try {
        return this.pool.getConnection();
      } catch (SQLException var2) {
        throw new SQLRuntimeException(var2);
      }
    }

    @Override
    public void close() {
      if (this.isOpen.getAndSet(false)) {
        this.pool.close();
      }
    }
  }

  private static HikariConfig createHikariConfig(
      ConnectionInfoSql connectionInfo,
      String poolName,
      int maxConnections,
      int minConnections,
      Path dataDir,
      String applicationName) {
    HikariConfig config = new HikariConfig();

    config.setUsername(connectionInfo.getUser().orElse(""));
    config.setPassword(getPassword(connectionInfo));
    config.setDataSourceClassName(getDataSourceClass(connectionInfo));
    config.setMaximumPoolSize(maxConnections);
    config.setMinimumIdle(minConnections);
    config.setInitializationFailTimeout(connectionInfo.getPool().getInitFailFast() ? 1 : -1);
    config.setIdleTimeout(Duration.parse("PT" + connectionInfo.getPool().getIdleTimeout()).toMillis());
    config.setPoolName(poolName);

    getInitSql(connectionInfo).ifPresent(config::setConnectionInitSql);

    config.setDataSourceProperties(getDriverOptions(connectionInfo, dataDir, applicationName));

    return config;
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

  //TODO: instantiate dataSource, apply driverOptions
  private static String getDataSourceClass(ConnectionInfoSql connectionInfo) {

    switch (connectionInfo.getDialect()) {
      case PGIS:
        return "org.postgresql.ds.PGSimpleDataSource";
      case GPKG:
        return "de.ii.xtraplatform.features.sql.infra.db.SpatialiteDataSource";
        // return "org.sqlite.SQLiteDataSource";
    }

    throw new IllegalStateException("SQL dialect not implemented: " + connectionInfo.getDialect());
  }

  private static Optional<String> getInitSql(ConnectionInfoSql connectionInfo) {

    switch (connectionInfo.getDialect()) {
      case PGIS:
        String initSql =
            "SET datestyle TO ISO, MDY; SET client_encoding TO UTF8; SET timezone TO UTC;";
        if (!connectionInfo.getSchemas().isEmpty()) {
          String schemas =
              connectionInfo.getSchemas().stream()
                  .map(
                      schema -> {
                        if (!Objects.equals(schema, schema.toLowerCase())) {
                          return String.format("\"%s\"", schema);
                        }
                        return schema;
                      })
                  .collect(Collectors.joining(","));

          return Optional.of(String.format("SET search_path TO %s,public; %s", schemas, initSql));
        }
        return Optional.of(initSql);
      case GPKG:
        return Optional.of(
            "SELECT CASE CheckGeoPackageMetaData() WHEN 1 THEN EnableGpkgMode() END;");
    }

    return Optional.empty();
  }

  private static Properties getDriverOptions(
      ConnectionInfoSql connectionInfo, Path dataDir, String applicationName) {
    Properties properties = new Properties();

    switch (connectionInfo.getDialect()) {
      case PGIS:
        properties.setProperty(
            "serverName",
            connectionInfo
                .getHost()
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "No 'host' given, required for PGIS connection")));
        properties.setProperty(
            "databaseName",
            Optional.ofNullable(Strings.emptyToNull(connectionInfo.getDatabase()))
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "No 'database' given, required for PGIS connection")));
        properties.setProperty("assumeMinServerVersion", "9.6");
        properties.setProperty("ApplicationName", applicationName);

        connectionInfo.getDriverOptions().entrySet().stream()
            .filter(
                option ->
                    ImmutableList.of(
                            "gssEncMode",
                            "ssl",
                            "sslmode",
                            "sslcert",
                            "sslkey",
                            "sslrootcert",
                            "sslpassword")
                        .contains(option.getKey()))
            .forEach(
                option ->
                    properties.setProperty(option.getKey(), String.valueOf(option.getValue())));
        break;
      case GPKG:
        Path path =
            Paths.get(connectionInfo.getDatabase()).isAbsolute()
                ? Paths.get(connectionInfo.getDatabase())
                : dataDir.resolve(connectionInfo.getDatabase());
        if (!path.toFile().exists()) {
          throw new IllegalArgumentException("GPKG database does not exist: " + path);
        }

        properties.setProperty("loadExtension", String.valueOf(true));
        properties.setProperty("url", String.format("jdbc:sqlite:%s", path));
        break;
    }

    return properties;
  }
}
