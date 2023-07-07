/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema.Builder;
import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;

public class OnlyQueryables implements SchemaVisitorTopDown<FeatureSchema, FeatureSchema> {

  private final boolean wildcard;
  private final List<String> included;
  private final List<String> excluded;
  private final String pathSeparator;
  private final Predicate<String> excludePathMatcher;

  public OnlyQueryables(
      List<String> included,
      List<String> excluded,
      String pathSeparator,
      Predicate<String> excludePathMatcher) {
    this.included = included;
    this.excluded = excluded;
    this.wildcard = included.contains("*");
    this.pathSeparator = pathSeparator;
    this.excludePathMatcher = excludePathMatcher;
  }

  @Override
  public FeatureSchema visit(
      FeatureSchema schema, List<FeatureSchema> parents, List<FeatureSchema> visitedProperties) {

    if (parents.stream().anyMatch(s -> excludePathMatcher.test(s.getSourcePath().orElse("")))) {
      // if the path is excluded, no property can be a queryable
      return null;
    }

    FeatureSchema schema2 = schema;

    if (schema.getAdditionalInfo().containsKey("concatIndex")
        && !schema.getTransformations().isEmpty()
        && schema.getTransformations().get(0).getRename().isPresent()) {
      schema2 =
          new ImmutableFeatureSchema.Builder()
              .from(schema)
              .name(schema.getTransformations().get(0).getRename().get())
              .path(List.of(schema.getTransformations().get(0).getRename().get()))
              .build();
    }

    if (schema2.queryable()) {
      String path = schema2.getFullPathAsString(pathSeparator);
      // ignore property, if it is not included (by default or explicitly) or if it is excluded
      if ((!wildcard && !included.contains(path)) || excluded.contains(path)) {
        return null;
      }
    } else if (!schema2.isObject()) {
      return null;
    }

    Map<String, FeatureSchema> visitedPropertiesMap =
        visitedProperties.stream()
            .filter(Objects::nonNull)
            .map(
                featureSchema ->
                    new SimpleImmutableEntry<>(
                        featureSchema.getFullPathAsString(pathSeparator), featureSchema))
            .collect(
                ImmutableMap.toImmutableMap(
                    Entry::getKey, Entry::getValue, (first, second) -> second));

    return new Builder().from(schema2).propertyMap(visitedPropertiesMap).concat(List.of()).build();
  }
}
