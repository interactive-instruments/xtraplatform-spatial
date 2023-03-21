/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.graphql.app;

import static de.ii.xtraplatform.base.domain.util.LambdaWithException.supplierMayThrow;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.Decoder;
import de.ii.xtraplatform.features.domain.FeatureEventHandler;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureTokenDecoder;
import de.ii.xtraplatform.features.domain.ImmutableSchemaMapping;
import de.ii.xtraplatform.features.domain.SchemaMapping;
import de.ii.xtraplatform.features.json.domain.DecoderJsonProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureTokenDecoderGraphQlJson2
    extends FeatureTokenDecoder<
        byte[], FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>>
    implements Decoder.Pipeline {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(FeatureTokenDecoderGraphQlJson2.class);
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
  private boolean inFeatures = false;
  private boolean inProperties = false;
  private boolean isCollection = false;
  private int lastNameIsArrayDepth = 0;
  private int startArray = 0;
  private int endArray = 0;

  private ModifiableContext<FeatureSchema, SchemaMapping> context;
  private List<List<String>> arrayPaths;
  private DecoderJsonProperties decoderJsonProperties;

  public FeatureTokenDecoderGraphQlJson2(
      FeatureSchema featureSchema, FeatureQuery query, String type, String wrapper) {
    this(featureSchema, query, type, wrapper, Optional.empty());
  }

  public FeatureTokenDecoderGraphQlJson2(
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
        context.mapping().getTargetSchemasByPath().entrySet().stream()
            .filter(entry -> entry.getValue().get(0).isArray())
            .map(entry -> entry.getKey())
            .collect(Collectors.toList());

    this.decoderJsonProperties =
        new DecoderJsonProperties(
            this, arrayPaths, supplierMayThrow(parser::getValueAsString), Optional.empty());
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

      switch (nextToken) {
        case NOT_AVAILABLE:
          feedMeMore = true;
          break;

        case FIELD_NAME:
          break;

        case START_OBJECT:
        case START_ARRAY:
          if (!inProperties) {
            if (inFeatures && context.path().size() == featureDepth) {
              this.inProperties = true;
              if (isCollection) {
                startFeature();
              }
            } else if (!inFeatures && Objects.equals(currentName, wrapper)) {
              startIfNecessary(nextToken == JsonToken.START_ARRAY);
            }
            break;
          }
          feedMeMore = decoderJsonProperties.parse(nextToken, currentName, featureDepth);
          break;
        case END_OBJECT:
          if (inProperties && context.path().size() == featureDepth) {
            this.inProperties = false;
            getDownstream().onFeatureEnd(context);
            if (!isCollection) {
              getDownstream().onEnd(context);
              this.inFeatures = false;
            }
            break;
          }
          feedMeMore = decoderJsonProperties.parse(nextToken, currentName, featureDepth);
          break;
        case END_ARRAY:
          if (!inProperties && context.path().size() == featureDepth) {
            getDownstream().onEnd(context);
            this.inFeatures = false;
            break;
          }
          feedMeMore = decoderJsonProperties.parse(nextToken, currentName, featureDepth);
          break;
        default:
          feedMeMore = decoderJsonProperties.parse(nextToken, currentName, featureDepth);
          break;
      }

    } catch (Throwable e) {
      throw new IllegalStateException("Could not parse JSON", e);
    }

    return feedMeMore;
  }

  @Override
  public ModifiableContext<FeatureSchema, SchemaMapping> context() {
    return context;
  }

  @Override
  public FeatureEventHandler<
          FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>>
      downstream() {
    return getDownstream();
  }

  private void startIfNecessary(boolean isCollection) {
    if (!started) {
      this.started = true;
      this.inFeatures = true;
      this.isCollection = isCollection;

      if (!isCollection) {
        context.metadata().isSingleFeature(true);
      }
      getDownstream().onStart(context);
      if (!isCollection) {
        startFeature();
      }
    }
  }

  private void startFeature() {
    context.pathTracker().track(type, 0);
    context.setIndexes(List.of());
    decoderJsonProperties.reset();
    getDownstream().onFeatureStart(context);
    this.featureDepth = context.path().size();
  }
}
