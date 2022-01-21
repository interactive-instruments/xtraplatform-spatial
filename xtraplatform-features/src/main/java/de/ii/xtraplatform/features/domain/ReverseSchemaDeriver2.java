/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public interface ReverseSchemaDeriver2<T extends SchemaBase<T>, U>
    extends SchemaVisitor<FeatureSchema, List<T>> {

  @Override
  default List<T> visit(FeatureSchema schema, List<List<T>> visitedProperties) {

    if (schema.isConstant()) {
      return ImmutableList.of();
    }

    U currentPath = parseSourcePath(schema);

    Map<List<String>, T> objectCache = new LinkedHashMap<>();

    List<T> properties =
        visitedProperties.stream()
            .flatMap(List::stream)
            .map(
                sourceSchema -> {
                  List<String> parentPath = sourceSchema.getParentPath();

                  if (parentPath.isEmpty()) {
                    return sourceSchema;
                  }

                  List<T> parents = createParents(currentPath, sourceSchema, objectCache);

                  if (!parents.isEmpty()) {
                    parents.forEach(t -> objectCache.put(t.getPath(), t));

                    return parents.get(0);
                  }

                  return sourceSchema;
                })
            .collect(Collectors.toList());

    properties =
        properties.stream()
            .map(sourceSchema -> objectCache.getOrDefault(sourceSchema.getPath(), sourceSchema))
            .distinct()
            .map(sourceSchema -> prependToParentPath(currentPath, sourceSchema))
            .collect(Collectors.toList());

    if (shouldIgnore(currentPath)) {
      return properties.stream()
          .map(sourceSchema -> prependToSourcePath(schema.getName(), sourceSchema))
          .collect(Collectors.toList());
    }

    T current = create(currentPath, schema);

    return ImmutableList.of(addChildren(current, properties));
  }

  List<T> createParents(U parentPath, T child, Map<List<String>, T> objectCache);

  T create(U path, FeatureSchema targetSchema);

  T addChildren(T parent, List<T> children);

  T prependToParentPath(U path, T schema);

  T prependToSourcePath(String parentSourcePath, T schema);

  U ignorablePath();

  default boolean shouldIgnore(U path) {
    return Objects.equals(path, ignorablePath());
  }

  U parseSourcePath(FeatureSchema sourceSchema);
}
