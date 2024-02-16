/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.app;

import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.blobs.domain.ResourceStore;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql.Dialect;
import de.ii.xtraplatform.features.sql.domain.ImmutableDbInfoGpkg;
import de.ii.xtraplatform.features.sql.domain.SqlClientBasic;
import de.ii.xtraplatform.features.sql.domain.SqlClientBasicFactory;
import de.ii.xtraplatform.features.sql.domain.SqlDialect;
import de.ii.xtraplatform.features.sql.domain.SqlDialect.DbInfo;
import de.ii.xtraplatform.features.sql.domain.SqlDialect.GeoInfo;
import de.ii.xtraplatform.features.sql.domain.SqlDialectGpkg;
import de.ii.xtraplatform.features.sql.domain.SqlDialectGpkg.DbInfoGpkg.SpatialMetadata;
import de.ii.xtraplatform.features.sql.domain.SqlDialectPostGis;
import de.ii.xtraplatform.features.sql.infra.db.SqlConnectorRx;
import de.ii.xtraplatform.features.sql.infra.db.SqlDataSourceFactory;
import de.ii.xtraplatform.features.sql.infra.db.SqlDataSourceFactoryImpl;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

public class SqlClientBasicFactorySimple implements SqlClientBasicFactory {
  private final SqlDataSourceFactory sqlDataSourceFactory;

  public SqlClientBasicFactorySimple(AppContext appContext, ResourceStore resourceStore) {
    this.sqlDataSourceFactory = new SqlDataSourceFactoryImpl(appContext, resourceStore, null);
  }

  @Override
  public SqlClientBasic create(
      String providerType, String providerId, ConnectionInfoSql connectionInfo) {
    DataSource dataSource = sqlDataSourceFactory.create(providerId, connectionInfo);

    return new SqlClientBasicSimple(dataSource, connectionInfo);
  }

  @Override
  public void dispose(SqlClientBasic sqlClient) {
    if (sqlClient instanceof SqlClientBasicSimple) {
      for (Connection connection : ((SqlClientBasicSimple) sqlClient).connections) {
        try {
          connection.close();
        } catch (SQLException e) {
          // ignore
        }
      }
      DataSource dataSource = ((SqlClientBasicSimple) sqlClient).dataSource;
      if (dataSource instanceof Closeable) {
        try {
          ((Closeable) dataSource).close();
        } catch (IOException e) {
          // ignore
        }
      }
    }
  }

  private static class SqlClientBasicSimple implements SqlClientBasic {
    private final DataSource dataSource;
    private final ConnectionInfoSql connectionInfo;

    private final SqlDialect dialect;
    private final List<Connection> connections;

    private SqlClientBasicSimple(DataSource dataSource, ConnectionInfoSql connectionInfo) {
      this.dataSource = dataSource;
      this.connectionInfo = connectionInfo;
      this.dialect =
          connectionInfo.getDialect() == Dialect.GPKG
              ? new SqlDialectGpkg()
              : new SqlDialectPostGis();
      this.connections = new ArrayList<>();
    }

    @Override
    public Connection getConnection() {
      Connection connection;
      try {
        connection =
            dataSource.getConnection(
                connectionInfo.getUser().orElse(""), SqlConnectorRx.getPassword(connectionInfo));
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }

      this.connections.add(connection);

      return connection;
    }

    @Override
    public SqlDialect getSqlDialect() {
      return dialect;
    }

    @Override
    public DbInfo getDbInfo() throws SQLException {
      if (connectionInfo.getDialect() == Dialect.GPKG) {
        // spatialite ist not available in simple client
        return ImmutableDbInfoGpkg.of("unknown", "unknown", SpatialMetadata.GPKG);
      }

      Connection connection = connections.isEmpty() ? getConnection() : connections.get(0);

      return dialect.getDbInfo(connection);
    }

    @Override
    public Map<String, GeoInfo> getGeoInfo() throws SQLException {
      Connection connection = connections.isEmpty() ? getConnection() : connections.get(0);

      return dialect.getGeoInfo(connection, getDbInfo());
    }
  }
}
