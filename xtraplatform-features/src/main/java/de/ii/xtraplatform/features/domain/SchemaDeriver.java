/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface SchemaDeriver<T extends SchemaBase<T>, U>
    extends SchemaVisitorTopDown<FeatureSchema, T> {

  @Override
  default T visit(FeatureSchema schema, List<FeatureSchema> parents, List<T> visitedProperties) {
    Optional<U> currentPath = parseSourcePath(schema);

    List<U> parentPaths =
        parents.stream()
            .map(this::parseSourcePath)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

    if (currentPath.isPresent()) {
      return create(schema, currentPath.get(), visitedProperties, parentPaths);
    }

    if (!parentPaths.isEmpty()) {
      return merge(schema, parentPaths.get(parentPaths.size()-1), visitedProperties);
    }

    throw new IllegalArgumentException();
  }

  Optional<U> parseSourcePath(FeatureSchema sourceSchema);

  T create(FeatureSchema targetSchema, U path, List<T> visitedProperties, List<U> parentPaths);

  // TODO
  T merge(FeatureSchema targetSchema, U parentPath, List<T> visitedProperties);
}
