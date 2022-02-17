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
import com.google.common.collect.ImmutableMap.Builder;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.features.sql.app.FeatureProviderSql;
import de.ii.xtraplatform.features.sql.domain.SqlClient;
import de.ii.xtraplatform.web.domain.Dropwizard;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.features.sql.domain.FeatureProviderSqlData;
import de.ii.xtraplatform.features.sql.domain.SqlConnector;
import de.ii.xtraplatform.features.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.features.domain.AbstractFeatureProvider;
import de.ii.xtraplatform.features.domain.ConnectionInfo;
import de.ii.xtraplatform.features.domain.FeatureStorePathParser;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.features.domain.Tuple;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
/*@Component
@Provides(properties = {
    @StaticServiceProperty(name = FeatureProvider2.PROVIDER_TYPE_KEY, type = "java.lang.String", value = FeatureProviderSql.PROVIDER_TYPE),
    @StaticServiceProperty(name = FeatureProviderConnector.CONNECTOR_TYPE_KEY, type = "java.lang.String", value = SqlConnectorSlick.CONNECTOR_TYPE)
})*/
public class SqlConnectorSlick implements SqlConnector {

  public static final String CONNECTOR_TYPE = "SLICK";
  //TODO
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

  private Closeable session;
  private SqlClient sqlClient;
  private Throwable connectionError;

  @AssistedInject
  public SqlConnectorSlick(AppContext appContext, Dropwizard dropwizard, @Assisted FeatureProviderSqlData data) {
    this.connectionInfo = (ConnectionInfoSql) data.getConnectionInfo();
    this.poolName = String.format("db.%s", data.getId());
    this.metricRegistry = dropwizard.getEnvironment().metrics();
    this.healthCheckRegistry = dropwizard.getEnvironment().healthChecks();

    int maxQueries = getMaxQueries(data);
    if (connectionInfo.getPool().getMaxConnections() > 0) {
      this.maxConnections = connectionInfo.getPool().getMaxConnections();
    } else {
      this.maxConnections = maxQueries * Runtime.getRuntime()
          .availableProcessors();
    }
    if (connectionInfo.getPool().getMinConnections() >= 0) {
      this.minConnections = connectionInfo.getPool().getMinConnections();
    } else {
      this.minConnections = maxConnections;
    }
    int capacity = maxConnections / maxQueries;
    //TODO
    this.queueSize = Math.max(1024, maxConnections * capacity * 2);

    this.dataDir = appContext.getDataDir();
    //LOGGER.debug("QUEUE {} {} {} {} {}", connectionInfo.getDatabase(), maxQueries, maxConnections, capacity, maxConnections * capacity * 2);

    this.applicationName = String.format("%s %s - %s", appContext.getName(), appContext.getVersion(), data.getId());
    this.providerId = data.getId();
  }

  @Override
  public String getType() {
    return String.format("%s/%s", FeatureProviderSql.PROVIDER_TYPE, CONNECTOR_TYPE);
  }

