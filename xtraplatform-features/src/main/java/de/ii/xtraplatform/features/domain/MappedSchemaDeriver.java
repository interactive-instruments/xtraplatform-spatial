/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface MappedSchemaDeriver<T extends SchemaBase<T>, U extends SourcePath>
    extends SchemaVisitorTopDown<FeatureSchema, List<T>> {

  @Override
  default List<T> visit(
      FeatureSchema schema, List<FeatureSchema> parents, List<List<T>> visitedProperties) {
    List<List<U>> parentPaths1 = getParentPaths(parents);

    List<U> currentPaths = parseSourcePaths(schema, parentPaths1);

    boolean nestedArray = parents.stream().anyMatch(SchemaBase::isArray);

    List<T> properties =
        visitedProperties.stream().flatMap(Collection::stream).collect(Collectors.toList());

    if (!currentPaths.isEmpty()) {
      return parentPaths1.stream()
          .flatMap(
              parentPath ->
                  currentPaths.stream()
                      .map(
                          currentPath -> {
                            List<String> fullPath =
                                Stream.concat(
                                        parentPath.stream().flatMap(p -> p.getFullPath().stream()),
                                        currentPath.getFullPath().stream())
                                    .collect(Collectors.toList());
                            List<T> matchingProperties =
                                properties.stream()
                                    .filter(
                                        prop ->
                                            Objects.equals(
                                                    prop.getParentPath(), currentPath.getFullPath())
                                                || Objects.equals(prop.getParentPath(), fullPath))
                                    .collect(Collectors.toList());

                            return create(
                                schema, currentPath, matchingProperties, parentPath, nestedArray);
                          }))
          .collect(Collectors.toList());
    }

    if (!parentPaths1.isEmpty()) {
      return parentPaths1.stream()
          .flatMap(
              parentPath ->
                  merge(
                      schema,
                      parentPath.isEmpty()
                          ? List.of()
                          : parentPath.get(parentPath.size() - 1).getFullPath(),
                      properties)
                      .stream())
          .collect(Collectors.toList());
    }

    throw new IllegalArgumentException();
  }

  default List<List<U>> getParentPaths(List<FeatureSchema> parents) {
    List<List<U>> current = List.of();

    for (FeatureSchema parent : parents) {
      current = getParentPaths(parent, current);
    }

    return current.isEmpty() ? List.of(List.of()) : current;
  }

  default List<List<U>> getParentPaths(FeatureSchema current, List<List<U>> parents) {
    List<U> children = parseSourcePaths(current, parents);

    if (parents.isEmpty()) {
      return children.stream().map(List::of).collect(Collectors.toList());
    }

    if (children.isEmpty()) {
      return parents;
    }

    return parents.stream()
        .flatMap(
            parent ->
                children.stream()
                    .map(
                        child ->
                            Stream.concat(parent.stream(), Stream.of(child))
                                .collect(Collectors.toList())))
        .collect(Collectors.toList());
  }

  List<U> parseSourcePaths(FeatureSchema sourceSchema, List<List<U>> parents);

  T create(
      FeatureSchema targetSchema,
      U path,
      List<T> visitedProperties,
      List<U> parentPaths,
      boolean nestedArray);

  List<T> merge(FeatureSchema targetSchema, List<String> parentPath, List<T> visitedProperties);
}
