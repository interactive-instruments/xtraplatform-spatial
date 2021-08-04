/**
 * Copyright 2021 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaMapping;
import java.time.ZoneId;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface PropertyTransformations {

  Logger LOGGER = LoggerFactory.getLogger(PropertyTransformations.class);

  String WILDCARD = "*";

  Map<String, PropertyTransformation> getTransformations();

  default PropertyTransformations withSubstitutions(Map<String, String> substitutions) {
    Map<String, PropertyTransformation> transformations = this.getTransformations();

    return new PropertyTransformations() {
      @Override
      public Map<String, PropertyTransformation> getTransformations() {
        return transformations;
      }

      @Override
      public TransformerChain<String, FeaturePropertyValueTransformer> getValueTransformations(
          SchemaMapping schemaMapping, Map<String, Codelist> codelists,
          Optional<ZoneId> defaultTimeZone) {
        return PropertyTransformations.super.getValueTransformations(schemaMapping, codelists,
            defaultTimeZone, substitutions::get);
      }

      @Override
      public TransformerChain<String, FeaturePropertyValueTransformer> getValueTransformations(
          SchemaMapping schemaMapping, Map<String, Codelist> codelists,
          Optional<ZoneId> defaultTimeZone, Function<String, String> substitutionLookup) {
        return PropertyTransformations.super.getValueTransformations(schemaMapping, codelists,
            defaultTimeZone, key -> substitutions.containsKey(key) ? substitutions.get(key)
                : substitutionLookup.apply(key));
      }

      @Override
      public PropertyTransformations mergeInto(PropertyTransformations source) {
        return PropertyTransformations.super.mergeInto(source).withSubstitutions(substitutions);
      }
    };
  }

  default TransformerChain<FeatureSchema, FeaturePropertySchemaTransformer> getSchemaTransformations(
      SchemaMapping schemaMapping, boolean inCollection,
      BiFunction<String, String, String> flattenedPathProvider) {
    return new SchemaTransformerChain(getTransformations(), schemaMapping, inCollection,
        flattenedPathProvider);
  }


  default TransformerChain<ModifiableContext, FeaturePropertyContextTransformer> getContextTransformations(
      SchemaMapping schemaMapping) {
    return new ContextTransformerChain(getTransformations(), schemaMapping);
  }

  default TransformerChain<String, FeaturePropertyValueTransformer> getValueTransformations(
      SchemaMapping schemaMapping, Map<String, Codelist> codelists,
      Optional<ZoneId> defaultTimeZone) {
    return getValueTransformations(schemaMapping, codelists, defaultTimeZone, key -> null);
  }

  default TransformerChain<String, FeaturePropertyValueTransformer> getValueTransformations(
      SchemaMapping schemaMapping, Map<String, Codelist> codelists,
      Optional<ZoneId> defaultTimeZone,
      Function<String, String> substitutionLookup) {
    return new ValueTransformerChain(getTransformations(), schemaMapping, codelists,
        defaultTimeZone, substitutionLookup);
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean hasDeprecatedTransformationKeys() {
    return getTransformations().keySet()
        .stream()
        .anyMatch(key -> key.matches(".*\\[[^\\]]*\\].*"));
  }

  default Map<String, PropertyTransformation> normalizeTransformationKeys(String buildingBlock,
      String collectionId) {
    return getTransformations().entrySet()
        .stream()
        // normalize property names
        .map(transformation -> {
          if (transformation.getKey().matches(".*\\[[^\\]]*\\].*"))
          // TODO use info for now, but upgrade to warn after some time
          {
            LOGGER.info(
                "The transformation key '{}' in collection '{}' uses a deprecated style that includes square brackets for arrays. The brackets have been dropped during hydration. Building block: {}.",
                transformation.getKey(), collectionId, buildingBlock);
          }
          return new AbstractMap.SimpleEntry<>(
              transformation.getKey().replaceAll("\\[[^\\]]*\\]", ""), transformation.getValue());
        })
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  default PropertyTransformations mergeInto(PropertyTransformations source) {
    Map<String, PropertyTransformation> mergedTransformations = new LinkedHashMap<>(
        source.getTransformations());

    getTransformations().forEach((key, transformation) -> {
      if (mergedTransformations.containsKey(key)) {
        mergedTransformations.put(key, transformation.mergeInto(mergedTransformations.get(key)));
      } else {
        mergedTransformations.put(key, transformation);
      }
    });

    return () -> mergedTransformations;
  }

}
