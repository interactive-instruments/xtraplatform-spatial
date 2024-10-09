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

  static boolean endsWith(List<?> source, List<?> target) {
    if (source.size() < target.size()) {
      return false;
    }

    for (int i = 0; i < target.size(); i++) {
      if (!Objects.equals(source.get(source.size() - target.size() + i), target.get(i))) {
        return false;
      }
    }

    return true;
  }

  static boolean intersects(List<?> source, List<?> target) {
    if (source.isEmpty() || target.isEmpty()) {
      return false;
    }

    int start = source.indexOf(target.get(0));

    if (start == -1) {
      return false;
    }

    for (int i = start; i < source.size(); i++) {
      if (i - start >= target.size()) {
        return false;
      }

      if (!Objects.equals(source.get(i), target.get(i - start))) {
        return false;
      }
    }

    return true;
  }

  static <T> List<T> interlace(List<T> source, List<T> target) {
    List<T> shortenedSource = source;

    if (intersects(source, target)) {
      int start = source.indexOf(target.get(0));
      shortenedSource = source.subList(0, start);
    }

    return Stream.concat(shortenedSource.stream(), target.stream()).collect(Collectors.toList());
  }

  @Override
  default List<T> visit(
      FeatureSchema schema, List<FeatureSchema> parents, List<List<T>> visitedProperties) {
    List<List<U>> parentPaths1 = getParentPaths(parents);

    List<U> currentPaths = parseSourcePaths(schema, parentPaths1);

    boolean nestedArray = parents.stream().anyMatch(SchemaBase::isArray);

    List<T> properties =
        visitedProperties.stream().flatMap(Collection::stream).collect(Collectors.toList());

    boolean isInConcat =
        !parents.isEmpty()
            && parents.get(parents.size() - 1).isObject()
            && !parents.get(parents.size() - 1).getConcat().isEmpty();

    boolean isVirtualObject =
        isInConcat
            && schema.isObject()
            && parentPaths1.stream().anyMatch(parent -> endsWith(parent, currentPaths));

    if (!currentPaths.isEmpty() && !isVirtualObject) {
      return parentPaths1.stream()
          .flatMap(
              parentPath ->
                  currentPaths.stream()
                      .filter(
                          currentPath -> {
                            if (isInConcat && !currentPath.parentsIntersect(parentPath)) {
                              return false;
                            }
                            return true;
                          })
                      .map(
                          currentPath -> {
                            U finalCurrentPath =
                                isInConcat
                                    ? currentPath.withoutParentIntersection(parentPath)
                                    : currentPath;
                            List<String> fullPath =
                                Stream.concat(
                                        parentPath.stream().flatMap(p -> p.getFullPath().stream()),
                                        finalCurrentPath.getFullPath().stream())
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
                                schema,
                                finalCurrentPath,
                                matchingProperties,
                                parentPath,
                                nestedArray);
                          }))
          .collect(Collectors.toList());
    }

    if (!parentPaths1.isEmpty()) {
      return parentPaths1.stream()
          .filter(parentPath -> !isVirtualObject || endsWith(parentPath, currentPaths))
          .flatMap(
              parentPath -> {
                List<String> fullParentPath =
                    parentPath.stream()
                        .flatMap(p -> p.getFullPath().stream())
                        .collect(Collectors.toList());
                List<T> matchingProperties =
                    isVirtualObject
                        ? properties.stream()
                            .filter(prop -> Objects.equals(prop.getParentPath(), fullParentPath))
                            .collect(Collectors.toList())
                        : properties;
                return merge(
                    schema,
                    parentPath.isEmpty()
                        ? List.of()
                        : parentPath.get(parentPath.size() - 1).getFullPath(),
                    matchingProperties)
                    .stream();
              })
          .collect(Collectors.toList());
    }

    throw new IllegalArgumentException();
  }

  default List<List<U>> getParentPaths(List<FeatureSchema> parents) {
    List<List<U>> current = List.of();
    List<FeatureSchema> relevantParents = parents;

    boolean isInConcat =
        parents.size() > 1
            && parents.get(parents.size() - 2).isObject()
            && !parents.get(parents.size() - 2).getConcat().isEmpty();

    boolean isVirtualObject =
        isInConcat
            && parents.get(parents.size() - 1).isObject()
            && parents.get(parents.size() - 2).getEffectiveSourcePaths().stream()
                .anyMatch(
                    sourcePath ->
                        Objects.equals(
                            sourcePath,
                            parents.get(parents.size() - 1).getEffectiveSourcePaths().get(0)));

    if (isVirtualObject) {
      relevantParents = parents.subList(0, parents.size() - 1);
    }

    for (FeatureSchema parent : relevantParents) {
      current = getParentPaths(parent, current);
    }

    return current.isEmpty() ? List.of(List.of()) : current;
  }

  default List<List<U>> getParentPaths(FeatureSchema current, List<List<U>> parents) {
    List<U> children = parseSourcePaths(current, parents);

    if (parents.isEmpty() || hasRootPath(current)) {
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

  boolean hasRootPath(FeatureSchema sourceSchema);

  T create(
      FeatureSchema targetSchema,
      U path,
      List<T> visitedProperties,
      List<U> parentPaths,
      boolean nestedArray);

  List<T> merge(FeatureSchema targetSchema, List<String> parentPath, List<T> visitedProperties);
}
