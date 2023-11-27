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
import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import java.util.List;

public class WithoutProperties implements SchemaVisitorTopDown<FeatureSchema, FeatureSchema> {

  private final List<String> fields;
  private final boolean skipGeometry;

  public WithoutProperties(List<String> fields, boolean skipGeometry) {
    this.fields = fields.contains("*") ? List.of() : fields;
    this.skipGeometry = skipGeometry;
  }

  @Override
  public FeatureSchema visit(
      FeatureSchema schema, List<FeatureSchema> parents, List<FeatureSchema> visitedProperties) {

    if (schema.isPrimaryGeometry() && skipGeometry) {
      return null;
    }

    if (!schema.isPrimaryGeometry()
        && !fields.isEmpty()
        && !parents.isEmpty()
        && !fields.contains(schema.getFullPathAsString())) {
      return null;
    }

    return new ImmutableFeatureSchema.Builder()
        .from(schema)
        .propertyMap(asMap(visitedProperties, FeatureSchema::getFullPathAsString))
        .build();
  }
}
