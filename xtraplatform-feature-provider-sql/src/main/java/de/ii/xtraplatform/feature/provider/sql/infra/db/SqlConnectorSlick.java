/**
 * Copyright 2019 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.infra.db;

import akka.Done;
import akka.NotUsed;
import akka.stream.alpakka.slick.javadsl.Slick;
import akka.stream.alpakka.slick.javadsl.SlickSession;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlConnector;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRow;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRowValues;
import de.ii.xtraplatform.feature.transformer.api.FeatureProviderDataTransformer;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * @author zahnen
 */
@Component
@Provides(properties = {
        @StaticServiceProperty(name = "providerType", type = "java.lang.String", value = "PGIS"),
        @StaticServiceProperty(name = "connectorType", type = "java.lang.String", value = SqlConnectorSlick.CONNECTOR_TYPE)
})
public class SqlConnectorSlick implements SqlConnector {

    public static final String CONNECTOR_TYPE = "SLICK";
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlConnectorSlick.class);

    private final ClassLoader classLoader;
    private final ConnectionInfoSql connectionInfo;

    private SlickSession session;

    public SqlConnectorSlick(@Context BundleContext context,
                             @Property(name = ".data") FeatureProviderDataTransformer data) {
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

        } catch (Throwable e) {
            //TODO: handle properly, service start should fail with error message, show in manager
            LOGGER.error("CONNECTING TO DB FAILED", e);
        }
    }

    @Invalidate
    private void onStop() {
        if (Objects.nonNull(session)) {
            session.close();
        }
    }

    @Override
    public CompletionStage<Done> runQuery(FeatureQuery query, Sink<SqlRow, CompletionStage<Done>> consumer,
                                          Map<String, String> additionalQueryParameters) {
        return null;
    }

    @Override
    public Source<SqlRow, NotUsed> getSourceStream(String query, SqlQueryOptions options) {
        return Slick.source(session, query, slickRow -> new SqlRowValues(slickRow, options.getAttributesContainer(), options.getContainerPriority()));
    }

    //TODO: to SlickConfig.create
    private static Config createSlickConfig(ConnectionInfoSql connectionInfo) {
        return ConfigFactory.parseMap(ImmutableMap.<String, Object>builder()
                .put("profile", getProfile(connectionInfo))
                .put("db", ImmutableMap.<String, Object>builder()
                        .put("user", connectionInfo.getUser())
                        .put("password", getPassword(connectionInfo))
                        .put("dataSourceClass", getDataSourceClass(connectionInfo))
                        .put("properties.serverName", connectionInfo.getHost())
                        .put("properties.databaseName", connectionInfo.getDatabase())
                        .put("numThreads", connectionInfo.getMaxThreads())
                        .put("initializationFailFast", true)
                        .build())
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
