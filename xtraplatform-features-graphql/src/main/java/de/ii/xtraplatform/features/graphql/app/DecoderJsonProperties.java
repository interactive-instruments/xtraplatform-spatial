/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.graphql.app;

import com.fasterxml.jackson.core.JsonToken;
import de.ii.xtraplatform.features.domain.Decoder.Pipeline;
import de.ii.xtraplatform.features.domain.FeatureEventHandler;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.MultiplicityTracker;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaMapping;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DecoderJsonProperties {

  private static final Logger LOGGER = LoggerFactory.getLogger(DecoderJsonProperties.class);

  private final ModifiableContext<FeatureSchema, SchemaMapping> context;
  private final FeatureEventHandler<
          FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>>
      downstream;
  private final List<List<String>> arrayPaths;
  private final MultiplicityTracker multiplicityTracker;
  private final Supplier<String> getValueAsString;
  private final Optional<String> nullValue;

  private int depth;

  public DecoderJsonProperties(
      Pipeline pipeline,
      List<List<String>> arrayPaths,
      Supplier<String> getValueAsString,
      Optional<String> nullValue) {
    this.context = pipeline.context();
    this.downstream = pipeline.downstream();
    this.arrayPaths = arrayPaths;
    this.multiplicityTracker = new MultiplicityTracker(arrayPaths);
    this.getValueAsString = getValueAsString;
    this.nullValue = nullValue;
    this.depth = 0;
  }

  public void reset() {
    multiplicityTracker.reset();
  }

  public boolean parse(JsonToken nextToken, String currentName, int featureDepth) {

    boolean feedMeMore = false;

    // TODO: null is end-of-input
    if (Objects.isNull(nextToken)) {
      return true; // or completestage???
    }

    switch (nextToken) {
      case NOT_AVAILABLE:
        feedMeMore = true;
        break;

      case FIELD_NAME:
        break;

      case START_OBJECT:
        if (Objects.nonNull(currentName)) {
          context.pathTracker().track(currentName);
        }

        // nested array_object start
        if (Objects.isNull(currentName)) {
          multiplicityTracker.track(context.pathTracker().asList());
          context.setIndexes(
              multiplicityTracker.getMultiplicitiesForPath(context.pathTracker().asList()));
          LOGGER.debug("ARR_OBJ {} {} {}", context.pathAsString(), context.indexes(), depth);
          downstream.onObjectStart(context);
          // feature in collection start
        }
        // nested object start?
        else if (Objects.nonNull(currentName)
            || arrayPaths.contains(context.pathTracker().asList())) {
          depth += 1;
          LOGGER.debug("OBJ {} {} {}", context.pathAsString(), context.indexes(), depth);
          downstream.onObjectStart(context);
        }
        break;

      case START_ARRAY:
        if (Objects.nonNull(currentName)) {
          context.pathTracker().track(currentName);
          depth += 1;
          LOGGER.debug("ARR {} {} {}", context.pathAsString(), context.indexes(), depth);
          downstream.onArrayStart(context);
        }
        break;

      case END_ARRAY:
        if (Objects.nonNull(currentName)) {
          downstream.onArrayEnd(context);

          depth -= 1;

          context.pathTracker().track(depth + featureDepth);
          LOGGER.debug("E ARR {} {} {}", context.pathAsString(), context.indexes(), depth);
        }
        break;

      case END_OBJECT:

        // end nested object
        if (Objects.nonNull(currentName)) {
          downstream.onObjectEnd(context);

          depth -= 1;
          // nested array_object end
        } else if (arrayPaths.contains(context.pathTracker().asList())) {
          downstream.onObjectEnd(context);
        }

        context.pathTracker().track(depth + featureDepth);
        LOGGER.debug("E OBJ {} {} {}", context.pathAsString(), context.indexes(), depth);
        break;

      case VALUE_STRING:
      case VALUE_NUMBER_INT:
      case VALUE_NUMBER_FLOAT:
      case VALUE_TRUE:
      case VALUE_FALSE:
      case VALUE_NULL:
        switch (nextToken) {
          case VALUE_STRING:
          case VALUE_NULL:
            context.setValueType(Type.STRING);
            break;
          case VALUE_NUMBER_INT:
            context.setValueType(Type.INTEGER);
            break;
          case VALUE_NUMBER_FLOAT:
            context.setValueType(Type.FLOAT);
            break;
          case VALUE_TRUE:
          case VALUE_FALSE:
            context.setValueType(Type.BOOLEAN);
            break;
        }

        context.pathTracker().track(currentName);

        if (nextToken == JsonToken.VALUE_NULL && nullValue.isPresent()) {
          context.setValue(nullValue.get());
        } else {
          context.setValue(getValueAsString.get());
        }

        downstream.onValue(context);

        context.pathTracker().track(depth + featureDepth);

        break;
    }

    return feedMeMore;
  }
}
