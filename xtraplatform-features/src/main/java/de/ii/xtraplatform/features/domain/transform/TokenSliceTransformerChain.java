/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaMapping;
import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class TokenSliceTransformerChain
    implements TransformerChain<List<Object>, FeaturePropertyTokenSliceTransformer>,
        SchemaVisitorTopDown<FeatureSchema, FeatureSchema> {

  public static final String OBJECT_TYPE_WILDCARD = "*{objectType=";

  private final SchemaMapping schemaMapping;
  private final Map<String, List<FeaturePropertyTokenSliceTransformer>> transformers;

  // TODO: save position and schema
  public TokenSliceTransformerChain(
      Map<String, List<PropertyTransformation>> allTransformations, SchemaMapping schemaMapping) {
    this.schemaMapping = schemaMapping;
    this.transformers =
        allTransformations.entrySet().stream()
            .flatMap(
                entry -> {
                  String propertyPath = entry.getKey();
                  List<PropertyTransformation> transformation = entry.getValue();

                  if (hasWildcard(propertyPath, OBJECT_TYPE_WILDCARD)) {
                    return createSliceTransformersForObjectType(
                        propertyPath, schemaMapping, transformation)
                        .entrySet()
                        .stream();
                  }

                  return Stream.of(
                      new SimpleEntry<>(
                          propertyPath, createSliceTransformers(propertyPath, transformation)));
                })
            .collect(
                ImmutableMap.toImmutableMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (first, second) ->
                        new ImmutableList.Builder<FeaturePropertyTokenSliceTransformer>()
                            .addAll(first)
                            .addAll(second)
                            .build()));
  }

  @Override
  public FeatureSchema visit(
      FeatureSchema schema, List<FeatureSchema> parents, List<FeatureSchema> visitedProperties) {
    return transformSchema(
        transformers,
        schema.getFullPathAsString(),
        new ImmutableFeatureSchema.Builder()
            .from(schema)
            .propertyMap(asMap(visitedProperties, FeatureSchema::getFullPathAsString))
            .build());
  }

  public void transform(FeatureEventBuffer buffer) {
    transformers
        .keySet()
        .forEach(
            path -> {
              schemaMapping
                  .getPositionsForTargetPath(Splitter.on('.').splitToList(path))
                  .forEach(
                      pos -> {
                        List<Object> slice = buffer.getSlice(pos);
                        List<Object> transformed = transform(path, slice);

                        buffer.replaceSlice(pos, transformed);
                      });
            });
  }

  @Nullable
  @Override
  public List<Object> transform(String path, List<Object> slice) {
    List<Object> transformed = slice;

    transformed = run(transformers, path, path, slice);

    return transformed;
  }

  @Override
  public boolean has(String path) {
    return transformers.containsKey(path);
  }

  @Override
  public List<FeaturePropertyTokenSliceTransformer> get(String path) {
    return transformers.get(path);
  }

  private List<Object> run(
      Map<String, List<FeaturePropertyTokenSliceTransformer>> sliceTransformers,
      String keyPath,
      String propertyPath,
      List<Object> slice) {
    List<Object> transformed = slice;

    if (sliceTransformers.containsKey(keyPath) && !sliceTransformers.get(keyPath).isEmpty()) {
      for (FeaturePropertyTokenSliceTransformer sliceTransformer : sliceTransformers.get(keyPath)) {
        transformed = sliceTransformer.transform(propertyPath, transformed);
      }
    } else if (sliceTransformers.containsKey(PropertyTransformations.WILDCARD)
        && !sliceTransformers.get(PropertyTransformations.WILDCARD).isEmpty()) {
      for (FeaturePropertyTokenSliceTransformer sliceTransformer :
          sliceTransformers.getOrDefault(PropertyTransformations.WILDCARD, ImmutableList.of())) {
        transformed = sliceTransformer.transform(propertyPath, transformed);
      }
    }

    return transformed;
  }

  private FeatureSchema transformSchema(
      Map<String, List<FeaturePropertyTokenSliceTransformer>> sliceTransformers,
      String path,
      FeatureSchema schema) {
    FeatureSchema transformed = schema;

    if (sliceTransformers.containsKey(path) && !sliceTransformers.get(path).isEmpty()) {
      for (FeaturePropertyTokenSliceTransformer sliceTransformer : sliceTransformers.get(path)) {
        transformed = sliceTransformer.transformSchema(transformed);
      }
    } else if (sliceTransformers.containsKey(PropertyTransformations.WILDCARD)
        && !sliceTransformers.get(PropertyTransformations.WILDCARD).isEmpty()) {
      for (FeaturePropertyTokenSliceTransformer sliceTransformer :
          sliceTransformers.getOrDefault(PropertyTransformations.WILDCARD, ImmutableList.of())) {
        transformed = sliceTransformer.transformSchema(transformed);
      }
    }

    return transformed;
  }

  private List<FeaturePropertyTokenSliceTransformer> createSliceTransformers(
      String path, List<PropertyTransformation> propertyTransformations) {
    List<FeaturePropertyTokenSliceTransformer> transformers = new ArrayList<>();

    propertyTransformations.forEach(
        propertyTransformation -> {
          propertyTransformation
              .getReduceStringFormat()
              .ifPresent(
                  stringFormat ->
                      transformers.add(
                          ImmutableFeaturePropertyTransformerReduceStringFormat.builder()
                              .propertyPath(path)
                              .parameter(stringFormat)
                              .build()));
        });

    return transformers;
  }

  private Map<String, List<FeaturePropertyTokenSliceTransformer>>
      createSliceTransformersForObjectType(
          String transformationKey,
          SchemaMapping schemaMapping,
          List<PropertyTransformation> propertyTransformation) {
    return explodeWildcard(
            transformationKey,
            OBJECT_TYPE_WILDCARD,
            schemaMapping,
            TokenSliceTransformerChain::matchesObjectType)
        .stream()
        .map(
            propertyPath ->
                new SimpleEntry<>(
                    propertyPath, createSliceTransformers(propertyPath, propertyTransformation)))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  static boolean matchesObjectType(FeatureSchema schema, String objectType) {
    return schema.getObjectType().isPresent()
        && Objects.equals(schema.getObjectType().get(), objectType);
  }
}