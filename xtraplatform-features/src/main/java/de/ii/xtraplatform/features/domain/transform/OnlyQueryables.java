/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema.Builder;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class OnlyQueryables implements SchemaVisitorTopDown<FeatureSchema, FeatureSchema> {

  private final boolean wildcard;
  private final List<String> included;
  private final List<String> excluded;
  private final String pathSeparator;
  private final Predicate<String> excludePathMatcher;
  private final boolean cleanupKeys;

  public OnlyQueryables(
      List<String> included,
      List<String> excluded,
      String pathSeparator,
      Predicate<String> excludePathMatcher,
      boolean cleanupKeys) {
    this.included = included;
    this.excluded = excluded;
    this.wildcard = included.contains("*");
    this.pathSeparator = pathSeparator;
    this.excludePathMatcher = excludePathMatcher;
    this.cleanupKeys = cleanupKeys;
  }

  @Override
  public FeatureSchema visit(
      FeatureSchema schema, List<FeatureSchema> parents, List<FeatureSchema> visitedProperties) {
    if (parents.isEmpty()) {
      PropertyTransformations propertyTransformations =
          schema.accept(new PropertyTransformationsCollector());
      SchemaTransformerChain schemaTransformations =
          propertyTransformations.getSchemaTransformations(null, false);

      return schema.accept(schemaTransformations).accept(new OnlyQueryablesIncluder());
    }

    return schema;
  }

  class OnlyQueryablesIncluder implements SchemaVisitorTopDown<FeatureSchema, FeatureSchema> {
    @Override
    public FeatureSchema visit(
        FeatureSchema schema, List<FeatureSchema> parents, List<FeatureSchema> visitedProperties) {

      if (parents.stream().anyMatch(this::isParentExcluded)) {
        return null;
      }

      if (schema.queryable()) {
        String path = cleanupPaths(schema).getFullPathAsString(pathSeparator);
        // ignore property, if it is not included (by default or explicitly) or if it is excluded
        if ((!wildcard && !included.contains(path)) || excluded.contains(path)) {
          return null;
        }
        if (excludePathMatcher.test(schema.getSourcePath().orElse(""))) {
          return null;
        }
      } else if (!schema.isObject()
          || (!parents.isEmpty() && visitedProperties.stream().noneMatch(Objects::nonNull))) {
        return null;
      }

      Map<String, FeatureSchema> visitedPropertiesMap =
          visitedProperties.stream()
              .filter(Objects::nonNull)
              .map(this::cleanupPathsIfDesired)
              .map(
                  property ->
                      new SimpleImmutableEntry<>(
                          property.getFullPathAsString(pathSeparator), property))
              .collect(
                  ImmutableMap.toImmutableMap(
                      Entry::getKey, Entry::getValue, (first, second) -> second));

      List<FeatureSchema> visitedConcat =
          schema.getConcat().stream()
              .map(concatSchema -> concatSchema.accept(this, parents))
              .collect(Collectors.toList());

      return new Builder()
          .from(adjustType(parents, schema))
          .propertyMap(visitedPropertiesMap)
          .concat(List.of())
          .build();
    }

    private FeatureSchema cleanupPathsIfDesired(FeatureSchema property) {
      if (cleanupKeys) {
        return cleanupPaths(property);
      }
      return property;
    }

    private String getKey(FeatureSchema property) {
      return cleanupKeys
          // TODO: separator
          ? property.getFullPathAsString(pathSeparator).replaceAll("(^|\\.)([0-9]+)_", "$1")
          : property.getFullPathAsString(pathSeparator);
    }

    private FeatureSchema adjustType(List<FeatureSchema> parents, FeatureSchema property) {
      if (!property.queryable()) {
        // not a queryable, we have an object that has embedded queryables
        return property;
      }

      if (parents.stream().noneMatch(SchemaBase::isArray)) {
        // nothing to do, the property is not embedded in an array
        return property;
      }

      if (property.isArray()) {
        // nothing to do, already an array
        return property;
      }

      return new ImmutableFeatureSchema.Builder()
          .from(property)
          .type(Type.VALUE_ARRAY)
          .valueType(property.getType())
          .build();
    }

    private boolean isParentExcluded(FeatureSchema parent) {
      // if the parent has multiple source paths or its target path is excluded, no property can
      // be a queryable
      return (parent.isMultiSource() && !parent.isFeature())
          || excludePathMatcher.test(parent.getSourcePath().orElse(""));
    }
  }

  static FeatureSchema cleanupPaths(FeatureSchema property) {
    if ((property.getPath().stream().anyMatch(elem -> elem.matches("^([0-9]+)_.*"))
        || property.getParentPath().stream().anyMatch(elem -> elem.matches("^([0-9]+)_.*")))) {
      return new ImmutableFeatureSchema.Builder()
          .from(property)
          .path(
              property.getPath().stream()
                  .map(elem -> elem.replaceAll("^([0-9]+)_", ""))
                  .collect(Collectors.toList()))
          .parentPath(
              property.getParentPath().stream()
                  .map(elem -> elem.replaceAll("^([0-9]+)_", ""))
                  .collect(Collectors.toList()))
          .build();
    }
    return property;
  }
}
