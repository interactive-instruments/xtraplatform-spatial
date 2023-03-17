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
import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

public class OnlySortables implements SchemaVisitorTopDown<FeatureSchema, FeatureSchema> {

  private final boolean wildcard;
  private final List<String> included;
  private final List<String> excluded;
  private final String pathSeparator;

  public OnlySortables(List<String> included, List<String> excluded, String pathSeparator) {
    this.included = included;
    this.excluded = excluded;
    this.wildcard = included.contains("*");
    this.pathSeparator = pathSeparator;
  }

  @Override
  public FeatureSchema visit(
      FeatureSchema schema, List<FeatureSchema> parents, List<FeatureSchema> visitedProperties) {

    if (schema.sortable()) {
      String path = schema.getFullPathAsString(pathSeparator);
      // ignore property, if it is not included (by default or explicitly) or if it is excluded
      if ((!wildcard && !included.contains(path)) || excluded.contains(path)) {
        return null;
      }
    } else if (!schema.isFeature()) {
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

    return new ImmutableFeatureSchema.Builder()
        .from(schema)
        .propertyMap(visitedPropertiesMap)
        .build();
  }
}
