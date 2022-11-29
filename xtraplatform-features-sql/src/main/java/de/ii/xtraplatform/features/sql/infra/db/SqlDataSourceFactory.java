/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.infra.db;

import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql;
import java.util.Optional;
import javax.sql.DataSource;

public interface SqlDataSourceFactory {

  DataSource create(String providerId, ConnectionInfoSql connectionInfoSql);

  Optional<String> getInitSql(ConnectionInfoSql connectionInfo);
}
