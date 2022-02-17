/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.infra.db;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.sql.app.FeatureSql;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql.Dialect;
import de.ii.xtraplatform.features.sql.domain.SqlClient;
import de.ii.xtraplatform.features.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.features.sql.domain.SqlRow;
import de.ii.xtraplatform.features.domain.Tuple;
import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import de.ii.xtraplatform.streams.domain.Reactive;
import java.io.Closeable;
import java.sql.Connection;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlClientSlick implements SqlClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlClientSlick.class);

    private final Closeable session;
    private final Dialect dialect;

    public SqlClientSlick(Closeable session, Dialect dialect) {
        this.session = session;
        this.dialect = dialect;
    }

    @Override
    public CompletableFuture<Collection<SqlRow>> run(String query, SqlQueryOptions options) {
        if (LOGGER.isDebugEnabled(MARKER.SQL)) {
            LOGGER.debug(MARKER.SQL, "Executing statement: {}", query);
        }
        return null;//SlickSql.run(session, query, positionedResult -> new SqlRowSlick().read(positionedResult, options)).toCompletableFuture();
    }

    @Override
    public Reactive.Source<SqlRow> getSourceStream(String query, SqlQueryOptions options) {
        if (LOGGER.isDebugEnabled(MARKER.SQL)) {
            LOGGER.debug(MARKER.SQL, "Executing statement: {}", query);
        }
        return null;//Reactive.Source.akka(SlickSql.source(session, query, positionedResult -> new SqlRowSlick().read(positionedResult, options)));
    }

    @Override
    public Reactive.Source<String> getMutationSource(FeatureSql feature, List<Function<FeatureSql, String>> toStatements,
                                                     List<Consumer<String>> idConsumers,
                                                     Object executionContext) {
        List<Function<FeatureSql, String>> toStatementsWithLog = toStatements.stream()
            .map(function -> (Function<FeatureSql, String>) featureSql -> {
                String statement = function.apply(featureSql);

                if (LOGGER.isDebugEnabled(MARKER.SQL)) {
                    LOGGER.debug(MARKER.SQL, "Executing statement: {}", statement);
                }

                return statement;
            })
            .collect(Collectors.toList());

        int[] i = {0};
        /*BiFunction<SlickSql.SlickRow, String, String> mapper = (slickRow, previousId) -> {
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
        };*/

        return null;//Reactive.Source.akka(SlickSql.source(session, executionContext, toStatementsWithLog, mapper, feature)
                      // .fold("", (id1, id2) -> id1.isEmpty() ? id2 : id1));
    }

    @Override
    public Reactive.Transformer<FeatureSql, String> getMutationFlow(
            Function<FeatureSql, List<Function<FeatureSql, Tuple<String, Consumer<String>>>>> mutations,
            Object executionContext, Optional<String> id) {

        Reactive.Transformer<FeatureSql, String> toQueries = Reactive.Transformer.flatMap(feature -> {
            List<Function<FeatureSql, Tuple<String, Consumer<String>>>> m = mutations.apply(feature);

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
                                                      Tuple<String, Consumer<String>> query = queryFunction.apply(feature);
                                                      return query.second();
                                                  })
                                                  .filter(Objects::nonNull)
                                                  .collect(Collectors.toList());

            return getMutationSource(feature, toStatements, idConsumers, executionContext);
        });

        if (id.isPresent()) {
            //TODO: check that feature id equals given id
            Reactive.Transformer<FeatureSql, FeatureSql> filter = Reactive.Transformer.filter(featureSql -> true);

            return filter.via(toQueries);
        }

        return toQueries;
    }

    @Override
    public Connection getConnection() {
        return null; //session.db().source().createConnection();
    }

    @Override
    public Map<String, String> getDbInfo() {
        switch (dialect) {
            case GPKG:
                return run("SELECT sqlite_version(),spatialite_version(),CASE CheckSpatialMetaData() WHEN 4 THEN 'GPKG' WHEN 3 THEN 'SPATIALITE' ELSE 'UNSUPPORTED' END;", SqlQueryOptions.withColumnTypes(String.class, String.class, String.class))
                    .join()
                    .stream()
                    .findFirst()
                    .map(sqlRow -> ImmutableMap.of(
                        "sqlite_version", (String) sqlRow.getValues().get(0),
                        "spatialite_version", (String) sqlRow.getValues().get(1),
                        "spatial_metadata", (String) sqlRow.getValues().get(2)
                    ))
                    .orElse(ImmutableMap.of());
            case PGIS:
        }

        return ImmutableMap.of();
    }
}
