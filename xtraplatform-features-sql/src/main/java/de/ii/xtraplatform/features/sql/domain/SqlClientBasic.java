/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.domain;

import de.ii.xtraplatform.features.sql.domain.SqlDialect.DbInfo;
import de.ii.xtraplatform.features.sql.domain.SqlDialect.GeoInfo;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

public interface SqlClientBasic {
  Connection getConnection();

  SqlDialect getSqlDialect();

  default DbInfo getDbInfo() throws SQLException {
    return getSqlDialect().getDbInfo(getConnection());
  }

  default Map<String, GeoInfo> getGeoInfo() throws SQLException {
    return getSqlDialect().getGeoInfo(getConnection(), getDbInfo());
  }
}
