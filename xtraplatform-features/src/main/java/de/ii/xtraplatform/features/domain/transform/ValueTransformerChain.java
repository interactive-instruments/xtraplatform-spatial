/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaMapping;
import java.time.ZoneId;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class ValueTransformerChain
    implements TransformerChain<String, FeaturePropertyValueTransformer> {

  public static final String TYPE_WILDCARD = "*{type=";
  public static final String VALUE_TYPE_WILDCARD = "*{valueType=";

  private final Map<String, List<FeaturePropertyValueTransformer>> transformers;

  public ValueTransformerChain(
      Map<String, List<PropertyTransformation>> allTransformations,
      SchemaMapping schemaMapping,
      Map<String, Codelist> codelists,
      Optional<ZoneId> defaultTimeZone,
      Function<String, String> substitutionLookup) {
    this.transformers =
        allTransformations.entrySet().stream()
            .flatMap(
                entry -> {
                  String propertyPath = entry.getKey();
                  List<PropertyTransformation> transformation = entry.getValue();

                  if (hasWildcard(propertyPath, TYPE_WILDCARD)) {
                    return createValueTransformersForType(
                        propertyPath,
                        schemaMapping,
                        transformation,
                        codelists,
                        defaultTimeZone,
                        substitutionLookup)
                        .entrySet()
                        .stream();
                  }

                  if (hasWildcard(propertyPath, VALUE_TYPE_WILDCARD)) {
                    return createContextTransformersForValueType(
                        propertyPath,
                        schemaMapping,
                        transformation,
                        codelists,
                        defaultTimeZone,
                        substitutionLookup)
                        .entrySet()
                        .stream();
                  }

                  if (hasWildcard(propertyPath, ContextTransformerChain.OBJECT_TYPE_WILDCARD)) {
                    return createContextTransformersForObjectType(
                        propertyPath,
                        schemaMapping,
                        transformation,
                        codelists,
                        defaultTimeZone,
                        substitutionLookup)
                        .entrySet()
                        .stream();
                  }

                  return Stream.of(
                      new SimpleEntry<>(
                          propertyPath,
                          createValueTransformers(
                              propertyPath,
                              transformation,
                              codelists,
                              defaultTimeZone,
                              substitutionLookup)));
                })
            .collect(
                ImmutableMap.toImmutableMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (first, second) ->
                        new ImmutableList.Builder<FeaturePropertyValueTransformer>()
                            .addAll(first)
                            .addAll(second)
                            .build()));
  }

  @Nullable
  @Override
  public String transform(String path, String value) {
    String transformed = value;

    transformed = run(transformers, path, path, value);

    return transformed;
  }

  @Override
  public boolean has(String path) {
    return transformers.containsKey(path);
  }

  @Override
  public List<FeaturePropertyValueTransformer> get(String path) {
    return transformers.get(path);
  }

  private String run(
      Map<String, List<FeaturePropertyValueTransformer>> valueTransformations,
      String keyPath,
      String propertyPath,
      String value) {
    String transformed = value;

    if (valueTransformations.containsKey(keyPath) && !valueTransformations.get(keyPath).isEmpty()) {
      for (FeaturePropertyValueTransformer valueTransformer : valueTransformations.get(keyPath)) {
        transformed = valueTransformer.transform(propertyPath, transformed);
        // abort chain, if the value has been transformed to null
        if (Objects.isNull(transformed)) {
          return null;
        }
      }
    }

    return transformed;
  }

  private List<FeaturePropertyValueTransformer> createValueTransformers(
      String path,
      List<PropertyTransformation> propertyTransformations,
      Map<String, Codelist> codelists,
      Optional<ZoneId> defaultTimeZone,
      Function<String, String> substitutionLookup) {
    List<FeaturePropertyValueTransformer> transformers = new ArrayList<>();

    propertyTransformations.forEach(
        propertyTransformation -> {
          propertyTransformation
              .getNullify()
              .forEach(
                  nullValue ->
                      transformers.add(
                          ImmutableFeaturePropertyTransformerNullValue.builder()
                              .propertyPath(path)
                              .parameter(nullValue)
                              .build()));

          propertyTransformation
              .getStringFormat()
              .ifPresent(
                  stringFormat ->
                      transformers.add(
                          ImmutableFeaturePropertyTransformerStringFormat.builder()
                              .propertyPath(path)
                              .parameter(stringFormat)
                              .substitutionLookup(substitutionLookup)
                              .build()));

          propertyTransformation
              .getReduceStringFormat()
              .ifPresent(
                  stringFormat ->
                      transformers.add(
                          ImmutableFeaturePropertyTransformerStringFormat.builder()
                              .propertyPath(path)
                              .parameter(stringFormat)
                              .substitutionLookup(substitutionLookup)
                              .build()));

          propertyTransformation
              .getDateFormat()
              .ifPresent(
                  dateFormat ->
                      transformers.add(
                          ImmutableFeaturePropertyTransformerDateFormat.builder()
                              .propertyPath(path)
                              .parameter(dateFormat)
                              .defaultTimeZone(defaultTimeZone)
                              .build()));

          propertyTransformation
              .getCodelist()
              .ifPresent(
                  codelist ->
                      transformers.add(
                          ImmutableFeaturePropertyTransformerCodelist.builder()
                              .propertyPath(path)
                              .parameter(codelist)
                              .codelists(codelists)
                              .build()));
        });

    return transformers;
  }

  private Map<String, List<FeaturePropertyValueTransformer>> createValueTransformersForType(
      String transformationKey,
      SchemaMapping schemaMapping,
      List<PropertyTransformation> propertyTransformation,
      Map<String, Codelist> codelists,
      Optional<ZoneId> defaultTimeZone,
      Function<String, String> substitutionLookup) {
    return explodeWildcard2(transformationKey, TYPE_WILDCARD, schemaMapping, this::matchesType)
        .stream()
        .map(
            property ->
                new SimpleEntry<>(
                    property.first(),
                    createValueTransformers(
                        property.first(),
                        propertyTransformation,
                        codelists,
                        defaultTimeZone,
                        (key) ->
                            "type".equals(key)
                                ? property.second().get(0).getRefType().orElse("")
                                : substitutionLookup.apply(key))))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private boolean matchesType(FeatureSchema schema, String type) {
    return schema.isValue() && Objects.equals(schema.getType(), Type.valueOf(type));
  }

  private Map<String, List<FeaturePropertyValueTransformer>> createContextTransformersForValueType(
      String transformationKey,
      SchemaMapping schemaMapping,
      List<PropertyTransformation> propertyTransformation,
      Map<String, Codelist> codelists,
      Optional<ZoneId> defaultTimeZone,
      Function<String, String> substitutionLookup) {
    return explodeWildcard(
            transformationKey, VALUE_TYPE_WILDCARD, schemaMapping, this::matchesValueType)
        .stream()
        .map(
            propertyPath ->
                new SimpleEntry<>(
                    propertyPath,
                    createValueTransformers(
                        propertyPath,
                        propertyTransformation,
                        codelists,
                        defaultTimeZone,
                        substitutionLookup)))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private boolean matchesValueType(FeatureSchema schema, String valueType) {
    return schema.isValue()
        && Objects.equals(schema.getValueType().orElse(schema.getType()), Type.valueOf(valueType));
  }

  private Map<String, List<FeaturePropertyValueTransformer>> createContextTransformersForObjectType(
      String transformationKey,
      SchemaMapping schemaMapping,
      List<PropertyTransformation> propertyTransformation,
      Map<String, Codelist> codelists,
      Optional<ZoneId> defaultTimeZone,
      Function<String, String> substitutionLookup) {
    return explodeWildcard(
            transformationKey,
            ContextTransformerChain.OBJECT_TYPE_WILDCARD,
            schemaMapping,
            ContextTransformerChain::matchesObjectType)
        .stream()
        .map(
            propertyPath ->
                new SimpleEntry<>(
                    propertyPath,
                    createValueTransformers(
                        propertyPath,
                        propertyTransformation,
                        codelists,
                        defaultTimeZone,
                        substitutionLookup)))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
