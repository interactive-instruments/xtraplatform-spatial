/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.json.domain;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import de.ii.xtraplatform.features.domain.Decoder;
import de.ii.xtraplatform.features.domain.FeatureEventHandler;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.FeaturePathTracker;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaMapping;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DecoderJson implements Decoder {

  private static final Logger LOGGER = LoggerFactory.getLogger(DecoderJson.class);
  private static final JsonFactory JSON_FACTORY = new JsonFactory();

  private final JsonParser parser;
  private final ByteArrayFeeder feeder;
  private final String type;
  private final String wrapper;
  private final Optional<String> nullValue;

  private boolean started;
  private int depth = -1;
  private int featureDepth = 0;
  private boolean inFeature = false;
  private boolean inProperties = false;
  private int lastNameIsArrayDepth = 0;
  private int startArray = 0;
  private int endArray = 0;

  public DecoderJson(String type, String wrapper, Optional<String> nullValue) {
    try {
      this.parser = JSON_FACTORY.createNonBlockingByteArrayParser();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    this.feeder = (ByteArrayFeeder) parser.getNonBlockingInputFeeder();
    this.type = type;
    this.wrapper = wrapper;
    this.nullValue = nullValue;
  }

  @Override
  public void decode(byte[] data, Pipeline pipeline) {
    LOGGER.debug("DECODE {}", new String(data, StandardCharsets.UTF_8));

    // TODO: optional wrapper and type, partial decoders?
    /*try {
      feeder.feedInput(data, 0, data.length);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    ModifiableContext<FeatureSchema, SchemaMapping> context = pipeline.context();
    FeaturePathTracker pathTracker = context.pathTracker();
    FeatureEventHandler<
            FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>>
        downstream = pipeline.downstream();

    boolean feedMeMore = false;
    while (!feedMeMore) {
      feedMeMore = advanceParser(context, pathTracker, downstream);
    }*/
  }

  @Override
  public void close() throws Exception {
    feeder.endOfInput();
  }

  private boolean advanceParser(
      ModifiableContext<FeatureSchema, SchemaMapping> context,
      FeaturePathTracker pathTracker,
      FeatureEventHandler<
              FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>>
          downstream) {

    boolean feedMeMore = false;

    try {
      JsonToken nextToken = parser.nextToken();
      String currentName = parser.currentName();

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
          if (Objects.nonNull(currentName) && !started) {
            if (Objects.equals(currentName, "data")) {
              break;
            }
            if (Objects.equals(currentName, wrapper)) {
              startIfNecessary(false, context, downstream);
              inProperties = true;
              featureDepth = depth;
              pathTracker.track(type, 0);
            }
          }
          if (Objects.nonNull(currentName) && started) {
            switch (currentName) {
                /*case "properties":
                  inProperties = true;
                  featureDepth = depth;
                  context.pathTracker().track(0);
                  break;
                case "geometry":
                  context.setInGeometry(true);
                  context.pathTracker().track(currentName, 0);
                  break;*/
              default:
                if (inProperties /* || inGeometry*/) {
                  // context.pathTracker().track(currentName);
                }
                break;
            }
            // nested array_object start
          } else if (depth > featureDepth && started) {
            downstream.onObjectStart(context);
            // feature in collection start
          } else if (depth == featureDepth && inFeature) {
            // inFeature = false;
            pathTracker.track(type, 0);
            downstream.onFeatureStart(context);
          }

          // nested object start?
          if (Objects.nonNull(currentName) || lastNameIsArrayDepth == 0) {
            depth += 1;
            if (depth > featureDepth && (inProperties /*|| inGeometry*/)
            /*&& !Objects.equals(currentName, "properties")*/ ) {
              // getDownstream().onObjectStart(context);
            }
          }
          break;

        case START_ARRAY:
          // start features array
          if (depth == 0 && Objects.nonNull(currentName)) {
            if (Objects.equals(currentName, wrapper)) {
              startIfNecessary(true, context, downstream);
              inProperties = true;
              // featureDepth = depth;
              pathTracker.track(type, 0);
            }
            // start prop/coord array
          } else if (Objects.nonNull(currentName) && (inProperties || context.inGeometry())) {
            if (!context.inGeometry()) {
              pathTracker.track(currentName);
            }
            lastNameIsArrayDepth += 1;
            depth += 1;

            downstream.onArrayStart(context);
            // start nested geo array
          } else if (context.inGeometry()) {
            if (endArray > 0) {
              for (int i = 0; i < endArray - 1; i++) {
                downstream.onArrayEnd(context);
              }
              endArray = 0;
            }
            depth += 1;
            startArray++;
          }
          break;

        case END_ARRAY:
          // end features array
          if (depth == 0 && Objects.nonNull(currentName)) {
            if (Objects.equals(currentName, wrapper)) {
              inFeature = false;
            }
            // end prop/coord array
          } else if (Objects.nonNull(currentName) && (inProperties || context.inGeometry())) {
            if (endArray > 0) {
              for (int i = 0; i < endArray - 1; i++) {
                downstream.onArrayEnd(context);
              }
              endArray = 0;
            }

            downstream.onArrayEnd(context);

            if (context.inGeometry()) {
              pathTracker.track(depth - featureDepth);
            }
            depth -= 1;
            if (inProperties) {
              pathTracker.track(depth - featureDepth);
            }
            lastNameIsArrayDepth -= 1;
            // end nested geo array
          } else if (context.inGeometry()) {
            endArray++;
            depth -= 1;
            pathTracker.track(depth - featureDepth);
          }
          break;

        case END_OBJECT:

          // end nested object
          if (Objects.nonNull(currentName) || lastNameIsArrayDepth == 0) {
            if (depth > featureDepth && (inProperties || context.inGeometry())
            /*&& !Objects.equals(currentName, "properties")*/ ) {
              // getDownstream().onObjectEnd(context);
            }

            // end geo
            if (context.inGeometry()) {
              pathTracker.track(depth - featureDepth);
            }

            depth -= 1;
          } else if (lastNameIsArrayDepth > 0) {
            downstream.onObjectEnd(context);
          }

          // end all
          if (depth == -1) {
            /*if (context.metadata().isSingleFeature()) {
              getDownstream().onFeatureEnd(context);
            }*/
            downstream.onEnd(context);
            // end feature in collection
          } else if (depth == featureDepth && inFeature) {
            // inFeature = false;
            downstream.onFeatureEnd(context);
          } else if (inFeature) {
            // featureConsumer.onPropertyEnd(pathTracker.asList());
          }

          /*if (Objects.equals(currentName, "properties")) {
            inProperties = false;
          }
          if (Objects.equals(currentName, "geometry")) {
            context.setInGeometry(false);
          }*/
          if (inProperties) {
            pathTracker.track(depth - featureDepth);
          }
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

          if (nextToken == JsonToken.VALUE_NULL && nullValue.isEmpty()) {
            break;
          }
          if (nextToken == JsonToken.VALUE_NULL
              && nullValue.isPresent()
              && (Objects.equals(currentName, "geometry")
                  || Objects.equals(currentName, "properties"))) {
            if (Objects.equals(currentName, "properties")) {
              pathTracker.track(type, 0);
            } else {
              pathTracker.track(currentName, 0);
            }
            context.setValue(nullValue.get());
            downstream.onValue(context);
          }

          // feature or collection prop value
          if (depth == 0 && Objects.nonNull(currentName)) {
            /*switch (currentName) {
              case "numberReturned":
                context.metadata().numberReturned(parser.getLongValue());
                break;
              case "numberMatched":
                context.metadata().numberMatched(parser.getLongValue());
                break;
              case "type":
                if (!parser.getValueAsString().equals("Feature")) {
                  break;
                }
              case "id":
                startIfNecessary(false);
                if (!currentName.equals("id")) {
                  break;
                }

                context.pathTracker().track(currentName, 0);
                context.setValue(parser.getValueAsString());

                getDownstream().onValue(context);

                context.pathTracker().track(0);
                break;
            }*/
            // feature id or props or geo value
          } else if (((inProperties || context.inGeometry()))
              || (inFeature && Objects.equals(currentName, "id"))) {

            if (Objects.nonNull(currentName)) {
              if (context.inGeometry() && Objects.equals(currentName, "type")) {
                context.setGeometryType(
                    GeoJsonGeometryType.forString(parser.getValueAsString())
                        .toSimpleFeatureGeometry());
                downstream.onObjectStart(context);
                break;
              }
              pathTracker.track(currentName);
            }

            if (context.inGeometry() && startArray > 0) {
              for (int i = 0; i < startArray - 1; i++) {
                downstream.onArrayStart(context);
              }
              startArray = 0;
            }

            if (nextToken == JsonToken.VALUE_NULL && nullValue.isPresent()) {
              context.setValue(nullValue.get());
            } else {
              context.setValue(parser.getValueAsString());
            }

            downstream.onValue(context);

            // feature id
            if (Objects.equals(currentName, "id")) {
              // context.pathTracker().track(0);
            }
            // why reset depth?
            else if (!context.inGeometry()) {
              pathTracker.track(depth - featureDepth);
            } else if (context.inGeometry()) {
              pathTracker.track(depth - featureDepth);
            }
          }
          break;
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not parse GeoJSON: " + e.getMessage());
    }

    return feedMeMore;
  }

  private void startIfNecessary(
      boolean isCollection,
      ModifiableContext<FeatureSchema, SchemaMapping> context,
      FeatureEventHandler<
              FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>>
          downstream) {
    if (!started) {
      started = true;
      inFeature = true;
      if (isCollection) {
        featureDepth = 0; // 1;
      } else {
        context.metadata().isSingleFeature(true);
      }
      downstream.onStart(context);
      if (!isCollection) {
        context.pathTracker().track(type, 0);
        downstream.onFeatureStart(context);
      }
    }
  }
}
