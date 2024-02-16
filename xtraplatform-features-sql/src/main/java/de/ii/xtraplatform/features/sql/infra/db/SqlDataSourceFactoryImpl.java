/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.infra.db;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.base.Strings;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.blobs.domain.ResourceStore;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.spatialite.domain.SpatiaLiteLoader;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConnection;
import org.sqlite.SQLiteDataSource;

@Singleton
@AutoBind
public class SqlDataSourceFactoryImpl implements SqlDataSourceFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(SqlDataSourceFactoryImpl.class);

  private final Path dataDir;
  private final ResourceStore featuresStore;
  private final String applicationName;
  private final SpatiaLiteLoader spatiaLiteLoader;

  private boolean spatiaLiteInitialized;

  @Inject
  public SqlDataSourceFactoryImpl(
      AppContext appContext, ResourceStore blobStore, SpatiaLiteLoader spatiaLiteLoader) {
    this.dataDir = appContext.getDataDir();
    this.featuresStore = blobStore.with("features");
    this.applicationName =
        String.format("%s %s - %%s", appContext.getName(), appContext.getVersion());
    this.spatiaLiteLoader = spatiaLiteLoader;
    this.spatiaLiteInitialized = false;
  }

  @Override
  public DataSource create(String providerId, ConnectionInfoSql connectionInfo) {
    switch (connectionInfo.getDialect()) {
      case PGIS:
        return createPgis(providerId, connectionInfo);
      case GPKG:
        return createGpkg(providerId, connectionInfo);
    }

    throw new IllegalStateException(
        String.format("Dialect not supported: %s", connectionInfo.getDialect()));
  }

  @Override
  public Optional<String> getInitSql(ConnectionInfoSql connectionInfo) {
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

  private DataSource createPgis(String providerId, ConnectionInfoSql connectionInfo) {
    PGSimpleDataSource ds = new PGSimpleDataSource();

    ds.setAssumeMinServerVersion("9.6");
    ds.setApplicationName(String.format(applicationName, providerId));

    ds.setServerName(
        connectionInfo
            .getHost()
            .orElseThrow(
                () ->
                    new IllegalArgumentException("No 'host' given, required for PGIS connection")));

    ds.setDatabaseName(
        Optional.ofNullable(Strings.emptyToNull(connectionInfo.getDatabase()))
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "No 'database' given, required for PGIS connection")));

    connectionInfo
        .getDriverOptions()
        .forEach(
            (key, value) -> {
              switch (key) {
                case "gssEncMode":
                  ds.setGssEncMode(String.valueOf(value));
                  break;
                case "ssl":
                  ds.setSsl(Objects.equals(value, "true"));
                  break;
                case "sslmode":
                  ds.setSslMode(String.valueOf(value));
                  break;
                case "sslcert":
                  ds.setSslCert(String.valueOf(value));
                  break;
                case "sslkey":
                  ds.setSslKey(String.valueOf(value));
                  break;
                case "sslrootcert":
                  ds.setSslRootCert(String.valueOf(value));
                  break;
                case "sslpassword":
                  ds.setSslPassword(String.valueOf(value));
                  break;
              }
            });

    return ds;
  }

  private DataSource createGpkg(String providerId, ConnectionInfoSql connectionInfo) {
    Path source = Path.of(connectionInfo.getDatabase());

    if (!source.isAbsolute()) {
      Optional<Path> localPath = Optional.empty();
      try {
        localPath = featuresStore.asLocalPath(source, false);
      } catch (IOException e) {
        // continue
      }
      if (localPath.isPresent()) {
        source = localPath.get();
      } else {
        throw new IllegalStateException("GPKG database not found: " + source);
      }
    } else {
      throw new IllegalStateException(
          "GPKG database reference must be a path relative to resources/features. Found: "
              + source);
    }

    if (!spatiaLiteInitialized && Objects.nonNull(spatiaLiteLoader)) {
      spatiaLiteLoader.load();

      this.spatiaLiteInitialized = true;
    }

    SQLiteDataSource ds =
        new SQLiteDataSource() {
          @Override
          public SQLiteConnection getConnection(String username, String password)
              throws SQLException {
            SQLiteConnection connection = super.getConnection(username, password);

            if (Objects.nonNull(spatiaLiteLoader)) {
              try (var statement = connection.createStatement()) {
                // connection was created a few milliseconds before, so set query timeout is omitted
                // (we assume it will succeed)
                statement.execute(
                    String.format(
                        "SELECT load_extension('%s');", spatiaLiteLoader.getExtensionPath()));
              }
            }

            return connection;
          }
        };

    ds.setLoadExtension(true);
    ds.setUrl(String.format("jdbc:sqlite:%s", source));

    return ds;
  }
}
