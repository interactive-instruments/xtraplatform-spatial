/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import de.ii.xtraplatform.features.domain.ImmutableMappingInfo.Builder;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new", attributeBuilderDetection = true)
public interface SchemaMapping extends SchemaMappingBase<FeatureSchema> {

  @Value.Default
  default BiFunction<String, Boolean, String> getSourcePathTransformer() {
    return (path, isValue) -> path;
  }

  @Override
  default FeatureSchema schemaWithGeometryType(
      FeatureSchema schema, SimpleFeatureGeometry geometryType) {
    return new ImmutableFeatureSchema.Builder().from(schema).geometryType(geometryType).build();
  }

  static SchemaMapping of(FeatureSchema schema) {
    return new ImmutableSchemaMapping.Builder().targetSchema(schema).build();
  }

  @Override
  @Value.Derived
  @Value.Auxiliary
  default int getNumberOfTargets() {
    return getTargetSchema()
        .accept(new SchemaToPathsVisitor<>(false, getSourcePathTransformer()))
        .asMap()
        .keySet()
        .size();
  }

  @Override
  @Value.Derived
  @Value.Auxiliary
  default Map<List<String>, List<FeatureSchema>> getSchemasBySourcePath() {
    return getTargetSchema()
        .accept(new SchemaToPathsVisitor<>(false, getSourcePathTransformer()))
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
                  ArrayList<FeatureSchema> schemas = new ArrayList<>(first);
                  schemas.addAll(second);
                  return schemas;
                }));
  }

  @Override
  @Value.Derived
  @Value.Auxiliary
  default Map<List<String>, List<FeatureSchema>> getSchemasByTargetPath() {
    return getTargetSchema().accept(new SchemaToPathsVisitor<>(true)).asMap().entrySet().stream()
        .map(
            entry ->
                new SimpleImmutableEntry<>(entry.getKey(), Lists.newArrayList(entry.getValue())))
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey,
                Entry::getValue,
                (first, second) -> {
                  ArrayList<FeatureSchema> schemas = new ArrayList<>(first);
                  schemas.addAll(second);
                  return schemas;
                }));
  }

  @Override
  @Value.Derived
  @Value.Auxiliary
  default Map<List<String>, List<Integer>> getPositionsBySourcePath() {
    return getPositionsByPath(
        new SchemaToPathsVisitor<>(false, getSourcePathTransformer()), SchemaMapping::cleanPath);
  }

  @Override
  @Value.Derived
  @Value.Auxiliary
  default Map<List<String>, List<Integer>> getPositionsByTargetPath() {
    return getPositionsByPath(new SchemaToPathsVisitor<>(true), Function.identity());
  }

  @Value.Derived
  @Value.Auxiliary
  default Map<List<String>, List<MappingInfo>> forSourcePath() {
    return forPath(
        new SchemaToPathsVisitor<>(false, getSourcePathTransformer()),
        SchemaMapping::cleanPath,
        false);
  }

  @Value.Derived
  @Value.Auxiliary
  default Map<List<String>, List<MappingInfo>> forTargetPath() {
    return forPath(new SchemaToPathsVisitor<>(true), Function.identity(), true);
  }

  default Map<List<String>, List<Integer>> getPositionsByPath(
      SchemaToPathsVisitor<FeatureSchema> pathsVisitor,
      Function<List<String>, List<String>> pathCleaner) {
    final int[] i = {0};
    return getTargetSchema().accept(pathsVisitor).asMap().keySet().stream()
        .map(
            path -> new SimpleImmutableEntry<>(pathCleaner.apply(path), Lists.newArrayList(i[0]++)))
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey,
                Entry::getValue,
                (first, second) -> {
                  ArrayList<Integer> positions = new ArrayList<>(first);
                  positions.addAll(second);
                  return positions;
                }));
  }

  default Map<List<String>, List<MappingInfo>> forPath(
      SchemaToPathsVisitor<FeatureSchema> pathsVisitor,
      Function<List<String>, List<String>> pathCleaner,
      boolean useTargetPath) {
    return getTargetSchema().accept(pathsVisitor).asMap().keySet().stream()
        .map(pathCleaner)
        .map(
            path -> {
              List<FeatureSchema> schemas =
                  useTargetPath ? getSchemasForTargetPath(path) : getSchemasForSourcePath(path);
              List<Integer> positions =
                  useTargetPath
                      ? getPositionsByTargetPath().getOrDefault(path, List.of())
                      : getPositionsBySourcePath().getOrDefault(path, List.of());
              List<List<FeatureSchema>> parentSchemas =
                  useTargetPath
                      ? getParentSchemasForTargetPath(path)
                      : getParentSchemasForSourcePath(path);
              List<List<Integer>> parentPositions =
                  parentSchemas.stream()
                      .map(
                          parents ->
                              parents.stream()
                                  .map(
                                      parent ->
                                          getPositionsByTargetPath()
                                              .getOrDefault(parent.getFullPath(), List.of(-1))
                                              .get(0))
                                  .collect(Collectors.toList()))
                      .collect(Collectors.toList());

              Preconditions.checkState(
                  schemas.size() == positions.size()
                      && schemas.size() == parentSchemas.size()
                      && schemas.size() == parentPositions.size()
                      && parentPositions.stream()
                          .flatMap(List::stream)
                          .noneMatch(pos -> pos == -1));

              // TODO: if there is ever more than one value, loses position of duplicates
              List<MappingInfo> mappingInfos = new ArrayList<>();
              for (int i = 0; i < schemas.size(); i++) {
                mappingInfos.add(
                    new Builder()
                        .schema(schemas.get(i))
                        .position(positions.get(i))
                        .parentSchemas(parentSchemas.get(i))
                        .parentPositions(parentPositions.get(i))
                        .build());
              }

              return new SimpleImmutableEntry<>(path, mappingInfos);
            })
        .collect(
            ImmutableMap.toImmutableMap(
                Entry::getKey,
                Entry::getValue,
                (first, second) -> {
                  ArrayList<MappingInfo> mappingInfos = new ArrayList<>(first);
                  mappingInfos.addAll(second);
                  return mappingInfos;
                }));
  }

  static List<String> cleanPath(List<String> path) {
    if (path.get(path.size() - 1).contains("{")) {
      List<String> key = new ArrayList<>(path.subList(0, path.size() - 1));
      key.add(cleanPath(path.get(path.size() - 1)));
      return key;
    }
    return path;
  }

  // TODO: static cleanup method in PathParser
  static String cleanPath(String path) {
    if (path.contains("{")) {
      int i = path.indexOf("{");
      if (path.startsWith("filter", i + 1)) {
        return path.substring(0, i + 2) + cleanPath(path.substring(i + 2));
      }
      return path.substring(0, path.indexOf("{"));
    }
    return path;
  }

  // TODO: still needed?
  @Override
  default Optional<String> getPathSeparator() {
    // if (useTargetPaths()) {
    return getTargetSchema().getTransformations().stream()
        .filter(transformation -> transformation.getFlatten().isPresent())
        .findFirst()
        .flatMap(PropertyTransformation::getFlatten);
    // }

    // return Optional.empty();
  }
}
