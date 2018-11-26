/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.pgis;

import akka.japi.Pair;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public abstract class SqlFeatureInserts {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlFeatureInserts.class);

    protected abstract SqlPathTree getSqlPaths();

    @Value.Default
    protected boolean withId() {
        return false;
    }

    public List<Function<NestedSqlInsertRow, Pair<String, Optional<Consumer<String>>>>> getQueries(Map<String, List<Integer>> rows) {
        return toSql2(null, getSqlPaths(), null, rows, ImmutableList.of(0));
    }

    //TODO: to builder
    public NestedSqlInsertRow getValueContainer(Map<String, List<Integer>> multiplicities) {
        return getValueContainer(getSqlPaths(), multiplicities, 0);
    }

    private NestedSqlInsertRow getValueContainer(SqlPathTree nestedPath, Map<String, List<Integer>> multiplicities, int parentRow) {
        final ListMultimap<String, NestedSqlInsertRow> rows = ArrayListMultimap.create();

        nestedPath.getChildren()
                  .stream()
                  .sorted(Comparator.comparing(SqlPathTree::getType))
                  .forEach(nestedPath1 -> {
                      int defaultRowCount = nestedPath1.getType() != SqlPathTree.TYPE.ID_1_N && nestedPath1.getType() != SqlPathTree.TYPE.ID_M_N ? 1 : 0;
                      int rowCount = multiplicities.getOrDefault(nestedPath1.getTableName(), ImmutableList.of(defaultRowCount))
                                                   .get(parentRow);
                      for (int i = 0; i < rowCount; i++) {
                          rows.put(nestedPath1.getPath(), getValueContainer(nestedPath1, multiplicities, i));
                      }
                  });

        return new NestedSqlInsertRow(nestedPath.getPath(), rows);
    }

    public List<Function<NestedSqlInsertRow, Pair<String, Optional<Consumer<String>>>>> toSql(SqlPathTree parentPath, SqlPathTree mainPath, SqlPathTree nestedPath, List<Integer> parentRows) {

        List<String> columns3 = nestedPath.getColumns()
                                          .stream()
                                          .filter(col -> withId() || !col.equals("id"))
                                          .collect(Collectors.toList());
        List<String> columns2 = columns3.stream()
                                        .map(col -> col.startsWith("ST_AsText(ST_ForcePolygonCCW(") ? col.substring("ST_AsText(ST_ForcePolygonCCW(".length(), col.length() - 2) : col)
                                        .collect(Collectors.toList());
        List<String> columnPaths2 = nestedPath.getColumns()
                                              .stream()
                                              .map(col -> nestedPath.getPath() + "/" + col)
                                              .filter(col -> withId() || !col.endsWith("/id"))
                                              .collect(Collectors.toList());
        /*List<String> columnPaths2 = nestedPath.getColumnPaths()
                                              .stream()
                                              .filter(col -> !col.endsWith("/id"))
                                              .collect(Collectors.toList());*/
        ;
        if (nestedPath.getType() == SqlPathTree.TYPE.MERGED) {
            //TODO fullPath
            columnPaths2.add(0, mainPath.getTableName() + ".id");
            columns2.add(0, "id");
        } else if (nestedPath.getType() == SqlPathTree.TYPE.ID_1_N) {
            columnPaths2.add(0, parentPath.getTableName() + ".id");
            columns2.add(0, nestedPath.getJoinPathElements()
                                      .get(0)
                                      .second()
                                      .get()
                                      .get(1));
        }

        ImmutableList.Builder<Function<NestedSqlInsertRow, Pair<String, Optional<Consumer<String>>>>> queries = ImmutableList.builder();

        //int rowCount = type == TYPE.ID_M_N || type == TYPE.ID_1_N ? rows.get(path) != null ? rows.get(path) : 0 : 1;

        //for (int row = 0; row < rowCount; row++) {
        String tableName = nestedPath.getTableName();
        String columnNames = Joiner.on(',')
                                   .skipNulls()
                                   .join(columns2);
        if (!columnNames.isEmpty()) {
            columnNames = "(" + columnNames + ")";
        }
        String finalColumnNames = columnNames;

        String returningId = nestedPath.getType() != SqlPathTree.TYPE.ID_1_N ? " RETURNING id" : " RETURNING null";
        Optional<String> returningName = nestedPath.getType() != SqlPathTree.TYPE.ID_1_N ? Optional.of(tableName + ".id") : Optional.empty();

        Function<NestedSqlInsertRow, Pair<String, Optional<Consumer<String>>>> mainQuery = nestedRow -> {
            NestedSqlInsertRow currentRow = nestedRow.getNested(nestedPath.getTrail(), parentRows);
            Map<String, String> ids = ImmutableMap.<String, String>builder()
                    .putAll(nestedRow.getNested(mainPath.getTrail(), ImmutableList.of()).ids)
                    .putAll(parentPath != null && !Objects.equals(parentPath, mainPath) ? nestedRow.getNested(parentPath.getTrail(), parentRows).ids : ImmutableMap.of())
                    .build();
            String values = getColumnValues(columnPaths2, columns3, currentRow.values, ids);
            if (!values.isEmpty()) {
                values = "VALUES (" + values + ")";
            } else {
                values = "DEFAULT VALUES";
            }

            String query = String.format("INSERT INTO %s %s %s%s;", tableName, finalColumnNames, values, returningId);
            return new Pair<>(query, returningName.map(name -> id -> currentRow.ids.put(name, id)));
        };

        if (nestedPath.getType() != SqlPathTree.TYPE.REF)
            queries.add(mainQuery);

        if (nestedPath.getType() == SqlPathTree.TYPE.ID_M_N) {
            queries.addAll(toSqlRefs(parentPath, nestedPath, parentRows));
        }

        if (nestedPath.getType() == SqlPathTree.TYPE.ID_1_1) {
            queries.addAll(toSqlRef(parentPath, nestedPath, parentRows));
        }

        return queries.build();
    }

    // TODO: rows = current index path one based from json (shorter) (1,1,1), (1,2,1), (1,2,2)
    // --> shorten to last two for now --> (1,1), (2,1), (2,2)
    // --> iterate lists and elements --> increase first --> add new elem --> increase last --> elem = last
    // --> 1 - (1) --> 1 - (1) --> 2 - (1,1) --> 1 - (1,1) --> 2 - (1,1) --> 2 - (1,2)
    public List<Function<NestedSqlInsertRow, Pair<String, Optional<Consumer<String>>>>> toSql2(SqlPathTree parentParentPath, SqlPathTree parentPath, SqlPathTree mainPath, Map<String, List<Integer>> rows, List<Integer> parentRows) {
        //Stream<NestedSqlInsert> stream = type == TYPE.MERGED ? Stream.concat(nestedPaths.stream(), Stream.of(this)) : Stream.concat(Stream.of(this), nestedPaths.stream());
        Stream<SqlPathTree> stream = parentPath.getType() == SqlPathTree.TYPE.MERGED ? Stream.concat(parentPath.getChildren()
                                                                                                               .stream(), Stream.of(parentPath)) : Stream.concat(Stream.of(parentPath), parentPath.getChildren()
                                                                                                                                   .stream());

        SqlPathTree main = mainPath != null ? mainPath : Stream.concat(Stream.of(parentPath), parentPath.getChildren()
                                                                                                        .stream())
                                                               .filter(nestedPath -> nestedPath.getType() == SqlPathTree.TYPE.MAIN)
                                                               .findFirst()
                                                               .orElse(null);

        return stream
                .sorted(Comparator.comparing(SqlPathTree::getType))
                .flatMap(nestedPath -> {
                    //TODO
                    //int parentRow = 0;
                    ImmutableList.Builder<Function<NestedSqlInsertRow, Pair<String, Optional<Consumer<String>>>>> builder = ImmutableList.builder();

                    if (nestedPath.equals(parentPath)) {
                        builder.addAll(toSql(parentParentPath, main, nestedPath, parentRows));
                    } else {
                        int defaultRowCount = nestedPath.getType() != SqlPathTree.TYPE.ID_1_N && nestedPath.getType() != SqlPathTree.TYPE.ID_M_N ? 1 : 0;
                        //TODO ???
                        // rows rr 1 oa 2 fk 0,3
                        // parentRows ft 0 oo 0 rr 0 oa 0 1 fk 0
                        //
                        // rr rows.get --> [1] .get(0) --> 1
                        // oa rows.get --> [1] .get(0) --> 1
                        // rr rows.get --> [1] .get(0) --> 1

                        int rowCount = defaultRowCount;
                        List<Integer> rowsOrDefault = rows.getOrDefault(nestedPath.getTableName(), ImmutableList.of());
                        if (rowsOrDefault.size() > parentRows.get(parentRows.size() - 1)) {
                            rowCount = rowsOrDefault.get(parentRows.get(parentRows.size() - 1));
                        }

                        for (int i = 0; i < rowCount; i++) {
                            List<Integer> newParentRows = ImmutableList.<Integer>builder().addAll(parentRows)
                                                                                          .add(i)
                                                                                          .build();
                            builder.addAll(toSql2(parentPath, nestedPath, main, rows, newParentRows));
                        }
                    }

                    return builder.build()
                                  .stream();
                })
                .collect(Collectors.toList());
    }

    public List<Function<NestedSqlInsertRow, Pair<String, Optional<Consumer<String>>>>> toSqlRefs(SqlPathTree parentPath, SqlPathTree nestedPath, List<Integer> parentRows) {

        Map<String, Pair<String, String>> refs = new LinkedHashMap<>();
        String[] lastRef = new String[2];

        nestedPath.getJoinPathElements()
                  .forEach(pathElem -> {
                      List<String> fields = pathElem.second()
                                                    .get();
                      if (lastRef[1] != null && pathElem.second()
                                                        .isPresent()) {
                          refs.put(lastRef[0], new Pair<>(lastRef[1], fields.get(0)));
                      }
                      if (fields.size() == 2) {
                          lastRef[0] = pathElem.first();
                          lastRef[1] = fields.get(1);
                      }
                  });

        ImmutableList.Builder<Function<NestedSqlInsertRow, Pair<String, Optional<Consumer<String>>>>> queries = ImmutableList.builder();
        String table = nestedPath.getJoinPathElements()
                                 .get(0)
                                 .first();
        Pair<String, String> ref = refs.get(table);

        String columnNames = String.format("%s,%s", ref.first(), ref.second());

        String sourceIdColumn = ref.first()
                            .replace('_', '.');
        String targetIdColumn = lastRef[0] + '.' + lastRef[1];

        queries.add(nestedRow -> {
            Map<String, String> ids = nestedRow.getNested(nestedPath.getTrail(), parentRows).ids;
            Map<String, String> parentIds = nestedRow.getNested(nestedPath.getParentPaths(), parentRows).ids;

            /*if (row >= values.get(column2)
                             .size()) {
                throw new IllegalStateException(String.format("No values found for row %s of %s", row, path));
            }*/
            String columnValues = String.format("%s,%s", parentIds.get(sourceIdColumn), ids.get(targetIdColumn));

            return new Pair<>(String.format("INSERT INTO %s (%s) VALUES (%s) RETURNING null;", table, columnNames, columnValues), Optional.empty());
        });

        return queries.build();
    }

    public List<Function<NestedSqlInsertRow, Pair<String, Optional<Consumer<String>>>>> toSqlRef(SqlPathTree parentPath, SqlPathTree nestedPath, List<Integer> parentRows) {

        String table = parentPath.getTableName();
        String column = nestedPath.getJoinPathElements()
                                  .get(0)
                                  .second()
                                  .get()
                                  .get(0);
        String columnKey = nestedPath.getTableName() + ".id";
        String refKey = table + ".id";

        return ImmutableList.of(nestedRow -> {
            Map<String, String> ids = nestedRow.getNested(nestedPath.getTrail(), parentRows).ids;
            Map<String, String> parentIds = nestedRow.getNested(nestedPath.getParentPaths(), parentRows).ids;

            /*if (row >= values.get(columnKey)
                             .size()) {
                throw new IllegalStateException(String.format("No values found for row %s of %s", row, path));
            }*/

            return new Pair<>(String.format("UPDATE %s SET %s=%s WHERE id=%s RETURNING null;", table, column, ids.get(columnKey), parentIds.get(refKey)), Optional.empty());
        });
    }

    String getColumnValues(List<String> columnPaths, List<String> columnNames, Map<String, String> values, Map<String, String> ids) {
        List<String> columnValues = columnPaths.stream()
                                               .map(col -> col.endsWith(".id") ? ids.get(col) : values.get(col))
                                               .collect(Collectors.toList());

        for (int j = 0; j < columnNames.size(); j++) {
            if (columnNames.get(j)
                           .startsWith("ST_AsText(ST_ForcePolygonCCW(")) {
                columnValues.set(j, "ST_ForcePolygonCW(ST_GeomFromText(" + columnValues.get(j) + ",25832))"); //TODO srid from config
                break;
            }
        }

        return columnValues.stream()
                           .collect(Collectors.joining(","));
    }

}
