/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.domain;

import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ConstantsResolver implements SchemaVisitorTopDown<FeatureSchema, FeatureSchema> {
  final int[] constantCounter = {0};

  @Override
  public FeatureSchema visit(
      FeatureSchema schema, List<FeatureSchema> parents, List<FeatureSchema> visitedProperties) {
    List<FeatureSchema> visitedProperties2 =
        visitedProperties.stream()
            .filter(Objects::nonNull)
            .map(featureSchema -> normalizeConstants(schema.getName(), featureSchema))
            .collect(Collectors.toList());

    return new ImmutableFeatureSchema.Builder()
        .from(schema)
        .propertyMap(asMap(visitedProperties2, FeatureSchema::getFullPathAsString))
        .build();
  }

  private FeatureSchema normalizeConstants(String parent, FeatureSchema schema) {
    if (schema.getConstantValue().isPresent() && schema.getSourcePaths().isEmpty()) {
      String constantValue =
          schema.getType() == SchemaBase.Type.STRING
              ? String.format("'%s'", schema.getConstantValue().get())
              : schema.getConstantValue().get();
      String constantSourcePath =
          String.format(
              "%sconstant_%s_%d{constant=%s}",
              schema.getSourcePath().orElse(""), parent, constantCounter[0]++, constantValue);

      return new ImmutableFeatureSchema.Builder()
          .from(schema)
          .sourcePath(Optional.empty())
          .addSourcePaths(constantSourcePath)
          .build();
    }
    return schema;
  }
}
