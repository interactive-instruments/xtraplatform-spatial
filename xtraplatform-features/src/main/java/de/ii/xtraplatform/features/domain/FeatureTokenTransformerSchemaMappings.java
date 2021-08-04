/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertySchemaTransformer;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyTransformerFlatten;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyTransformerFlatten.INCLUDE;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureTokenTransformerSchemaMappings extends FeatureTokenTransformer {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(FeatureTokenTransformerSchemaMappings.class);

  private final PropertyTransformations propertyTransformations;
  private final boolean isOverview;
  private Map<String, List<FeaturePropertySchemaTransformer>> propertySchemaTransformers;
  private ModifiableContext newContext;
  private NestingTracker nestingTracker;

  public FeatureTokenTransformerSchemaMappings(
      PropertyTransformations propertyTransformations, FeatureQuery query) {
    this.propertyTransformations = propertyTransformations;
    this.isOverview = !query.returnsSingleFeature();
  }

  @Override
  public void onStart(ModifiableContext context) {
    //TODO: slow, precompute, same for original in decoder
    SchemaMapping schemaMapping = SchemaMapping.withTargetPaths(getContext().mapping());
    this.newContext = createContext()
        .setMapping(schemaMapping)
        .setQuery(getContext().query())
        .setMetadata(getContext().metadata());

    this.propertySchemaTransformers = propertyTransformations.getSchemaTransformations(isOverview, this::getFlattenedPropertyPath);

    boolean flattenObjects = propertySchemaTransformers.containsKey(PropertyTransformations.WILDCARD)
        && propertySchemaTransformers.get(PropertyTransformations.WILDCARD)
        .stream()
        .anyMatch(featurePropertySchemaTransformer -> featurePropertySchemaTransformer instanceof FeaturePropertyTransformerFlatten
        && (((FeaturePropertyTransformerFlatten) featurePropertySchemaTransformer).include() == INCLUDE.ALL
        || ((FeaturePropertyTransformerFlatten) featurePropertySchemaTransformer).include() == INCLUDE.OBJECTS));

    boolean flattenArrays = propertySchemaTransformers.containsKey(PropertyTransformations.WILDCARD)
        && propertySchemaTransformers.get(PropertyTransformations.WILDCARD)
        .stream()
        .anyMatch(featurePropertySchemaTransformer -> featurePropertySchemaTransformer instanceof FeaturePropertyTransformerFlatten
            && (((FeaturePropertyTransformerFlatten) featurePropertySchemaTransformer).include() == INCLUDE.ALL
            || ((FeaturePropertyTransformerFlatten) featurePropertySchemaTransformer).include() == INCLUDE.ARRAYS));

    this.nestingTracker = new NestingTracker(getDownstream(), newContext, ImmutableList.of(), flattenObjects, flattenArrays);

    if (flattenObjects) {
      newContext.putTransformed(FeaturePropertyTransformerFlatten.TYPE, "TRUE");
    }

    getDownstream().onStart(newContext);
  }

  @Override
  public void onEnd(ModifiableContext context) {
    getDownstream().onEnd(newContext);
  }

  @Override
  public void onFeatureStart(ModifiableContext context) {
    getDownstream().onFeatureStart(newContext);
  }

  @Override
  public void onFeatureEnd(ModifiableContext context) {
    while (nestingTracker.isNested()) {
      nestingTracker.close();
    }

    getDownstream().onFeatureEnd(newContext);
  }

  @Override
  public void onObjectStart(ModifiableContext context) {
    if (context.schema()
        .filter(FeatureSchema::isGeometry)
        .isPresent()) {
      handleNesting(context.schema().get(), context.parentSchemas(), context.indexes());

      newContext.pathTracker().track(context.schema().get().getFullPath());
      newContext.setInGeometry(true);
      newContext.setGeometryType(context.geometryType());
      newContext.setGeometryDimension(context.geometryDimension());

      //TODO: warn or error if types do not match?
      if (context.geometryType().isPresent() && context.schema().get().getGeometryType().isPresent()) {
        if (context.geometryType().get() != context.schema().get().getGeometryType().get()) {
          newContext.setCustomSchema(new ImmutableFeatureSchema.Builder().from(context.schema().get()).geometryType(context.geometryType().get()).build());
        }
      }

      getDownstream().onObjectStart(newContext);
    }
    if (context.schema()
        .filter(FeatureSchema::isObject)
        .isEmpty()) {
      return;
    }

    handleNesting(context.schema().get(), context.parentSchemas(), context.indexes());
  }
  //TODO: geometry arrays
  @Override
  public void onObjectEnd(ModifiableContext context) {
    if (context.schema()
        .filter(FeatureSchema::isGeometry)
        .isPresent()) {
      newContext.setInGeometry(false);
      newContext.setGeometryType(Optional.empty());
      newContext.setGeometryDimension(OptionalInt.empty());
      getDownstream().onObjectEnd(newContext);
    }
  }

  @Override
  public void onArrayStart(ModifiableContext context) {
    if (context.inGeometry()) {
      getDownstream().onArrayStart(newContext);
    }
    if (context.schema()
        .filter(FeatureSchema::isArray)
        .isEmpty()) {
      return;
    }

    handleNesting(context.schema().get(), context.parentSchemas(), context.indexes());
  }

  @Override
  public void onArrayEnd(ModifiableContext context) {
    if (context.inGeometry()) {
      getDownstream().onArrayEnd(newContext);
    }
  }

  @Override
  public void onValue(ModifiableContext context) {
    if (context.schema().isEmpty()) {
      return;
    }

    if (!context.inGeometry()) {
      handleNesting(context.schema().get(), context.parentSchemas(), context.indexes());
      newContext.pathTracker().track(context.schema().get().getFullPath());
    }

    for (FeaturePropertySchemaTransformer schemaTransformer : propertySchemaTransformers.getOrDefault(newContext.pathTracker().toString(),
        ImmutableList.of())) {
      FeatureSchema transform = schemaTransformer.transform(newContext.schema().get());
      if (Objects.isNull(transform)) {
        return;
      }
      newContext.setCustomSchema(transform);
    }

    for (FeaturePropertySchemaTransformer schemaTransformer : propertySchemaTransformers.getOrDefault("*",
        ImmutableList.of())) {
      FeatureSchema transform = schemaTransformer.transform(newContext.schema().get());
      if (Objects.isNull(transform)) {
        return;
      }
      newContext.setCustomSchema(transform);
    }

    newContext.setValue(context.value());
    newContext.setValueType(context.valueType());

    try {
      getDownstream().onValue(newContext);
    } catch (Throwable e) {
      throw e;
    } finally {
      newContext.setCustomSchema(null);
    }

  }

  private void handleNesting(FeatureSchema schema, List<FeatureSchema> parentSchemas,
      List<Integer> indexes) {

    while (nestingTracker.isNested() &&
        (nestingTracker.doesNotStartWithPreviousPath(schema.getFullPath()) ||
            (/*nestingTracker.inObject() && nestingTracker.isSamePath(schema.getFullPath()) ||*/
                (nestingTracker.inArray() && nestingTracker.isSamePath(schema.getFullPath())
                    && nestingTracker.hasParentIndexChanged(indexes))))) {
      nestingTracker.close();
    }

    if (nestingTracker.inObject() && newContext.inArray() && nestingTracker
        .doesStartWithPreviousPath(schema.getFullPath()) && nestingTracker
        .hasIndexChanged(indexes)) {
      nestingTracker.closeObject();
      newContext.setIndexes(indexes);
      nestingTracker.openObject();
    }

    if (schema.isArray() && /*nestingTracker.isFirst(indexes) &&*/ !nestingTracker
        .isSamePath(schema.getFullPath())) {
      openParents(parentSchemas, indexes);
      newContext.pathTracker().track(schema.getFullPath());
      nestingTracker.openArray();
    } else if (schema.isObject() && schema.isArray() && nestingTracker.isFirst(indexes)) {
      newContext.pathTracker().track(schema.getFullPath());
      newContext.setIndexes(indexes);
      nestingTracker.openObject();
    } else if (schema.isObject() && !schema.isArray() && !nestingTracker
        .isSamePath(schema.getFullPath())) {
      openParents(parentSchemas, indexes);
      newContext.pathTracker().track(schema.getFullPath());
      nestingTracker.openObject();
    } else if (schema.isValue() && (!schema.isArray() || nestingTracker.isFirst(indexes))) {
      openParents(parentSchemas, indexes);
    }
  }

  private void openParents(List<FeatureSchema> parentSchemas, List<Integer> indexes) {
    if (parentSchemas.isEmpty()) {
      return;
    }

    FeatureSchema parent = parentSchemas.get(0);

    if (parent.getSourcePath().isPresent()) {
      return;
    }

    if (parent.isArray()) {
      handleNesting(parent, parentSchemas.subList(1, parentSchemas.size()), indexes);
    }
    if (parent.isObject()) {
      handleNesting(parent, parentSchemas.subList(1, parentSchemas.size()), indexes);
    }
  }

  private String getFlattenedPropertyPath(String separator, String name) {
    return nestingTracker.getFlattenedPropertyPath(separator, name);
  }
}
