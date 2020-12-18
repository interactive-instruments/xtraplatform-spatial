/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.infra.db;

import akka.NotUsed;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.alpakka.slick.javadsl.SlickSession;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import de.ii.xtraplatform.feature.provider.sql.SlickSql;
import de.ii.xtraplatform.feature.provider.sql.app.FeatureSql;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlClient;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRow;
import java.sql.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.ExecutionContext;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SqlClientSlick implements SqlClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlClientSlick.class);

    private final SlickSession session;

    public SqlClientSlick(SlickSession session) {
        this.session = session;
    }

    @Override
    public Source<SqlRow, NotUsed> getSourceStream(String query, SqlQueryOptions options) {
        return SlickSql.source(session, query, positionedResult -> new SqlRowSlick().read(positionedResult, options));
    }

    @Override
    public <T> CompletionStage<String> executeMutation(List<Function<T, String>> mutations, T mutationContext,
                                                       List<Consumer<String>> idConsumers,
                                                       ActorMaterializer materializer) {

        int[] i = {0};
        BiFunction<SlickSql.SlickRow, String, String> mapper = (slickRow, previousId) -> {
            LOGGER.debug("QUERY {}", i[0]);
            // null not allowed as return value
            String id = slickRow.nextString();
            LOGGER.debug("RETURNED {}", id);
            idConsumers.get(i[0])
                       .accept(id);
            //LOGGER.debug("VALUES {}", values);
            LOGGER.debug("");
            i[0]++;

            return previousId != null ? previousId : id;
        };

        return SlickSql.source(session, materializer.system()
                                                    .dispatcher(), mutations, mapper, mutationContext)
                       .runWith(Sink.fold("", (id1, id2) -> id1.isEmpty() ? id2 : id1), materializer);
    }

    @Override
    public Source<String, NotUsed> getMutationSource(FeatureSql feature, List<Function<FeatureSql, String>> toStatements,
                                                     List<Consumer<String>> idConsumers,
                                                     ExecutionContext executionContext) {
        int[] i = {0};
        BiFunction<SlickSql.SlickRow, String, String> mapper = (slickRow, previousId) -> {
            LOGGER.debug("QUERY {}", i[0]);
            // null not allowed as return value
            String id = slickRow.nextString();
            LOGGER.debug("RETURNED {}", id);
            idConsumers.get(i[0])
                       .accept(id);
            //LOGGER.debug("VALUES {}", values);
            LOGGER.debug("");
            i[0]++;

            return previousId != null ? previousId : id;
        };

        return SlickSql.source(session, executionContext, toStatements, mapper, feature)
                       .fold("", (id1, id2) -> id1.isEmpty() ? id2 : id1);
    }

    @Override
    public Flow<FeatureSql, String, NotUsed> getMutationFlow(
            Function<FeatureSql, List<Function<FeatureSql, Pair<String, Consumer<String>>>>> mutations,
            ExecutionContext executionContext, Optional<String> id) {

        Flow<FeatureSql, FeatureSql, NotUsed> flow = Flow.create();

        if (id.isPresent()) {
            //TODO: check that feature id equals given id
            flow = flow.filter(feature -> true);
        }

        return flow.flatMapMerge(1, feature -> {
            List<Function<FeatureSql, Pair<String, Consumer<String>>>> m = mutations.apply(feature);

            List<Function<FeatureSql, String>> toStatements = m.stream()
                                                               .map(queryFunction -> Objects.isNull(queryFunction.apply(feature).first())
                                                                       ? null
                                                                       : (Function<FeatureSql, String>) feature2 -> queryFunction.apply(feature2)
                                                                                                                                 .first())
                                                               .filter(Objects::nonNull)
                                                               .collect(Collectors.toList());

            List<Consumer<String>> idConsumers = m.stream()
                                                  .map(queryFunction -> {
                                                      //TODO
                                                      Pair<String, Consumer<String>> query = queryFunction.apply(feature);
                                                      return query.second();
                                                  })
                                                  .filter(Objects::nonNull)
                                                  .collect(Collectors.toList());

            return getMutationSource(feature, toStatements, idConsumers, executionContext);
        });
    }

    @Override
    public Connection getConnection() {
        return session.db().source().createConnection();
    }
}
