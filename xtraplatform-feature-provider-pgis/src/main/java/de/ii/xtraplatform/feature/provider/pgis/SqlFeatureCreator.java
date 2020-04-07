/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.pgis;

import akka.NotUsed;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.alpakka.slick.javadsl.Slick;
import akka.stream.alpakka.slick.javadsl.SlickSession;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import de.ii.xtraplatform.feature.provider.sql.SlickSql;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
//@Value.Immutable
//@Value.Style(deepImmutablesDetection = true)
public class SqlFeatureCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlFeatureCreator.class);

    private final SlickSession session;
    private final ActorMaterializer materializer;
    private final SqlFeatureInserts inserts;
    private ListMultimap<String, List<Integer>> multiplicities;
    private NestedSqlInsertRow valueContainer;

    public SqlFeatureCreator(SlickSession session, ActorMaterializer materializer, SqlFeatureInserts inserts) {
        this.session = session;
        this.materializer = materializer;
        this.inserts = inserts;
        this.multiplicities = ArrayListMultimap.create();
        this.valueContainer = inserts.getValueContainer(ImmutableMap.of());
    }

    public void reset() {
        this.multiplicities = ArrayListMultimap.create();
        this.valueContainer = inserts.getValueContainer(ImmutableMap.of());
    }

    //protected abstract Map<String, String> getProperties();

    // Map of Inserts with Sub-Inserts
    //@Value.Derived
    //protected abstract SqlFeatureInserts getInserts();

    public CompletionStage<String> runQueries() {
        Map<String, List<Integer>> rows = computeCountsPerParentIndex(multiplicities);
        List<Function<NestedSqlInsertRow, Pair<String, Optional<Consumer<String>>>>> queries = inserts.getQueries(rows);


        /*List<Source<SlickRow, ?>> sources = queries.stream().limit(2)
                                                .map(queryFunction -> {
                                                    Pair<String, Optional<String>> query = queryFunction.apply(values);
                                          return Slick.source(session, query.first(), slickRow -> {
                                              LOGGER.debug("QUERY {}", query.first());
                                              if (query.second().isPresent()) {
                                                  String id = slickRow.nextString();
                                                  LOGGER.debug("RETURNED {} {}",query.second().get(), id);
                                                  values.put(query.second().get(), id);
                                                  LOGGER.debug("VALUES {}", values);
                                              }
                                              LOGGER.debug("");

                                              return slickRow;
                                          });
                                      })
                                                .collect(Collectors.toList());*/

        List<Function<NestedSqlInsertRow, String>> queryFunctions = queries.stream()
                                      .map(queryFunction -> (Function<NestedSqlInsertRow, String>) ctx -> queryFunction.apply(ctx).first())
                                      .collect(Collectors.toList());

        List<Optional<Consumer<String>>> idConsumers = queries.stream()
                                                              .map(queryFunction -> {
                                                                  //TODO
                                                                  Pair<String, Optional<Consumer<String>>> query = queryFunction.apply(valueContainer);
                                                                  return query.second();
                                                              })
                                                              .collect(Collectors.toList());

        int[] i = {0};
        BiFunction<SlickSql.SlickRow, String, String> mapper = (slickRow, previousId) -> {
            LOGGER.debug("QUERY {}", i[0]);
            // null not allowed as return value
            String id = "NONE";
            if (idConsumers.get(i[0])
                     .isPresent()) {
                id = slickRow.nextString();
                LOGGER.debug("RETURNED {}", id);
                idConsumers.get(i[0])
                     .get()
                     .accept(id);
            }
            //LOGGER.debug("VALUES {}", values);
            LOGGER.debug("");
            i[0]++;

            return previousId != null ? previousId : id;
        };

        return SlickSql.source(session, queryFunctions, mapper, valueContainer, materializer.system())
                .runWith(Sink.fold("", (id1, id2) -> id1.isEmpty() ? id2 : id1), materializer);

        /*return Source.from(queries)
                     .flatMapConcat(queryFunction -> {
                         //TODO
                         Pair<String, Optional<Consumer<String>>> query = queryFunction.apply(valueContainer);
                         return Slick.source(session, query.first(), slickRow -> {
                             LOGGER.debug("QUERY {}", query.first());
                             // null not allowed as return value
                             String id = "NONE";
                             if (query.second()
                                      .isPresent()) {
                                 id = slickRow.nextString();
                                 LOGGER.debug("RETURNED {}", id);
                                 query.second()
                                      .get()
                                      .accept(id);
                             }
                             //LOGGER.debug("VALUES {}", values);
                             LOGGER.debug("");

                             return id;
                         });
                     })
                     .runWith(Sink.fold("", (id1, id2) -> id1.isEmpty() ? id2 : id1), materializer);*/
    }

    public CompletionStage<String> runQueries(String id) {
        //TODO
        Optional<String> idPath = inserts.getSqlPaths()
                                         //TODO: find by type MAIN?
                                        .findChildEndsWith("/[id=id]osirisobjekt")
                                        .getColumnPaths()
                                        .stream()
                                         //TODO: mark as instance id?
                                        .filter(p -> p.endsWith("/id"))
                                        .findFirst();
        if (!idPath.isPresent()) {
            throw new IllegalStateException();
        }

        property(idPath.get(), id);

        Map<String, List<Integer>> rows = computeCountsPerParentIndex(multiplicities);
        List<Function<NestedSqlInsertRow, Pair<String, Optional<Consumer<String>>>>> queries = inserts.getQueries(rows);


        /*List<Source<SlickRow, ?>> sources = queries.stream().limit(2)
                                                .map(queryFunction -> {
                                                    Pair<String, Optional<String>> query = queryFunction.apply(values);
                                          return Slick.source(session, query.first(), slickRow -> {
                                              LOGGER.debug("QUERY {}", query.first());
                                              if (query.second().isPresent()) {
                                                  String id = slickRow.nextString();
                                                  LOGGER.debug("RETURNED {} {}",query.second().get(), id);
                                                  values.put(query.second().get(), id);
                                                  LOGGER.debug("VALUES {}", values);
                                              }
                                              LOGGER.debug("");

                                              return slickRow;
                                          });
                                      })
                                                .collect(Collectors.toList());*/

        List<Function<NestedSqlInsertRow, String>> queryFunctions = queries.stream()
                                                                           .map(queryFunction -> (Function<NestedSqlInsertRow, String>) ctx -> queryFunction.apply(ctx).first())
                                                                           .collect(Collectors.toList());

        List<Optional<Consumer<String>>> idConsumers = queries.stream()
                                                              .map(queryFunction -> {
                                                                  //TODO
                                                                  Pair<String, Optional<Consumer<String>>> query = queryFunction.apply(valueContainer);
                                                                  return query.second();
                                                              })
                                                              .collect(Collectors.toList());

        queryFunctions.add(0, v -> String.format("DELETE FROM osirisobjekt WHERE id=(SELECT id FROM %s WHERE id=%s)", inserts.getSqlPaths().getTableName(), id));
        //idConsumers.add(0, Optional.empty());


        int[] i = {0};
        BiFunction<SlickSql.SlickRow, String, String> mapper = (slickRow, previousId) -> {
            LOGGER.debug("QUERY {}", i[0]);
            // null not allowed as return value
            String id2 = "NONE";
            if (idConsumers.get(i[0])
                           .isPresent()) {
                id2 = slickRow.nextString();
                LOGGER.debug("RETURNED {}", id2);
                idConsumers.get(i[0])
                           .get()
                           .accept(id2);
            }
            //LOGGER.debug("VALUES {}", values);
            LOGGER.debug("");
            i[0]++;

            return previousId != null ? previousId : id2;
        };

        return SlickSql.source(session, queryFunctions, mapper, valueContainer, materializer.system())
                       .runWith(Sink.fold("", (id1, id2) -> id1.isEmpty() ? id2 : id1), materializer);

        /*return Source.from(queries)
                     .flatMapConcat(queryFunction -> {
                         //TODO
                         Pair<String, Optional<Consumer<String>>> query = queryFunction.apply(valueContainer);
                         return Slick.source(session, query.first(), slickRow -> {
                             LOGGER.debug("QUERY {}", query.first());
                             // null not allowed as return value
                             String id = "NONE";
                             if (query.second()
                                      .isPresent()) {
                                 id = slickRow.nextString();
                                 LOGGER.debug("RETURNED {}", id);
                                 query.second()
                                      .get()
                                      .accept(id);
                             }
                             //LOGGER.debug("VALUES {}", values);
                             LOGGER.debug("");

                             return id;
                         });
                     })
                     .runWith(Sink.fold("", (id1, id2) -> id1.isEmpty() ? id2 : id1), materializer);*/
    }

    //TODO: to builder???
    public void property(String path, String value) {
        // delegate to insert with longest matching paths
        //SqlFeatureQuery insert = getInserts().get(path.subList(0, path.size()-1));
        //insert.put(path.get(path.size()-1), value);
        //path.add(0, "");
        valueContainer.addValue(path, value);
    }

    // for multiplicities
    public void row(String path, List<Integer> multiplicities) {
        //rows.compute(path, (key, count) -> count == null ? 1 : count + 1);
        valueContainer.addRow(path);
        // TODO: increment, nested
        // TODO: do we need rows at all? we could inject valueContainer, they have the same lifetime
        this.multiplicities.put(path.substring(path.lastIndexOf("]") + 1), multiplicities);
        //this.multiplicities.compute(path.substring(path.lastIndexOf("]")+1), (key, count) -> count == null ? ImmutableList.of(1) : ImmutableList.of(count.get(0)+1));
    }

    //.put("ortsangaben", ImmutableList.of(2))
    //            .put("ortsangaben_flurstueckskennzeichen", ImmutableList.of(1, 2))
    private Map<String, List<Integer>> computeCountsPerParentIndex(ListMultimap<String, List<Integer>> multiplicities) {
        Map<String, List<Integer>> countsPerParentIndex = new LinkedHashMap<>();

        multiplicities.keySet()
                      .forEach(key -> {
                          List<Integer> counts = new ArrayList<>();
                          int[] lastParent = {0};
                          multiplicities.get(key)
                                        .forEach(list -> {
                                            List<Integer> indices = list.size() > 2 ? list.subList(list.size() - 2, list.size()) : list;
                                            boolean parent = list.size() > 1;

                                            // if parent instances have no children
                                            if (parent) {
                                                for (int i = indices.get(0) - lastParent[0]; i > 1; i--) {
                                                    counts.add(0);
                                                }
                                            }

                                            if (parent && indices.get(0) > lastParent[0]) {
                                                counts.add(1);
                                                lastParent[0] = indices.get(0);
                                            } else if (!parent && counts.isEmpty()) {
                                                counts.add(1);
                                            }
                                            counts.set(counts.size() - 1, indices.get(indices.size() - 1));
                                        });
                          countsPerParentIndex.put(key, counts);
                      });

        return countsPerParentIndex;
    }
}
