/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.pgis;

import akka.Done;
import akka.NotUsed;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.alpakka.slick.javadsl.Slick;
import akka.stream.alpakka.slick.javadsl.SlickRow;
import akka.stream.alpakka.slick.javadsl.SlickSession;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.ii.xtraplatform.feature.provider.sql.SQL_PATH_TYPE_DEPRECATED;
import de.ii.xtraplatform.feature.provider.sql.app.FeatureStoreQueryGeneratorSql;
import de.ii.xtraplatform.feature.provider.sql.app.FilterEncoderSqlImpl;
import de.ii.xtraplatform.feature.provider.sql.app.SqlMultiplicityTracker;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableMetaQueryResult;
import de.ii.xtraplatform.feature.provider.sql.domain.MetaQueryResult;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import de.ii.xtraplatform.features.domain.FeatureConsumer;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * @author zahnen
 */
public class SqlFeatureSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlFeatureSource.class);

    private final SlickSession session;
    private final SqlFeatureQueries queries;
    private final ActorMaterializer materializer;
    private final FilterEncoderSqlImpl queryEncoder;
    private final boolean computeNumberMatched;
    private final FeatureStoreQueryGeneratorSql queryGeneratorSql;

    public SqlFeatureSource(SlickSession session, SqlFeatureQueries queries, ActorMaterializer materializer, boolean computeNumberMatched, FeatureTypeMapping mappings) {
        this.session = session;
        this.queries = queries;
        this.materializer = materializer;
        this.queryEncoder = new FilterEncoderSqlImpl(queries.getMainQuery(), mappings);
        this.computeNumberMatched = computeNumberMatched;
        this.queryGeneratorSql = new FeatureStoreQueryGeneratorSql(null, null);
    }

    public CompletionStage<Done> runQuery(FeatureQuery query, FeatureConsumer consumer) {
        boolean[] meta = {false};
        boolean[] started = {false};
        boolean[] opened = {false};
        String[] currentId = {null};
        int[] rowCount = {0};
        final Map<Integer, List<String>> previousPath = Maps.newHashMap(ImmutableMap.of(0, new ArrayList<>()));

        //TODO
        String mainTable = query.getType();
        List<String> mainTablePath = ImmutableList.of(mainTable);

        SqlMultiplicityTracker multiplicityTracker = new SqlMultiplicityTracker(ImmutableList.copyOf(queries.getMultiTables()));

        return createRowStream(query)
                .runForeach(slickRowInfo -> {
                    if (slickRowInfo.getPath()
                                    .size() >= 3 && queries.getMultiTables()
                                                           .contains(slickRowInfo.getName())) {

                        boolean same = slickRowInfo.getPath()
                                                   .equals(previousPath.get(0));

                        multiplicityTracker.track(slickRowInfo.getPath(), slickRowInfo.getIds().stream().map(s -> Long.parseLong(s)).collect(Collectors.toList()));


                    }

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Sql row: {}", slickRowInfo);
                    }

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("MULTI2 {}", multiplicityTracker.getMultiplicitiesForPath(slickRowInfo.getPath()));
                    }



                    if (!slickRowInfo.getName()
                                     .equals("META")) {
                        rowCount[0]++;
                    }

                    if (slickRowInfo.getName()
                                    .equals("META")) {


                        Optional<String> count = slickRowInfo.next()
                                                             .map(c -> c.value);

                        OptionalLong numberReturned = count.isPresent() ? OptionalLong.of(Long.valueOf(count.get())) : OptionalLong.empty();

                        Optional<String> count2 = slickRowInfo.next()
                                                              .map(c -> c.value);
                        OptionalLong numberMatched = count2.isPresent() && !count2.get()
                                                                                  .equals("-1") ? OptionalLong.of(Long.valueOf(count2.get())) : OptionalLong.empty();

                        consumer.onStart(numberReturned, numberMatched, ImmutableMap.of());
                        meta[0] = true;
                    } else if (started[0] && opened[0] && !Objects.equals(slickRowInfo.getIds()
                                                                                      .get(0), currentId[0])) {
                        consumer.onFeatureEnd(mainTablePath);
                        opened[0] = false;
                        multiplicityTracker.reset();
                        previousPath.put(0, new ArrayList<>());
                    } else if (!started[0]) {
                        started[0] = true;
                    }
                    if (!Objects.equals(slickRowInfo.getIds()
                                                    .get(0), currentId[0])) {
                        consumer.onFeatureStart(mainTablePath, ImmutableMap.of());
                        opened[0] = true;
                    }

                    Optional<ColumnValueInfo> columnValueInfo = slickRowInfo.next();
                    while (columnValueInfo.isPresent()) {
                        if (Objects.nonNull(columnValueInfo.get().value)) {
                            consumer.onPropertyStart(columnValueInfo.get().path, multiplicityTracker.getMultiplicitiesForPath(slickRowInfo.getPath()), ImmutableMap.of());
                            consumer.onPropertyText(columnValueInfo.get().value);
                            consumer.onPropertyEnd(columnValueInfo.get().path);
                        }
                        columnValueInfo = slickRowInfo.next();
                    }

                    if (Objects.equals(slickRowInfo.getIds()
                                                   .get(0), currentId[0])) {

                        previousPath.put(0, slickRowInfo.getPath());
                    }
                    currentId[0] = slickRowInfo.getIds()
                                               .get(0);

                }, materializer)
                .exceptionally(throwable -> {
                    if (throwable instanceof WebApplicationException) {
                        throw (WebApplicationException) throwable;
                    }
                    LOGGER.error(throwable.getMessage());
                    LOGGER.debug("STREAM FAILURE", throwable);

                    if (isIdFilter(query.getFilter())) {
                        throw new InternalServerErrorException();
                    }

                    try {
                        if (!meta[0]) {
                            consumer.onStart(OptionalLong.of(0), OptionalLong.empty(), ImmutableMap.of());
                        }
                    } catch (Exception e) {
                        //ignore
                    }
                    return Done.getInstance();
                })
                .whenComplete((done, throwable) -> {
                    if (isIdFilter(query.getFilter()) && rowCount[0] == 0) {
                        throw new NotFoundException();
                    }

                    try {
                        if (opened[0]) {
                            consumer.onFeatureEnd(mainTablePath);
                        }
                        consumer.onEnd();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private MetaQueryResult runMetaQuery(String metaQuery) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("META QUERY: {}", metaQuery);
        }

        final ImmutableMetaQueryResult.Builder resultBuilder = ImmutableMetaQueryResult.builder();

        Slick.source(session, metaQuery, slickRow -> {
            resultBuilder.minKey(slickRow.nextLong()).maxKey(slickRow.nextLong()).numberReturned(slickRow.nextLong()).numberMatched(slickRow.nextLong());
            return slickRow;
        })
             .runWith(Sink.ignore(), materializer)
             .exceptionally(throwable -> {
                 resultBuilder.minKey(0).maxKey(0).numberReturned(0).numberMatched(0);
                 return Done.getInstance();
             })
             .toCompletableFuture()
             .join();

        ImmutableMetaQueryResult metaQueryResult = resultBuilder.build();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("META QUERY RESULT: {}", metaQueryResult);
        }

        return metaQueryResult;
    }

    private Source<SlickRowCustom, NotUsed> createRowStream(FeatureQuery request) {
        // TODO: if limit and or offset, get min max ids: SELECT MIN(SKEY) AS col1, MAX(SKEY) AS col2 FROM (SELECT fundorttiere.id AS SKEY FROM fundorttiere ORDER BY 1 LIMIT 3) AS A
        // TODO: then add or append to whereClause for all queries: WHERE (fundorttiere.id >= col1 AND fundorttiere.id <= col2)

        // TODO: for filter against distant field, add where clause to min max subquery: value IN (SELECT geom.geom FROM geom WHERE geom.id=fundorttiere.id)
        // TODO: for distant geometry: fundorttiere.id IN (SELECT geom.id FROM geom WHERE st_intersects(geom.geom, st_geometry('POLYGON...',srid)) = 'TRUE' )


        // TODO: numberReturned query: SELECT count(*) FROM subquery as above, can be combined I guess
        // TODO: numberMatched query: SELECT count(*) FROM subquery as above, but without limit and offset

        FeatureQuery mainQuery = null;
        FeatureQuery subQuery = null;
        String mainFilter = null;
        String subFilter = null;
        long numberReturned = 0;
        long numberMatched = 0;

        boolean hasFilter = !Strings.isNullOrEmpty(request.getFilter());
        boolean hasIdFilter = hasFilter && isIdFilter(request.getFilter());
        boolean hasConstraints = hasFilter || request.getLimit() > 0 || request.getOffset() > 0;

        if (hasFilter) {
            mainFilter = queryEncoder.encode(request.getFilter(), null);

            mainQuery = ImmutableFeatureQuery.builder()
                                             .type(request.getType())
                                             .limit(request.getLimit())
                                             .offset(request.getOffset())
                                             .filter(mainFilter)
                                             .build();
        }

        if (hasIdFilter) {
            subQuery = mainQuery;
        } else if (hasConstraints) {
            mainQuery = request;
            //TODO can be min or max or both
            String metaQuery = queryGeneratorSql.getMetaQuery(queries.getMainQuery(), mainQuery.getLimit(), mainQuery.getOffset(), mainQuery.getFilter(), computeNumberMatched);
            MetaQueryResult metaQueryResult = runMetaQuery(metaQuery);

            numberReturned = metaQueryResult.getNumberReturned();
            numberMatched = metaQueryResult.getNumberMatched();
            subFilter = createSubFilter(request, queries.getMainQuery()
                                                        .getSortColumn(), OptionalLong.of(metaQueryResult.getMinKey()), OptionalLong.of(metaQueryResult.getMaxKey()));
            if (hasFilter) {
                subFilter += " AND " + mainFilter;
            }


            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("META CLAUSE {}", subFilter);
            }

            subQuery = ImmutableFeatureQuery.builder()
                                            .type(request.getType())
                                            .filter(subFilter)
                                            .build();

            if (!hasFilter) {
                mainQuery = subQuery;
            }

        } else {
            mainQuery = subQuery = request;
        }

        if (request.hitsOnly()) {
            return Source.single(new SlickRowMeta(numberMatched));
        } else {
            FeatureQuery finalSubQuery = subQuery;
            FeatureQuery finalMainQuery = mainQuery;

            Map<Integer, List<Integer>> dependencies = getDependencies(queries); /*ImmutableMap.of(
                    9, ImmutableList.of(0, 1, 2, 4, 5, 6, 7, 8),
                    2, ImmutableList.of(3)
            );*/

            Map<Integer, Predicate<Pair<SlickRowInfo, SlickRowInfo>>> matchers = getMatchers(dependencies); /*ImmutableMap.of(
                    9, matchRows(),
                    2, matchRows()
            );*/

            List<String> collect = queries.getQueries()
                                          .stream()
                                          .map(query -> query.toSql(finalSubQuery.getFilter(), finalSubQuery.getLimit(), finalSubQuery.getOffset()))
                                          .map(query -> String.format("'%s'", query))
                                          .collect(Collectors.toList());
            LOGGER.debug("QUERIES: {}", collect);

            int[] i = {0};
            Source<SlickRowCustom, NotUsed>[] slickRows = queries.getQueries()
                                                                 .stream()
                                                                 .map(query -> {
                                                                     String query2 = query.toSql(finalSubQuery.getFilter(), finalSubQuery.getLimit(), finalSubQuery.getOffset());
                                                                     if (LOGGER.isTraceEnabled()) {
                                                                         LOGGER.trace("Values query: {}", query2);
                                                                     }

                                                                     return Slick.source(session, query2, toSlickRowInfo(query, i[0]++));
                                                                 })
                                                                 .toArray((IntFunction<Source<SlickRowCustom, NotUsed>[]>) Source[]::new);

            int mainQueryIndex = queries.getQueries()
                                        .indexOf(queries.getMainQuery());

            return mergeAndSort(slickRows)
                    .prepend(Source.single(new SlickRowMeta(numberReturned, numberMatched)));

            //return MixAndMatch.create(dependencies, matchers, mainQueryIndex, slickRows)
            //                  .prepend(Source.single(new SlickRowMeta(count, count2)));
        }
    }

    static <T extends Comparable<T>> Source<T, NotUsed> mergeAndSort(Source<T, NotUsed>... sources) {
        return mergeAndSort(sources[0], sources[1], Arrays.asList(sources)
                                                          .subList(2, sources.length));
    }

    static <T extends Comparable<T>> Source<T, NotUsed> mergeAndSort(Source<T, NotUsed> source1, Source<T, NotUsed> source2, Iterable<Source<T, NotUsed>> rest) {
        Comparator<T> comparator = Comparator.naturalOrder();
        Source<T, NotUsed> mergedAndSorted = source1.mergeSorted(source2, comparator);
        for (Source<T, NotUsed> source3 : rest) {
            mergedAndSorted = mergedAndSorted.mergeSorted(source3, comparator);
        }
        return mergedAndSorted;
    }

    private Map<Integer, Predicate<Pair<SlickRowInfo, SlickRowInfo>>> getMatchers(Map<Integer, List<Integer>> dependencies) {
        return dependencies.entrySet()
                           .stream()
                           .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), matchRows()))
                           .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<Integer, List<Integer>> getDependencies(SqlFeatureQueries queries) {
        int mainQueryIndex = queries.getQueries()
                                    .indexOf(queries.getMainQuery());
        List<Integer> all = IntStream.range(0, queries.getQueries()
                                                      .size())
                                     .boxed()
                                     .collect(Collectors.toList());
        all.remove(mainQueryIndex);

        Map<Integer, List<Integer>> dependencies = new LinkedHashMap<>();
        Map<String, List<Integer>> pdependencies = new LinkedHashMap<>();
        Map<String, Integer> parents = new LinkedHashMap<>();
        dependencies.put(mainQueryIndex, all);

        List<SqlFeatureQuery> queries1 = queries.getQueries();
        for (int i = 0; i < queries1.size(); i++) {
            SqlFeatureQuery q = queries1.get(i);
            if (q.getSqlPath()
                 .getType() == SQL_PATH_TYPE_DEPRECATED.ID_M_N) {
                //all.remove(i);
                //dependencies.put(i, new ArrayList<>());
                parents.putIfAbsent(q.getSqlPath()
                                     .getPath(), i);
            }
            if (q.getSqlPathParent()
                 .isPresent() && q.getSqlPathParent()
                                  .get()
                                  .getType() == SQL_PATH_TYPE_DEPRECATED.ID_M_N) {
                pdependencies.putIfAbsent(q.getSqlPathParent()
                                           .get()
                                           .getPath(), new ArrayList<>());
                pdependencies.get(q.getSqlPathParent()
                                   .get()
                                   .getPath())
                             .add(i);
            }
        }

        pdependencies.keySet()
                     .forEach(p -> {
                         //TODO
                         if (p.equals("/[id=osirisobjekt_id]osirisobjekt_2_raumreferenz/[raumreferenz_id=id]raumreferenz"))
                             return;
                         Integer index = parents.get(p);
                         if (index != null) {
                             all.removeAll(pdependencies.get(p));
                             dependencies.putIfAbsent(index, new ArrayList<>());
                             dependencies.get(index)
                                         .addAll(pdependencies.get(p));
                         }
                     });

        return dependencies;
    }

    private String createSubFilter(FeatureQuery request, String keyField, OptionalLong minKey, OptionalLong maxKey) {
        String filter = "";
        if (minKey.isPresent()) {
            filter += keyField + " >= " + minKey.getAsLong();
        }
        if (minKey.isPresent() && maxKey.isPresent()) {
            filter += " AND ";
        }
        if (maxKey.isPresent()) {
            filter += keyField + " <= " + maxKey.getAsLong();
        }
        return "(" + filter + ")";
    }

    private boolean isIdFilter(String filter) {
        return Strings.nullToEmpty(filter)
                      .startsWith("IN ('");// TODO: matcher
    }

    private Predicate<Pair<SlickRowInfo, SlickRowInfo>> matchRows() {
        return rows -> {
            if (Objects.nonNull(rows.first()) && Objects.nonNull(rows.second())) {
                if (rows.second().ids.size() > rows.first().ids.size()) {
                    return rows.second().ids.subList(0, rows.first().ids.size())
                                            .equals(rows.first().ids);
                } else {
                    return rows.second().ids.subList(0, rows.first().ids.size() - 1)
                                            .equals(rows.first().ids.subList(0, rows.first().ids.size() - 1));
                }
            }
            return false;
        };
    }

    private Function<SlickRow, SlickRowInfo> toSlickRowInfo(SqlFeatureQuery query, int priority) {
        return slickRow -> new SlickRowInfo(slickRow, query, priority);
    }

    @Override
    public String toString() {
        return "SqlFeatureSource{" +
                "queries=" + queries +
                '}';
    }

    interface SlickRowCustom extends Comparable<SlickRowCustom> {
        Optional<ColumnValueInfo> next();

        default List<String> getIds() {
            return Lists.newArrayList((String) null);
        }

        String getName();

        default List<String> getPath() {
            return ImmutableList.of();
        }
    }

    static class SlickRowMeta implements SlickRowCustom {
        private final long count;
        private final long count2;
        private boolean done;
        private boolean done2;
        private boolean noNumberReturned;

        SlickRowMeta(long count, long count2) {
            this.count = count;
            this.count2 = count2;
        }

        SlickRowMeta(long count2) {
            this.noNumberReturned = true;
            this.count = 0;
            this.count2 = count2;
        }

        @Override
        public Optional<ColumnValueInfo> next() {
            if (!done) {
                this.done = true;
                return noNumberReturned ? Optional.empty() : Optional.of(new ColumnValueInfo(ImmutableList.of(), "numberReturned", Long.toString(count)));
            }
            if (!done2) {
                this.done2 = true;
                return Optional.of(new ColumnValueInfo(ImmutableList.of(), "numberMatched", Long.toString(count2)));
            }
            return Optional.empty();
        }

        @Override
        public String getName() {
            return "META";
        }

        @Override
        public int compareTo(SlickRowCustom slickRowCustom) {
            return 0;
        }
    }

    static class SlickRowInfo implements SlickRowCustom {
        //protected final String id;
        protected final String name;
        protected final List<String> path;
        protected final List<List<String>> paths;
        protected final List<String> columns;
        protected final List<String> values;
        protected final List<String> ids;
        protected final List<String> idNames;
        protected int columnCount = 0;
        protected final int priority;

        SlickRowInfo(SlickRow delegate, SqlFeatureQuery query, int priority) {
            //this.id = delegate.nextString();
            this.name = query.getTableName();
            this.paths = query.getMatchPaths();
            this.path = paths.get(0)
                             .subList(0, paths.get(0)
                                              .size() - 1);
            this.columns = query.getColumnNames();

            this.ids = IntStream.range(0, query.getSortFields()
                                               .size())
                                .mapToObj(i -> delegate.nextString())
                                .collect(Collectors.toList());

            this.idNames = query.getSortFields();

            this.values = IntStream.range(0, columns.size())
                                   .mapToObj(i -> delegate.nextString())
                                   .collect(Collectors.toList());
            this.priority = priority;
        }

        @Override
        public int compareTo(SlickRowCustom rowCustom) {
            SlickRowInfo row = (SlickRowInfo) rowCustom;
            int size = 0;
            for (int i = 0; i < idNames.size() && i < row.idNames.size(); i++) {
                if (!idNames.get(i)
                            .equals(row.idNames.get(i))) {
                    break;
                }
                size = i + 1;
            }

            int result2 = compareIdLists(ids.subList(0, size), row.ids.subList(0, size));
            int result = result2 == 0 ? priority - row.priority : result2;

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Compare: {}[{}{}] <=> {}[{}{}] -> {}({})", name, idNames, ids, row.name, row.idNames, row.ids, result, result2);
            }

            return result;
        }


        @Override
        public List<String> getIds() {
            return ids;
        }

        @Override
        public List<String> getPath() {
            return path;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Optional<ColumnValueInfo> next() {
            if (columnCount >= columns.size()) {
                return Optional.empty();
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("NEXT: {} {} {} {}", paths.get(columnCount), ids, columnCount, columns.get(columnCount));
            }
            return Optional.of(new ColumnValueInfo(paths.get(columnCount), columns.get(columnCount), values.get(columnCount++)));
        }

        @Override
        public String toString() {
            return "SlickRowInfo{" +
                    "id='" + ids + '\'' +
                    "name='" + name + '\'' +
                    ", values=" + values +
                    '}';
        }
    }

    static int compareIdLists(List<String> ids1, List<String> ids2) {
        for (int i = 0; i < ids1.size(); i++) {
            int result = Integer.valueOf(ids1.get(i))
                                .compareTo(Integer.valueOf(ids2.get(i)));
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    static class ColumnValueInfo {
        private final List<String> path;
        private final String name;
        private final String value;

        ColumnValueInfo(List<String> path, String name, String value) {
            this.path = path;
            this.name = name;
            this.value = value;
        }
    }
}
