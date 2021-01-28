/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.infra.db;

import akka.stream.alpakka.slick.javadsl.SlickSession;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import de.ii.xtraplatform.dropwizard.domain.Dropwizard;
import de.ii.xtraplatform.feature.provider.sql.app.FeatureProviderSql;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlClient;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlConnector;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.features.domain.AbstractFeatureProvider;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureStorePathParser;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceController;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.reflect.ClassTag$;
import slick.basic.DatabaseConfig;
import slick.basic.DatabaseConfig$;
import slick.jdbc.JdbcDataSource;
import slick.jdbc.JdbcProfile;
import slick.jdbc.hikaricp.HikariCPJdbcDataSource;

import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author zahnen
 */
@Component
@Provides(properties = {
        @StaticServiceProperty(name = FeatureProvider2.PROVIDER_TYPE_KEY, type = "java.lang.String", value = FeatureProviderSql.PROVIDER_TYPE),
        @StaticServiceProperty(name = FeatureProviderConnector.CONNECTOR_TYPE_KEY, type = "java.lang.String", value = SqlConnectorSlick.CONNECTOR_TYPE)
})
public class SqlConnectorSlick implements SqlConnector {

    public static final String CONNECTOR_TYPE = "SLICK";
    //TODO
    private static final SqlQueryOptions NO_OPTIONS = SqlQueryOptions.withColumnTypes(String.class);
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlConnectorSlick.class);

    private final ClassLoader classLoader;
    private final ConnectionInfoSql connectionInfo;
    private final String poolName;
    private final MetricRegistry metricRegistry;
    private final HealthCheckRegistry healthCheckRegistry;
    private final int maxConnections;
    private final int minConnections;
    private final int queueSize;

    private SlickSession session;
    private SqlClient sqlClient;
    private Throwable connectionError;

    @ServiceController(value=false)
    private boolean controller;

    public SqlConnectorSlick(@Context BundleContext context, @Requires Dropwizard dropwizard,
                             @Property(name = ".data") FeatureProviderDataV2 data) {
        // bundle class loader has to be passed to Slick for initialization
        this.classLoader = context.getBundle()
                                  .adapt(BundleWiring.class)
                                  .getClassLoader();
        this.connectionInfo = (ConnectionInfoSql) data.getConnectionInfo();
        this.poolName = String.format("db.%s", data.getId());
        this.metricRegistry = dropwizard.getEnvironment().metrics();
        this.healthCheckRegistry = dropwizard.getEnvironment().healthChecks();

        int maxQueries = getMaxQueries(data);
        if (connectionInfo.getMaxConnections() > 0) {
            this.maxConnections = connectionInfo.getMaxConnections();
        } else {
            this.maxConnections = maxQueries * Runtime.getRuntime()
                                                      .availableProcessors();
        }
        if (connectionInfo.getMinConnections() >= 0) {
            this.minConnections = connectionInfo.getMinConnections();
        } else {
            this.minConnections = maxConnections;
        }
        int capacity = maxConnections / maxQueries;
        //TODO
        this.queueSize = Math.max(1024, maxConnections * capacity * 2);

        //LOGGER.debug("QUEUE {} {} {} {} {}", connectionInfo.getDatabase(), maxQueries, maxConnections, capacity, maxConnections * capacity * 2);
    }

    //TODO: better way to get maxQueries
    private int getMaxQueries(FeatureProviderDataV2 data) {
        FeatureStorePathParser pathParser = FeatureProviderSql.createPathParser((ConnectionInfoSql) data.getConnectionInfo(), null);
        Map<String, FeatureStoreTypeInfo> typeInfos = AbstractFeatureProvider.createTypeInfos(pathParser, data.getTypes());
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

    @Validate
    private void onStart() {
        try {
            // bundle class loader has to be passed to Slick for initialization
            Thread.currentThread()
                  .setContextClassLoader(classLoader);
            Config slickConfig = createSlickConfig(connectionInfo, poolName, maxConnections, minConnections, queueSize, healthCheckRegistry);
            DatabaseConfig<JdbcProfile> databaseConfig = DatabaseConfig$.MODULE$.forConfig("", slickConfig, classLoader, ClassTag$.MODULE$.apply(JdbcProfile.class));

            this.session = SlickSession.forConfig(databaseConfig);
            this.sqlClient = new SqlClientSlick(session);

            JdbcDataSource source = session.db()
                                           .source();
            if (source instanceof HikariCPJdbcDataSource) {
                ((HikariCPJdbcDataSource)source).ds().setMetricRegistry(metricRegistry);
                //TODO: not allowed
                //((HikariCPJdbcDataSource)source).ds().setHealthCheckRegistry(healthCheckRegistry);
            }

            this.controller = true;

        } catch (Throwable e) {
            //TODO: handle properly, service start should fail with error message, show in manager
            //LOGGER.error("CONNECTING TO DB FAILED", e);
            this.connectionError = e;

            this.controller = true;
        }
    }

    @Invalidate
    private void onStop() {
        this.sqlClient = null;
        if (Objects.nonNull(session)) {
            session.close();
        }
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
    public SqlClient getSqlClient() {
        return sqlClient;
    }

    //TODO: to SlickConfig.create
    private static Config createSlickConfig(ConnectionInfoSql connectionInfo, String poolName, int maxConnections, int minConnections, int queueSize, HealthCheckRegistry healthCheckRegistry) {
        ImmutableMap.Builder<String, Object> databaseConfig = ImmutableMap.<String, Object>builder()
                .put("user", connectionInfo.getUser())
                .put("password", getPassword(connectionInfo))
                .put("dataSourceClass", getDataSourceClass(connectionInfo))
                .put("properties.serverName", connectionInfo.getHost())
                .put("properties.databaseName", connectionInfo.getDatabase())
                .put("numThreads", maxConnections)
                .put("minimumIdle", minConnections)
                .put("queueSize", queueSize)
                .put("initializationFailFast", connectionInfo.getInitFailFast())
                .put("poolName", poolName);

        if (!connectionInfo.getSchemas().isEmpty()) {
            databaseConfig.put("connectionInitSql", String.format("SET search_path TO %s,public;", Joiner.on(',').join(connectionInfo.getSchemas())));
        }

        return ConfigFactory.parseMap(ImmutableMap.<String, Object>builder()
                .put("profile", getProfile(connectionInfo))
                .put("db", databaseConfig.build())
                .build());
    }

    private static String getPassword(ConnectionInfoSql connectionInfo) {
        String password = connectionInfo.getPassword();
        try {
            password = new String(Base64.getDecoder()
                                        .decode(password), Charsets.UTF_8);
        } catch (IllegalArgumentException e) {
            //ignore if not valid base64
        }

        return password;
    }

    private static String getProfile(ConnectionInfoSql connectionInfo) {
        String profile;

        switch (connectionInfo.getDialect()) {
            case PGIS:
            default:
                profile = "slick.jdbc.PostgresProfile$";
                break;
        }

        return profile;
    }

    private static String getDataSourceClass(ConnectionInfoSql connectionInfo) {
        String dataSourceClass;

        switch (connectionInfo.getDialect()) {
            case PGIS:
            default:
                dataSourceClass = "org.postgresql.ds.PGSimpleDataSource";
                break;
        }

        return dataSourceClass;
    }

}
