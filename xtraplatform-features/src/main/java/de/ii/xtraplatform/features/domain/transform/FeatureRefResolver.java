/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema.Builder;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import java.util.List;
import java.util.Optional;

public class FeatureRefResolver implements SchemaVisitorTopDown<FeatureSchema, FeatureSchema> {

  public static final String ID = "id";
  public static final String TYPE = "type";
  public static final String TITLE = "title";
  public static final String SUB_ID = "{{id}}";
  public static final String SUB_TYPE = "{{type}}";
  public static final String SUB_TITLE = "{{title}}";

  public FeatureRefResolver() {}

  @Override
  public FeatureSchema visit(
      FeatureSchema schema, List<FeatureSchema> parents, List<FeatureSchema> visitedProperties) {

    // TODO
    if (schema.isFeatureRef()
        && schema.getProperties().isEmpty()
        && schema.getCoalesce().isEmpty()
        && schema.getConcat().isEmpty()) {
      String sourcePath = schema.getSourcePath().orElse("");
      Optional<String> objectSourcePath =
          sourcePath.contains("/")
              ? Optional.of(sourcePath.substring(0, sourcePath.lastIndexOf('/')))
              : Optional.empty();
      String idSourcePath =
          sourcePath.contains("/")
              ? sourcePath.substring(sourcePath.lastIndexOf('/') + 1)
              : sourcePath;

      FeatureSchema build =
          new Builder()
              .from(schema)
              .type(Type.OBJECT)
              .valueType(Optional.empty())
              .sourcePath(objectSourcePath)
              .putProperties2(
                  ID,
                  new Builder()
                      .type(schema.getValueType().orElse(Type.STRING))
                      .sourcePath(idSourcePath))
              .putProperties2(TITLE, new Builder().type(Type.STRING).sourcePath(idSourcePath))
              .putProperties2(
                  TYPE, new Builder().type(Type.STRING).constantValue(schema.getRefType()))
              .build();
      return build;
    }

    return new ImmutableFeatureSchema.Builder()
        .from(schema)
        .propertyMap(asMap(visitedProperties, FeatureSchema::getFullPathAsString))
        .build();
  }
}
