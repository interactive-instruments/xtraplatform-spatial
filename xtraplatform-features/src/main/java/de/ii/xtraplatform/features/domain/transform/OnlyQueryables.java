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

public class OnlyQueryables implements SchemaVisitorTopDown<FeatureSchema, FeatureSchema> {

  private final boolean wildcard;
  private final List<String> included;
  private final List<String> excluded;
  private final String pathSeparator;
  private final Predicate<String> excludePathMatcher;

  public OnlyQueryables(
      List<String> included,
      List<String> excluded,
      String pathSeparator,
      Predicate<String> excludePathMatcher) {
    this.included = included;
    this.excluded = excluded;
    this.wildcard = included.contains("*");
    this.pathSeparator = pathSeparator;
    this.excludePathMatcher = excludePathMatcher;
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

      if (parents.stream()
          .anyMatch(
              parent ->
                  parent.isMultiSource()
                      || excludePathMatcher.test(parent.getSourcePath().orElse("")))) {
        // if the parent has multiple source paths or its target path is excluded, no property can
        // be a queryable
        return null;
      }

      if (schema.queryable()) {
        String path = schema.getFullPathAsString(pathSeparator);
        // ignore property, if it is not included (by default or explicitly) or if it is excluded
        if ((!wildcard && !included.contains(path)) || excluded.contains(path)) {
          return null;
        }
      } else if (!schema.isObject()
          || (!parents.isEmpty() && visitedProperties.stream().noneMatch(Objects::nonNull))) {
        return null;
      }

      Map<String, FeatureSchema> visitedPropertiesMap =
          visitedProperties.stream()
              .filter(Objects::nonNull)
              .map(
                  property ->
                      new SimpleImmutableEntry<>(
                          property.getFullPathAsString(pathSeparator), property))
              .collect(
                  ImmutableMap.toImmutableMap(
                      Entry::getKey, Entry::getValue, (first, second) -> second));

      return new Builder()
          .from(adjustType(parents, schema))
          .propertyMap(visitedPropertiesMap)
          .build();
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
  }
}
