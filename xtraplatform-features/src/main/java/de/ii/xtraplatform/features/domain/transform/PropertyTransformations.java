/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaMapping;
import java.time.ZoneId;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface PropertyTransformations {

  Logger LOGGER = LoggerFactory.getLogger(PropertyTransformations.class);

  String WILDCARD = "*";

  /**
   * @langEn [Property transformations](../../providers/details/transformations.md) do not affect
   *     data sources, they are applied on-the-fly as part of the encoding. Filter expressions do
   *     not take transformations into account, they have to be based on the source values. That
   *     means queryable properties (see `queryables` in [Features Core](features_core.md)) should
   *     not use transformations in most cases. The exception to the rule is the HTML encoding,
   *     where readability might be more important than filter support.
   * @langDe [Property-Transformationen](../../providers/details/transformations.md) erfolgen bei
   *     der Aufbereitung der Daten für die Rückgabe über die API. Die Datenhaltung selbst bleibt
   *     unverändert. Alle Filterausdrücke (siehe `queryables` in [Features Core](features_core.md))
   *     wirken unabhängig von etwaigen Transformationen bei der Ausgabe und müssen auf der Basis
   *     der Werte in der Datenhaltung formuliert sein - die Transformationen sind i.A. nicht
   *     umkehrbar und eine Berücksichtigung der inversen Transformationen bei Filterausdrücken wäre
   *     kompliziert und nur unvollständig möglich. Insofern sollten Eigenschaften, die queryable
   *     sein sollen, möglichst bereits in der Datenquelle transformiert sein. Eine Ausnahme sind
   *     typischerweise Transformationen in der HTML-Ausgabe, wo direkte Lesbarkeit i.d.R. wichtiger
   *     ist als die Filtermöglichkeit.
   * @default {}
   */
  Map<String, List<PropertyTransformation>> getTransformations();

  default boolean hasTransformation(String key, Predicate<PropertyTransformation> predicate) {
    return getTransformations().containsKey(key)
        && getTransformations().get(key).stream().anyMatch(predicate);
  }

  default Map<String, List<PropertyTransformation>> withTransformation(
      String key, PropertyTransformation transformation) {
    Map<String, List<PropertyTransformation>> transformations =
        new LinkedHashMap<>(getTransformations());

    transformations.compute(
        key,
        (ignore, existingTransformations) ->
            Objects.isNull(existingTransformations)
                ? ImmutableList.of(transformation)
                : Stream.concat(existingTransformations.stream(), Stream.of(transformation))
                    .collect(Collectors.toList()));

    return transformations;
  }

  default PropertyTransformations withSubstitutions(Map<String, String> substitutions) {
    Map<String, List<PropertyTransformation>> transformations = this.getTransformations();

    return new PropertyTransformations() {
      @Override
      public Map<String, List<PropertyTransformation>> getTransformations() {
        return transformations;
      }

      @Override
      public TransformerChain<String, FeaturePropertyValueTransformer> getValueTransformations(
          SchemaMapping schemaMapping,
          Map<String, Codelist> codelists,
          Optional<ZoneId> defaultTimeZone) {
        return PropertyTransformations.super.getValueTransformations(
            schemaMapping, codelists, defaultTimeZone, substitutions::get);
      }

      @Override
      public TransformerChain<String, FeaturePropertyValueTransformer> getValueTransformations(
          SchemaMapping schemaMapping,
          Map<String, Codelist> codelists,
          Optional<ZoneId> defaultTimeZone,
          Function<String, String> substitutionLookup) {
        return PropertyTransformations.super.getValueTransformations(
            schemaMapping,
            codelists,
            defaultTimeZone,
            key ->
                substitutions.containsKey(key)
                    ? substitutions.get(key)
                    : substitutionLookup.apply(key));
      }

      @Override
      public PropertyTransformations mergeInto(PropertyTransformations source) {
        return PropertyTransformations.super.mergeInto(source).withSubstitutions(substitutions);
      }
    };
  }

  default TransformerChain<FeatureSchema, FeaturePropertySchemaTransformer>
      getSchemaTransformations(
          SchemaMapping schemaMapping,
          boolean inCollection,
          BiFunction<String, String, String> flattenedPathProvider) {
    return new SchemaTransformerChain(
        getTransformations(), schemaMapping, inCollection, flattenedPathProvider);
  }

  default TransformerChain<
          ModifiableContext<FeatureSchema, SchemaMapping>, FeaturePropertyContextTransformer>
      getContextTransformations(SchemaMapping schemaMapping) {
    return new ContextTransformerChain(getTransformations(), schemaMapping);
  }

  default TransformerChain<String, FeaturePropertyValueTransformer> getValueTransformations(
      SchemaMapping schemaMapping,
      Map<String, Codelist> codelists,
      Optional<ZoneId> defaultTimeZone) {
    return getValueTransformations(schemaMapping, codelists, defaultTimeZone, key -> null);
  }

  default TransformerChain<String, FeaturePropertyValueTransformer> getValueTransformations(
      SchemaMapping schemaMapping,
      Map<String, Codelist> codelists,
      Optional<ZoneId> defaultTimeZone,
      Function<String, String> substitutionLookup) {
    return new ValueTransformerChain(
        getTransformations(), schemaMapping, codelists, defaultTimeZone, substitutionLookup);
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean hasDeprecatedTransformationKeys() {
    return getTransformations().keySet().stream().anyMatch(key -> key.matches(".*\\[[^\\]]*\\].*"));
  }

  // TODO: Check method instead of hydration
  default Map<String, List<PropertyTransformation>> normalizeTransformationKeys(
      String buildingBlock, String collectionId) {
    return getTransformations().entrySet().stream()
        // normalize property names
        .map(
            transformation -> {
              if (transformation.getKey().matches(".*\\[[^\\]]*\\].*"))
              // TODO use info for now, but upgrade to warn after some time
              {
                LOGGER.info(
                    "The transformation key '{}' in collection '{}' uses a deprecated style that includes square brackets for arrays. The brackets have been dropped during hydration. Building block: {}.",
                    transformation.getKey(),
                    collectionId,
                    buildingBlock);
              }
              return new AbstractMap.SimpleEntry<>(
                  transformation.getKey().replaceAll("\\[[^\\]]*\\]", ""),
                  transformation.getValue());
            })
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  default PropertyTransformations mergeInto(PropertyTransformations source) {
    Map<String, List<PropertyTransformation>> mergedTransformations =
        new LinkedHashMap<>(source.getTransformations());

    getTransformations()
        .forEach(
            (key, transformation) -> {
              if (mergedTransformations.containsKey(key)
                  && !Objects.equals(mergedTransformations.get(key), transformation)) {
                List<PropertyTransformation> chained = new ArrayList<>();
                chained.addAll(mergedTransformations.get(key));
                chained.addAll(transformation);
                mergedTransformations.put(key, chained);
              } else {
                mergedTransformations.put(key, transformation);
              }
            });

    return () -> mergedTransformations;
  }
}
