/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.domain;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import de.ii.xtraplatform.features.domain.SchemaMappingBase;
import de.ii.xtraplatform.features.domain.SchemaToMappingVisitor;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new", attributeBuilderDetection = true)
public interface SchemaMappingSql extends SchemaMappingBase<SchemaSql> {

  BiFunction<String, Boolean, String> getSourcePathTransformer();

  @Value.Derived
  @Value.Auxiliary
  default Map<List<String>, List<SchemaSql>> getSchemasByTargetPath() {
    return getTargetSchema()
        .accept(new SchemaToMappingVisitor<>(getSourcePathTransformer()))
        .asMap()
        .entrySet()
        .stream()
        // TODO: removal of first path element only makes sense for geojson, so change in parser
        .map(
            entry ->
                new AbstractMap.SimpleImmutableEntry<>(
                    entry.getKey().subList(1, entry.getKey().size()),
                    Lists.newArrayList(entry.getValue())))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  default SchemaSql schemaWithGeometryType(SchemaSql schema, SimpleFeatureGeometry geometryType) {
    return new ImmutableSchemaSql.Builder().from(schema).geometryType(geometryType).build();
  }
}
