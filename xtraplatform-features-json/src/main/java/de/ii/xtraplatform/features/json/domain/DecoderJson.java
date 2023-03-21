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
import de.ii.xtraplatform.features.domain.SchemaMapping;
import java.io.IOException;
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
  private List<List<String>> arrayPaths;
  private DecoderJsonProperties decoderJsonProperties;
  private boolean inProperties = false;
  private boolean isArray = false;
  private int featureDepth;

  public DecoderJson(String type, String wrapper, Optional<String> nullValue) {
    try {
      this.parser = JSON_FACTORY.createNonBlockingByteArrayParser();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    this.feeder = (ByteArrayFeeder) parser.getNonBlockingInputFeeder();
  }

  @Override
  public void decode(byte[] data, Pipeline pipeline) {
    if (Objects.isNull(decoderJsonProperties)) {
      this.arrayPaths =
          pipeline.context().mapping().getTargetSchemasByPath().entrySet().stream()
              .filter(entry -> entry.getValue().get(0).isArray())
              .map(entry -> entry.getKey())
              .collect(Collectors.toList());

      this.decoderJsonProperties =
          new DecoderJsonProperties(
              pipeline, arrayPaths, supplierMayThrow(parser::getValueAsString), Optional.empty());

      this.featureDepth = pipeline.context().pathTracker().asList().size();
    }

    // TODO: optional wrapper and type, partial decoders?
    try {
      feeder.feedInput(data, 0, data.length);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    boolean feedMeMore = false;
    while (!feedMeMore) {
      feedMeMore = advanceParser(pipeline.context(), pipeline.downstream());
    }
  }

  @Override
  public void close() throws Exception {
    feeder.endOfInput();
  }

  private boolean advanceParser(
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
}
