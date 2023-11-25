/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.transform.DynamicTargetSchemaTransformer;
import de.ii.xtraplatform.features.domain.transform.FeatureEventBuffer;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyTransformerFlatten;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.features.domain.transform.SchemaTransformerChain;
import de.ii.xtraplatform.features.domain.transform.TokenSliceTransformerChain;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureTokenTransformerMappings extends FeatureTokenTransformer {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(FeatureTokenTransformerMappings.class);
  private static final boolean USE_TRACKER = false;

  private final Map<String, PropertyTransformations> propertyTransformations;
  private Map<String, SchemaTransformerChain> schemaTransformerChains;
  private Map<String, TokenSliceTransformerChain> sliceTransformerChains;
  private FeatureEventBuffer<
          FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>>
      downstream;
  private ModifiableContext<FeatureSchema, SchemaMapping> newContext;
  private NestingTracker2 nestingTracker;

  public FeatureTokenTransformerMappings(
      Map<String, PropertyTransformations> propertyTransformations) {
    this.propertyTransformations = propertyTransformations;
  }

  @Override
  protected void init() {
    super.init();
  }

  @Override
  public void onStart(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    this.schemaTransformerChains =
        context.mappings().entrySet().stream()
            .map(
                entry ->
                    new SimpleImmutableEntry<>(
                        entry.getKey(),
                        propertyTransformations
                            .get(entry.getKey())
                            .getSchemaTransformations(
                                entry.getValue(),
                                (!(context.query() instanceof FeatureQuery)
                                    || !((FeatureQuery) context.query()).returnsSingleFeature()))))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    this.sliceTransformerChains =
        context.mappings().entrySet().stream()
            .map(
                entry ->
                    new SimpleImmutableEntry<>(
                        entry.getKey(),
                        propertyTransformations
                            .get(entry.getKey())
                            .getTokenSliceTransformations(entry.getValue())))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    Map<String, SchemaMapping> newMappings =
        context.mappings().entrySet().stream()
            .map(
                entry ->
                    Map.entry(
                        entry.getKey(),
                        new ImmutableSchemaMapping.Builder()
                            .from(entry.getValue())
                            .dynamicTransformer(
                                sliceTransformerChains
                                        .get(entry.getKey())
                                        .has(PropertyTransformations.WILDCARD)
                                    ? sliceTransformerChains
                                        .get(entry.getKey())
                                        .get(PropertyTransformations.WILDCARD)
                                        .stream()
                                        .filter(
                                            transformer ->
                                                transformer
                                                    instanceof FeaturePropertyTransformerFlatten)
                                        .map(flatten -> (DynamicTargetSchemaTransformer) flatten)
                                        .findFirst()
                                    : Optional.empty())
                            .targetSchema(
                                entry
                                    .getValue()
                                    .getTargetSchema()
                                    .accept(schemaTransformerChains.get(entry.getKey()))
                                    .accept(sliceTransformerChains.get(entry.getKey())))
                            .build()))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    this.newContext =
        createContext()
            .setMappings(newMappings)
            .setQuery(context.query())
            .setMetadata(context.metadata())
            .setIsUseTargetPaths(true);

    this.downstream = new FeatureEventBuffer<>(getDownstream(), newContext, context.mappings());

    this.nestingTracker = new NestingTracker2(downstream);
    downstream.onStart(newContext);
  }

  @Override
  public void onEnd(ModifiableContext<FeatureSchema, SchemaMapping> context) {

    downstream.onEnd(newContext);
  }

  @Override
  public void onFeatureStart(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    newContext.pathTracker().track(List.of());
    newContext.setType(context.type());

    downstream.onFeatureStart(newContext);
    downstream.bufferStart();
    downstream.next(0);

    // applyTokenSliceTransformers(context.type(), false);
  }

  @Override
  public void onFeatureEnd(ModifiableContext<FeatureSchema, SchemaMapping> context) {

    // TODO: dangerous, infinite loop
    if (USE_TRACKER) {
      while (nestingTracker.isNested()) {
        if (nestingTracker.inObject()) {
          context.pathTracker().track(nestingTracker.getCurrentNestingPayload2());
          int pos = context.pos();

          if (pos > -1) {
            downstream.next(pos, context.parentPos());
            nestingTracker.closeObject(context.mapping());
          }
        } else if (nestingTracker.inArray()) {
          context.pathTracker().track(nestingTracker.getCurrentNestingPayload2());
          int pos = context.pos();
          if (pos > -1) {
            downstream.next(pos, context.parentPos());
            nestingTracker.closeArray(context.mapping());
          }
        }
      }
    }

    // System.out.println(downstream.toFixture("STUFF"));

    applyTokenSliceTransformers(context.type(), true);

    // System.out.println(downstream.toFixture("STUFF2"));

    downstream.bufferStop(true);

    newContext.pathTracker().track(List.of());
    downstream.onFeatureEnd(newContext);
  }

  private void applyTokenSliceTransformers(String type, boolean post) {
    sliceTransformerChains.get(type).transform(downstream);
  }

  @Override
  public void onObjectStart(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    int pos = context.pos();
    if (pos > -1) {
      downstream.next(pos, context.parentPos());

      if (context.schema().filter(schema -> schema.isObject() || schema.isSpatial()).isPresent()) {
        FeatureSchema schema = context.schema().get();
        if (USE_TRACKER) {
          if (schema.isSpatial()) {
            nestingTracker.openObject(
                schema,
                context.path(),
                context.indexes(),
                context.geometryType(),
                context.mapping());
          } else {
            nestingTracker.open(schema, context);
          }
        } else {
          downstream.onObjectStart(
              schema.getFullPath(), context.geometryType(), context.geometryDimension());
        }
      }
    }
  }

  @Override
  public void onObjectEnd(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    int pos = context.pos();
    if (pos > -1) {
      downstream.next(pos, context.parentPos());

      if (USE_TRACKER) {
        nestingTracker.closeObject(context.mapping());
      } else {
        if (context
            .schema()
            .filter(schema -> schema.isObject() || schema.isSpatial())
            .isPresent()) {
          FeatureSchema schema = context.schema().get();
          downstream.onObjectEnd(schema.getFullPath());
        }
      }
    }
  }

  @Override
  public void onArrayStart(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    int pos = context.pos();
    if (pos > -1) {
      downstream.next(pos, context.parentPos());

      if (context.schema().filter(schema -> schema.isArray() || schema.isSpatial()).isPresent()) {
        FeatureSchema schema = context.schema().get();
        if (USE_TRACKER) {
          if (schema.isSpatial()) {
            nestingTracker.openArray(schema, context.path(), context.indexes(), context.mapping());
          } else {
            nestingTracker.open(schema, context);
          }
        } else {
          downstream.onArrayStart(schema.getFullPath());
        }
      }
    }
  }

  @Override
  public void onArrayEnd(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    int pos = context.pos();
    if (pos > -1) {
      downstream.next(pos, context.parentPos());

      if (USE_TRACKER) {
        nestingTracker.closeArray(context.mapping());
      } else {
        if (context.schema().filter(schema -> schema.isArray() || schema.isSpatial()).isPresent()) {
          FeatureSchema schema = context.schema().get();
          downstream.onArrayEnd(schema.getFullPath());
        }
      }
    }
  }

  @Override
  public void onValue(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    int pos = context.pos();
    if (pos > -1) {
      downstream.next(pos, context.parentPos());

      if (USE_TRACKER && context.schema().filter(FeatureSchema::isArray).isPresent()) {
        FeatureSchema schema = context.schema().get();

        if (schema.getSourcePaths().size() > 1) {
          ArrayList<Integer> parentPos = new ArrayList<>(context.parentPos());
          parentPos.addAll(context.mapping().getPositionsForTargetPath(schema.getFullPath()));

          downstream.next(pos, parentPos);
        }

        if (!nestingTracker.isSamePath(schema.getFullPath())) {
          nestingTracker.openArray(schema, context.path(), context.indexes(), context.mapping());
        }
      }

      if (context.schema().filter(FeatureSchema::isValue).isPresent()) {
        FeatureSchema schema = context.schema().get();

        if (USE_TRACKER) {
          nestingTracker.open(schema, context);
        }

        Type valueType =
            schema.isSpatial()
                ? context.valueType()
                : schema.getValueType().orElse(schema.getType());

        downstream.onValue(schema.getFullPath(), context.value(), valueType);
      }
    }
  }
}
