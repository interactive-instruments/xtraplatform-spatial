/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema.Builder;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import java.util.List;

public class ImplicitMappingResolver implements SchemaVisitorTopDown<FeatureSchema, FeatureSchema> {

  public boolean needsResolving(FeatureSchema schema) {
    if (schema.isFeature() && !schema.getConcat().isEmpty()) {
      return false;
    }
    return ((schema.isObject() || schema.isArray()) && schema.getSourcePath().isEmpty())
        || (schema.isObject()
            && schema.getSourcePath().isPresent()
            && schema.getValueNames().isEmpty())
        || schema.getType() == Type.VALUE_ARRAY;
  }

  @Override
  public FeatureSchema visit(
      FeatureSchema schema, List<FeatureSchema> parents, List<FeatureSchema> visitedProperties) {
    if (needsResolving(schema)) {
      return new Builder()
          .from(schema)
          .propertyMap(asMap(visitedProperties, FeatureSchema::getFullPathAsString))
          .transformations(List.of())
          .addTransformations(
              new ImmutablePropertyTransformation.Builder().wrap(schema.getType()).build())
          .addAllTransformations(schema.getTransformations())
          .build();
    }

    return new Builder()
        .from(schema)
        .propertyMap(asMap(visitedProperties, FeatureSchema::getFullPathAsString))
        .build();
  }
}
