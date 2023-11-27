/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.graphql.app;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureTokenDecoder;
import de.ii.xtraplatform.features.domain.ImmutableSchemaMapping;
import de.ii.xtraplatform.features.domain.MultiplicityTracker;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaMapping;
import de.ii.xtraplatform.features.json.domain.GeoJsonGeometryType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureTokenDecoderGraphQlJson
    extends FeatureTokenDecoder<
        byte[], FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>> {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(FeatureTokenDecoderGraphQlJson.class);
  private static final JsonFactory JSON_FACTORY = new JsonFactory();

  private final JsonParser parser;
  private final ByteArrayFeeder feeder;
  private final FeatureSchema featureSchema;
  private final FeatureQuery featureQuery;
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

  private ModifiableContext<FeatureSchema, SchemaMapping> context;
  private List<List<String>> arrayPaths;
  private MultiplicityTracker multiplicityTracker;

  public FeatureTokenDecoderGraphQlJson(
      FeatureSchema featureSchema, FeatureQuery query, String type, String wrapper) {
    this(featureSchema, query, type, wrapper, Optional.empty());
  }

  public FeatureTokenDecoderGraphQlJson(
      FeatureSchema featureSchema,
      FeatureQuery query,
      String type,
      String wrapper,
      Optional<String> nullValue) {
    try {
      this.parser = JSON_FACTORY.createNonBlockingByteArrayParser();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    this.feeder = (ByteArrayFeeder) parser.getNonBlockingInputFeeder();
    this.featureSchema = featureSchema;
    this.featureQuery = query;
    this.type = type;
    this.wrapper = wrapper;
    this.nullValue = nullValue;
  }

  @Override
  protected void init() {
    this.context =
        createContext()
            .setType(featureSchema.getName())
            .setMappings(
                ImmutableMap.of(
                    featureSchema.getName(),
                    new ImmutableSchemaMapping.Builder()
                        .targetSchema(featureSchema)
                        .sourcePathTransformer((path, isValue) -> path)
                        .build()))
            .setQuery(featureQuery);

    this.arrayPaths =
        context.mapping().getSchemasByTargetPath().entrySet().stream()
            .filter(entry -> entry.getValue().get(0).isArray())
            .map(entry -> entry.getKey())
            .collect(Collectors.toList());
    this.multiplicityTracker = new MultiplicityTracker(arrayPaths);
  }

  @Override
  protected void cleanup() {
    feeder.endOfInput();
  }

  @Override
  public void onPush(byte[] bytes) {
    feedInput(bytes);
  }

  // for unit tests
  void parse(String data) throws Exception {
    byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
    feedInput(dataBytes);
    cleanup();
  }

  private void feedInput(byte[] data) {
    try {
      feeder.feedInput(data, 0, data.length);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    boolean feedMeMore = false;
    while (!feedMeMore) {
      feedMeMore = advanceParser();
    }
  }

  public boolean advanceParser() {

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
            // TODO: wrapper
            if (Objects.equals(currentName, "data")) {
              // TODO
              depth = 0;
              break;
            }
            // TODO: propsParent
            if (Objects.equals(currentName, wrapper)) {
              startIfNecessary(false);
              inProperties = true;
              featureDepth = depth;
              context.pathTracker().track(type, 0);
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
                  context.pathTracker().track(currentName);
                }
                break;
            }
            // nested array_object start
          } else if (Objects.isNull(currentName) && depth > featureDepth && started) {
            multiplicityTracker.track(context.pathTracker().asList());
            context.setIndexes(
                multiplicityTracker.getMultiplicitiesForPath(context.pathTracker().asList()));
            LOGGER.debug("OBJ {} {}", context.pathAsString(), context.indexes());
            getDownstream().onObjectStart(context);
            break;
            // feature in collection start
          } else if (depth == featureDepth && inFeature) {
            // inFeature = false;
            context.pathTracker().track(type, 0);
            context.setIndexes(List.of());
            multiplicityTracker.reset();
            getDownstream().onFeatureStart(context);
            depth = 1;
            break;
          }

          // nested object start?
          if (Objects.nonNull(currentName)
              || arrayPaths.contains(
                  context.pathTracker().asList()) /*|| lastNameIsArrayDepth == 0*/) {
            if (depth > featureDepth && (inProperties /*|| inGeometry*/)
            /*&& !Objects.equals(currentName, "properties")*/ ) {
              LOGGER.debug("OBJ2 {} {}", context.pathAsString(), context.indexes());
              getDownstream().onObjectStart(context);
            }
            depth += 1;
          }
          break;

        case START_ARRAY:
          // start features array
          if (depth == 0 && Objects.nonNull(currentName)) {
            if (Objects.equals(currentName, wrapper)) {
              startIfNecessary(true);
              inProperties = true;
              // featureDepth = depth;
              context.pathTracker().track(type, 0);
            }
            // start prop/coord array
          } else if (Objects.nonNull(currentName) && (inProperties || context.inGeometry())) {
            if (!context.inGeometry()) {
              context.pathTracker().track(currentName);
            }
            // lastNameIsArrayDepth += 1;
            depth += 1;

            getDownstream().onArrayStart(context);
            // start nested geo array
          } else if (context.inGeometry()) {
            if (endArray > 0) {
              for (int i = 0; i < endArray - 1; i++) {
                getDownstream().onArrayEnd(context);
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
                getDownstream().onArrayEnd(context);
              }
              endArray = 0;
            }

            getDownstream().onArrayEnd(context);

            if (context.inGeometry()) {
              context.pathTracker().track(depth - featureDepth);
            }
            depth -= 1;
            if (inProperties) {
              context.pathTracker().track(depth - featureDepth);
            }
            // lastNameIsArrayDepth -= 1;
            // end nested geo array
          } else if (context.inGeometry()) {
            endArray++;
            depth -= 1;
            context.pathTracker().track(depth - featureDepth);
          }
          break;

        case END_OBJECT:

          // end nested object
          if (Objects.nonNull(currentName) /*||  lastNameIsArrayDepth == 0*/) {
            if (depth > featureDepth && (inProperties || context.inGeometry())
            /*&& !Objects.equals(currentName, "properties")*/ ) {
              getDownstream().onObjectEnd(context);
            }

            // end geo
            if (context.inGeometry()) {
              context.pathTracker().track(depth - featureDepth);
            }

            depth -= 1;
            // nested array_object end
          } else if (arrayPaths.contains(
              context.pathTracker().asList()) /*lastNameIsArrayDepth > 0*/) {
            // context.pathTracker().track(depth - lastNameIsArrayDepth);
            getDownstream().onObjectEnd(context);
          }

          // end all
          if (depth == -1) {
            /*if (context.metadata().isSingleFeature()) {
              getDownstream().onFeatureEnd(context);
            }*/
            getDownstream().onEnd(context);
            // end feature in collection
          } else if (depth == featureDepth + 1 && inFeature) {
            inFeature = false;
            depth = 0;
            getDownstream().onFeatureEnd(context);
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
            // context.pathTracker().track(depth - featureDepth);
            context.pathTracker().track(depth);
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
              context.pathTracker().track(type, 0);
            } else {
              context.pathTracker().track(currentName, 0);
            }
            context.setValue(nullValue.get());
            getDownstream().onValue(context);
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
                getDownstream().onObjectStart(context);
                break;
              }
              context.pathTracker().track(currentName);
            }

            if (context.inGeometry() && startArray > 0) {
              for (int i = 0; i < startArray - 1; i++) {
                getDownstream().onArrayStart(context);
              }
              startArray = 0;
            }

            if (nextToken == JsonToken.VALUE_NULL && nullValue.isPresent()) {
              context.setValue(nullValue.get());
            } else {
              context.setValue(parser.getValueAsString());
            }

            getDownstream().onValue(context);

            // feature id
            if (Objects.equals(currentName, "id")) {
              // context.pathTracker().track(0);
            }
            // why reset depth?
            else if (!context.inGeometry()) {
              context.pathTracker().track(depth - featureDepth);
            } else if (context.inGeometry()) {
              context.pathTracker().track(depth - featureDepth);
            }
          }
          break;
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not parse GeoJSON: " + e.getMessage());
    }

    return feedMeMore;
  }

  private void startIfNecessary(boolean isCollection) {
    if (!started) {
      started = true;
      inFeature = true;
      if (isCollection) {
        featureDepth = 0; // 1;
      } else {
        context.metadata().isSingleFeature(true);
      }
      getDownstream().onStart(context);
      if (!isCollection) {
        context.pathTracker().track(type, 0);
        getDownstream().onFeatureStart(context);
      }
    }
  }
}
