/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyContextTransformer;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertySchemaTransformer;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyTransformerFlatten;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyTransformerFlatten.INCLUDE;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyTransformerObjectReduce;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.features.domain.transform.TransformerChain;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureTokenTransformerSchemaMappings extends FeatureTokenTransformer {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(FeatureTokenTransformerSchemaMappings.class);

  private final Map<String, PropertyTransformations> propertyTransformations;
  private ModifiableContext<FeatureSchema, SchemaMapping> newContext;
  private Map<String, NestingTracker> nestingTrackers;
  private Map<String, TransformerChain<FeatureSchema, FeaturePropertySchemaTransformer>>
      schemaTransformerChains;
  private Map<
          String,
          TransformerChain<
              ModifiableContext<FeatureSchema, SchemaMapping>, FeaturePropertyContextTransformer>>
      contextTransformerChains;
  private NestingTracker nestingTracker;
  private TransformerChain<FeatureSchema, FeaturePropertySchemaTransformer> schemaTransformerChain;
  private TransformerChain<
          ModifiableContext<FeatureSchema, SchemaMapping>, FeaturePropertyContextTransformer>
      contextTransformerChain;
  private Map<String, Boolean> flattened;
  private final List<List<String>> indexedArrays;
  private final List<List<String>> openedArrays;
  private final Set<String> coalesces;

  public FeatureTokenTransformerSchemaMappings(
      Map<String, PropertyTransformations> propertyTransformations) {
    this.propertyTransformations = propertyTransformations;
    this.indexedArrays = new ArrayList<>();
    this.openedArrays = new ArrayList<>();
    this.schemaTransformerChains = new LinkedHashMap<>();
    this.contextTransformerChains = new LinkedHashMap<>();
    this.nestingTrackers = new LinkedHashMap<>();
    this.coalesces = new HashSet<>();
  }

  @Override
  public void onStart(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    // TODO: who uses original, simplify
    // TODO: slow, precompute, same for original in decoder
    ImmutableMap<String, SchemaMapping> mappings =
        getContext().mappings().entrySet().stream()
            .map(
                entry ->
                    new SimpleImmutableEntry<>(
                        entry.getKey(), SchemaMapping.withTargetPaths(entry.getValue())))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
    // SchemaMapping schemaMapping = SchemaMapping.withTargetPaths(getContext().mapping());
    this.newContext =
        createContext()
            .setMappings(mappings)
            .setQuery(getContext().query())
            .setMetadata(getContext().metadata());

    this.schemaTransformerChains =
        mappings.entrySet().stream()
            .map(
                entry ->
                    new SimpleImmutableEntry<>(
                        entry.getKey(),
                        propertyTransformations
                            .get(entry.getKey())
                            .getSchemaTransformations(
                                entry.getValue(),
                                (!(context.query() instanceof FeatureQuery)
                                    || !((FeatureQuery) context.query()).returnsSingleFeature()),
                                this::getFlattenedPropertyPath)))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    this.contextTransformerChains =
        mappings.entrySet().stream()
            .map(
                entry ->
                    new SimpleImmutableEntry<>(
                        entry.getKey(),
                        propertyTransformations
                            .get(entry.getKey())
                            .getContextTransformations(entry.getValue())))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    this.nestingTrackers =
        schemaTransformerChains.entrySet().stream()
            .map(
                entry ->
                    new SimpleImmutableEntry<>(entry.getKey(), getNestingTracker(entry.getValue())))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    this.flattened =
        schemaTransformerChains.entrySet().stream()
            .map(
                entry ->
                    new SimpleImmutableEntry<>(entry.getKey(), flattenObjects(entry.getValue())))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    getDownstream().onStart(newContext);
  }

  private boolean flattenObjects(
      TransformerChain<FeatureSchema, FeaturePropertySchemaTransformer> schemaTransformerChain) {
    return schemaTransformerChain.has(PropertyTransformations.WILDCARD)
        && schemaTransformerChain.get(PropertyTransformations.WILDCARD).stream()
            .anyMatch(
                featurePropertySchemaTransformer ->
                    featurePropertySchemaTransformer instanceof FeaturePropertyTransformerFlatten
                        && (((FeaturePropertyTransformerFlatten) featurePropertySchemaTransformer)
                                    .include()
                                == INCLUDE.ALL
                            || ((FeaturePropertyTransformerFlatten)
                                        featurePropertySchemaTransformer)
                                    .include()
                                == INCLUDE.OBJECTS));
  }

  private NestingTracker getNestingTracker(
      TransformerChain<FeatureSchema, FeaturePropertySchemaTransformer> schemaTransformerChain) {
    boolean flattenObjects = flattenObjects(schemaTransformerChain);

    boolean flattenArrays =
        schemaTransformerChain.has(PropertyTransformations.WILDCARD)
            && schemaTransformerChain.get(PropertyTransformations.WILDCARD).stream()
                .anyMatch(
                    featurePropertySchemaTransformer ->
                        featurePropertySchemaTransformer
                                instanceof FeaturePropertyTransformerFlatten
                            && (((FeaturePropertyTransformerFlatten)
                                            featurePropertySchemaTransformer)
                                        .include()
                                    == INCLUDE.ALL
                                || ((FeaturePropertyTransformerFlatten)
                                            featurePropertySchemaTransformer)
                                        .include()
                                    == INCLUDE.ARRAYS));

    return new NestingTracker(
        getDownstream(), newContext, ImmutableList.of(), flattenObjects, flattenArrays, true);
  }

  @Override
  public void onEnd(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    getDownstream().onEnd(newContext);
  }

  @Override
  public void onFeatureStart(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    this.nestingTracker = nestingTrackers.get(context.type());
    this.schemaTransformerChain = schemaTransformerChains.get(context.type());
    this.contextTransformerChain = contextTransformerChains.get(context.type());
    this.coalesces.clear();

    if (flattened.get(context.type())) {
      newContext.putTransformed(FeaturePropertyTransformerFlatten.TYPE, "TRUE");
    } else {
      newContext.transformed().remove(FeaturePropertyTransformerFlatten.TYPE);
    }

    newContext.pathTracker().track(List.of());

    newContext.setType(context.type());

    // TODO: why is this needed at all, what does withTargetPaths do?
    // if (context.query() instanceof MultiFeatureQuery) {
    // TODO: slow, precompute, same for original in decoder
    // SchemaMapping schemaMapping = SchemaMapping.withTargetPaths(context.mapping());
    // newContext.setMapping(schemaMapping);
    // }

    getDownstream().onFeatureStart(newContext);
  }

  @Override
  public void onFeatureEnd(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    while (nestingTracker.isNested()) {
      // nestingTracker.close();
      if (nestingTracker.inObject()) {
        // nestingTracker.closeObject();
        closeObject();
      } else if (nestingTracker.inArray()) {
        closeArray();
      }
    }

    newContext.pathTracker().track(List.of());

    getDownstream().onFeatureEnd(newContext);
  }

  @Override
  public void onObjectStart(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    if (context.schema().filter(FeatureSchema::isSpatial).isPresent()) {
      handleNesting(context.schema().get(), context.parentSchemas(), context.indexes());

      newContext.pathTracker().track(context.schema().get().getFullPath());
      newContext.setInGeometry(true);
      newContext.setGeometryType(context.geometryType());
      newContext.setGeometryDimension(context.geometryDimension());

      // TODO: warn or error if types do not match?
      if (context.geometryType().isPresent()
          && context.schema().get().getGeometryType().isPresent()) {
        if (context.geometryType().get() != context.schema().get().getGeometryType().get()) {
          newContext.setCustomSchema(
              new ImmutableFeatureSchema.Builder()
                  .from(context.schema().get())
                  .geometryType(context.geometryType().get())
                  .build());
        }
      }

      if (!newContext.shouldSkip()) {
        FeatureSchema transform1 =
            schemaTransformerChain.transform(newContext.pathAsString(), context.schema().get());
        if (Objects.isNull(transform1)) {
          newContext.setCustomSchema(null);
          return;
        }
        newContext.setCustomSchema(transform1);

        getDownstream().onObjectStart(newContext);

        newContext.setCustomSchema(null);
      }
    }
    if (context.schema().filter(FeatureSchema::isObject).isEmpty()) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(
            "OBJECT NOT FOUND {} {}",
            context.pathAsString(),
            Objects.nonNull(context.mapping())
                ? context.mapping().getTargetSchemasByPath().keySet()
                : "{}");
      }
      return;
    }

    handleNesting(context.schema().get(), context.parentSchemas(), context.indexes());
  }

  // TODO: geometry arrays
  @Override
  public void onObjectEnd(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    if (context.schema().filter(FeatureSchema::isSpatial).isPresent()) {
      if (!newContext.shouldSkip()) {
        getDownstream().onObjectEnd(newContext);
      }
      newContext.setInGeometry(false);
      newContext.setGeometryType(Optional.empty());
      newContext.setGeometryDimension(OptionalInt.empty());
    }
  }

  @Override
  public void onArrayStart(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    if (context.inGeometry() && !newContext.shouldSkip()) {
      FeatureSchema transform1 =
          schemaTransformerChain.transform(newContext.pathAsString(), context.schema().get());
      if (Objects.isNull(transform1)) {
        newContext.setCustomSchema(null);
        return;
      }

      getDownstream().onArrayStart(newContext);
    }
    if (context.schema().filter(FeatureSchema::isArray).isEmpty()) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(
            "ARRAY NOT FOUND {} {}",
            context.pathAsString(),
            Objects.nonNull(context.mapping())
                ? context.mapping().getTargetSchemasByPath().keySet()
                : "{}");
      }
      return;
    }

    handleNesting(context.schema().get(), context.parentSchemas(), context.indexes());
  }

  @Override
  public void onArrayEnd(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    if (context.inGeometry() && !newContext.shouldSkip()) {
      getDownstream().onArrayEnd(newContext);
    }
  }

  @Override
  public void onValue(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    if (context.inGeometry()) {
      newContext.setValue(context.value());
      newContext.setValueType(context.valueType());

      FeatureSchema transformed =
          schemaTransformerChain.transform(newContext.pathAsString(), context.schema().get());
      if (Objects.isNull(transformed)) {
        clearValueContext();
        return;
      }
      newContext.setCustomSchema(transformed);

      pushValue();
      return;
    }
    if (context.schema().isEmpty()) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(
            "VALUE NOT FOUND {} {}",
            context.pathAsString(),
            Objects.nonNull(context.mapping())
                ? context.mapping().getTargetSchemasByPath().keySet()
                : "{}");
      }
      return;
    }

    FeatureSchema schema = context.schema().get();

    // TODO: when to clear valueBuffer
    // TODO: what about parent arrays
    // TODO: special value buffer for choice
    if (schema.getEffectiveSourcePaths().size() > 1) {
      String column = context.path().get(context.path().size() - 1);
      String column2 = column;
      for (int i = 0; i < schema.getEffectiveSourcePaths().size(); i++) {
        if (String.join("/", context.path()).endsWith(schema.getEffectiveSourcePaths().get(i))) {
          column2 = schema.getEffectiveSourcePaths().get(i);
        }
      }
      int index = schema.getEffectiveSourcePaths().indexOf(column2);
      if (schema.isArray()) {
        if (index >= 0) {
          List<Integer> indexes =
              new ArrayList<>(
                  index == 0
                      ? context.indexes()
                      : context.indexes().subList(0, Math.max(0, context.indexes().size() - 1)));
          indexes.add(index + 1);
          context.setIndexes(indexes);

          if (schema.getConcat().size() > index) {
            context.setValueType(schema.getConcat().get(index).getType());
          }
        }
      } else if (!schema.getCoalesce().isEmpty()) {
        if (Objects.nonNull(context.value()) && !coalesces.contains(schema.getFullPathAsString())) {
          coalesces.add(schema.getFullPathAsString());
          if (schema.getCoalesce().size() > index) {
            context.setValueType(schema.getCoalesce().get(index).getType());
          }
        } else {
          return;
        }
      }
    }

    if (schema.getAdditionalInfo().containsKey("concatIndex")) {
      int index = Integer.parseInt(schema.getAdditionalInfo().get("concatIndex"));
      int arrayIndex =
          schema.getAdditionalInfo().containsKey("concatArray")
              ? Math.max(0, (int) context.index() - 1)
              : 0;

      if (index >= 1) {
        int prev = Integer.parseInt(context.transformed().get("concatIndex"));
        int prevArray = Integer.parseInt(context.transformed().get("concatArray"));
        if (prev < index || prevArray < arrayIndex) {
          List<Integer> indexes =
              new ArrayList<>(
                  newContext.indexes().subList(0, Math.max(0, newContext.indexes().size() - 1)));
          indexes.add(newContext.indexes().get(newContext.indexes().size() - 1) + 1);

          closeObject();
          newContext.setIndexes(indexes);
          FeatureSchema parentSchema = context.parentSchemas().get(0);
          openObject(parentSchema);
        }
      }
      context.putTransformed("concatIndex", String.valueOf(index));
      context.putTransformed("concatArray", String.valueOf(arrayIndex));
    } else if (schema.getAdditionalInfo().containsKey("coalesceIndex")) {
      int arrayIndex =
          schema.getAdditionalInfo().containsKey("coalesceArray")
              ? Math.max(0, (int) context.index() - 1)
              : 0;
      if (Objects.nonNull(context.value())
          && !coalesces.contains(schema.getParentPath().toString())) {
        coalesces.add(schema.getParentPath().toString());
        context.putTransformed(
            "coalesceIndex", schema.getAdditionalInfo().get("coalesceIndex") + arrayIndex);
      } else if (!Objects.equals(
          schema.getAdditionalInfo().get("coalesceIndex") + arrayIndex,
          context.transformed().get("coalesceIndex"))) {
        return;
      }
    }

    handleNesting(schema, context.parentSchemas(), context.indexes());
    newContext.pathTracker().track(schema.getFullPath());
    newContext.setValue(context.value());
    newContext.setValueType(context.valueType());

    contextTransformerChain.transform(newContext.pathAsString(), newContext);

    FeatureSchema transformed = schemaTransformerChain.transform(newContext.pathAsString(), schema);

    if (Objects.isNull(transformed)) {
      clearValueContext();
      return;
    }

    newContext.setCustomSchema(transformed);
    pushValue();
  }

  private void pushValue() {
    try {
      if (!newContext.shouldSkip()) {
        getDownstream().onValue(newContext);
      }
    } finally {
      clearValueContext();
    }
  }

  private void clearValueContext() {
    newContext.setCustomSchema(null);
    newContext.setValue(null);
    newContext.setValueType(null);
  }

  private void handleNesting(
      FeatureSchema schema, List<FeatureSchema> parentSchemas, List<Integer> indexes) {

    while (nestingTracker.isNested()
        && (nestingTracker.doesNotStartWithPreviousPath(schema.getFullPath())
            || (nestingTracker.inArray()
                && nestingTracker.isSamePath(schema.getFullPath())
                && nestingTracker.hasParentIndexChanged(indexes)))) {

      if (nestingTracker.inObject()) {
        closeObject();
      } else if (nestingTracker.inArray()) {
        closeArray();
      }
    }

    if (nestingTracker.inObject()
        && newContext.inArray()
        && nestingTracker.isSamePath(schema.getFullPath())
        && nestingTracker.hasIndexChanged(indexes)) {
      closeObject();
      newContext.setIndexes(indexes);
      openObject(schema);
    }

    if (schema.isArray() && !nestingTracker.isSamePath(schema.getFullPath())) {
      openParents(parentSchemas, indexes);
      newContext.pathTracker().track(schema.getFullPath());
      openArray(schema);
    } else if (schema.isObject() && schema.isArray() && nestingTracker.isFirst(indexes)) {
      newContext.pathTracker().track(schema.getFullPath());
      newContext.setIndexes(indexes);
      openObject(schema);
    } else if (schema.isObject()
        && !schema.isArray()
        && !nestingTracker.isSamePath(schema.getFullPath())) {
      openParents(parentSchemas, indexes);
      newContext.pathTracker().track(schema.getFullPath());
      openObject(schema);
    } else if (schema.isValue() && (!schema.isArray() || nestingTracker.isFirst(indexes))) {
      openParents(parentSchemas, indexes);
    }

    if (schema.isValue() && schema.isArray()) {
      newContext.setIndexes(indexes);
    }
  }

  private void openObject(FeatureSchema schema) {
    contextTransformerChain.transform(newContext.pathAsString(), newContext);

    FeatureSchema transform1 = schemaTransformerChain.transform(newContext.pathAsString(), schema);

    if (Objects.isNull(transform1)) {
      newContext.setCustomSchema(null);
      return;
    }

    newContext.setCustomSchema(transform1);

    nestingTracker.openObject();

    newContext.setCustomSchema(null);
  }

  private void openArray(FeatureSchema schema) {
    contextTransformerChain.transform(newContext.pathAsString(), newContext);

    FeatureSchema transform1 = schemaTransformerChain.transform(newContext.pathAsString(), schema);

    if (Objects.isNull(transform1)) {
      newContext.setCustomSchema(null);
      return;
    }

    newContext.setCustomSchema(transform1);

    nestingTracker.openArray();

    newContext.setCustomSchema(null);
  }

  private void closeObject() {
    newContext.pathTracker().track(nestingTracker.getCurrentNestingPath());
    String path = newContext.pathAsString();

    if (newContext.transformed().containsKey(path)
        && newContext.transformed().get(path).equals(FeaturePropertyTransformerObjectReduce.TYPE)) {

      contextTransformerChain.transform(path, newContext);

      try {
        if (!newContext.shouldSkip()) {
          getDownstream().onValue(newContext);
        }
      } catch (Throwable e) {
        throw e;
      } finally {

        newContext.setCustomSchema(null);
        newContext.setValue(null);
        newContext.setValueType(null);
        newContext.valueBuffer().clear();

        newContext.setIsBuffering(true);

        nestingTracker.closeObject();

        newContext.setIsBuffering(false);
      }
    } else {
      nestingTracker.closeObject();
    }
  }

  private void closeArray() {
    newContext.pathTracker().track(nestingTracker.getCurrentNestingPath());

    nestingTracker.closeArray();

    if (newContext.indexes().size() > nestingTracker.arrayDepth()) {
      newContext.setIndexes(
          new ArrayList<>(newContext.indexes().subList(0, nestingTracker.arrayDepth())));
    }
  }

  private void openParents(List<FeatureSchema> parentSchemas, List<Integer> indexes) {
    if (parentSchemas.size() < 2) {
      return;
    }

    FeatureSchema parent = parentSchemas.get(0);

    if (parent.getSourcePath().isPresent()) {
      return;
    }

    List<Integer> newIndexes = new ArrayList<>(newContext.indexes());
    List<List<String>> arrays = new ArrayList<>();

    for (int i = parentSchemas.size() - 1; i >= 0; i--) {
      FeatureSchema schema = parentSchemas.get(i);

      if (schema.getType() == Type.OBJECT_ARRAY && schema.getSourcePath().isEmpty()) {
        arrays.add(schema.getFullPath());
        if (!indexedArrays.contains(schema.getFullPath())) {
          indexedArrays.add(schema.getFullPath());
          newIndexes.add(1);
          newContext.setIndexes(newIndexes);
        }
      }
    }

    indexedArrays.removeIf(strings -> !arrays.contains(strings));
    openedArrays.removeIf(strings -> !arrays.contains(strings));

    if (parent.isArray()) {
      if (!openedArrays.contains(parent.getFullPath())) {
        handleNesting(parent, parentSchemas.subList(1, parentSchemas.size()), newIndexes);
        if (parent.isObject()) {
          handleNesting(parent, parentSchemas.subList(1, parentSchemas.size()), newIndexes);
        }
        openedArrays.add(parent.getFullPath());
      }
    } else if (parent.isObject()) {
      handleNesting(parent, parentSchemas.subList(1, parentSchemas.size()), newIndexes);
    }
  }

  private String getFlattenedPropertyPath(String separator, String name) {
    return nestingTracker.getFlattenedPropertyPath(separator, name);
  }
}
