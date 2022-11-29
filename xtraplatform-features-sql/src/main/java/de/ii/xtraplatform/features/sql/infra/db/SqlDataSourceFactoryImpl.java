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
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.spatialite.domain.SpatiaLiteLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.sqlite.SQLiteDataSource;

@Singleton
@AutoBind
public class SqlDataSourceFactoryImpl implements SqlDataSourceFactory {

  private final Path dataDir;
  private final String applicationName;
  private final SpatiaLiteLoader spatiaLiteLoader;

  private boolean spatiaLiteInitialized;

  @Inject
  public SqlDataSourceFactoryImpl(AppContext appContext, SpatiaLiteLoader spatiaLiteLoader) {
    this.dataDir = appContext.getDataDir();
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
            String.format(
                "SELECT load_extension('%s'); SELECT CASE CheckGeoPackageMetaData() WHEN 1 THEN EnableGpkgMode() END;",
                spatiaLiteLoader.getExtensionPath()));
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
    Path path =
        Paths.get(connectionInfo.getDatabase()).isAbsolute()
            ? Paths.get(connectionInfo.getDatabase())
            : dataDir.resolve(connectionInfo.getDatabase());

    if (!path.toFile().exists()) {
      throw new IllegalArgumentException("GPKG database does not exist: " + path);
    }

    if (!spatiaLiteInitialized) {
      spatiaLiteLoader.load();

      this.spatiaLiteInitialized = true;
    }

    // SpatiaLiteDataSource ds = new SpatiaLiteDataSource(spatiaLiteLoader.getExtensionPath());
    SQLiteDataSource ds = new SQLiteDataSource();

    ds.setLoadExtension(true);
    ds.setUrl(String.format("jdbc:sqlite:%s", path));

    return ds;
  }
}
