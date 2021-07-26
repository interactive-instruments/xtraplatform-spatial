/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new", attributeBuilderDetection = true)
public interface SchemaMapping extends SchemaMappingBase<FeatureSchema> {

  @Override
  default FeatureSchema schemaWithGeometryType(FeatureSchema schema, SimpleFeatureGeometry geometryType) {
    return new ImmutableFeatureSchema.Builder().from(schema)
        .geometryType(geometryType)
        .build();
  }

  default SchemaMapping mappingWithTargetPaths() {
    Multimap<List<String>, FeatureSchema> accept = getTargetSchema()
        .accept(new SchemaToMappingVisitor<>(true));

    return null;
  }

  @Value.Default
  default boolean useTargetPaths() {
    return false;
  }

  static SchemaMapping withTargetPaths(SchemaMapping mapping) {
    return new ImmutableSchemaMapping.Builder().from(mapping).useTargetPaths(true).build();
  }

  @Value.Derived
  @Value.Auxiliary
  default Map<List<String>, List<FeatureSchema>> getTargetSchemasByPath() {
    return getTargetSchema().accept(new SchemaToMappingVisitor<>(useTargetPaths()))
        .asMap()
        .entrySet()
        .stream()
        .map(entry -> new AbstractMap.SimpleImmutableEntry<>(
            entry.getKey(), Lists.newArrayList(entry.getValue())))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
