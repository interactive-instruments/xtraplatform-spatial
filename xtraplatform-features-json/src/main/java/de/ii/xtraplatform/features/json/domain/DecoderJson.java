/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.json.domain;

import static de.ii.xtraplatform.base.domain.util.LambdaWithException.supplierMayThrow;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import de.ii.xtraplatform.features.domain.Decoder;
import de.ii.xtraplatform.features.domain.FeatureEventHandler;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaMapping;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DecoderJson implements Decoder {

  private static final Logger LOGGER = LoggerFactory.getLogger(DecoderJson.class);
  private static final JsonFactory JSON_FACTORY = new JsonFactory();

  private final JsonParser parser;
  private final ByteArrayFeeder feeder;
  private final Optional<String> nullValue;
  private List<List<String>> arrayPaths;
  private DecoderJsonProperties decoderJsonProperties;
  private boolean inProperties = false;
  private boolean isArray = false;
  private int featureDepth;
  private int valueIndex;

  public DecoderJson(Optional<String> nullValue) {
    try {
      this.parser = JSON_FACTORY.createNonBlockingByteArrayParser();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    this.feeder = (ByteArrayFeeder) parser.getNonBlockingInputFeeder();
    this.nullValue = nullValue;
  }

  @Override
  public void decode(byte[] data, Pipeline pipeline) {
    boolean isValues = pipeline.context().schema().filter(FeatureSchema::isValue).isPresent();
    boolean isSingleValue =
        isValues && pipeline.context().schema().filter(FeatureSchema::isArray).isEmpty();

    init(pipeline, isValues);

    try {
      feeder.feedInput(data, 0, data.length);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    boolean feedMeMore = false;
    while (!feedMeMore) {
      feedMeMore =
          isValues
              ? decodeValues(pipeline.context(), pipeline.downstream(), isSingleValue)
              : decodeObjects(pipeline.context(), pipeline.downstream());
    }
  }

  private void init(Pipeline pipeline, boolean isValues) {
    if (!isValues && Objects.isNull(decoderJsonProperties)) {
      this.arrayPaths =
          pipeline.context().mapping().getSchemasBySourcePath().entrySet().stream()
              .filter(entry -> entry.getValue().get(0).isArray())
              .map(entry -> entry.getKey())
              .collect(Collectors.toList());

      this.decoderJsonProperties =
          new DecoderJsonProperties(
              pipeline,
              arrayPaths,
              supplierMayThrow(parser::getValueAsString),
              Optional.empty(),
              Optional.empty());

      this.featureDepth = pipeline.context().pathTracker().asList().size();
    }
  }

  @Override
  public void reset(boolean full) {
    this.inProperties = false;
    this.isArray = false;
    this.valueIndex = 0;
    if (full) {
      this.decoderJsonProperties = null;
    } else if (Objects.nonNull(decoderJsonProperties)) {
      decoderJsonProperties.reset();
    }
  }

  @Override
  public void close() throws Exception {
    feeder.endOfInput();
  }

  private boolean decodeObjects(
      ModifiableContext<FeatureSchema, SchemaMapping> context,
      FeatureEventHandler<
              FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>>
          downstream) {

    boolean feedMeMore = false;

    try {
      JsonToken nextToken = parser.nextToken();
      String currentName = parser.currentName();

      // TODO: null is end-of-input
      if (Objects.isNull(nextToken)) {
        return true;
      }

      switch (nextToken) {
        case NOT_AVAILABLE:
          feedMeMore = true;
          break;

        case FIELD_NAME:
          break;

        case START_OBJECT:
          if (!inProperties) {
            if (context.path().size() == featureDepth) {
              this.inProperties = true;
              downstream.onObjectStart(context);
            }
            break;
          }
          feedMeMore = decoderJsonProperties.parse(nextToken, currentName, featureDepth);
          break;
        case START_ARRAY:
          if (!inProperties) {
            if (context.path().size() == featureDepth) {
              this.inProperties = true;
              this.isArray = true;
              downstream.onArrayStart(context);
            }
            break;
          }
          feedMeMore = decoderJsonProperties.parse(nextToken, currentName, featureDepth);
          break;
        case END_OBJECT:
          feedMeMore = decoderJsonProperties.parse(nextToken, currentName, featureDepth);
          break;
        case END_ARRAY:
          if (inProperties && isArray && context.path().size() == featureDepth) {
            this.inProperties = false;
            downstream.onArrayEnd(context);
            break;
          }
          feedMeMore = decoderJsonProperties.parse(nextToken, currentName, featureDepth);
          break;
        default:
          feedMeMore = decoderJsonProperties.parse(nextToken, currentName, featureDepth);
          break;
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not parse JSON: " + e.getMessage());
    }

    return feedMeMore;
  }

  private boolean decodeValues(
      ModifiableContext<FeatureSchema, SchemaMapping> context,
      FeatureEventHandler<
              FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>>
          downstream,
      boolean isSingleValue) {
    try {
      if (isSingleValue) {
        parser.nextToken();
        feeder.feedInput(new byte[] {32}, 0, 1);
      }

      JsonToken nextToken = parser.nextToken();

      if (Objects.isNull(nextToken)) {
        return true;
      }

      switch (nextToken) {
        case NOT_AVAILABLE:
          return true;

        case FIELD_NAME:
          break;

        case START_OBJECT:
        case END_OBJECT:
          throw new IllegalArgumentException("Objects not allowed in JSON with schema type VALUE.");
        case START_ARRAY:
          if (isArray) {
            throw new IllegalArgumentException(
                "Nested arrays not allowed in JSON with schema type VALUE.");
          }
          this.isArray = true;
          downstream.onArrayStart(context);
          break;
        case END_ARRAY:
          if (isArray) {
            this.isArray = false;
            downstream.onArrayEnd(context);
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

          if (nextToken == JsonToken.VALUE_NULL && nullValue.isPresent()) {
            context.setValue(nullValue.get());
          } else {
            context.setValue(parser.getValueAsString());
          }
          if (isArray) {
            ArrayList<Integer> indexes = new ArrayList<>(context.indexes());
            indexes.add(++valueIndex);
            context.setIndexes(indexes);
          }

          downstream.onValue(context);
          break;
        default:
          break;
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not parse JSON: " + e.getMessage());
    }

    return false;
  }
}