  //TODO: better way to get maxQueries
  private int getMaxQueries(FeatureProviderSqlData data) {
    FeatureStorePathParser pathParser = FeatureProviderSql
        .createPathParser(data.getSourcePathDefaults(), null);
    Map<String, FeatureStoreTypeInfo> typeInfos = AbstractFeatureProvider
        .createTypeInfos(pathParser, data.getTypes());
    int maxQueries = 0;

    for (FeatureStoreTypeInfo typeInfo : typeInfos.values()) {
      int numberOfQueries = typeInfo.getInstanceContainers()
          .get(0)
          .getAllAttributesContainers()
          .size();

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

  //TODO
  private void onStart() {
    /*try {
      // bundle class loader has to be passed to Slick for initialization
      Thread.currentThread()
          .setContextClassLoader(classLoader);
      Config slickConfig = createSlickConfig(connectionInfo, poolName, maxConnections,
          minConnections, queueSize, healthCheckRegistry, dataDir, applicationName);
      DatabaseConfig<JdbcProfile> databaseConfig = DatabaseConfig$.MODULE$
          .forConfig("", slickConfig, classLoader, ClassTag$.MODULE$.apply(JdbcProfile.class));

      this.session = SlickSession.forConfig(databaseConfig);
      this.sqlClient = new SqlClientSlick(session, connectionInfo.getDialect());

      JdbcDataSource source = session.db()
          .source();
      if (source instanceof HikariCPJdbcDataSource) {
        ((HikariCPJdbcDataSource) source).ds().setMetricRegistry(metricRegistry);
        //TODO: not allowed
        //((HikariCPJdbcDataSource)source).ds().setHealthCheckRegistry(healthCheckRegistry);
      }

      this.controller = true;

    } catch (Throwable e) {
      //TODO: handle properly, service start should fail with error message, show in manager
      //LOGGER.error("CONNECTING TO DB FAILED", e);
      this.connectionError = e;

      this.controller = true;
    }*/
  }

  //TODO
  private void onStop() {
    this.sqlClient = null;
    if (Objects.nonNull(session)) {
      try {
        session.close();
      } catch (IOException e) {
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
  public Tuple<Boolean, String> canBeSharedWith(ConnectionInfo connectionInfo, boolean checkAllParameters) {
    if (!(connectionInfo instanceof ConnectionInfoSql)) {
      return Tuple.of(false, "provider types do not match");
    }

    if (!this.connectionInfo.isShared() || !connectionInfo.isShared()) {
      return Tuple.of(false, "");
    }

    ConnectionInfoSql connectionInfoSql = (ConnectionInfoSql) connectionInfo;

    Builder<String, Boolean> matches = new Builder<String, Boolean>()
        .put("host", Objects.equals(this.connectionInfo.getHost(), connectionInfoSql.getHost()))
        .put("database", Objects.equals(this.connectionInfo.getDatabase(), connectionInfoSql.getDatabase()))
        .put("user", Objects.equals(this.connectionInfo.getUser(), connectionInfoSql.getUser()));

    if (checkAllParameters) {
      matches
          .put("password", Objects.equals(this.connectionInfo.getPassword(), connectionInfoSql.getPassword()))
          .put("minConnections", Objects.equals(this.connectionInfo.getPool().getMinConnections(), connectionInfoSql.getPool().getMinConnections()))
          .put("maxConnections", Objects.equals(this.connectionInfo.getPool().getMaxConnections(), connectionInfoSql.getPool().getMaxConnections()))
          .put("dialect", Objects.equals(this.connectionInfo.getDialect(), connectionInfoSql.getDialect()))
          .put("schema", Objects.equals(this.connectionInfo.getSchemas(), connectionInfoSql.getSchemas()))
          .put("initFailFast", Objects.equals(this.connectionInfo.getPool().getInitFailFast(), connectionInfoSql.getPool().getInitFailFast()))
          .put("idleTimeout", Objects.equals(this.connectionInfo.getPool().getIdleTimeout(), connectionInfoSql.getPool().getIdleTimeout()))
          .put("driverOptions", Objects.equals(this.connectionInfo.getDriverOptions(), connectionInfoSql.getDriverOptions()));
    }

    List<String> nonMatching = matches.build().entrySet().stream()
        .filter(entry -> !entry.getValue())
        .map(Entry::getKey)
        .collect(Collectors.toList());

    if (!nonMatching.isEmpty()) {
      return Tuple.of(false,
          String.format("parameters do not match [%s]", String.join(",", nonMatching)));
    }

    return Tuple.of(true, null);
  }

  @Override
  public SqlClient getSqlClient() {
    return sqlClient;
  }

  //TODO: to SlickConfig.create
  /*private static Config createSlickConfig(ConnectionInfoSql connectionInfo, String poolName,
      int maxConnections, int minConnections, int queueSize,
      HealthCheckRegistry healthCheckRegistry, Path dataDir, String applicationName) {
    ImmutableMap.Builder<String, Object> databaseConfig = ImmutableMap.<String, Object>builder()
        .put("user", connectionInfo.getUser().orElse(""))
        .put("password", getPassword(connectionInfo))
        .put("dataSourceClass", getDataSourceClass(connectionInfo))
        .put("numThreads", maxConnections)
        .put("minimumIdle", minConnections)
        .put("queueSize", queueSize)
        .put("initializationFailFast", connectionInfo.getPool().getInitFailFast())
        .put("idleTimeout", connectionInfo.getPool().getIdleTimeout())
        .put("poolName", poolName);

    getInitSql(connectionInfo)
        .ifPresent(initSql -> databaseConfig.put("connectionInitSql", initSql));
    databaseConfig.putAll(getDriverOptions(connectionInfo, dataDir, applicationName));

    return ConfigFactory.parseMap(ImmutableMap.<String, Object>builder()
        .put("profile", getProfile(connectionInfo))
        .put("db", databaseConfig.build())
        .build());
  }

  private static String getPassword(ConnectionInfoSql connectionInfo) {
    String password = connectionInfo.getPassword().orElse("");
    try {
      password = new String(Base64.getDecoder()
          .decode(password), Charsets.UTF_8);
    } catch (IllegalArgumentException e) {
      //ignore if not valid base64
    }

    return password;
  }

  private static String getProfile(ConnectionInfoSql connectionInfo) {

    switch (connectionInfo.getDialect()) {
      case PGIS:
        return "slick.jdbc.PostgresProfile$";
      case GPKG:
        return "slick.jdbc.SQLiteProfile$";
    }

    throw new IllegalStateException("SQL dialect not implemented: " + connectionInfo.getDialect());
  }

  private static String getDataSourceClass(ConnectionInfoSql connectionInfo) {

    switch (connectionInfo.getDialect()) {
      case PGIS:
        return "org.postgresql.ds.PGSimpleDataSource";
      case GPKG:
        return "de.ii.xtraplatform.feature.provider.sql.infra.db.SpatialiteDataSource";
      //return "org.sqlite.SQLiteDataSource";
    }

    throw new IllegalStateException("SQL dialect not implemented: " + connectionInfo.getDialect());
  }

  private static Optional<String> getInitSql(ConnectionInfoSql connectionInfo) {

    switch (connectionInfo.getDialect()) {
      case PGIS:
        String initSql = "SET datestyle TO ISO, MDY; SET client_encoding TO UTF8; SET timezone TO UTC;";
        if (!connectionInfo.getSchemas().isEmpty()) {
          String schemas = connectionInfo.getSchemas()
              .stream()
              .map(schema -> {
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
        return Optional
            .of("SELECT CASE CheckGeoPackageMetaData() WHEN 1 THEN EnableGpkgMode() END;");
    }

    return Optional.empty();
  }

  private static Map<String, Object> getDriverOptions(ConnectionInfoSql connectionInfo,
      Path dataDir, String applicationName) {
    switch (connectionInfo.getDialect()) {
      case PGIS:
        return Stream.concat(
            ImmutableMap.of(
                "properties.serverName", connectionInfo.getHost().orElseThrow(
                    () -> new IllegalArgumentException(
                        "No 'host' given, required for PGIS connection")),
                "properties.databaseName", Optional.ofNullable(Strings.emptyToNull(connectionInfo.getDatabase())).orElseThrow(() -> new IllegalArgumentException(
                    "No 'database' given, required for PGIS connection")),
                "properties.assumeMinServerVersion", "9.6",
                "properties.ApplicationName", applicationName
            ).entrySet().stream(),
            connectionInfo.getDriverOptions()
                .entrySet()
                .stream()
                .filter(option -> ImmutableList
                    .of("gssEncMode", "ssl", "sslmode", "sslcert", "sslkey", "sslrootcert",
                        "sslpassword")
                    .contains(option.getKey()))
                .map(option -> new SimpleImmutableEntry<>("properties." + option.getKey(),
                    option.getValue()))
        ).collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
      case GPKG:
        Path path = Paths.get(connectionInfo.getDatabase()).isAbsolute()
            ? Paths.get(connectionInfo.getDatabase())
            : dataDir.resolve(connectionInfo.getDatabase());
        if (!path.toFile().exists()) {
          throw new IllegalArgumentException("GPKG database does not exist: " + path);
        }
        return ImmutableMap.of(
            "properties.loadExtension", true,
            "properties.url", String.format("jdbc:sqlite:%s", path)
        );
    }

    return ImmutableMap.of();
  }*/
}
