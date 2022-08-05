/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.domain;

import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureStoreAttributesContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql.Dialect;
import de.ii.xtraplatform.features.sql.domain.ImmutableSqlRowMeta.Builder;
import de.ii.xtraplatform.streams.domain.Reactive;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

// TODO: class
public interface SqlConnector
    extends FeatureProviderConnector<SqlRow, SqlQueries, SqlQueryOptions> {

  // TODO
  SqlQueryOptions NO_OPTIONS = SqlQueryOptions.withColumnTypes(String.class);

  int getMaxConnections();

  int getMinConnections();

  int getQueueSize();

  Dialect getDialect();

  SqlClient getSqlClient();

  @Override
  default Reactive.Source<SqlRow> getSourceStream(SqlQueries query) {
    return getSourceStream(query, NO_OPTIONS);
  }

  // TODO: reuse instances of SqlRow, SqlColumn? (object pool, e.g.
  // https://github.com/chrisvest/stormpot, implement test with 100000 rows, measure)
  @Override
  default Reactive.Source<SqlRow> getSourceStream(SqlQueries query, SqlQueryOptions options) {

    // TODO for chunking:
    // - List<SqlQueries>
    // - counter with limit, decreased in metaResult1.flatMapConcat(metaResult... by numberReturned
    // - create List<Source<SqlRow, NotUsed>[]>, concat with  Source.combine(source1, source2,
    // Collections.singletonList(source3), Concat::create)
    // - return emptySource in metaResult1.flatMapConcat(metaResult... if counter < 0
    // - adjust limit for metaQuery, Math.min(chunkSize, counter)

    // TODO for multiple main tables: should work exactly like chunking

    Optional<String> metaQuery = query.getMetaQuery();

    Reactive.Source<SqlRowMeta> metaResult1 = getMetaResult(metaQuery, options);

    if (options.isHitsOnly()) {
      return metaResult1
          .via(
              Reactive.Transformer.flatMap(
                  metaResult -> Reactive.Source.single((SqlRow) metaResult)))
          .mapError(PSQL_CONTEXT);
    }

    // TODO: multiple main tables
    FeatureStoreInstanceContainer mainTable = query.getInstanceContainers().get(0);
    List<FeatureStoreAttributesContainer> attributesContainers =
        mainTable.getAllAttributesContainers();
    List<SchemaSql> tableSchemas = query.getTableSchemas();

    Reactive.Source<SqlRow> sqlRowSource =
        metaResult1.via(
            Reactive.Transformer.flatMap(
                metaResult -> {
                  int[] i = {0};
                  Reactive.Source<SqlRow>[] sqlRows =
                      query
                          .getValueQueries()
                          .apply(metaResult)
                          .map(
                              valueQuery ->
                                  getSqlClient()
                                      .getSourceStream(
                                          valueQuery,
                                          new ImmutableSqlQueryOptions.Builder()
                                              .from(options)
                                              .tableSchema(tableSchemas.get(i[0]))
                                              .containerPriority(i[0]++)
                                              .build()))
                          .toArray((IntFunction<Reactive.Source<SqlRow>[]>) Reactive.Source[]::new);
                  return mergeAndSort(sqlRows).prepend(Reactive.Source.single(metaResult));
                }));

    return sqlRowSource.mapError(PSQL_CONTEXT);
  }

  // TODO: simplify
  default Reactive.Source<SqlRowMeta> getMetaResult(
      Optional<String> metaQuery, SqlQueryOptions options) {
    if (!metaQuery.isPresent()) {
      return Reactive.Source.single(getMetaQueryResult(0L, 0L, 0L, 0L).build());
    }

    List<Class<?>> columnTypes =
        Stream.concat(
                IntStream.range(0, 2 + (options.getCustomSortKeys().size() * 2))
                    .mapToObj(i -> Object.class),
                Stream.of(Long.class, Long.class))
            .collect(Collectors.toList());

    return getSqlClient()
        .getSourceStream(metaQuery.get(), SqlQueryOptions.withColumnTypes(columnTypes))
        .via(Reactive.Transformer.map(sqlRow -> getMetaQueryResult(sqlRow.getValues())));
  }

  default Builder getMetaQueryResult(
      Object minKey, Object maxKey, Long numberReturned, Long numberMatched) {
    return new ImmutableSqlRowMeta.Builder()
        .minKey(minKey)
        .maxKey(maxKey)
        .numberReturned(Objects.nonNull(numberReturned) ? numberReturned : 0L)
        .numberMatched(
            Objects.nonNull(numberMatched) && numberMatched > -1
                ? OptionalLong.of(numberMatched)
                : OptionalLong.empty());
  }

  default SqlRowMeta getMetaQueryResult(List<Object> values) {
    int size = values.size();
    Builder builder =
        getMetaQueryResult(
            values.get(size - 4),
            values.get(size - 3),
            (Long) values.get(size - 2),
            (Long) values.get(size - 1));

    for (int i = 0; i < size - 4; i = i + 2) {
      builder.addCustomMinKeys(values.get(i)).addCustomMaxKeys(values.get(i + 1));
    }

    return builder.build();
  }

  static <T extends Comparable<T>> Reactive.Source<T> mergeAndSort(Reactive.Source<T>... sources) {
    if (sources.length == 1) {
      return sources[0];
    }

    return mergeAndSort(sources[0], sources[1], Arrays.asList(sources).subList(2, sources.length));
  }

  static <T extends Comparable<T>> Reactive.Source<T> mergeAndSort(
      Reactive.Source<T> source1, Reactive.Source<T> source2, Iterable<Reactive.Source<T>> rest) {
    Comparator<T> comparator = Comparator.naturalOrder();
    Reactive.Source<T> mergedAndSorted = source1.mergeSorted(source2, comparator);
    for (Reactive.Source<T> source3 : rest) {
      mergedAndSorted = mergedAndSorted.mergeSorted(source3, comparator);
    }
    return mergedAndSorted;
  }

  Function<Throwable, Throwable> PSQL_CONTEXT =
      throwable -> {
        if (throwable instanceof PSQLException) {
          PSQLException e = (PSQLException) throwable;
          String message =
              Optional.ofNullable(e.getServerErrorMessage())
                  .map(
                      serverErrorMessage -> {
                        StringBuilder totalMessage = new StringBuilder("\n  ");
                        String msg = serverErrorMessage.getSeverity();
                        if (msg != null) {
                          totalMessage.append(msg).append(": ");
                        }
                        msg = serverErrorMessage.getMessage();
                        if (msg != null) {
                          totalMessage.append(msg);
                        }
                        msg = serverErrorMessage.getDetail();
                        if (msg != null) {
                          totalMessage.append("\n  ").append("Detail: ").append(msg);
                        }

                        msg = serverErrorMessage.getHint();
                        if (msg != null) {
                          totalMessage.append("\n  ").append("Hint: ").append(msg);
                        }
                        msg = String.valueOf(serverErrorMessage.getPosition());
                        if (!msg.equals("0")) {
                          totalMessage.append("\n  ").append("Position: ").append(msg);
                        }
                        msg = serverErrorMessage.getWhere();
                        if (msg != null) {
                          totalMessage.append("\n  ").append("Where: ").append(msg);
                        }
                        return totalMessage.toString();
                      })
                  .orElseGet(e::getMessage);

          return new PSQLException(
              "Unexpected SQL query error: " + message, PSQLState.UNKNOWN_STATE, throwable);
        }
        return throwable;
      };
}
