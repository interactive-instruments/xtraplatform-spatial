/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.domain;

import akka.NotUsed;
import akka.stream.javadsl.Source;

public interface SqlClient {

    Source<SqlRow, NotUsed> getSourceStream(String query, SqlQueryOptions options);

}
