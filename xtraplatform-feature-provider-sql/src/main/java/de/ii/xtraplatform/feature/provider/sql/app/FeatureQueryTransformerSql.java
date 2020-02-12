package de.ii.xtraplatform.feature.provider.sql.app;

import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureQueryTransformer;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableSqlQueries;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueries;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRowMeta;
import de.ii.xtraplatform.features.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

class FeatureQueryTransformerSql implements FeatureQueryTransformer<SqlQueries> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureQueryTransformerSql.class);

    private final Map<String, FeatureStoreTypeInfo> typeInfos;
    private final FeatureStoreQueryGeneratorSql queryGenerator;
    private final boolean computeNumberMatched;

    FeatureQueryTransformerSql(Map<String, FeatureStoreTypeInfo> typeInfos,
                               FeatureStoreQueryGeneratorSql queryGenerator, boolean computeNumberMatched) {
        this.typeInfos = typeInfos;
        this.queryGenerator = queryGenerator;
        this.computeNumberMatched = computeNumberMatched;
    }

    //TODO: reuse instances of SqlRow, SqlColumn? (object pool, e.g. https://github.com/chrisvest/stormpot, implement test with 100000 rows, measure)
    /*public Source<SqlRow, NotUsed> getSqlRowStream(FeatureQuery featureQuery, FeatureStoreTypeInfo typeInfo) {
        //TODO: implement for multiple main tables
        FeatureStoreInstanceContainer mainTable = typeInfo.getInstanceContainers()
                                                          .get(0);
        List<FeatureStoreAttributesContainer> attributesContainers = mainTable.getAllAttributesContainers();

        Optional<String> metaQuery = featureQuery.hasIdFilter()
                ? Optional.empty()
                : Optional.of(queryGenerator.getMetaQuery(mainTable, featureQuery.getLimit(), featureQuery.getOffset(), featureQuery.getFilter(), computeNumberMatched));

        Source<SqlRow, NotUsed> sqlRowSource = getMetaResult(metaQuery).flatMapConcat(metaResult -> {

            //TODO: exception when only one table??? (test with adjusted daraa)
            int[] i = {0};
            Source<SqlRow, NotUsed>[] sqlRows = queryGenerator.getInstanceQueries(mainTable, featureQuery.getFilter(), metaResult.getMinKey(), metaResult.getMaxKey())
                                                              .map(query -> {
                                                                  if (LOGGER.isTraceEnabled()) {
                                                                      LOGGER.trace("Values query: {}", query);
                                                                  }

                                                                  return sqlConnector.getSourceStream(query, ImmutableSqlQueryOptions.builder()
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
    private Source<SqlRowMeta, NotUsed> getMetaResult(Optional<String> metaQuery) {
        if (!metaQuery.isPresent()) {
            return Source.single(getMetaQueryResult(0L, 0L, 0L, 0L));
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Meta query: {}", metaQuery.get());
        }

        return sqlConnector.getSourceStream(metaQuery.get(), SqlQueryOptions.withColumnTypes(Long.class, Long.class, Long.class, Long.class))
                           .map(sqlRow -> getMetaQueryResult(sqlRow.getValues()))
                           .recover(new PFBuilder<Throwable, SqlRowMeta>().matchAny(throwable -> getMetaQueryResult(0L, 0L, 0L, 0L))
                                                                          .build());
    }

    private SqlRowMeta getMetaQueryResult(Long minKey, Long maxKey, Long numberReturned, Long numberMatched) {
        return ImmutableSqlRowMeta.builder()
                                  .minKey(Objects.nonNull(minKey) ? minKey : 0L)
                                  .maxKey(Objects.nonNull(maxKey) ? maxKey : 0L)
                                  .numberReturned(Objects.nonNull(numberReturned) ? numberReturned : 0L)
                                  .numberMatched(Objects.nonNull(numberMatched) && numberMatched > -1 ? OptionalLong.of(numberMatched) : OptionalLong.empty())
                                  .build();
    }

    private SqlRowMeta getMetaQueryResult(List<Object> values) {
        return getMetaQueryResult((Long) values.get(0), (Long) values.get(1), (Long) values.get(2), (Long) values.get(3));
    }

    private static <T extends Comparable<T>> Source<T, NotUsed> mergeAndSort(Source<T, NotUsed>... sources) {
        return mergeAndSort(sources[0], sources[1], Arrays.asList(sources)
                                                          .subList(2, sources.length));
    }

    private static <T extends Comparable<T>> Source<T, NotUsed> mergeAndSort(Source<T, NotUsed> source1,
                                                                             Source<T, NotUsed> source2,
                                                                             Iterable<Source<T, NotUsed>> rest) {
        Comparator<T> comparator = Comparator.naturalOrder();
        Source<T, NotUsed> mergedAndSorted = source1.mergeSorted(source2, comparator);
        for (Source<T, NotUsed> source3 : rest) {
            mergedAndSorted = mergedAndSorted.mergeSorted(source3, comparator);
        }
        return mergedAndSorted;
    }*/

    //TODO: rest of code in this class, so mainly the query multiplexing, goes to SqlConnector
    //TODO: should merge QueryTransformer with QueryGenerator
    @Override
    public SqlQueries transformQuery(FeatureQuery featureQuery,
                                     Map<String, String> additionalQueryParameters) {
        //TODO: either pass as parameter, or check for null here
        FeatureStoreTypeInfo typeInfo = typeInfos.get(featureQuery.getType());

        //TODO: implement for multiple main tables
        FeatureStoreInstanceContainer mainTable = typeInfo.getInstanceContainers()
                                                          .get(0);

        Optional<String> metaQuery = featureQuery.hasIdFilter()
                ? Optional.empty()
                : Optional.of(queryGenerator.getMetaQuery(mainTable, featureQuery.getLimit(), featureQuery.getOffset(), featureQuery.getFilter(), computeNumberMatched));

        Function<SqlRowMeta, Stream<String>> valueQueries = metaResult -> queryGenerator.getInstanceQueries(mainTable, featureQuery.getFilter(), metaResult.getMinKey(), metaResult.getMaxKey());

        return ImmutableSqlQueries.builder()
                                  .metaQuery(metaQuery)
                                  .valueQueries(valueQueries)
                                  .instanceContainers(typeInfo.getInstanceContainers())
                                  .build();
    }
}
