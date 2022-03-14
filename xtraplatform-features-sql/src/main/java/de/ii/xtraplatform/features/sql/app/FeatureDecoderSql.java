/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.app;

import de.ii.xtraplatform.features.sql.domain.SchemaSql;
import de.ii.xtraplatform.features.sql.domain.SqlRow;
import de.ii.xtraplatform.features.sql.domain.SqlRowMeta;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureStoreMultiplicityTracker;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.features.domain.FeatureTokenDecoder;
import de.ii.xtraplatform.features.domain.ImmutableSchemaMapping;
import de.ii.xtraplatform.features.domain.NestingTracker;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaMapping;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureDecoderSql extends FeatureTokenDecoder<SqlRow, FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureDecoderSql.class);

  private final FeatureSchema featureSchema;
  private final FeatureQuery featureQuery;
  private final List<String> mainTablePath;
  private final FeatureStoreMultiplicityTracker multiplicityTracker;
  private final boolean isSingleFeature;

  private boolean started;
  private boolean featureStarted;
  private Object currentId;
  private boolean isAtLeastOneFeatureWritten;

  private ModifiableContext<FeatureSchema, SchemaMapping> context;
  private GeometryDecoderWkt geometryDecoder;
  private NestingTracker nestingTracker;

  public FeatureDecoderSql(List<FeatureStoreTypeInfo> typeInfos,
      List<SchemaSql> tableSchemas,
      FeatureSchema featureSchema, FeatureQuery query) {
    this.featureSchema = featureSchema;
    this.featureQuery = query;

    //TODO: support multiple main tables
    SchemaSql tableSchema = tableSchemas.get(0);

    this.mainTablePath = tableSchema.getFullPath();
    List<List<String>> multiTables = tableSchema.getAllObjects().stream()
        .filter(schema -> !schema.getRelation().isEmpty())
        .map(SchemaBase::getFullPath)
        .collect(Collectors.toList());
    this.multiplicityTracker = new SqlMultiplicityTracker(multiTables);
    this.isSingleFeature = query.returnsSingleFeature();
  }

  @Override
  protected void init() {
    this.context = createContext()
        .setMapping(new ImmutableSchemaMapping.Builder().targetSchema(featureSchema).build())
        .setQuery(featureQuery);
    this.geometryDecoder = new GeometryDecoderWkt(getDownstream(), context);
    this.nestingTracker = new NestingTracker(getDownstream(), context, mainTablePath, false, false,
        false);
  }

  @Override
  protected void cleanup() {
    while (nestingTracker.isNested()) {
      nestingTracker.close();
    }

    if (isAtLeastOneFeatureWritten) {
      getDownstream().onFeatureEnd(context);
    }

    getDownstream().onEnd(context);
  }

  @Override
  public void onPush(SqlRow sqlRow) {
    if (sqlRow instanceof SqlRowMeta) {
      handleMetaRow((SqlRowMeta) sqlRow);
      return;
    }

    //TODO: should't happen, test with exception in onStart
    if (!started) {
      return;
    }

    //if (sqlRow instanceof SqlRowValues) {
    handleValueRow(sqlRow);
    //    return;
    //}
  }

  private void handleMetaRow(SqlRowMeta sqlRow) {

    context.metadata().numberReturned(sqlRow.getNumberReturned());
    context.metadata().numberMatched(sqlRow.getNumberMatched());
    context.metadata().isSingleFeature(isSingleFeature);

    getDownstream().onStart(context);

    this.started = true;
  }

  private void handleValueRow(SqlRow sqlRow) {

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Sql row: {}", sqlRow);
    }

    Object featureId = sqlRow.getIds().get(0);

    if (nestingTracker.isNotMain(sqlRow.getPath())) {
      multiplicityTracker.track(sqlRow.getPath(), sqlRow.getIds());

      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("Multiplicities {} {}", sqlRow.getPath(),
            multiplicityTracker.getMultiplicitiesForPath(sqlRow.getPath()));
      }
    }

    if (!Objects.equals(currentId, featureId)) {
      if (featureStarted) {
        getDownstream().onFeatureEnd(context);
        this.featureStarted = false;
        multiplicityTracker.reset();
      }

      context.pathTracker().track(sqlRow.getPath());
      getDownstream().onFeatureStart(context);
      this.featureStarted = true;
      this.currentId = featureId;
    }

    handleNesting(sqlRow, multiplicityTracker.getMultiplicitiesForPath(sqlRow.getPath()));

    handleColumns(sqlRow);

    if (!isAtLeastOneFeatureWritten) {
      this.isAtLeastOneFeatureWritten = true;
    }

  }

  //TODO: move general parts to NestingTracker
  private void handleNesting(SqlRow sqlRow, List<Integer> indexes)  {
    while (nestingTracker.isNested() &&
        (nestingTracker.doesNotStartWithPreviousPath(sqlRow.getPath()) ||
            (nestingTracker.inObject() && nestingTracker.isSamePath(sqlRow.getPath()) ||
                (nestingTracker.inArray() && nestingTracker.isSamePath(sqlRow.getPath()) && nestingTracker.hasParentIndexChanged(indexes))))) {
      nestingTracker.close();
    }

    if (nestingTracker.inObject() && context.inArray() && nestingTracker.doesStartWithPreviousPath(sqlRow.getPath()) && nestingTracker.hasIndexChanged(indexes)) {
      nestingTracker.closeObject();
      nestingTracker.openObject();
    }

    if (nestingTracker.isNotMain(sqlRow.getPath())) {
      context.pathTracker().track(sqlRow.getPath());

      if (nestingTracker.isFirst(indexes)) {
        nestingTracker.openArray();
      }

      context.setIndexes(indexes);

      nestingTracker.openObject();
    }
  }

  //TODO: schemaIndex
  private void handleColumns(SqlRow sqlRow)  {

    Map<List<String>, Integer> schemaIndexes = new HashMap<>();

    for (int i = 0; i < sqlRow.getValues()
        .size() && i < sqlRow.getColumnPaths()
        .size(); i++) {
      if (Objects.nonNull(sqlRow.getValues()
          .get(i))) {

        context.pathTracker().track(sqlRow.getColumnPaths().get(i));
        if (!schemaIndexes.containsKey(sqlRow.getColumnPaths().get(i))) {
          schemaIndexes.put(sqlRow.getColumnPaths().get(i), 0);
        } else {
          schemaIndexes.put(sqlRow.getColumnPaths().get(i), schemaIndexes.get(sqlRow.getColumnPaths().get(i)) + 1);
        }

        if (sqlRow.getSpatialAttributes().size() > i && Objects.equals(sqlRow.getSpatialAttributes().get(i), true)) {
          try {
            context.setSchemaIndex(-1);
            geometryDecoder.decode((String) sqlRow.getValues().get(i));
          } catch (IOException e) {
            throw new IllegalStateException("Error parsing WKT geometry", e);
          }
        } else {
          //TODO: is that correct or do we need the column type?
          context.setValueType(Type.STRING);
          context.setValue((String) sqlRow.getValues().get(i));
          context.setSchemaIndex(schemaIndexes.get(sqlRow.getColumnPaths().get(i)));
          getDownstream().onValue(context);
        }
      }
    }

    context.setSchemaIndex(-1);

    if (nestingTracker.isNotMain(sqlRow.getPath())) {
      context.pathTracker().track(sqlRow.getPath());
    }
  }
}
