/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.infra.db;

import akka.stream.alpakka.slick.javadsl.SlickSession;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV1;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlClient;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlConnector;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueryOptions;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
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
import slick.jdbc.JdbcProfile;

import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

/**
 * @author zahnen
 */
@Component
@Provides(properties = {
        @StaticServiceProperty(name = "providerType", type = "java.lang.String", value = "SQL"),
        @StaticServiceProperty(name = "connectorType", type = "java.lang.String", value = SqlConnectorSlick.CONNECTOR_TYPE)
})
public class SqlConnectorSlick implements SqlConnector {

    public static final String CONNECTOR_TYPE = "SLICK";
    //TODO
    private static final SqlQueryOptions NO_OPTIONS = SqlQueryOptions.withColumnTypes(String.class);
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlConnectorSlick.class);

    private final ClassLoader classLoader;
    private final ConnectionInfoSql connectionInfo;

    private SlickSession session;
    private SqlClient sqlClient;
    private Throwable connectionError;

    @ServiceController(value=false)
    private boolean controller;

    public SqlConnectorSlick(@Context BundleContext context,
                             @Property(name = ".data") FeatureProviderDataV1 data) {
        // bundle class loader has to be passed to Slick for initialization
        this.classLoader = context.getBundle()
                                  .adapt(BundleWiring.class)
                                  .getClassLoader();
        this.connectionInfo = (ConnectionInfoSql) data.getConnectionInfo();
    }

    @Validate
    private void onStart() {
        try {
            // bundle class loader has to be passed to Slick for initialization
            Thread.currentThread()
                  .setContextClassLoader(classLoader);
            DatabaseConfig<JdbcProfile> databaseConfig = DatabaseConfig$.MODULE$.forConfig("", createSlickConfig(connectionInfo), classLoader, ClassTag$.MODULE$.apply(JdbcProfile.class));

            this.session = SlickSession.forConfig(databaseConfig);
            this.sqlClient = new SqlClientSlick(session);

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
        if (Objects.isNull(sqlClient)) {
            throw new IllegalStateException("not connected to database");
        }

        return sqlClient;
    }

    /*@Override
    public CompletionStage<Done> runQuery(FeatureQuery query, Sink<SqlRow, CompletionStage<Done>> consumer,
                                          Map<String, String> additionalQueryParameters) {
        return null;
    }

    @Override
    public Source<SqlRow, NotUsed> getSourceStream(String query) {
        return getSourceStream(query, NO_OPTIONS);
    }

    @Override
    public Source<SqlRow, NotUsed> getSourceStream(String query, SqlQueryOptions options) {
        return SlickSql.source(session, query, positionedResult -> new SqlRowSlick().read(positionedResult, options));


        //return Slick.source(session, query, slickRow -> options.isPlain() ? ModifiableSqlRowPlain.create(). : new SqlRowValues(slickRow, options.getAttributesContainer().get(), options.getContainerPriority()));
    }*/

    //TODO: to SlickConfig.create
    private static Config createSlickConfig(ConnectionInfoSql connectionInfo) {
        ImmutableMap.Builder<String, Object> databaseConfig = ImmutableMap.<String, Object>builder()
                .put("user", connectionInfo.getUser())
                .put("password", getPassword(connectionInfo))
                .put("dataSourceClass", getDataSourceClass(connectionInfo))
                .put("properties.serverName", connectionInfo.getHost())
                .put("properties.databaseName", connectionInfo.getDatabase())
                .put("numThreads", connectionInfo.getMaxThreads())
                .put("initializationFailFast", connectionInfo.getInitFailFast());

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
