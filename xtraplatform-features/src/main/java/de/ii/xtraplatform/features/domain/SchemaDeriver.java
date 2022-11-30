/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.features.domain.SchemaBase.Role;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class SchemaDeriver<T> implements SchemaVisitorTopDown<FeatureSchema, T> {

  protected static final String SECONDARY_GEOMETRY = "SECONDARY_GEOMETRY";

  private final List<Codelist> codelists;

  public SchemaDeriver(List<Codelist> codelists) {
    this.codelists = codelists;
  }

  @Override
  public final T visit(
      FeatureSchema schema, List<FeatureSchema> parents, List<T> visitedProperties) {
    if (parents.isEmpty()) {
      return deriveRootSchema(schema, visitedProperties);
    }
    if (schema.isValue()) {
      return deriveValueSchema(schema);
    }

    return deriveObjectSchema(schema, visitedProperties);
  }

  private T deriveRootSchema(FeatureSchema schema, List<T> visitedProperties) {

    Map<String, T> definitions = extractDefinitions(visitedProperties);

    Map<String, T> properties =
        visitedProperties.stream()
            .filter(property -> Objects.nonNull(property) && getPropertyName(property).isPresent())
            .map(
                property ->
                    new SimpleEntry<>(
                        getNameWithoutRole(getPropertyName(property).get()), property))
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

    List<String> required =
        visitedProperties.stream()
            .filter(
                property ->
                    Objects.nonNull(property)
                        && getPropertyName(property).isPresent()
                        && isPropertyRequired(property))
            .map(property -> getPropertyName(property).get())
            .collect(Collectors.toList());

    return buildRootSchema(schema, properties, definitions, required);
  }

  private Map<String, T> extractDefinitions(List<T> properties) {
    return extractDefinitions(properties.stream())
        .collect(
            ImmutableMap.toImmutableMap(
                def -> getPropertyName(def).get(), def -> def, (first, second) -> second));
  }

  protected Stream<T> extractDefinitions(Stream<T> properties) {
    return Stream.empty();
  }

  private T deriveObjectSchema(FeatureSchema schema, List<T> visitedProperties) {

    Map<String, T> properties =
        visitedProperties.stream()
            .filter(property -> Objects.nonNull(property) && getPropertyName(property).isPresent())
            .map(
                property ->
                    new SimpleEntry<>(
                        getNameWithoutRole(getPropertyName(property).get()), property))
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

    List<String> required =
        visitedProperties.stream()
            .filter(
                property ->
                    Objects.nonNull(property)
                        && getPropertyName(property).isPresent()
                        && isPropertyRequired(property))
            .map(property -> getPropertyName(property).get())
            .collect(Collectors.toList());

    T objectSchema = buildObjectSchema(schema, properties, required);

    String objectType = schema.getObjectType().orElse(getObjectType(schema));

    objectSchema = withRefWrapper(objectSchema, objectType);

    if (schema.isArray()) {
      objectSchema = withArrayWrapper(objectSchema);
    }

    if (schema.getConstraints().isPresent()) {
      objectSchema =
          withConstraints(objectSchema, schema.getConstraints().get(), schema, codelists);
    }

    return objectSchema;
  }

  private T deriveValueSchema(FeatureSchema schema) {
    T valueSchema = null;
    Type propertyType = schema.getType();
    String propertyName = schema.getName();
    Optional<String> label = schema.getLabel();
    Optional<String> description = schema.getDescription();
    Optional<String> unit = schema.getUnit();

    switch (propertyType) {
      case FLOAT:
      case INTEGER:
      case STRING:
      case BOOLEAN:
      case DATETIME:
      case DATE:
        valueSchema = getSchemaForLiteralType(propertyType, label, description, unit);
        break;
      case VALUE_ARRAY:
        valueSchema =
            getSchemaForLiteralType(
                schema.getValueType().orElse(Type.UNKNOWN), label, description, unit);
        break;
      case GEOMETRY:
        valueSchema = getSchemaForGeometry(schema);
        break;
      case UNKNOWN:
      default:
        break;
    }

    if (propertyType == Type.GEOMETRY) {
      valueSchema =
          withName(
              valueSchema,
              schema.isPrimaryGeometry()
                  ? getNameWithRole(Role.PRIMARY_GEOMETRY.name(), propertyName)
                  : getNameWithRole(SECONDARY_GEOMETRY, propertyName));
    } else {
      valueSchema =
          withName(
              valueSchema,
              schema.isId() ? getNameWithRole(Role.ID.name(), propertyName) : propertyName);
    }

    if (schema.isArray()) {
      valueSchema = withArrayWrapper(valueSchema);
    }

    if (schema.getConstraints().isPresent()) {
      valueSchema = withConstraints(valueSchema, schema.getConstraints().get(), schema, codelists);
    }

    return valueSchema;
  }

  protected abstract Optional<String> getPropertyName(T property);

  protected abstract boolean isPropertyRequired(T property);

  protected abstract Map<String, T> getNestedProperties(T property);

  protected abstract T buildRootSchema(
      FeatureSchema schema,
      Map<String, T> properties,
      Map<String, T> definitions,
      List<String> requiredProperties);

  protected abstract T buildObjectSchema(
      FeatureSchema schema, Map<String, T> properties, List<String> requiredProperties);

  protected abstract T getSchemaForLiteralType(
      Type type, Optional<String> label, Optional<String> description, Optional<String> unit);

  protected abstract T getSchemaForGeometry(FeatureSchema schema);

  protected abstract T withName(T schema, String propertyName);

  protected abstract T withRequired(T schema);

  protected abstract T withConstraints(
      T schema, SchemaConstraints constraints, FeatureSchema property, List<Codelist> codelists);

  protected abstract T withRefWrapper(T schema, String objectType);

  protected abstract T withArrayWrapper(T schema);

  protected final String getNameWithRole(String role, String propertyName) {
    return String.format("_%s_ROLE_%s", role, propertyName);
  }

  protected String getNameWithoutRole(String name) {
    int index = name.indexOf("_ROLE_");
    if (index > -1) {
      return name.substring(index + 6);
    }

    return name;
  }

  protected boolean nameHasRole(String name, String role) {
    return name.startsWith(getNameWithRole(role, ""));
  }

  protected boolean nameHasRole(String name) {
    return name.contains("_ROLE_");
  }

  protected Optional<T> findByRole(Map<String, T> properties, Role role) {
    return findByRole(properties, role.name());
  }

  protected Optional<T> findByRole(Map<String, T> properties, String role) {
    return properties.values().stream()
        .flatMap(
            property -> {
              Collection<T> nestedProperties = getNestedProperties(property).values();
              if (!nestedProperties.isEmpty()) {
                return nestedProperties.stream();
              }
              return Stream.of(property);
            })
        .filter(
            property ->
                getPropertyName(property).filter(name -> nameHasRole(name, role)).isPresent())
        .findFirst();
  }

  // TODO: nested
  protected Map<String, T> withoutRoles(Map<String, T> properties, Role... roles) {
    return properties.entrySet().stream()
        .filter(
            entry ->
                getPropertyName(entry.getValue())
                    .filter(
                        name ->
                            Arrays.stream(roles).noneMatch(role -> nameHasRole(name, role.name())))
                    .isPresent())
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  // TODO: nested
  protected Map<String, T> withoutRoles(Map<String, T> properties) {
    return properties.entrySet().stream()
        .filter(
            entry ->
                getPropertyName(entry.getValue()).filter(name -> !nameHasRole(name)).isPresent())
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public static String getObjectType(FeatureSchema schema) {
    return "type_" + Integer.toHexString(schema.hashCode());
  }
}
