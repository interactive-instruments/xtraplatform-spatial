package de.ii.xtraplatform.feature.provider.sql.domain;

import akka.Done;
import akka.NotUsed;
import akka.japi.pf.PFBuilder;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import de.ii.xtraplatform.feature.provider.api.FeatureProviderConnector;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
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

//TODO: class
public interface SqlConnector extends FeatureProviderConnector<SqlRow, SqlQueries, SqlQueryOptions> {

    //TODO
    SqlQueryOptions NO_OPTIONS = SqlQueryOptions.withColumnTypes(String.class);

    SqlClient getSqlClient();

    @Deprecated
    @Override
    default CompletionStage<Done> runQuery(final FeatureQuery query, final Sink<SqlRow, CompletionStage<Done>> consumer,
                                           final Map<String, String> additionalQueryParameters) {
        return null;
    }

    @Override
    default Source<SqlRow, NotUsed> getSourceStream(SqlQueries query) {
        return getSourceStream(query, NO_OPTIONS);
    }

    //TODO: reuse instances of SqlRow, SqlColumn? (object pool, e.g. https://github.com/chrisvest/stormpot, implement test with 100000 rows, measure)
    @Override
    default Source<SqlRow, NotUsed> getSourceStream(SqlQueries query, SqlQueryOptions options) {

        Optional<String> metaQuery = query.getMetaQuery();

        Source<SqlRowMeta, NotUsed> metaResult1 = getMetaResult(metaQuery);

        //TODO: multiple main tables
        FeatureStoreInstanceContainer mainTable = query.getInstanceContainers()
                                                       .get(0);
        List<FeatureStoreAttributesContainer> attributesContainers = mainTable.getAllAttributesContainers();

        Source<SqlRow, NotUsed> sqlRowSource = metaResult1.flatMapConcat(metaResult -> {

            int[] i = {0};
            Source<SqlRow, NotUsed>[] sqlRows = query.getValueQueries()
                                                     .apply(metaResult)
                                                     .map(valueQuery -> {
                                                                  /*if (LOGGER.isTraceEnabled()) {
                                                                      LOGGER.trace("Values query: {}", valueQuery);
                                                                  }*/

                                                         return getSqlClient().getSourceStream(valueQuery, ImmutableSqlQueryOptions.builder()
                                                                                                                                   .attributesContainer(attributesContainers.get(i[0]))
                                                                                                                                   .containerPriority(i[0]++)
                                                                                                                                   .build());
                                                     })
                                                     .toArray((IntFunction<Source<SqlRow, NotUsed>[]>) Source[]::new);
            return mergeAndSort(sqlRows)
                    .prepend(Source.single(metaResult));
        });

        return sqlRowSource;
    }

    //TODO: simplify
    default Source<SqlRowMeta, NotUsed> getMetaResult(Optional<String> metaQuery) {
        if (!metaQuery.isPresent()) {
            return Source.single(getMetaQueryResult(0L, 0L, 0L, 0L));
        }

        /*if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Meta query: {}", metaQuery.get());
        }*/

        return getSqlClient().getSourceStream(metaQuery.get(), SqlQueryOptions.withColumnTypes(Long.class, Long.class, Long.class, Long.class))
                             .map(sqlRow -> getMetaQueryResult(sqlRow.getValues()))
                             .recover(new PFBuilder<Throwable, SqlRowMeta>().matchAny(throwable -> getMetaQueryResult(0L, 0L, 0L, 0L))
                                                                            .build());
    }

    default SqlRowMeta getMetaQueryResult(Long minKey, Long maxKey, Long numberReturned, Long numberMatched) {
        return ImmutableSqlRowMeta.builder()
                                  .minKey(Objects.nonNull(minKey) ? minKey : 0L)
                                  .maxKey(Objects.nonNull(maxKey) ? maxKey : 0L)
                                  .numberReturned(Objects.nonNull(numberReturned) ? numberReturned : 0L)
                                  .numberMatched(Objects.nonNull(numberMatched) && numberMatched > -1 ? OptionalLong.of(numberMatched) : OptionalLong.empty())
                                  .build();
    }

    default SqlRowMeta getMetaQueryResult(List<Object> values) {
        return getMetaQueryResult((Long) values.get(0), (Long) values.get(1), (Long) values.get(2), (Long) values.get(3));
    }

    static <T extends Comparable<T>> Source<T, NotUsed> mergeAndSort(Source<T, NotUsed>... sources) {
        if (sources.length == 1) {
            return sources[0];
        }

        return mergeAndSort(sources[0], sources[1], Arrays.asList(sources)
                                                          .subList(2, sources.length));
    }

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
}
