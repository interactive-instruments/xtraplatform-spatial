package de.ii.xtraplatform.feature.provider.sql.app;

import akka.NotUsed;
import akka.japi.pf.PFBuilder;
import akka.stream.javadsl.Source;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreAttributesContainer;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableFeatureStoreAttribute;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableFeatureStoreInstanceContainer;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableMetaQueryResult;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableSqlQueryOptions;
import de.ii.xtraplatform.feature.provider.sql.domain.MetaQueryResult;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlConnector;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRow;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRowMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;

class FeatureReaderSql {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureReaderSql.class);

    private final SqlConnector sqlConnector;
    private final FeatureStoreQueryGeneratorSql queryGenerator;
    private final boolean computeNumberMatched;

    FeatureReaderSql(SqlConnector sqlConnector,
                     FeatureStoreQueryGeneratorSql queryGenerator, boolean computeNumberMatched) {
        this.sqlConnector = sqlConnector;
        this.queryGenerator = queryGenerator;
        this.computeNumberMatched = computeNumberMatched;
    }

    //TODO: reuse instances of SqlRow, SqlColumn? (object pool, e.g. https://github.com/chrisvest/stormpot, implement test with 100000 rows, measure)
    public Source<SqlRow, NotUsed> getSqlRowStream(FeatureQuery featureQuery, FeatureStoreTypeInfo typeInfo) {
        //TODO: implement for multiple main tables
        FeatureStoreInstanceContainer mainTable = typeInfo.getInstanceContainers()
                                                          .get(0);
        List<FeatureStoreAttributesContainer> attributesContainers = mainTable.getAllAttributesContainers();

        Optional<String> metaQuery = featureQuery.hasIdFilter()
                ? Optional.empty()
                : Optional.of(queryGenerator.getMetaQuery(mainTable, featureQuery.getLimit(), featureQuery.getOffset(), featureQuery.getFilter(), computeNumberMatched));

        Source<SqlRow, NotUsed> sqlRowSource = getMetaRow(metaQuery).flatMapConcat(metaQueryResult -> {

            //TODO: exception when only one table??? (test with adjusted daraa)
            int[] i = {0};
            Source<SqlRow, NotUsed>[] sqlRows = queryGenerator.getInstanceQueries(mainTable, featureQuery.getFilter(), metaQueryResult.getMinKey(), metaQueryResult.getMaxKey())
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
                    //TODO: merge MetaQueryResult and SqlRowMeta, prepend self
                    .prepend(Source.single(new SqlRowMeta(metaQueryResult.getNumberReturned(), metaQueryResult.getNumberMatched())));
        });

        return sqlRowSource;
    }

    //TODO: simplify
    private Source<MetaQueryResult, NotUsed> getMetaRow(Optional<String> metaQuery) {
        if (!metaQuery.isPresent()) {
            return Source.single(getMetaQueryResult(0, 0, 0, 0));
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Meta query: {}", metaQuery.get());
        }

        FeatureStoreInstanceContainer metaContainer = ImmutableFeatureStoreInstanceContainer.builder()
                                                                                            .name("meta")
                                                                                            .sortKey("")
                                                                                            .addAttributes(
                                                                                                    ImmutableFeatureStoreAttribute.builder()
                                                                                                                                  .name("minKey")
                                                                                                                                  .build(),
                                                                                                    ImmutableFeatureStoreAttribute.builder()
                                                                                                                                  .name("maxKey")
                                                                                                                                  .build(),
                                                                                                    ImmutableFeatureStoreAttribute.builder()
                                                                                                                                  .name("numberReturned")
                                                                                                                                  .build(),
                                                                                                    ImmutableFeatureStoreAttribute.builder()
                                                                                                                                  .name("numberMatched")
                                                                                                                                  .build()
                                                                                            )
                                                                                            .build();

        return sqlConnector.getSourceStream(metaQuery.get(), ImmutableSqlQueryOptions.builder()
                                                                                     .attributesContainer(metaContainer)
                                                                                     .containerPriority(0)
                                                                                     .build())
                           .map(sqlRow -> getMetaQueryResult(sqlRow.next()
                                                                   .map(SqlRow.SqlColumn::getValue)
                                                                   .map(Long::parseLong)
                                                                   .orElse(0L), sqlRow.next()
                                                                                      .map(SqlRow.SqlColumn::getValue)
                                                                                      .map(Long::parseLong)
                                                                                      .orElse(0L), sqlRow.next()
                                                                                                         .map(SqlRow.SqlColumn::getValue)
                                                                                                         .map(Long::parseLong)
                                                                                                         .orElse(0L), sqlRow.next()
                                                                                                                            .map(SqlRow.SqlColumn::getValue)
                                                                                                                            .map(Long::parseLong)
                                                                                                                            .orElse(0L)))
                           .recover(new PFBuilder<Throwable, MetaQueryResult>().matchAny(throwable -> getMetaQueryResult(0, 0, 0, 0))
                                                                               .build());
    }

    private MetaQueryResult getMetaQueryResult(long minKey, long maxKey, long numberReturned, long numberMatched) {
        return ImmutableMetaQueryResult.builder()
                                       .minKey(minKey)
                                       .maxKey(maxKey)
                                       .numberReturned(numberReturned)
                                       .numberMatched(numberMatched)
                                       .build();
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
    }
}
