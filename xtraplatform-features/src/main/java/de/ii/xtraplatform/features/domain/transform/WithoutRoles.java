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
import de.ii.xtraplatform.features.domain.SchemaBase.Role;
import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

public class WithoutRoles implements SchemaVisitorTopDown<FeatureSchema, FeatureSchema> {

  public WithoutRoles() {}

  @Override
  public FeatureSchema visit(
      FeatureSchema schema, List<FeatureSchema> parents, List<FeatureSchema> visitedProperties) {

    Map<String, FeatureSchema> visitedPropertiesMap =
        visitedProperties.stream()
            .map(
                featureSchema ->
                    new SimpleImmutableEntry<>(featureSchema.getFullPathAsString(), featureSchema))
            .collect(
                ImmutableMap.toImmutableMap(
                    Entry::getKey, Entry::getValue, (first, second) -> second));

    Optional<Role> embeddedRole =
        schema.getRole().filter(r -> r != Role.EMBEDDED_FEATURE).or(schema::getEmbeddedRole);
    Optional<Role> role = schema.getRole().filter(r -> r == Role.EMBEDDED_FEATURE);

    return new ImmutableFeatureSchema.Builder()
        .from(schema)
        .role(role)
        .embeddedRole(embeddedRole)
        .propertyMap(visitedPropertiesMap)
        .build();
  }
}
