/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.app;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.Tuple;
import de.ii.xtraplatform.features.sql.domain.SchemaSql;
import de.ii.xtraplatform.features.sql.domain.SqlPathDefaults;
import de.ii.xtraplatform.features.sql.domain.SqlRelation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
class SqlInsertGenerator2 implements FeatureStoreInsertGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(SqlInsertGenerator2.class);

  private final EpsgCrs nativeCrs;
  private final CrsTransformerFactory crsTransformerFactory;
  private final SqlPathDefaults sqlOptions;

  public SqlInsertGenerator2(
      EpsgCrs nativeCrs, CrsTransformerFactory crsTransformerFactory, SqlPathDefaults sqlOptions) {
    this.nativeCrs = nativeCrs;
    this.crsTransformerFactory = crsTransformerFactory;
    this.sqlOptions = sqlOptions;
  }

  SqlPathDefaults getSqlOptions() {
    return sqlOptions;
  }

  public Function<FeatureSql, Tuple<String, Consumer<String>>> createInsert(
      SchemaSql schema, List<Integer> parentRows, Optional<String> id) {

    Optional<SqlRelation> parentRelation = schema.getRelation().stream().findFirst();

    Optional<SchemaSql> idProperty = schema.getIdProperty();

    Map<String, String> valueOverrides = new LinkedHashMap<>();

    if (idProperty.isPresent() && id.isPresent()) {
      valueOverrides.put(
          idProperty.get().getName(),
          idProperty.get().getType() == Type.STRING ? String.format("'%s'", id.get()) : id.get());
    }

    // TODO: id instead of primaryKey if isPresent
    String primaryKey = schema.getPrimaryKey().orElse(sqlOptions.getPrimaryKey());

    Set<String> columns0 =
        schema.getProperties().stream()
            // TODO: filter out mutations.ignore=true
            // TODO: filter out primaryKey if not mutations.ignore=false
            .filter(property -> !Objects.equals(property.getName(), primaryKey))
            .map(SchemaBase::getName)
            .collect(ImmutableSet.toImmutableSet());

    // TODO: add id if present
    Set<String> columns =
        idProperty.isPresent() && id.isPresent()
            ? ImmutableSet.<String>builder()
                .add(idProperty.get().getName())
                .addAll(columns0)
                .build()
            : columns0;

    // TODO: from Syntax
    List<String> columns2 =
        columns.stream()
            .map(
                col ->
                    col.startsWith("ST_AsText(ST_ForcePolygonCCW(")
                        ? col.substring("ST_AsText(ST_ForcePolygonCCW(".length(), col.length() - 2)
                        : col)
            .collect(Collectors.toList());

    List<String> sortKeys = new ArrayList<>();

    if (parentRelation.isPresent()) {
      // TODO: is this merged?
      if (parentRelation.get().isOne2One()
          && Objects.equals(
              parentRelation.get().getSourceSortKey().orElse("id"),
              parentRelation.get().getSourceField())) {
        // TODO fullPath, sortKey
        sortKeys.add(
            0,
            String.format(
                "%s.%s",
                parentRelation.get().getSourceContainer(),
                parentRelation.get().getSourceSortKey().orElse("id")));
        if (!columns2.contains(primaryKey)) {
          columns2.add(0, primaryKey);
        }

      } else if (parentRelation.get().isOne2N()) {
        sortKeys.add(
            0,
            String.format(
                "%s.%s",
                parentRelation.get().getSourceContainer(),
                parentRelation.get().getSourceSortKey().get()));
        columns2.add(0, parentRelation.get().getTargetField());
      }
    }

    String tableName = schema.getName();
    String columnNames = Joiner.on(',').skipNulls().join(columns2);
    if (!columnNames.isEmpty()) {
      columnNames = "(" + columnNames + ")";
    }
    String finalColumnNames = columnNames;

    String returningValue =
        " RETURNING "
            + (parentRelation.isPresent() && parentRelation.get().isOne2N()
                ? " null"
                : idProperty.isPresent() ? idProperty.get().getName() : primaryKey);
    Optional<String> returningName =
        parentRelation.isPresent() && parentRelation.get().isOne2N()
            ? Optional.empty()
            : idProperty.isPresent()
                ? Optional.of(tableName + "." + idProperty.get().getName())
                : Optional.of(tableName + "." + primaryKey);

    return feature -> {
      Optional<ObjectSql> currentRow =
          schema.isFeature()
              ? Optional.of(feature)
              : feature.getNestedObject(schema.getFullPath(), parentRows);

      if (!currentRow.isPresent()) {
        return Tuple.of(null, null);
      }

      // TODO: pass id to getValues if given
      String values =
          getColumnValues(
              sortKeys,
              columns,
              currentRow.get().getValues(nativeCrs, crsTransformerFactory),
              currentRow.get().getIds(),
              valueOverrides);

      if (!values.isEmpty()) {
        values = "VALUES (" + values + ")";
      } else {
        values = "DEFAULT VALUES";
      }

      String query =
          String.format(
              "INSERT INTO %s %s %s%s;", tableName, finalColumnNames, values, returningValue);

      Consumer<String> idConsumer =
          returningName
              .map(name -> (Consumer<String>) returned -> feature.putChildrenIds(name, returned))
              .orElse(returned -> {});

      return Tuple.of(query, idConsumer);
    };
  }

  public Function<FeatureSql, Tuple<String, Consumer<String>>> createJunctionInsert(
      SchemaSql schema, List<Integer> parentRows) {

    if (schema.getRelation().isEmpty() || !schema.getRelation().get(0).isM2N()) {
      throw new IllegalArgumentException();
    }

    SqlRelation relation = schema.getRelation().get(0);

    String table = relation.getJunction().get();
    String columnNames =
        String.format(
            "%s,%s", relation.getJunctionSource().get(), relation.getJunctionTarget().get());
    String sourceIdColumn =
        String.format("%s.%s", relation.getSourceContainer(), relation.getSourceField());
    String targetIdColumn =
        String.format("%s.%s", relation.getTargetContainer(), relation.getTargetField());

    return feature -> {
      Optional<ObjectSql> currentRow = feature.getNestedObject(schema.getFullPath(), parentRows);

      if (!currentRow.isPresent()) {
        return Tuple.of(null, null);
      }

      Map<String, String> ids = currentRow.get().getIds();

      String columnValues =
          String.format("%s,%s", ids.get(sourceIdColumn), ids.get(targetIdColumn));

      return Tuple.of(
          String.format(
              "INSERT INTO %s (%s) VALUES (%s) RETURNING null;", table, columnNames, columnValues),
          id -> {});
    };
  }

  public Function<FeatureSql, Tuple<String, Consumer<String>>> createForeignKeyUpdate(
      SchemaSql schema, List<Integer> parentRows) {

    if (schema.getRelation().isEmpty()
        || !(schema.getRelation().get(0).isOne2One() || schema.getRelation().get(0).isOne2N())) {
      throw new IllegalArgumentException();
    }

    SqlRelation relation = schema.getRelation().get(0);

    String table = relation.getSourceContainer();
    String refKey = String.format("%s.%s", table, relation.getSourceSortKey().get());
    String column = relation.getSourceField();
    String columnKey =
        String.format("%s.%s", relation.getTargetContainer(), relation.getTargetField());

    return feature -> {
      Optional<ObjectSql> currentRow = feature.getNestedObject(schema.getFullPath(), parentRows);

      if (!currentRow.isPresent()) {
        return Tuple.of(null, null);
      }

      Map<String, String> ids = currentRow.get().getIds();

      return Tuple.of(
          String.format(
              "UPDATE %s SET %s=%s WHERE id=%s RETURNING null;",
              table, column, ids.get(columnKey), ids.get(refKey)),
          id -> {});
    };
  }

  // TODO: from syntax
  // TODO: separate column and id column names
  String getColumnValues(
      List<String> idKeys,
      Set<String> columnNames,
      Map<String, String> values,
      Map<String, String> ids,
      Map<String, String> valueOverrides) {

    return Stream.concat(
            idKeys.stream().map(ids::get),
            columnNames.stream()
                .map(
                    name -> {
                      // TODO: value transformer?
                      if (name.startsWith("ST_AsText(ST_ForcePolygonCCW(")) {
                        return String.format(
                            "ST_ForcePolygonCW(ST_GeomFromText(%s,25832))",
                            values.get(name)); // TODO srid from config
                      }
                      if (valueOverrides.containsKey(name)) {
                        return valueOverrides.get(name);
                      }
                      return values.get(name);
                    }))
        .collect(Collectors.joining(","));
  }
}
