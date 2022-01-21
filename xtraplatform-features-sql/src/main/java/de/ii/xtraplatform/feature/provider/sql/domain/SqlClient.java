/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.domain;

import akka.NotUsed;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Source;
import de.ii.xtraplatform.feature.provider.sql.app.FeatureSql;
import java.sql.Connection;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import scala.concurrent.ExecutionContext;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

public interface SqlClient {

    CompletableFuture<Collection<SqlRow>> run(String query, SqlQueryOptions options);

    Source<SqlRow, NotUsed> getSourceStream(String query, SqlQueryOptions options);

    <T> CompletionStage<String> executeMutation(List<Function<T, String>> mutations, T mutationContext,
                                                List<Consumer<String>> idConsumers,
                                                ActorMaterializer materializer);



    Source<String, NotUsed> getMutationSource(FeatureSql feature, List<Function<FeatureSql, String>> mutations,
                                              List<Consumer<String>> idConsumers,
                                              ExecutionContext executionContext);

    Flow<FeatureSql, String, NotUsed> getMutationFlow(
            Function<FeatureSql, List<Function<FeatureSql, Pair<String, Consumer<String>>>>> mutations,
            ExecutionContext executionContext, Optional<String> id);

    Connection getConnection();

    Map<String,String> getDbInfo();
}
