/**
 * Copyright 2020 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import akka.japi.Pair;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql;
import de.ii.xtraplatform.features.domain.FeatureStoreRelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author zahnen
 */
class SqlInsertGenerator2 implements FeatureStoreInsertGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlInsertGenerator2.class);

    private final EpsgCrs nativeCrs;
    private final CrsTransformerFactory crsTransformerFactory;

    public SqlInsertGenerator2(EpsgCrs nativeCrs, CrsTransformerFactory crsTransformerFactory) {
        this.nativeCrs = nativeCrs;
        this.crsTransformerFactory = crsTransformerFactory;
    }

    public Function<FeatureSql, Pair<String, Consumer<String>>> createInsert(
            SchemaSql schema,
            List<Integer> parentRows, boolean withId) {

        Optional<FeatureStoreRelation> parentRelation = schema.getRelation();

        Set<String> columns = withId
                ? ImmutableSet.<String>builder().add(schema.getPrimaryKey()
                                                           .get())
                                                .addAll(schema.getValueNames())
                                                .build()
                : schema.getValueNames()
                        .stream()
                        .filter(name -> !Objects.equals(name, schema.getPrimaryKey()
                                                                    .get()))
                        .collect(ImmutableSet.toImmutableSet());

        //TODO: from Syntax
        List<String> columns2 = columns.stream()
                                       .map(col -> col.startsWith("ST_AsText(ST_ForcePolygonCCW(") ? col.substring("ST_AsText(ST_ForcePolygonCCW(".length(), col.length() - 2) : col)
                                       .collect(Collectors.toList());

        List<String> idKeys = new ArrayList<>();

        if (parentRelation.isPresent()) {
            //TODO: is this merged?
            if (parentRelation.get()
                              .isOne2One() && Objects.equals(parentRelation.get()
                                                                           .getSourceSortKey(), parentRelation.get()
                                                                                                              .getSourceField())) {
                //TODO fullPath, sortKey
                idKeys.add(0, String.format("%s.%s", parentRelation.get()
                                                                   .getSourceContainer(), parentRelation.get()
                                                                                                        .getSourceSortKey()));
                if (!columns2.contains(schema.getPrimaryKey()
                                             .get())) {
                    columns2.add(0, schema.getPrimaryKey()
                                          .get());
                }

            } else if (parentRelation.get()
                                     .isOne2N()) {
                idKeys.add(0, String.format("%s.%s", parentRelation.get()
                                                                   .getSourceContainer(), parentRelation.get()
                                                                                                        .getSourceSortKey()));
                columns2.add(0, parentRelation.get()
                                              .getTargetField());
            }
        }

        String tableName = schema.getName();
        String columnNames = Joiner.on(',')
                                   .skipNulls()
                                   .join(columns2);
        if (!columnNames.isEmpty()) {
            columnNames = "(" + columnNames + ")";
        }
        String finalColumnNames = columnNames;

        //TODO: primaryKey instead of id
        String returningId = parentRelation.isPresent() && parentRelation.get()
                                                                         .isOne2N() ? " RETURNING null" : " RETURNING id";
        Optional<String> returningName = parentRelation.isPresent() && parentRelation.get()
                                                                                     .isOne2N() ? Optional.empty() : Optional.of(tableName + ".id");


        return feature -> {
            Optional<ObjectSql> currentRow = schema.isFeature() ? Optional.of(feature) : feature.getNestedObject(schema.getFullPath(), parentRows);

            if (!currentRow.isPresent()) {
                return new Pair<>(null, null);
            }

            String values = getColumnValues(idKeys, columns, currentRow.get().getValues(nativeCrs, crsTransformerFactory), currentRow.get().getIds());

            if (!values.isEmpty()) {
                values = "VALUES (" + values + ")";
            } else {
                values = "DEFAULT VALUES";
            }

            String query = String.format("INSERT INTO %s %s %s%s;", tableName, finalColumnNames, values, returningId);

            Consumer<String> idConsumer = returningName.map(name -> (Consumer<String>) id -> feature.putChildrenIds(name, id))
                                                       .orElse(id -> {
                                                       });

            return new Pair<>(query, idConsumer);
        };
    }

    public Function<FeatureSql, Pair<String, Consumer<String>>> createJunctionInsert(
            SchemaSql schema, List<Integer> parentRows) {

        if (!schema.getRelation()
                   .isPresent() || !schema.getRelation()
                                          .get()
                                          .isM2N()) {
            throw new IllegalArgumentException();
        }

        FeatureStoreRelation relation = schema.getRelation()
                                              .get();

        String table = relation.getJunction()
                               .get();
        String columnNames = String.format("%s,%s", relation.getJunctionSource()
                                                            .get(), relation.getJunctionTarget()
                                                                            .get());
        String sourceIdColumn = String.format("%s.%s", relation.getSourceContainer(), relation.getSourceField());
        String targetIdColumn = String.format("%s.%s", relation.getTargetContainer(), relation.getTargetField());


        return feature -> {
            Optional<ObjectSql> currentRow = feature.getNestedObject(schema.getFullPath(), parentRows);

            if (!currentRow.isPresent()) {
                return new Pair<>(null, null);
            }

            Map<String, String> ids = currentRow.get().getIds();

            String columnValues = String.format("%s,%s", ids.get(sourceIdColumn), ids.get(targetIdColumn));

            return new Pair<>(String.format("INSERT INTO %s (%s) VALUES (%s) RETURNING null;", table, columnNames, columnValues), id -> {});
        };
    }

    public Function<FeatureSql, Pair<String, Consumer<String>>> createForeignKeyUpdate(
            SchemaSql schema, List<Integer> parentRows) {

        if (!schema.getRelation()
                   .isPresent() || !(schema.getRelation()
                                           .get()
                                           .isOne2One() || schema.getRelation()
                                                                 .get()
                                                                 .isOne2N())) {
            throw new IllegalArgumentException();
        }

        FeatureStoreRelation relation = schema.getRelation()
                                              .get();

        String table = relation.getSourceContainer();
        String refKey = String.format("%s.%s", table, relation.getSourceSortKey());
        String column = relation.getSourceField();
        String columnKey = String.format("%s.%s", relation.getTargetContainer(), relation.getTargetField());


        return feature -> {
            Optional<ObjectSql> currentRow = feature.getNestedObject(schema.getFullPath(), parentRows);

            if (!currentRow.isPresent()) {
                return new Pair<>(null, null);
            }

            Map<String, String> ids = currentRow.get().getIds();

            return new Pair<>(String.format("UPDATE %s SET %s=%s WHERE id=%s RETURNING null;", table, column, ids.get(columnKey), ids.get(refKey)), id -> {});
        };
    }

    //TODO: from syntax
    //TODO: separate column and id column names
    String getColumnValues(List<String> idKeys, Set<String> columnNames, Map<String, String> values,
                           Map<String, String> ids) {

        return Stream.concat(
                idKeys.stream()
                      .map(ids::get),
                columnNames.stream()
                           .map(name -> {
                               //TODO: value transformer?
                               if (name.startsWith("ST_AsText(ST_ForcePolygonCCW(")) {
                                   return String.format("ST_ForcePolygonCW(ST_GeomFromText(%s,25832))", values.get(name)); //TODO srid from config
                               }
                               return values.get(name);
                           }))
                     .collect(Collectors.joining(","));
    }
}
