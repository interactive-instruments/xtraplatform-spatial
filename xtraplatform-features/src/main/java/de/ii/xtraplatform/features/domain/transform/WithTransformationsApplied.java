/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WithTransformationsApplied implements
    SchemaVisitorTopDown<FeatureSchema, FeatureSchema> {

  private final PropertyTransformations additionalTransformations;
  private final boolean preferSchemaTransformations;

  public WithTransformationsApplied() {
    this(new LinkedHashMap<>());
  }

  public WithTransformationsApplied(
      Map<String, PropertyTransformation> additionalTransformations) {
    this(() -> additionalTransformations.entrySet()
    .stream()
    .map(entry -> new SimpleEntry<>(entry.getKey(), ImmutableList.of(entry.getValue())))
    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)), true);
  }

  public WithTransformationsApplied(PropertyTransformations additionalTransformations) {
    this(additionalTransformations, false);
  }

  public WithTransformationsApplied(PropertyTransformations additionalTransformations, boolean preferSchemaTransformations) {
    this.additionalTransformations = additionalTransformations;
    this.preferSchemaTransformations = preferSchemaTransformations;
  }

  public Optional<String> getFlatteningSeparator(FeatureSchema schema) {
    return getFeatureTransformations(schema)
        .flatMap(PropertyTransformation::getFlatten);
  }

  @Override
  public FeatureSchema visit(FeatureSchema schema, List<FeatureSchema> parents,
      List<FeatureSchema> visitedProperties) {

    if (parents.isEmpty()) {
      Optional<String> flatten = getFlatteningSeparator(schema);

      if (flatten.isPresent()) {
        String separator = flatten.get();

        Map<String, FeatureSchema> flatProperties = flattenProperties(schema.getProperties(), null, separator, null, " > ")
            .stream()
            .map(property -> new SimpleEntry<>(property.getName(), property))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

        return new ImmutableFeatureSchema.Builder()
            .from(schema)
            .propertyMap(flatProperties)
            .build();
      }
    }

    return schema;
  }

  private List<FeatureSchema> flattenProperties(List<FeatureSchema> properties, String parentName, String nameSeparator, String parentLabel, String labelSeparator) {
    String prefix = Objects.nonNull(parentName)
        ? parentName + nameSeparator
        : "";
    String labelPrefix = Objects.nonNull(parentLabel)
        ? parentLabel + labelSeparator
        : "";

    return properties.stream()
        .flatMap(property -> property.isObject()
            ? flattenProperties(property.getProperties(), flatName(property, prefix), nameSeparator, flatLabel(property, labelPrefix), labelSeparator).stream()
            : Stream.of(flattenProperty(property, prefix, labelPrefix)))
        .collect(Collectors.toList());
  }

  private FeatureSchema flattenProperty(FeatureSchema property, String namePrefix, String labelPrefix) {
    return new ImmutableFeatureSchema.Builder()
        .from(property)
        .type(property.getValueType().orElse(property.getType()))
        .valueType(Optional.empty())
        .name(flatName(property, namePrefix))
        .label(flatLabel(property, labelPrefix))
        .build();
  }

  private String flatName(FeatureSchema property, String prefix) {
    return prefix + property.getName() + (property.isArray() ? "[]" : "");
  }

  private String flatLabel(FeatureSchema property, String prefix) {
    return prefix + property.getLabel().orElse(property.getName());
  }

  private Optional<PropertyTransformation> getFeatureTransformations(FeatureSchema schema) {
    PropertyTransformations schemaTransformations = () -> ImmutableMap.of(PropertyTransformations.WILDCARD, schema.getTransformations());

    PropertyTransformations mergedTransformations = preferSchemaTransformations
        ? schemaTransformations.mergeInto(additionalTransformations)
        : additionalTransformations.mergeInto(schemaTransformations);

    List<PropertyTransformation> featureTransformations = mergedTransformations
        .getTransformations()
        .get(PropertyTransformations.WILDCARD);

    return Optional.ofNullable(featureTransformations).filter(list -> !list.isEmpty()).map(list -> list.get(list.size()-1));
  }
}
