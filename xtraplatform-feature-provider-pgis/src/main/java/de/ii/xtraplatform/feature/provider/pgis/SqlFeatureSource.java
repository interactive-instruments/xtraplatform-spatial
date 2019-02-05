/**
 * Copyright 2018 interactive instruments GmbH
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
import de.ii.xtraplatform.feature.provider.api.FeatureConsumer;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
import de.ii.xtraplatform.feature.provider.api.ImmutableFeatureQuery;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import org.geotools.filter.text.cql2.CQLException;
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
    private final FeatureQueryEncoderSql queryEncoder;
    private final boolean numberMatched;

    public SqlFeatureSource(SlickSession session, SqlFeatureQueries queries, ActorMaterializer materializer, boolean numberMatched, FeatureTypeMapping mappings) {
        this.session = session;
        this.queries = queries;
        this.materializer = materializer;
        this.queryEncoder = new FeatureQueryEncoderSql(queries, mappings);
        this.numberMatched = numberMatched;
    }

    public CompletionStage<Done> runQuery(FeatureQuery query, FeatureConsumer consumer) {
        boolean[] meta = {false};
        boolean[] started = {false};
        boolean[] opened = {false};
        String[] currentId = {null};
        int[] rowCount = {0};
        final Map<Integer, List<String>> previousPath = Maps.newHashMap(ImmutableMap.of(0, new ArrayList<>()));
        //Map<String, List<Integer>> previousMultiplicities = new LinkedHashMap<>();
        //Map<String, Set<String>> children = new LinkedHashMap<>();

        //TODO
        String mainTable = query.getType();
        List<String> mainTablePath = ImmutableList.of(mainTable);
        //Map<String, List<Integer>> multiplicities = new HashMap<>();
        SqlMultiplicityTracker multiplicityTracker = new SqlMultiplicityTracker(queries.getMultiTables());

        return createRowStream(query)
                .runForeach(slickRowInfo -> {
                    if (slickRowInfo.getPath()
                                    .size() >= 3 && queries.getMultiTables()
                                                           .contains(slickRowInfo.getName())) {

                        boolean same = slickRowInfo.getPath()
                                                   .equals(previousPath.get(0));

                        multiplicityTracker.track(slickRowInfo.getPath(), slickRowInfo.getIds());

                        /*if (same) {
                            List<Integer> multi = multiplicities.get(slickRowInfo.getName());
                            multi.set(multi.size() - 1, multi.get(multi.size() - 1) + 1);
                            multiplicities.put(slickRowInfo.getName(), multi);

                            String table = slickRowInfo.getPath()
                                                       .get(slickRowInfo.getPath()
                                                                        .size() - 1)
                                                       .substring(slickRowInfo.getPath()
                                                                              .get(slickRowInfo.getPath()
                                                                                               .size() - 1)
                                                                              .indexOf("]") + 1);
                            children.getOrDefault(table, new HashSet<>())
                                    .forEach(c -> {
                                        List<Integer> multi2 = multiplicities.get(c);
                                        if (multi2 != null) {
                                            multi2.set(multi2.size() - 1, 0);
                                            multiplicities.put(c, multi2);
                                        }
                                    });
                        } else {
                            //TODO
                            List<Integer> multi = new ArrayList<>();
                            List<String> parentTables = new ArrayList<>();
                            String child = null;
                            boolean increased = false;
                            int increasedMultiplicity = 0;
                            for (int i = 0; i < slickRowInfo.getPath()
                                                            .size(); i++) {
                                String table = slickRowInfo.getPath()
                                                           .get(i)
                                                           .substring(slickRowInfo.getPath()
                                                                                  .get(i)
                                                                                  .indexOf("]") + 1);
                                if (queries.getMultiTables()
                                           .contains(table)) {
                                    if (multiplicities.containsKey(table)) {

                                        //path is not the same as last, but was seen before



                                        if (i == slickRowInfo.getPath()
                                                             .size() - 1) {
                                            int m = multiplicities.get(table)
                                                                  .get(multiplicities.get(table)
                                                                                     .size() - 1);

                                            int mp = increased ? multiplicities.get(table)
                                                                               .get(increasedMultiplicity) : -1;



                                            if (!increased || mp < multi.get(increasedMultiplicity)) {
                                                m++;
                                                LOGGER.debug("CURR_MULTI_INC {} {}", table, m);
                                            } else {
                                                int m2 = multi.remove(increasedMultiplicity);
                                                multi.add(increasedMultiplicity, m2 + 1);
                                                LOGGER.debug("PREV_MULTI_INC {} {}", increasedMultiplicity, m2);
                                                if (m==0) m = 1;
                                            }
                                            increased = false;
                                            increasedMultiplicity = -1;

                                            multi.add(m);
                                            child = table;

                                            children.getOrDefault(table, new HashSet<>())
                                                    .forEach(c -> {
                                                        List<Integer> multi2 = multiplicities.get(c);
                                                        if (multi2 != null) {
                                                            multi2.set(multi2.size() - 1, 0);
                                                            multiplicities.put(c, multi2);
                                                        }
                                                    });
                                        } else {
                                            int m = multiplicities.get(table)
                                                                  .get(multiplicities.get(table)
                                                                                     .size() - 1);
                                            if (i == previousPath.get(0).size()-1 && previousPath.get(0).get(i).equals(slickRowInfo.getPath().get(i))) {
                                                //m++;
                                                increased = true;
                                                //LOGGER.debug("PREV_MULTI_INC {} {}", table, m);
                                                increasedMultiplicity = multi.size();
                                            }

                                            multi.add(m);
                                            parentTables.add(table);
                                        }
                                    } else {
                                        multi.add(1);
                                        if (i == slickRowInfo.getPath()
                                                             .size() - 1) {
                                            child = table;
                                        } else {
                                            parentTables.add(table);
                                        }
                                    }
                                }
                            }
                            multiplicities.put(slickRowInfo.getName(), multi);

                            if (child != null) {
                                for (String parent : parentTables) {
                                    children.putIfAbsent(parent, new HashSet<>());
                                    children.get(parent)
                                            .add(child);
                                }
                            }
                        }*/
                    }
                    //LOGGER.debug("MULTI {}", multiplicities.get(slickRowInfo.getName()));
                    LOGGER.debug("MULTI2 {}", multiplicityTracker.getMultiplicitiesForPath(slickRowInfo.getPath()));

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

                        consumer.onStart(numberReturned, numberMatched);
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
                        consumer.onFeatureStart(mainTablePath);
                        opened[0] = true;
                    }

                    Optional<ColumnValueInfo> columnValueInfo = slickRowInfo.next();
                    while (columnValueInfo.isPresent()) {
                        if (Objects.nonNull(columnValueInfo.get().value)) {
                            consumer.onPropertyStart(columnValueInfo.get().path, multiplicityTracker.getMultiplicitiesForPath(slickRowInfo.getPath()));
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
                            consumer.onStart(OptionalLong.of(0), OptionalLong.empty());
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

    private Source<SlickRowCustom, NotUsed> createRowStream(FeatureQuery request) {
        // TODO: if limit and or offset, get min max ids: SELECT MIN(SKEY) AS col1, MAX(SKEY) AS col2 FROM (SELECT fundorttiere.id AS SKEY FROM fundorttiere ORDER BY 1 LIMIT 3) AS A
        // TODO: then add or append to whereClause for all queries: WHERE (fundorttiere.id >= col1 AND fundorttiere.id <= col2)

        // TODO: for filter against distant field, add where clause to min max subquery: value IN (SELECT geom.geom FROM geom WHERE geom.id=fundorttiere.id)
        // TODO: for distant geometry: fundorttiere.id IN (SELECT geom.id FROM geom WHERE st_intersects(geom.geom, st_geometry('POLYGON...',srid)) = 'TRUE' )


        // TODO: numberReturned query: SELECT count(*) FROM subquery as above, can be combined I guess
        // TODO: numberMatched query: SELECT count(*) FROM subquery as above, but without limit and offset

        FeatureQuery mainQuery = null;
        FeatureQuery subQuery = null;
        String subFilter = null;
        long count = 0;
        long count2 = 0;

        if (!Strings.isNullOrEmpty(request.getFilter())) {
            if (!isIdFilter(request.getFilter())) {
                try {
                    String filter = queryEncoder.encodeFilter(request.getFilter());

                    FeatureQuery request2 = ImmutableFeatureQuery.builder()
                                                                 .type(request.getType())
                                                                 .limit(request.getLimit())
                                                                 .offset(request.getOffset())
                                                                 .filter(filter)
                                                                 .build();

                    //TODO can be min or max or both
                    String query = createMetaQuery(request2);
                    LOGGER.debug("META QUERY {}", query);
                    final long[] minKey = new long[1];
                    final long[] maxKey = new long[1];
                    final long[] numberReturned = new long[1];
                    final long[] numberMatched = new long[1];
                    Slick.source(session, query, slickRow -> {
                        minKey[0] = slickRow.nextLong();
                        maxKey[0] = slickRow.nextLong();
                        numberReturned[0] = slickRow.nextLong();
                        numberMatched[0] = slickRow.nextLong();
                        return slickRow;
                    })
                         .runWith(Sink.ignore(), materializer)
                         .exceptionally(throwable -> {
                             minKey[0] = 0;
                             maxKey[0] = 0;
                             numberReturned[0] = 0;
                             numberMatched[0] = 0;
                             return Done.getInstance();
                         })
                         .toCompletableFuture()
                         .join();
                    LOGGER.debug("META INFO {} {} {} {}", minKey[0], maxKey[0], numberReturned[0], numberMatched[0]);

                    count = numberReturned[0];
                    count2 = numberMatched[0];
                    subFilter = createSubFilter(request, queries.getMainQuery()
                                                                .getSortColumn(), OptionalLong.of(minKey[0]), OptionalLong.of(maxKey[0])) + " AND " + filter;


                    LOGGER.debug("META CLAUSE {}", subFilter);

                    mainQuery = ImmutableFeatureQuery.builder()
                                                     .type(request.getType())
                                                     .limit(request.getLimit())
                                                     .offset(request.getOffset())
                                                     .filter(filter)
                                                     .build();

                    subQuery = ImmutableFeatureQuery.builder()
                                                    .type(request.getType())
                                                    .filter(subFilter)
                                                    .build();
                } catch (CQLException e) {
                    throw new RuntimeException(e);
                }
            } else {
                mainQuery = subQuery = request;
            }
        } else if (request.getLimit() > 0 || request.getOffset() > 0) {
            //TODO can be min or max or both
            String query = createMetaQuery(request);
            LOGGER.debug("META QUERY {}", query);
            final long[] minKey = new long[1];
            final long[] maxKey = new long[1];
            final long[] numberReturned = new long[1];
            final long[] numberMatched = new long[1];
            Slick.source(session, query, slickRow -> {
                minKey[0] = slickRow.nextLong();
                maxKey[0] = slickRow.nextLong();
                numberReturned[0] = slickRow.nextLong();
                numberMatched[0] = slickRow.nextLong();
                return slickRow;
            })
                 .runWith(Sink.ignore(), materializer)
                 .exceptionally(throwable -> {
                     minKey[0] = 0;
                     maxKey[0] = 0;
                     numberReturned[0] = 0;
                     numberMatched[0] = 0;
                     return Done.getInstance();
                 })
                 .toCompletableFuture()
                 .join();
            LOGGER.debug("META INFO {} {} {} {}", minKey[0], maxKey[0], numberReturned[0], numberMatched[0]);

            count = numberReturned[0];
            count2 = numberMatched[0];
            subFilter = createSubFilter(request, queries.getMainQuery()
                                                        .getSortColumn(), OptionalLong.of(minKey[0]), OptionalLong.of(maxKey[0]));


            LOGGER.debug("META CLAUSE {}", subFilter);

            mainQuery = subQuery = ImmutableFeatureQuery.builder()
                                                        .type(request.getType())
                                                        .filter(subFilter)
                                                        .build();
        } else {
            mainQuery = subQuery = request;
        }

        if (request.hitsOnly()) {
            return Source.single(new SlickRowMeta(count2));
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
            int[] i = {0};
            Source<SlickRowCustom, NotUsed>[] slickRows = queries.getQueries()
                                                                 .stream()
                                                                 .map(query -> Slick.source(session, query.equals(queries.getMainQuery()) ? query.toSql(finalMainQuery.getFilter(), finalMainQuery.getLimit(), finalMainQuery.getOffset()) : query.toSql(finalSubQuery.getFilter(), finalSubQuery.getLimit(), finalSubQuery.getOffset()), toSlickRowInfo(query, i[0]++)))
                                                                 .toArray((IntFunction<Source<SlickRowCustom, NotUsed>[]>) Source[]::new);

            int mainQueryIndex = queries.getQueries()
                                        .indexOf(queries.getMainQuery());

            return mergeAndSort(slickRows)
                    .prepend(Source.single(new SlickRowMeta(count, count2)));

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
                 .getType() == SqlPathTree.TYPE.ID_M_N) {
                //all.remove(i);
                //dependencies.put(i, new ArrayList<>());
                parents.putIfAbsent(q.getSqlPath()
                                     .getPath(), i);
            }
            if (q.getSqlPathParent()
                 .isPresent() && q.getSqlPathParent()
                                  .get()
                                  .getType() == SqlPathTree.TYPE.ID_M_N) {
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

    private String createMetaQuery(FeatureQuery request) {
        String limit2 = request.getLimit() > 0 ? " LIMIT " + request.getLimit() : "";
        String offset2 = request.getOffset() > 0 ? " OFFSET " + request.getOffset() : "";
        String where = !Strings.isNullOrEmpty(request.getFilter()) ? " WHERE " + request.getFilter() : "";

        String nr = String.format("SELECT MIN(SKEY) AS col1, MAX(SKEY) AS col2, count(*) AS col3 FROM (SELECT %2$s AS SKEY FROM %1$s%5$s ORDER BY 1%3$s%4$s) AS A", queries.getMainQuery()
                                                                                                                                                                           .getTableName(), queries.getMainQuery()
                                                                                                                                                                                                   .getSortColumn(), limit2, offset2, where);

        if (!numberMatched) {
            return String.format("SELECT *,-1 FROM (%s) AS B", nr);
        } else {
            String nm = String.format("SELECT count(*) AS col4 FROM (SELECT %2$s AS SKEY FROM %1$s%3$s ORDER BY 1) AS B", queries.getMainQuery()
                                                                                                                                 .getTableName(), queries.getMainQuery()
                                                                                                                                                         .getSortColumn(), where);
            return String.format("SELECT * FROM (%s) AS C, (%s) AS D", nr, nm);
        }
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
            //int size = Math.min(ids.size(), row.ids.size());
            //if (ids.size() != row.ids.size() && size > 1)
            //    size = size -1;
            int result = compareIdLists(ids.subList(0, size), row.ids.subList(0, size));


            return result == 0 ? priority - row.priority : result;
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
            LOGGER.debug("NEXT: {} {} {} {}", paths.get(columnCount), ids, columnCount, columns.get(columnCount));
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
