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
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaBase.Scope;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FeatureRefResolver implements SchemaVisitorTopDown<FeatureSchema, FeatureSchema> {

  public static final String ID = "id";
  public static final String TYPE = "type";
  public static final String TITLE = "title";
  public static final String QUERYABLE = "queryable";
  public static final String URI_TEMPLATE = "uriTemplate";
  public static final String KEY_TEMPLATE = "keyTemplate";
  public static final String SUB_ID = "{{id}}";
  public static final String SUB_TYPE = "{{type}}";
  public static final String SUB_TITLE = "{{title}}";
  public static final String SUB_URI_TEMPLATE = "{{uriTemplate}}";
  public static final String SUB_KEY_TEMPLATE = "{{keyTemplate}}";
  public static final String REF_TYPE_DYNAMIC = "DYNAMIC";

  private final Set<String> connectors;

  public FeatureRefResolver(Set<String> connectors) {
    this.connectors = connectors.stream().map(c -> "[" + c + "]").collect(Collectors.toSet());
  }

  private boolean isConnected(String sourcePath) {
    return connectors.stream().anyMatch(sourcePath::contains);
  }

  private boolean isConnected(Optional<String> sourcePath) {
    return sourcePath.isPresent() && isConnected(sourcePath.get());
  }

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

    Map<String, FeatureSchema> visitedPropertiesMap =
        asMap(visitedProperties, FeatureSchema::getFullPathAsString);

    if (visitedProperties.stream().anyMatch(SchemaBase::isFeatureRef)) {
      visitedPropertiesMap =
          asMap(
              visitedProperties.stream()
                  .flatMap(
                      property -> {
                        if (property.isFeatureRef()
                            && (isStatic(property.getRefType())
                                || property.getProperties().stream()
                                    .anyMatch(
                                        p ->
                                            p.getName().equals("type")
                                                && p.getSourcePath().isPresent()))) {
                          Optional<FeatureSchema> idProperty =
                              property.getProperties().stream()
                                  .filter(Objects::nonNull)
                                  .filter(p -> Objects.equals(p.getName(), FeatureRefResolver.ID))
                                  .findFirst();
                          if (idProperty.isPresent()) {
                            return Stream.of(
                                property,
                                new Builder()
                                    .name(property.getName() + "_" + QUERYABLE)
                                    .addPath(property.getName() + "_" + QUERYABLE)
                                    .parentPath(property.getParentPath())
                                    .type(property.isArray() ? Type.VALUE_ARRAY : Type.VALUE)
                                    .valueType(idProperty.get().getType())
                                    .refType(property.getRefType().get())
                                    .label(property.getLabel())
                                    .description(property.getDescription())
                                    .sourcePath(
                                        property.getSourcePath().map(s -> s + "/").orElse("")
                                            + idProperty.get().getSourcePath().orElse(""))
                                    .excludedScopes(property.getExcludedScopes())
                                    .addAllExcludedScopes(
                                        Scope.allBut(Scope.QUERYABLE, Scope.SORTABLE))
                                    .addTransformations(
                                        new ImmutablePropertyTransformation.Builder()
                                            .rename(property.getName())
                                            .build())
                                    .build());
                          }
                        }
                        return Stream.of(property);
                      })
                  .collect(Collectors.toList()),
              FeatureSchema::getFullPathAsString);
    }

    return new ImmutableFeatureSchema.Builder()
        .from(schema)
        .propertyMap(visitedPropertiesMap)
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
    boolean isStatic = isStatic(refType);
    List<Scope> excludedScopes = isStatic ? List.of(Scope.QUERYABLE, Scope.SORTABLE) : List.of();

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

      Builder builder =
          new Builder()
              .from(schema)
              .type(schema.isArray() ? Type.OBJECT_ARRAY : Type.OBJECT)
              .valueType(Optional.empty())
              .sourcePath(objectSourcePath);

      if (objectSourcePath.isPresent() && isConnected(objectSourcePath.get())) {
        builder
            .sourcePath(Optional.empty())
            .addTransformations(
                new ImmutablePropertyTransformation.Builder()
                    .objectMapDuplicate(Map.of(TITLE, ID))
                    .build())
            .addTransformations(
                new ImmutablePropertyTransformation.Builder()
                    .objectAddConstants(Map.of(TYPE, refType.orElse("")))
                    .build())
            .putProperties2(
                ID,
                new Builder()
                    .type(valueType)
                    .sourcePath(sourcePath)
                    .excludedScopes(excludedScopes));

        if (schema.getRefUriTemplate().isPresent()) {
          builder.addTransformations(
              new ImmutablePropertyTransformation.Builder()
                  .objectAddConstants(Map.of(URI_TEMPLATE, schema.getRefUriTemplate().get()))
                  .build());
        }
        if (schema.getRefKeyTemplate().isPresent()) {
          builder.addTransformations(
              new ImmutablePropertyTransformation.Builder()
                  .objectAddConstants(Map.of(KEY_TEMPLATE, schema.getRefKeyTemplate().get()))
                  .build());
        }
      } else {
        builder
            .putProperties2(
                ID,
                new Builder()
                    .type(valueType)
                    .sourcePath(idSourcePath)
                    .excludedScopes(excludedScopes))
            .putProperties2(
                TITLE,
                new Builder()
                    .type(Type.STRING)
                    .sourcePath(idSourcePath)
                    .excludedScopes(excludedScopes))
            .putProperties2(
                TYPE,
                new Builder()
                    .type(Type.STRING)
                    .constantValue(refType)
                    .excludedScopes(excludedScopes));

        if (schema.getRefUriTemplate().isPresent()) {
          builder.putProperties2(
              URI_TEMPLATE,
              new Builder().type(Type.STRING).constantValue(schema.getRefUriTemplate()));
        }
        if (schema.getRefKeyTemplate().isPresent()) {
          builder.putProperties2(
              KEY_TEMPLATE,
              new Builder().type(Type.STRING).constantValue(schema.getRefKeyTemplate()));
        }
      }

      return builder.build();
    }

    List<FeatureSchema> newVisitedProperties = new ArrayList<>(properties);
    List<PropertyTransformation> newTransformations = new ArrayList<>(schema.getTransformations());

    if (properties.stream().noneMatch(schema1 -> Objects.equals(schema1.getName(), TITLE))) {
      if (isConnected(schema.getSourcePath())) {
        newTransformations.add(
            new ImmutablePropertyTransformation.Builder()
                .objectMapDuplicate(Map.of(TITLE, ID))
                .build());
      } else {
        FeatureSchema idSchema =
            properties.stream()
                .filter(schema1 -> Objects.equals(schema1.getName(), ID))
                .findFirst()
                .orElseThrow();

        newVisitedProperties.add(
            new Builder()
                .from(idSchema)
                .name(TITLE)
                .type(Type.STRING)
                .path(List.of(TITLE))
                .excludedScopes(excludedScopes)
                .build());
      }
    }

    if (properties.stream().noneMatch(schema1 -> Objects.equals(schema1.getName(), TYPE))
        && schema.getRefType().isPresent()) {
      if (isConnected(schema.getSourcePath())) {
        newTransformations.add(
            new ImmutablePropertyTransformation.Builder()
                .objectAddConstants(Map.of(TYPE, refType.orElse("")))
                .build());
      } else {
        newVisitedProperties.add(
            new Builder()
                .name(TYPE)
                .type(Type.STRING)
                .path(List.of(TYPE))
                .parentPath(schema.getPath())
                .constantValue(refType)
                .excludedScopes(excludedScopes)
                .build());
      }
    }
    if (schema.getRefUriTemplate().isPresent()) {
      if (isConnected(schema.getSourcePath())) {
        newTransformations.add(
            new ImmutablePropertyTransformation.Builder()
                .objectAddConstants(Map.of(URI_TEMPLATE, schema.getRefUriTemplate().get()))
                .build());
      } else {
        newVisitedProperties.add(
            new Builder()
                .name(URI_TEMPLATE)
                .type(Type.STRING)
                .path(List.of(URI_TEMPLATE))
                .parentPath(schema.getPath())
                .constantValue(schema.getRefUriTemplate())
                .excludedScopes(excludedScopes)
                .build());
      }
    }
    if (schema.getRefKeyTemplate().isPresent()) {
      if (isConnected(schema.getSourcePath())) {
        newTransformations.add(
            new ImmutablePropertyTransformation.Builder()
                .objectAddConstants(Map.of(KEY_TEMPLATE, schema.getRefKeyTemplate().get()))
                .build());
      } else {
        newVisitedProperties.add(
            new Builder()
                .name(KEY_TEMPLATE)
                .type(Type.STRING)
                .path(List.of(KEY_TEMPLATE))
                .parentPath(schema.getPath())
                .constantValue(schema.getRefKeyTemplate())
                .excludedScopes(excludedScopes)
                .build());
      }
    }

    return new ImmutableFeatureSchema.Builder()
        .from(schema)
        .type(schema.isArray() ? Type.OBJECT_ARRAY : Type.OBJECT)
        .refType(refType.orElse(REF_TYPE_DYNAMIC))
        .propertyMap(asMap(newVisitedProperties, FeatureSchema::getFullPathAsString))
        .transformations(newTransformations)
        .build();
  }

  private static boolean isStatic(Optional<String> refType) {
    return refType.filter(refType2 -> !Objects.equals(refType2, REF_TYPE_DYNAMIC)).isPresent();
  }
}
