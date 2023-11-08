/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.app;

import de.ii.xtraplatform.features.domain.ConnectorFactory;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql.Dialect;
import de.ii.xtraplatform.features.sql.domain.SqlClientBasic;
import de.ii.xtraplatform.features.sql.domain.SqlClientBasicFactory;
import de.ii.xtraplatform.features.sql.domain.SqlConnector;
import de.ii.xtraplatform.features.sql.domain.SqlDialect;
import de.ii.xtraplatform.features.sql.domain.SqlDialectGpkg;
import de.ii.xtraplatform.features.sql.domain.SqlDialectPostGis;
import java.sql.Connection;

public class SqlClientBasicFactoryDefault implements SqlClientBasicFactory {
  private final ConnectorFactory connectorFactory;

  public SqlClientBasicFactoryDefault(ConnectorFactory connectorFactory) {
    this.connectorFactory = connectorFactory;
  }

  @Override
  public SqlClientBasic create(
      String providerType, String providerId, ConnectionInfoSql connectionInfo) {
    SqlConnector connector =
        (SqlConnector) connectorFactory.createConnector(providerType, providerId, connectionInfo);

    if (!connector.isConnected()) {
      connectorFactory.disposeConnector(connector);

      RuntimeException connectionError =
          connector
              .getConnectionError()
              .map(
                  throwable ->
                      throwable instanceof RuntimeException
                          ? (RuntimeException) throwable
                          : new RuntimeException(throwable))
              .orElse(new IllegalStateException("unknown reason"));

      throw connectionError;
    }

    return new SqlClientBasicDefault(connector, connectionInfo);
  }

  @Override
  public void dispose(SqlClientBasic sqlClient) {
    if (sqlClient instanceof SqlClientBasicDefault) {
      connectorFactory.disposeConnector(((SqlClientBasicDefault) sqlClient).connector);
    }
  }

  private static class SqlClientBasicDefault implements SqlClientBasic {
    private final SqlConnector connector;
    private final SqlDialect dialect;

    private SqlClientBasicDefault(SqlConnector connector, ConnectionInfoSql connectionInfo) {
      this.connector = connector;
      this.dialect =
          connectionInfo.getDialect() == Dialect.GPKG
              ? new SqlDialectGpkg()
              : new SqlDialectPostGis();
    }

    @Override
    public Connection getConnection() {
      return connector.getSqlClient().getConnection();
    }

    @Override
    public SqlDialect getSqlDialect() {
      return dialect;
    }
  }
}
