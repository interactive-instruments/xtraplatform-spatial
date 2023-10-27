/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.transform.FeatureEventBuffer;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureTokenTransformerMappings extends FeatureTokenTransformer {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(FeatureTokenTransformerMappings.class);
  private FeatureEventBuffer<
          FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>>
      downstream;
  private NestingTracker2 nestingTracker;

  public FeatureTokenTransformerMappings(
      Map<String, PropertyTransformations> propertyTransformations) {}

  @Override
  protected void init() {
    super.init();
  }

  @Override
  public void onStart(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    this.downstream = new FeatureEventBuffer<>(getDownstream(), context);
    // featureWriter.forEach(f -> featureWriters.add(f.apply(downstream)));
    // executePipeline(featureWriters.iterator()).accept(FeatureTokenType.INPUT, context);

    this.nestingTracker = new NestingTracker2(downstream);
    downstream.onStart(context);
  }

  @Override
  public void onEnd(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    // executePipeline(featureWriters.iterator()).accept(FeatureTokenType.INPUT_END, context);

    downstream.onEnd(context);
  }

  @Override
  public void onFeatureStart(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    // executePipeline(featureWriters.iterator()).accept(FeatureTokenType.FEATURE, context);

    downstream.onFeatureStart(context);
    downstream.bufferStart();
    downstream.next(0);
  }

  @Override
  public void onFeatureEnd(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    // executePipeline(featureWriters.iterator()).accept(FeatureTokenType.FEATURE_END, context);

    while (nestingTracker.isNested()) {
      if (nestingTracker.inObject()) {
        context.pathTracker().track(nestingTracker.getCurrentNestingPayload());
        int pos = context.pos();
        if (pos > -1) {
          downstream.next(pos);
          nestingTracker.closeObject();
        }
      } else if (nestingTracker.inArray()) {
        context.pathTracker().track(nestingTracker.getCurrentNestingPayload());
        int pos = context.pos();
        if (pos > -1) {
          downstream.next(pos);
          nestingTracker.closeArray();
        }
      }
    }

    downstream.bufferStop(true);
    downstream.onFeatureEnd(context);
  }

  @Override
  public void onObjectStart(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    int pos = context.pos();
    if (pos > -1) {
      downstream.next(pos, context.parentPos());
      // executePipeline(featureWriters.iterator()).accept(FeatureTokenType.OBJECT, context);

      if (context.schema().filter(schema -> schema.isObject() || schema.isSpatial()).isPresent()) {
        FeatureSchema schema = context.schema().get();
        // if (!nestingTracker.isSamePath(schema.getFullPath())) {
        nestingTracker.openObject(schema.getFullPath(), context.path(), context.geometryType());
        // }
      }
    }
  }

  @Override
  public void onObjectEnd(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    int pos = context.pos();
    if (pos > -1) {
      downstream.next(pos, context.parentPos());
      // executePipeline(featureWriters.iterator()).accept(FeatureTokenType.OBJECT_END, context);

      nestingTracker.closeObject();
    }
  }

  @Override
  public void onArrayStart(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    int pos = context.pos();
    if (pos > -1) {
      downstream.next(pos, context.parentPos());
      // executePipeline(featureWriters.iterator()).accept(FeatureTokenType.ARRAY, context);

      if (context.schema().filter(schema -> schema.isArray() || schema.isSpatial()).isPresent()) {
        FeatureSchema schema = context.schema().get();
        // if (!nestingTracker.isSamePath(schema.getFullPath())) {
        nestingTracker.openArray(schema.getFullPath(), context.path());
        // }
      }
    }
  }

  @Override
  public void onArrayEnd(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    int pos = context.pos();
    if (pos > -1) {
      downstream.next(pos, context.parentPos());
      // executePipeline(featureWriters.iterator()).accept(FeatureTokenType.ARRAY_END, context);

      nestingTracker.closeArray();
    }
  }

  @Override
  public void onValue(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    downstream.next(context.pos(), context.parentPos());
    // executePipeline(featureWriters.iterator()).accept(FeatureTokenType.VALUE, context);

    if (context.schema().filter(FeatureSchema::isArray).isPresent()) {
      FeatureSchema schema = context.schema().get();
      if (!nestingTracker.isSamePath(schema.getFullPath())) {
        nestingTracker.openArray(schema.getFullPath(), context.path());
      }
    }

    if (context.schema().filter(FeatureSchema::isValue).isPresent()) {
      FeatureSchema schema = context.schema().get();
      downstream.onValue(
          /*schema.getFullPath()*/ context.path(), context.value(), context.valueType());
    }
  }
}
