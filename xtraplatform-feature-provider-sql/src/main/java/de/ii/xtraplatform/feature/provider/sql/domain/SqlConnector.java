/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.domain;

import akka.Done;
import akka.NotUsed;
import akka.japi.JavaPartialFunction;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableSqlRowMeta.Builder;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureStoreAttributesContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreInstanceContainer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletionStage;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;

//TODO: class
public interface SqlConnector extends
    FeatureProviderConnector<SqlRow, SqlQueries, SqlQueryOptions> {

  //TODO
  SqlQueryOptions NO_OPTIONS = SqlQueryOptions.withColumnTypes(String.class);

  int getMaxConnections();

  int getMinConnections();

  int getQueueSize();

  SqlClient getSqlClient();

  @Deprecated
  @Override
  default CompletionStage<Done> runQuery(final FeatureQuery query,
      final Sink<SqlRow, CompletionStage<Done>> consumer,
      final Map<String, String> additionalQueryParameters) {
    return null;
  }

  @Override
  default Source<SqlRow, NotUsed> getSourceStream(SqlQueries query) {
    return getSourceStream(query, NO_OPTIONS).mapError(PSQL_CONTEXT);
  }

  //TODO: reuse instances of SqlRow, SqlColumn? (object pool, e.g. https://github.com/chrisvest/stormpot, implement test with 100000 rows, measure)
  @Override
  default Source<SqlRow, NotUsed> getSourceStream(SqlQueries query, SqlQueryOptions options) {

    Optional<String> metaQuery = query.getMetaQuery();

    Source<SqlRowMeta, NotUsed> metaResult1 = getMetaResult(metaQuery, options);

    //TODO: multiple main tables
    FeatureStoreInstanceContainer mainTable = query.getInstanceContainers()
        .get(0);
    List<FeatureStoreAttributesContainer> attributesContainers = mainTable
        .getAllAttributesContainers();

    Source<SqlRow, NotUsed> sqlRowSource = metaResult1.flatMapConcat(metaResult -> {

      int[] i = {0};
      Source<SqlRow, NotUsed>[] sqlRows = query.getValueQueries()
          .apply(metaResult)
          .map(valueQuery -> getSqlClient()
              .getSourceStream(valueQuery, new ImmutableSqlQueryOptions.Builder()
                  .from(options)
                  .attributesContainer(attributesContainers.get(i[0]))
                  .containerPriority(i[0]++)
                  .build()))
          .toArray((IntFunction<Source<SqlRow, NotUsed>[]>) Source[]::new);
      return mergeAndSort(sqlRows)
          .prepend(Source.single(metaResult));
    });

    return sqlRowSource.mapError(PSQL_CONTEXT);
  }

  //TODO: simplify
  default Source<SqlRowMeta, NotUsed> getMetaResult(Optional<String> metaQuery,
      SqlQueryOptions options) {
    if (!metaQuery.isPresent()) {
      return Source.single(getMetaQueryResult(0L, 0L, 0L, 0L).build());
    }

    List<Class<?>> columnTypes = Stream
        .concat(IntStream.range(0, 2 + (options.getCustomSortKeys().size() * 2))
            .mapToObj(i -> Object.class), Stream.of(Long.class, Long.class))
        .collect(Collectors.toList());

    return getSqlClient().getSourceStream(metaQuery.get(),
        SqlQueryOptions.withColumnTypes(columnTypes))
        .map(sqlRow -> getMetaQueryResult(sqlRow.getValues()));
  }

  default Builder getMetaQueryResult(Object minKey, Object maxKey, Long numberReturned,
      Long numberMatched) {
    return new ImmutableSqlRowMeta.Builder()
        .minKey(minKey)
        .maxKey(maxKey)
        .numberReturned(Objects.nonNull(numberReturned) ? numberReturned : 0L)
        .numberMatched(
            Objects.nonNull(numberMatched) && numberMatched > -1 ? OptionalLong.of(numberMatched)
                : OptionalLong.empty());
  }

  default SqlRowMeta getMetaQueryResult(List<Object> values) {
    int size = values.size();
    Builder builder = getMetaQueryResult(values.get(size - 4), values.get(size - 3),
        (Long) values.get(size - 2), (Long) values.get(size - 1));

    for (int i = 0; i < size - 4; i = i + 2) {
      builder.addCustomMinKeys(values.get(i)).addCustomMaxKeys(values.get(i + 1));
    }

    return builder.build();
  }

  static <T extends Comparable<T>> Source<T, NotUsed> mergeAndSort(Source<T, NotUsed>... sources) {
    if (sources.length == 1) {
      return sources[0];
    }

    return mergeAndSort(sources[0], sources[1], Arrays.asList(sources)
        .subList(2, sources.length));
  }

  //TODO 08.10.: feuerwehr has 1 join, so 3 queries
  // it seems that it always works with maxThreads=6, so 2 * number of queries
  // tested multiple times for 5 minutes with 8 parallel requests, 128 per second
  // reducing maxThreads to 5 triggers the error, even with less parallel requests
  // but testing luftreinhalteplan with maxThreads=14 did not seem to work reliable -> test again
  // could it be that feuerwehr works not because it is 2 * 3 queries but 3 * 2 queries? so meta query does not count
  // then luftreinhalteplan would be 3 * 5, so maxThreads=15 -> test

  static <T extends Comparable<T>> Source<T, NotUsed> mergeAndSort(Source<T, NotUsed> source1,
      Source<T, NotUsed> source2,
      Iterable<Source<T, NotUsed>> rest) {
    Comparator<T> comparator = Comparator.naturalOrder();
    Source<T, NotUsed> mergedAndSorted = source1.mergeSorted(source2, comparator);
    for (Source<T, NotUsed> source3 : rest) {
      mergedAndSorted = mergedAndSorted.mergeSorted(source3, comparator);
    }
    return mergedAndSorted;
  }

  JavaPartialFunction<Throwable, Throwable> PSQL_CONTEXT = new JavaPartialFunction<>() {
    @Override
    public Throwable apply(Throwable throwable, boolean isCheck) {
      if (throwable instanceof PSQLException) {
        PSQLException e = (PSQLException) throwable;
        String message = Optional.ofNullable(e.getServerErrorMessage())
            .map(serverErrorMessage -> {
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

        return new PSQLException("Unexpected SQL query error: " + message, PSQLState.UNKNOWN_STATE);
      }
      return throwable;
    }
  };
}
