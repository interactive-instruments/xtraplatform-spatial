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
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

  @Override
  @Value.Derived
  @Value.Auxiliary
  default Map<List<String>, List<FeatureSchema>> getTargetSchemasByPath() {

    ImmutableMap<List<String>, List<FeatureSchema>> original = getTargetSchema().accept(
            new SchemaToMappingVisitor<>(useTargetPaths()))
        .asMap()
        .entrySet()
        .stream()
        .map(entry -> new SimpleImmutableEntry<>(
            entry.getKey(), Lists.newArrayList(entry.getValue())))
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    ImmutableMap<List<String>, List<FeatureSchema>> newer = getTargetSchema().accept(
            new SchemaToSourcePathsVisitor<>())
        .asMap()
        .entrySet()
        .stream()
        .map(entry -> {
          List<String> key;
          if (entry.getKey().get(entry.getKey().size() - 1).contains("{")) {
            key = new ArrayList<>(
                entry.getKey().subList(0, entry.getKey().size() - 1));
            key.add(entry.getKey().get(entry.getKey().size() - 1)
                .substring(0, entry.getKey().get(entry.getKey().size() - 1).indexOf("{")));
          } else {
            key = entry.getKey();
          }
          return new SimpleImmutableEntry<>(
              key, Lists.newArrayList(entry.getValue()));
        })
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue, (first, second) -> {
          ArrayList<FeatureSchema> featureSchemas = new ArrayList<>(first);
          featureSchemas.addAll(second);
          return featureSchemas;
        }));

    if (useTargetPaths()) {
      return original;
    }

    return newer;
  }
}
