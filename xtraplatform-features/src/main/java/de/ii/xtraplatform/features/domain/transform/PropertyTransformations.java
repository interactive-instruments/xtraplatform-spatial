/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.ImmutableFeaturePropertyTransformerCodelist;
import de.ii.xtraplatform.features.domain.ImmutableFeaturePropertyTransformerNullValue;
import de.ii.xtraplatform.features.domain.transform.FeaturePropertyTransformerFlatten.INCLUDE;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface PropertyTransformations {

    Logger LOGGER = LoggerFactory.getLogger(PropertyTransformations.class);

    default PropertyTransformations withSubstitutions(Map<String, String> substitutions) {
        Map<String, PropertyTransformation> transformations = this.getTransformations();

        return new PropertyTransformations() {
            @Override
            public Map<String, PropertyTransformation> getTransformations() {
                return transformations;
            }

            @Override
            public Map<String, List<FeaturePropertyValueTransformer>> getValueTransformations(
                Map<String, Codelist> codelists) {
                return PropertyTransformations.super.getValueTransformations(codelists, substitutions);
            }
        };
    }

    Map<String, PropertyTransformation> getTransformations();

    default Map<String, List<FeaturePropertySchemaTransformer>> getSchemaTransformations(
        boolean isOverview) {
        return getSchemaTransformations(isOverview, (separator, name) -> name);
    }

    default Map<String, List<FeaturePropertySchemaTransformer>> getSchemaTransformations(
        boolean isOverview, BiFunction<String, String, String> flattenedPathProvider) {
        Map<String, List<FeaturePropertySchemaTransformer>> transformations = new LinkedHashMap<>();

        getTransformations().forEach((property, mapping) -> {
            transformations.putIfAbsent(property, new ArrayList<>());

            mapping.getRename()
                   .ifPresent(rename -> transformations.get(property)
                                                       .add(ImmutableFeaturePropertyTransformerRename.builder()
                                                                                                     .parameter(rename)
                                                                                                     .build()));

            mapping.getRemove()
                   .ifPresent(remove -> transformations.get(property)
                                                       .add(ImmutableFeaturePropertyTransformerRemove.builder()
                                                                                                     .parameter(remove)
                                                                                                     .isOverview(isOverview)
                                                                                                     .build()));

            mapping.getFlatten()
                .ifPresent(flatten -> transformations.get(property)
                    .add(ImmutableFeaturePropertyTransformerFlatten.builder()
                        .parameter(flatten)
                        .flattenedPathProvider(flattenedPathProvider)
                        .build()));

            mapping.getFlattenObjects()
                .ifPresent(flatten -> transformations.get(property)
                    .add(ImmutableFeaturePropertyTransformerFlatten.builder()
                        .parameter(flatten)
                        .include(INCLUDE.OBJECTS)
                        .flattenedPathProvider(flattenedPathProvider)
                        .build()));

            mapping.getFlattenArrays()
                .ifPresent(flatten -> transformations.get(property)
                    .add(ImmutableFeaturePropertyTransformerFlatten.builder()
                        .parameter(flatten)
                        .include(INCLUDE.ARRAYS)
                        .flattenedPathProvider(flattenedPathProvider)
                        .build()));
        });

        return transformations;
    }

    default Map<String, List<FeaturePropertyValueTransformer>> getValueTransformations(Map<String, Codelist> codelists) {
        return getValueTransformations(codelists, ImmutableMap.of());
    }

    default Map<String, List<FeaturePropertyValueTransformer>> getValueTransformations(Map<String, Codelist> codelists,
                                                                                        Map<String, String> substitutions) {
        Map<String, List<FeaturePropertyValueTransformer>> transformations = new LinkedHashMap<>();

        getTransformations().forEach((property, mapping) -> {
            transformations.putIfAbsent(property, new ArrayList<>());

            mapping.getNull()
                    .ifPresent(nullValue -> transformations.get(property)
                                                           .add(
                                                               ImmutableFeaturePropertyTransformerNullValue
                                                                   .builder()
                                                                                                            .propertyName(property)
                                                                                                            .parameter(nullValue)
                                                                                                            .build()));

            mapping.getStringFormat()
                   .ifPresent(stringFormat -> transformations.get(property)
                                                             .add(
                                                                 ImmutableFeaturePropertyTransformerStringFormat
                                                                     .builder()
                                                                                                                 .propertyName(property)
                                                                                                                 .parameter(stringFormat)
                                                                                                                 .substitutions(substitutions)
                                                                                                                 .build()));

            mapping.getDateFormat()
                   .ifPresent(dateFormat -> transformations.get(property)
                                                           .add(ImmutableFeaturePropertyTransformerDateFormat.builder()
                                                                                                             .propertyName(property)
                                                                                                             .parameter(dateFormat)
                                                                                                             .build()));


            mapping.getCodelist()
                   .ifPresent(codelist -> transformations.get(property)
                                                         .add(
                                                             ImmutableFeaturePropertyTransformerCodelist
                                                                 .builder()
                                                                                                         .propertyName(property)
                                                                                                         .parameter(codelist)
                                                                                                         .codelists(codelists)
                                                                                                         .build()));
        });

        return transformations;
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean hasDeprecatedTransformationKeys() {
        return getTransformations().keySet()
                                   .stream()
                                   .anyMatch(key -> key.matches(".*\\[[^\\]]*\\].*"));
    }

    default Map<String, PropertyTransformation> normalizeTransformationKeys(String buildingBlock, String collectionId) {
        return getTransformations().entrySet()
                                   .stream()
                                   // normalize property names
                                   .map(transformation -> {
                                       if (transformation.getKey().matches(".*\\[[^\\]]*\\].*"))
                                           // TODO use info for now, but upgrade to warn after some time
                                           LOGGER.info("The transformation key '{}' in collection '{}' uses a deprecated style that includes square brackets for arrays. The brackets have been dropped during hydration. Building block: {}.", transformation.getKey(), collectionId, buildingBlock);
                                       return new AbstractMap.SimpleEntry<>(transformation.getKey().replaceAll("\\[[^\\]]*\\]", ""), transformation.getValue());
                                   })
                                   .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
