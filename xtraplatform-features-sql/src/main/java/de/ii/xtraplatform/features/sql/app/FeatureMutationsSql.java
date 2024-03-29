/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.app;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaVisitor;
import de.ii.xtraplatform.features.domain.Tuple;
import de.ii.xtraplatform.features.sql.domain.SchemaSql;
import de.ii.xtraplatform.features.sql.domain.SqlClient;
import de.ii.xtraplatform.features.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.features.sql.domain.SqlRelation;
import de.ii.xtraplatform.streams.domain.Reactive;
import de.ii.xtraplatform.streams.domain.Reactive.Transformer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureMutationsSql {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureMutationsSql.class);

  private final Supplier<SqlClient> sqlClient;
  private final FeatureStoreInsertGenerator generator;

  public FeatureMutationsSql(Supplier<SqlClient> sqlClient, FeatureStoreInsertGenerator generator) {
    this.sqlClient = sqlClient;
    this.generator = generator;
  }

  public Reactive.Transformer<FeatureSql, String> getCreatorFlow(
      SchemaSql schema, Object executionContext, EpsgCrs crs) {

    RowCursor rowCursor = new RowCursor(schema.getPath());

    return sqlClient
        .get()
        .getMutationFlow(
            feature ->
                createInstanceInserts(
                    schema, feature.getRowCounts(), rowCursor, Optional.empty(), crs),
            executionContext,
            Optional.empty());
  }

  public Reactive.Transformer<FeatureSql, String> getUpdaterFlow(
      SchemaSql schema, Object executionContext, String id, EpsgCrs crs) {

    RowCursor rowCursor = new RowCursor(schema.getPath());

    return sqlClient
        .get()
        .getMutationFlow(
            feature ->
                createInstanceInserts(
                    schema, feature.getRowCounts(), rowCursor, Optional.of(id), crs),
            executionContext,
            Optional.of(id));
  }

  public Reactive.Source<String> getDeletionSource(SchemaSql schema, String id) {
    Tuple<String, Consumer<String>> delete = createInstanceDelete(schema, id).apply(null);

    return sqlClient
        .get()
        .getSourceStream(delete.first(), SqlQueryOptions.withColumnTypes(String.class))
        .via(Transformer.map(sqlRow -> (String) sqlRow.getValues().get(0)));
  }

  // TODO: shouldn't id be part of the feature already?
  // TODO: delete generator
  public Object getUpdater(FeatureStoreInstanceContainer instanceContainer, String id) {
    /*
    return new FeatureProcessorSql(feature -> {
    Optional<FeatureStoreAttributesContainer> mainAttributesContainer = instanceContainer.getMainAttributesContainer();

    if (!mainAttributesContainer.isPresent()) {
        throw new IllegalStateException();
    }

    List<FeatureStoreRelation> path = mainAttributesContainer.get() instanceof FeatureStoreRelatedContainer ? ((FeatureStoreRelatedContainer) mainAttributesContainer.get()).getInstanceConnection() : ImmutableList.of();
    String idAttribute = mainAttributesContainer.get()
                                                .getIdAttribute()
                                                .get()
                                                .getName();

    //TODO
    //valueContainer.addValue(path, idAttribute, id);

    List<Function<FeatureSql, Pair<String, Optional<Consumer<String>>>>> queries = createInstanceInserts(instanceContainer, feature.getRowCounts(), true);

    //TODO
    //queryFunctions.add(0, v -> String.format("DELETE FROM osirisobjekt WHERE id=(SELECT id FROM %s WHERE id=%s)", instanceContainer.getName(), id));
    //idConsumers.add(0, Optional.empty());

    return runQueries(queries, feature);
    });

     */
    return null;
  }

  class StatementsVisitor
      implements SchemaVisitor<
          SchemaSql, List<Function<FeatureSql, Tuple<String, Consumer<String>>>>> {
    private final Map<List<String>, List<Integer>> rowNesting;
    private final RowCursor rowCursor;
    private final Optional<String> id;

    StatementsVisitor(
        Map<List<String>, List<Integer>> rowNesting, RowCursor rowCursor, Optional<String> id) {
      this.rowNesting = rowNesting;
      this.rowCursor = rowCursor;
      this.id = id;
    }

    @Override
    public List<Function<FeatureSql, Tuple<String, Consumer<String>>>> visit(
        SchemaSql schema,
        List<List<Function<FeatureSql, Tuple<String, Consumer<String>>>>> visitedProperties) {

      if (!schema.isObject()) {
        return ImmutableList.of();
      }

      List<Function<FeatureSql, Tuple<String, Consumer<String>>>> before = new ArrayList<>();
      List<Function<FeatureSql, Tuple<String, Consumer<String>>>> after = new ArrayList<>();

      after.addAll(createObjectInserts(schema, rowNesting, rowCursor, id, null));

      for (int i = 0; i < schema.getProperties().size(); i++) {
        if (schema.isObject()) {
          if (isMain(schema)) {
            before.addAll(0, visitedProperties.get(i));
          } else if (isMerge(schema)) {
            before.addAll(visitedProperties.get(i));
          } else {
            after.addAll(visitedProperties.get(i));
          }
        }
      }

      return Stream.concat(before.stream(), after.stream())
          .collect(ImmutableList.toImmutableList());
    }

    private boolean isMain(SchemaSql schemaSql) {
      return schemaSql.getProperties().stream()
          .anyMatch(
              property ->
                  property.getRole().isPresent() && property.getRole().get() == SchemaBase.Role.ID);
    }

    // TODO
    private boolean isMerge(SchemaSql schemaSql) {
      return !schemaSql.getRelation().isEmpty()
          && schemaSql.getRelation().get(0).isOne2One()
          && Objects.equals(
              schemaSql.getRelation().get(0).getSourceSortKey().get(),
              schemaSql.getRelation().get(0).getSourceField());
    }
  }

  List<Function<FeatureSql, Tuple<String, Consumer<String>>>> createInstanceInserts(
      SchemaSql schema,
      Map<List<String>, List<Integer>> rowNesting,
      RowCursor rowCursor,
      Optional<String> id,
      EpsgCrs crs) {
    boolean withId = id.isPresent();

    Stream<Function<FeatureSql, Tuple<String, Consumer<String>>>> instance =
        withId
            ? Stream.concat(
                Stream.of(createInstanceDelete(schema, id.get())),
                createObjectInserts(schema, rowNesting, rowCursor, id, crs).stream())
            : createObjectInserts(schema, rowNesting, rowCursor, id, crs).stream();

    return Stream.concat(
            instance,
            schema.getProperties().stream()
                .filter(SchemaSql::isObject)
                .flatMap(
                    childSchema ->
                        createInstanceInserts(
                            childSchema, rowNesting, rowCursor, Optional.empty(), crs)
                            .stream()))
        .collect(Collectors.toList());
  }

  // TODO: to InsertGenerator
  Function<FeatureSql, Tuple<String, Consumer<String>>> createInstanceDelete(
      SchemaSql schema, String id) {

    String table = schema.getName();
    String idColumn =
        schema
            .getIdProperty()
            .map(SchemaBase::getName)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No property with role ID found for '"
                            + schema.getSourcePath().orElse(schema.getName())
                            + "'."));

    return featureSql ->
        Tuple.of(
            String.format("DELETE FROM %s WHERE %s=%s RETURNING %2$s", table, idColumn, id),
            ignore -> {});
  }

  List<Function<FeatureSql, Tuple<String, Consumer<String>>>> createObjectInserts(
      SchemaSql schema,
      Map<List<String>, List<Integer>> rowNesting,
      RowCursor rowCursor,
      Optional<String> id,
      EpsgCrs crs) {

    if (schema.isFeature()) {
      return createAttributesInserts(schema, rowCursor.get(schema.getPath()), id, crs);
    }

    if (schema.getRelation().isEmpty()) {
      return ImmutableList.of();
    }

    SqlRelation relation = schema.getRelation().get(0);

    String parentName = relation.getSourceContainer();

    if (!relation.isM2N() && !relation.isOne2N()) {
      List<Integer> newParentRows =
          rowCursor.track(schema.getFullPath(), schema.getParentPath(), 0);

      return createAttributesInserts(schema, newParentRows, id, crs);
    }

    // TODO: what are the keys?
    // TODO: err
    if (!rowNesting.containsKey(schema.getFullPath())) {
      // throw new IllegalStateException();
      return ImmutableList.of();
    }

    // TODO: nested m:n test
    List<Integer> numberOfRowsPerParentRow = rowNesting.get(schema.getFullPath()); // [1,2] | [2]
    int currentParentRow = rowCursor.getCurrent(schema.getParentPath()); // 0|1 | 0
    int rowCount = numberOfRowsPerParentRow.get(currentParentRow); // 1|2 | 2

    return IntStream.range(0, rowCount)
        .mapToObj(
            currentRow -> {
              List<Integer> newParentRows =
                  rowCursor.track(schema.getFullPath(), schema.getParentPath(), currentRow);

              return createAttributesInserts(schema, newParentRows, id, crs);
            })
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  List<Function<FeatureSql, Tuple<String, Consumer<String>>>> createAttributesInserts(
      SchemaSql schema, List<Integer> parentRows, Optional<String> id, EpsgCrs crs) {

    ImmutableList.Builder<Function<FeatureSql, Tuple<String, Consumer<String>>>> queries =
        ImmutableList.builder();

    queries.add(generator.createInsert(schema, parentRows, id, crs));

    if (!schema.getRelation().isEmpty()) {
      SqlRelation relation = schema.getRelation().get(0);

      if (relation.isM2N()) {
        queries.add(generator.createJunctionInsert(schema, parentRows));
      }

      boolean isOne2OneWithForeignKey =
          relation.isOne2One()
              && !Objects.equals(
                  relation.getSourceSortKey().orElse("id"), relation.getSourceField());

      if (isOne2OneWithForeignKey) {
        queries.add(generator.createForeignKeyUpdate(schema, parentRows));
      }
    }

    return queries.build();
  }
}
