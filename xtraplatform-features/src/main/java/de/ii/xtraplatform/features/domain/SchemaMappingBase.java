/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.SchemaBase.Role;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.immutables.value.Value;

public interface SchemaMappingBase<T extends SchemaBase<T>> {

  T getTargetSchema();

  int getNumberOfTargets();

  Map<List<String>, List<T>> getSchemasBySourcePath();

  Map<List<String>, List<T>> getSchemasByTargetPath();

  Map<List<String>, List<Integer>> getPositionsByTargetPath();

  Map<List<String>, List<Integer>> getPositionsBySourcePath();

  @Value.Derived
  @Value.Auxiliary
  default Map<List<String>, List<List<T>>> getParentSchemasBySourcePath() {
    return getParentSchemasByPath(getSchemasBySourcePath());
  }

  @Value.Derived
  @Value.Auxiliary
  default Map<List<String>, List<List<T>>> getParentSchemasByTargetPath() {
    return getParentSchemasByPath(getSchemasByTargetPath());
  }

  default Map<List<String>, List<List<T>>> getParentSchemasByPath(
      Map<List<String>, List<T>> schemasByPath) {
    return schemasByPath.entrySet().stream()
        .map(
            entry -> {
              List<List<T>> parentSchemas =
                  entry.getValue().stream()
                      .map(this::findParentSchemas)
                      .collect(Collectors.toList());

              return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), parentSchemas);
            })
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Value.Derived
  @Value.Auxiliary
  default Map<List<String>, List<List<Integer>>> getParentPositionsBySourcePath() {
    return getParentPositionsByPath(
        getParentSchemasBySourcePath(), this::getPositionsForTargetPath);
  }

  @Value.Derived
  @Value.Auxiliary
  default Map<List<String>, List<List<Integer>>> getParentPositionsByTargetPath() {
    return getParentPositionsByPath(
        getParentSchemasByTargetPath(), this::getPositionsForTargetPath);
  }

  default Map<List<String>, List<List<Integer>>> getParentPositionsByPath(
      Map<List<String>, List<List<T>>> parentSchemasByPath,
      Function<List<String>, List<Integer>> getPositionsForPath) {
    return parentSchemasByPath.entrySet().stream()
        .map(
            entry -> {
              List<List<Integer>> parentPositions =
                  entry.getValue().stream()
                      .map(
                          parents ->
                              parents.stream()
                                  .flatMap(
                                      parent ->
                                          getPositionsForPath.apply(parent.getFullPath()).stream()
                                              .filter(pos -> pos > 0))
                                  .collect(Collectors.toList()))
                      .collect(Collectors.toList());

              return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), parentPositions);
            })
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Value.Derived
  @Value.Auxiliary
  default List<T> getAllSchemas() {
    return getTargetSchema()
        .accept(
            (schema1, visitedProperties) ->
                Stream.concat(
                        Stream.of(schema1), visitedProperties.stream().flatMap(Collection::stream))
                    .collect(Collectors.toList()));
  }

  default List<T> getSchemasForSourcePath(List<String> path) {
    return getSchemasBySourcePath().getOrDefault(path, ImmutableList.of());
  }

  default List<T> getSchemasForTargetPath(List<String> path) {
    return getSchemasByTargetPath().getOrDefault(path, ImmutableList.of());
  }

  default List<List<T>> getParentSchemasForSourcePath(List<String> path) {
    return getParentSchemasBySourcePath().getOrDefault(path, ImmutableList.of());
  }

  default List<List<T>> getParentSchemasForTargetPath(List<String> path) {
    return getParentSchemasByTargetPath().getOrDefault(path, ImmutableList.of());
  }

  default List<Integer> getPositionsForSourcePath(List<String> path) {
    return getPositionsBySourcePath().getOrDefault(path, ImmutableList.of());
  }

  default List<Integer> getPositionsForTargetPath(List<String> path) {
    return getPositionsByTargetPath().getOrDefault(path, ImmutableList.of());
  }

  default Optional<T> getTargetSchema(Type type) {
    return getSchemasByTargetPath().values().stream()
        .flatMap(Collection::stream)
        .filter(ts -> ts.getType() == type)
        .findFirst();
  }

  default Optional<T> getTargetSchema(Role role) {
    return getSchemasByTargetPath().values().stream()
        .flatMap(Collection::stream)
        .filter(ts -> ts.getRole().isPresent() && ts.getRole().get() == role)
        .findFirst();
  }

  default Optional<T> findParentSchema(T schema) {
    return getAllSchemas().stream()
        .filter(parent -> parent.getProperties().contains(schema))
        .findFirst();
  }

  default List<T> findParentSchemas(T schema) {
    List<T> parents = new ArrayList<>();
    Optional<T> current = Optional.ofNullable(schema);

    while (current.isPresent()) {
      current = findParentSchema(current.get());
      current.ifPresent(parents::add);
    }

    return parents;
  }

  T schemaWithGeometryType(T schema, SimpleFeatureGeometry geometryType);

  default Optional<String> getPathSeparator() {
    return Optional.empty();
  }
}
