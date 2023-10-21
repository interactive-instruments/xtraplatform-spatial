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
import de.ii.xtraplatform.features.domain.FeatureSchemaBase.Scope;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

public class OnlyReturnablesAndReceivables
    implements SchemaVisitorTopDown<FeatureSchema, FeatureSchema> {

  public OnlyReturnablesAndReceivables() {}

  @Override
  public FeatureSchema visit(
      FeatureSchema schema, List<FeatureSchema> parents, List<FeatureSchema> visitedProperties) {

    if (schema.getIsReturnable().orElse(true)
        || schema.getScope().filter(s -> s.equals(Scope.MUTATIONS)).isPresent()) {
      Map<String, FeatureSchema> visitedPropertiesMap =
          visitedProperties.stream()
              .filter(Objects::nonNull)
              .map(property -> new SimpleImmutableEntry<>(property.getFullPathAsString(), property))
              .collect(
                  ImmutableMap.toImmutableMap(
                      Entry::getKey, Entry::getValue, (first, second) -> second));

      return new ImmutableFeatureSchema.Builder()
          .from(schema)
          .propertyMap(visitedPropertiesMap)
          .build();
    }

    return null;
  }
}
