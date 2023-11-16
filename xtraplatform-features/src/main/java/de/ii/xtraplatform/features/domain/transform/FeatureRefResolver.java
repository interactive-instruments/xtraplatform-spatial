/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain.transform;

import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema.Builder;
import de.ii.xtraplatform.features.domain.MappingOperationResolver;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class FeatureRefResolver implements SchemaVisitorTopDown<FeatureSchema, FeatureSchema> {

  public static final String ID = "id";
  public static final String TYPE = "type";
  public static final String TITLE = "title";
  public static final String URI_TEMPLATE = "uriTemplate";
  public static final String KEY_TEMPLATE = "keyTemplate";
  public static final String SUB_ID = "{{id}}";
  public static final String SUB_TYPE = "{{type}}";
  public static final String SUB_TITLE = "{{title}}";
  public static final String SUB_URI_TEMPLATE = "{{uriTemplate}}";
  public static final String SUB_KEY_TEMPLATE = "{{keyTemplate}}";
  public static final String REF_TYPE_DYNAMIC = "DYNAMIC";

  public FeatureRefResolver() {}

  @Override
  public FeatureSchema visit(
      FeatureSchema schema, List<FeatureSchema> parents, List<FeatureSchema> visitedProperties) {
    if (schema.isFeatureRef()) {
      if (!schema.getConcat().isEmpty()) {
        ImmutableFeatureSchema visited =
            new Builder()
                .from(schema)
                .type(schema.isArray() ? Type.OBJECT_ARRAY : Type.OBJECT)
                .refType(schema.getRefType().orElse(REF_TYPE_DYNAMIC))
                .propertyMap(Map.of())
                .concat(resolveAll(schema.getConcat(), schema.getValueType(), schema.getRefType()))
                .build();

        return MappingOperationResolver.resolveConcat(visited);
      }
      if (!schema.getCoalesce().isEmpty()) {
        ImmutableFeatureSchema visited =
            new Builder()
                .from(schema)
                .type(schema.isArray() ? Type.OBJECT_ARRAY : Type.OBJECT)
                .refType(schema.getRefType().orElse(REF_TYPE_DYNAMIC))
                .propertyMap(Map.of())
                .coalesce(
                    resolveAll(schema.getCoalesce(), schema.getValueType(), schema.getRefType()))
                .build();

        FeatureSchema featureSchema = MappingOperationResolver.resolveCoalesce(visited);
        return featureSchema;
      }

      return resolve(schema, visitedProperties, Optional.empty(), Optional.empty());
    }

    return new ImmutableFeatureSchema.Builder()
        .from(schema)
        .propertyMap(asMap(visitedProperties, FeatureSchema::getFullPathAsString))
        .build();
  }

  public List<FeatureSchema> resolveAll(
      List<FeatureSchema> schemas,
      Optional<Type> fallbackValueType,
      Optional<String> fallbackRefType) {
    return schemas.stream()
        .map(schema -> resolve(schema, schema.getProperties(), fallbackValueType, fallbackRefType))
        .collect(Collectors.toList());
  }

  public FeatureSchema resolve(
      FeatureSchema schema,
      List<FeatureSchema> properties,
      Optional<Type> fallbackValueType,
      Optional<String> fallbackRefType) {
    Type valueType = schema.getValueType().orElse(fallbackValueType.orElse(Type.STRING));
    Optional<String> refType = schema.getRefType().or(() -> fallbackRefType);

    if (properties.isEmpty()) {
      String sourcePath = schema.getSourcePath().orElse("");
      Optional<String> objectSourcePath =
          sourcePath.contains("/")
              ? Optional.of(sourcePath.substring(0, sourcePath.lastIndexOf('/')))
              : Optional.empty();
      String idSourcePath =
          sourcePath.contains("/")
              ? sourcePath.substring(sourcePath.lastIndexOf('/') + 1)
              : sourcePath;

      FeatureSchema build =
          new Builder()
              .from(schema)
              .type(schema.isArray() ? Type.OBJECT_ARRAY : Type.OBJECT)
              .valueType(Optional.empty())
              .sourcePath(objectSourcePath)
              .putProperties2(ID, new Builder().type(valueType).sourcePath(idSourcePath))
              .putProperties2(TITLE, new Builder().type(Type.STRING).sourcePath(idSourcePath))
              .putProperties2(TYPE, new Builder().type(Type.STRING).constantValue(refType))
              .build();
      return build;
    }

    List<FeatureSchema> newVisitedProperties = new ArrayList<>(properties);

    if (properties.stream().noneMatch(schema1 -> Objects.equals(schema1.getName(), TITLE))) {
      FeatureSchema idSchema =
          properties.stream()
              .filter(schema1 -> Objects.equals(schema1.getName(), ID))
              .findFirst()
              .orElseThrow();

      newVisitedProperties.add(
          new Builder().from(idSchema).name(TITLE).type(Type.STRING).path(List.of(TITLE)).build());
    }

    if (properties.stream().noneMatch(schema1 -> Objects.equals(schema1.getName(), TYPE))
        && schema.getRefType().isPresent()) {
      newVisitedProperties.add(
          new Builder()
              .name(TYPE)
              .type(Type.STRING)
              .path(List.of(TYPE))
              .parentPath(schema.getPath())
              .constantValue(refType)
              .build());
    }
    if (schema.getRefUriTemplate().isPresent()) {
      newVisitedProperties.add(
          new Builder()
              .name(URI_TEMPLATE)
              .type(Type.STRING)
              .path(List.of(URI_TEMPLATE))
              .parentPath(schema.getPath())
              .constantValue(schema.getRefUriTemplate())
              .build());
    }
    if (schema.getRefKeyTemplate().isPresent()) {
      newVisitedProperties.add(
          new Builder()
              .name(KEY_TEMPLATE)
              .type(Type.STRING)
              .path(List.of(KEY_TEMPLATE))
              .parentPath(schema.getPath())
              .constantValue(schema.getRefKeyTemplate())
              .build());
    }

    return new ImmutableFeatureSchema.Builder()
        .from(schema)
        .type(schema.isArray() ? Type.OBJECT_ARRAY : Type.OBJECT)
        .refType(refType.orElse(REF_TYPE_DYNAMIC))
        .propertyMap(asMap(newVisitedProperties, FeatureSchema::getFullPathAsString))
        .build();
  }
}
