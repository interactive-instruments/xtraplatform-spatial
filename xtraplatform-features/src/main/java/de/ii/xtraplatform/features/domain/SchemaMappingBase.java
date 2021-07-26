/**
 * Copyright 2021 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import de.ii.xtraplatform.features.domain.SchemaBase.Role;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.immutables.value.Value;

public interface SchemaMappingBase<T extends SchemaBase<T>> {

  T getTargetSchema();

  @Value.Derived
  @Value.Auxiliary
  default Map<List<String>, List<T>> getTargetSchemasByPath() {
    return getTargetSchema().accept(new SchemaToMappingVisitor<>())
        .asMap()
        .entrySet()
        .stream()
        .map(entry -> new AbstractMap.SimpleImmutableEntry<>(
            entry.getKey(), Lists.newArrayList(entry.getValue())))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Value.Derived
  @Value.Auxiliary
  default Map<List<String>, List<List<T>>> getParentSchemasByPath() {
    return getTargetSchemasByPath()
        .entrySet()
        .stream()
        .map(entry -> {
          List<List<T>> parentSchemas = entry.getValue()
              .stream()
              .map(this::findParentSchemas)
              .collect(Collectors.toList());

          return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), parentSchemas);
        })
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Value.Derived
  @Value.Auxiliary
  default List<T> getAllSchemas() {
    return getTargetSchema().accept((schema1, visitedProperties) -> Stream
        .concat(Stream.of(schema1), visitedProperties.stream().flatMap(Collection::stream))
        .collect(Collectors.toList()));

  }

  default List<T> getTargetSchemas(List<String> path) {
    return getTargetSchemasByPath().getOrDefault(path, ImmutableList.of());
  }

  default Optional<T> getTargetSchema(Type type) {
    return getTargetSchemasByPath().values().stream().flatMap(Collection::stream)
        .filter(ts -> ts.getType() == type).findFirst();
  }

  default Optional<T> getTargetSchema(Role role) {
    return getTargetSchemasByPath().values().stream().flatMap(Collection::stream)
        .filter(ts -> ts.getRole().isPresent() && ts.getRole().get() == role).findFirst();
  }

  default List<List<T>> getParentSchemas(List<String> path) {
    return getParentSchemasByPath().getOrDefault(path, ImmutableList.of());
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
}
