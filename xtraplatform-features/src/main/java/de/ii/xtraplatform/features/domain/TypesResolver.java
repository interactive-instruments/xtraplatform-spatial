/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema.Builder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public interface TypesResolver extends FeatureSchemaTransformer {

  boolean needsResolving(FeatureSchema type);

  default boolean needsResolving(PartialObjectSchema partial) {
    return false;
  }

  FeatureSchema resolve(FeatureSchema type);

  default boolean needsResolving(Map<String, FeatureSchema> types) {
    return types.values().stream()
            .flatMap(type -> Stream.concat(Stream.of(type), type.getAllNestedProperties().stream()))
            .anyMatch(this::needsResolving)
        || types.values().stream()
            .flatMap(type -> type.getAllNestedPartials().stream())
            .anyMatch(this::needsResolving);
  }

  default Map<String, FeatureSchema> resolve(Map<String, FeatureSchema> types) {
    Map<String, FeatureSchema> resolvedTypes = new LinkedHashMap<>();

    types.forEach(
        (key, value) -> {
          FeatureSchema resolved = value.accept(this, List.of());

          if (Objects.nonNull(resolved)) {
            resolvedTypes.put(key, resolved);
          }
        });

    return resolvedTypes;
  }

  @Override
  default FeatureSchema visit(
      FeatureSchema schema,
      List<FeatureSchema> parents,
      List<FeatureSchema> visitedProperties,
      List<PartialObjectSchema> visitedPartials) {
    ImmutableFeatureSchema visited =
        new Builder()
            .from(schema)
            .propertyMap(asMap(visitedProperties.stream()))
            .allOf(visitedPartials)
            .build();

    if (needsResolving(visited)) {
      return resolve(visited);
    }

    return visited;
  }
}
