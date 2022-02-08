/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.domain;

import de.ii.xtraplatform.feature.provider.sql.app.FeatureSql;
import de.ii.xtraplatform.features.domain.Tuple;
import de.ii.xtraplatform.streams.domain.Reactive;
import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import scala.concurrent.ExecutionContext;

public interface SqlClient {

    CompletableFuture<Collection<SqlRow>> run(String query, SqlQueryOptions options);

    Reactive.Source<SqlRow> getSourceStream(String query, SqlQueryOptions options);


    Reactive.Source<String> getMutationSource(FeatureSql feature, List<Function<FeatureSql, String>> mutations,
                                              List<Consumer<String>> idConsumers,
                                              ExecutionContext executionContext);

    Reactive.Transformer<FeatureSql, String> getMutationFlow(
            Function<FeatureSql, List<Function<FeatureSql, Tuple<String, Consumer<String>>>>> mutations,
            ExecutionContext executionContext, Optional<String> id);

    Connection getConnection();

    Map<String,String> getDbInfo();
}
