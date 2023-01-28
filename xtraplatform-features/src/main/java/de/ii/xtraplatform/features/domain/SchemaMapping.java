/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiFunction;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new", attributeBuilderDetection = true)
public interface SchemaMapping extends SchemaMappingBase<FeatureSchema> {

  BiFunction<String, Boolean, String> getSourcePathTransformer();

  @Override
  default FeatureSchema schemaWithGeometryType(
      FeatureSchema schema, SimpleFeatureGeometry geometryType) {
    return new ImmutableFeatureSchema.Builder().from(schema).geometryType(geometryType).build();
  }

  default SchemaMapping mappingWithTargetPaths() {
    Multimap<List<String>, FeatureSchema> accept =
        getTargetSchema().accept(new SchemaToMappingVisitor<>(true, getSourcePathTransformer()));

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

    ImmutableMap<List<String>, List<FeatureSchema>> original =
        getTargetSchema()
            .accept(new SchemaToMappingVisitor<>(useTargetPaths(), getSourcePathTransformer()))
            .asMap()
            .entrySet()
            .stream()
            .map(
                entry ->
                    new SimpleImmutableEntry<>(
                        entry.getKey(), Lists.newArrayList(entry.getValue())))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    ImmutableMap<List<String>, List<FeatureSchema>> newer =
        getTargetSchema()
            .accept(new SchemaToSourcePathsVisitor<>(getSourcePathTransformer()))
            .asMap()
            .entrySet()
            .stream()
            .map(
                entry ->
                    new SimpleImmutableEntry<>(
                        cleanPath(entry.getKey()), Lists.newArrayList(entry.getValue())))
            .collect(
                ImmutableMap.toImmutableMap(
                    Entry::getKey,
                    Entry::getValue,
                    (first, second) -> {
                      ArrayList<FeatureSchema> featureSchemas = new ArrayList<>(first);
                      featureSchemas.addAll(second);
                      return featureSchemas;
                    }));

    if (useTargetPaths()) {
      return original;
    }

    return newer;
  }

  default List<String> cleanPath(List<String> path) {
    if (path.get(path.size() - 1).contains("{")) {
      List<String> key = new ArrayList<>(path.subList(0, path.size() - 1));
      key.add(cleanPath(path.get(path.size() - 1)));
      return key;
    }
    return path;
  }

  // TODO: static cleanup method in PathParser
  default String cleanPath(String path) {
    if (path.contains("{")) {
      int i = path.indexOf("{");
      if (path.startsWith("filter", i + 1)) {
        return path.substring(0, i + 2) + cleanPath(path.substring(i + 2));
      }
      return path.substring(0, path.indexOf("{"));
    }
    return path;
  }

  @Override
  default Optional<String> getPathSeparator() {
    if (useTargetPaths()) {
      return getTargetSchema().getTransformations().stream()
          .filter(transformation -> transformation.getFlatten().isPresent())
          .findFirst()
          .flatMap(PropertyTransformation::getFlatten);
    }

    return Optional.empty();
  }
}
