/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface MappedSchemaDeriver<T extends SchemaBase<T>, U>
    extends SchemaVisitorTopDown<FeatureSchema, List<T>> {

  @Override
  default List<T> visit(FeatureSchema schema, List<FeatureSchema> parents, List<List<T>> visitedProperties) {
    Optional<U> currentPath = parseSourcePath(schema);

    List<U> parentPaths =
        parents.stream()
            .map(this::parseSourcePath)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

    List<T> properties = visitedProperties.stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toList());

    if (currentPath.isPresent()) {
      return ImmutableList.of(create(schema, currentPath.get(), properties, parentPaths));
    }

    if (!parentPaths.isEmpty()) {
      return merge(schema, parentPaths.get(parentPaths.size()-1), properties);
    }

    throw new IllegalArgumentException();
  }

  Optional<U> parseSourcePath(FeatureSchema sourceSchema);

  T create(FeatureSchema targetSchema, U path, List<T> visitedProperties, List<U> parentPaths);

  // TODO
  List<T> merge(FeatureSchema targetSchema, U parentPath, List<T> visitedProperties);
}
