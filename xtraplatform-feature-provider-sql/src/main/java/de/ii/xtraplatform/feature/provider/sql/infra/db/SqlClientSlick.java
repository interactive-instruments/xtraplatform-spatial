/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.infra.db;

import akka.NotUsed;
import akka.stream.alpakka.slick.javadsl.SlickSession;
import akka.stream.javadsl.Source;
import de.ii.xtraplatform.feature.provider.sql.SlickSql;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlClient;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRow;

public class SqlClientSlick implements SqlClient {

    private final SlickSession session;

    public SqlClientSlick(SlickSession session) {
        this.session = session;
    }

    @Override
    public Source<SqlRow, NotUsed> getSourceStream(String query, SqlQueryOptions options) {
        return SlickSql.source(session, query, positionedResult -> new SqlRowSlick().read(positionedResult, options));
    }
}
