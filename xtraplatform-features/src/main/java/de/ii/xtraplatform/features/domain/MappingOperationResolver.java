/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MappingOperationResolver implements TypesResolver {

  @Override
  public boolean needsResolving(FeatureSchema type) {
    return hasMerge(type) || hasConcat(type) || hasCoalesce(type);
  }

  @Override
  public FeatureSchema resolve(FeatureSchema type) {
    FeatureSchema resolved = type;

    if (hasMerge(type)) {
      resolved = resolveMerge(type);
    }

    if (hasConcat(type)) {
      resolved = resolveConcat(type);
    }

    if (hasCoalesce(type)) {
      resolved = resolveCoalesce(type);
    }

    return resolved;
  }

  private FeatureSchema resolveMerge(FeatureSchema type) {
    Map<String, FeatureSchema> props = new LinkedHashMap<>();

    type.getMerge()
        .forEach(
            partial -> {
              if (partial.getSourcePath().isPresent()) {
                partial
                    .getPropertyMap()
                    .forEach(
                        (key, schema) -> {
                          props.put(
                              key,
                              new ImmutableFeatureSchema.Builder()
                                  .from(schema)
                                  .sourcePath(
                                      schema
                                          .getSourcePath()
                                          .map(
                                              sourcePath ->
                                                  String.format(
                                                      "%s/%s",
                                                      partial.getSourcePath().get(), sourcePath)))
                                  .sourcePaths(
                                      schema.getSourcePaths().stream()
                                          .map(
                                              sourcePath ->
                                                  String.format(
                                                      "%s/%s",
                                                      partial.getSourcePath().get(), sourcePath))
                                          .collect(Collectors.toList()))
                                  .build());
                        });
              } else {
                props.putAll(partial.getPropertyMap());
              }
            });

    return new ImmutableFeatureSchema.Builder()
        .from(type)
        .merge(List.of())
        .propertyMap(props)
        .build();
  }

  private FeatureSchema resolveConcat(FeatureSchema type) {
    if (type.getType() == Type.VALUE_ARRAY || type.getType() == Type.FEATURE_REF_ARRAY) {
      String basePath = type.getSourcePath().map(p -> p + "/").orElse("");

      ImmutableFeatureSchema.Builder builder =
          new ImmutableFeatureSchema.Builder().from(type).sourcePath(Optional.empty());

      for (FeatureSchema concat : type.getConcat()) {
        builder.addSourcePaths(basePath + concat.getSourcePath().orElse(""));
      }

      if (type.getType() == Type.FEATURE_REF_ARRAY
          && type.getConcat().stream().anyMatch(s -> s.getType() == Type.STRING)) {
        builder.concat(
            type.getConcat().stream()
                .map(
                    s -> {
                      if (s.getType() == Type.STRING) {
                        return new ImmutableFeatureSchema.Builder()
                            .from(s)
                            .type(type.getType())
                            .build();
                      }
                      return s;
                    })
                .collect(Collectors.toList()));
      }

      return builder.build();
    }

    if (type.getType() == Type.OBJECT_ARRAY) {
      String basePath = type.getSourcePath().map(p -> p + "/").orElse("");

      ImmutableFeatureSchema.Builder builder =
          new ImmutableFeatureSchema.Builder().from(type).sourcePath(Optional.empty());

      for (int i = 0; i < type.getConcat().size(); i++) {
        String basePath2 =
            basePath + type.getConcat().get(i).getSourcePath().map(p -> p + "/").orElse("");

        for (FeatureSchema prop : type.getConcat().get(i).getProperties()) {
          builder.putProperties2(
              i + "_" + prop.getName(),
              new ImmutableFeatureSchema.Builder()
                  .from(prop)
                  .sourcePath(basePath2 + prop.getSourcePath().orElse(""))
                  .path(List.of(i + "_" + prop.getName()))
                  .putAdditionalInfo("concatIndex", String.valueOf(i))
                  .putAdditionalInfo(
                      type.getConcat().get(i).isArray() ? "concatArray" : "concatValue", "true")
                  .transformations(List.of())
                  .addTransformations(
                      new ImmutablePropertyTransformation.Builder().rename(prop.getName()).build())
                  .addAllTransformations(prop.getTransformations()));
        }
      }

      if (type.getConcat().stream().anyMatch(s -> s.getType() == Type.STRING)) {
        builder.concat(
            type.getConcat().stream()
                .map(
                    s -> {
                      if (s.getType() == Type.STRING) {
                        return new ImmutableFeatureSchema.Builder()
                            .from(s)
                            .type(type.getType())
                            .build();
                      }
                      return s;
                    })
                .collect(Collectors.toList()));
      }

      return builder.build();
    }

    return type;
  }

  private FeatureSchema resolveCoalesce(FeatureSchema type) {
    if (type.isValue() && !type.isArray()) {
      String basePath = type.getSourcePath().map(p -> p + "/").orElse("");

      ImmutableFeatureSchema.Builder builder =
          new ImmutableFeatureSchema.Builder().from(type).sourcePath(Optional.empty());

      for (FeatureSchema coalesce : type.getCoalesce()) {
        builder.addSourcePaths(basePath + coalesce.getSourcePath().orElse(""));
      }

      if (type.getType() == Type.FEATURE_REF
          && type.getCoalesce().stream().anyMatch(s -> s.getType() == Type.STRING)) {
        builder.coalesce(
            type.getCoalesce().stream()
                .map(
                    s -> {
                      if (s.getType() == Type.STRING) {
                        return new ImmutableFeatureSchema.Builder()
                            .from(s)
                            .type(type.getType())
                            .build();
                      }
                      return s;
                    })
                .collect(Collectors.toList()));
      }

      return builder.build();
    }

    if (type.isObject() && !type.isArray()) {
      String basePath = type.getSourcePath().map(p -> p + "/").orElse("");

      ImmutableFeatureSchema.Builder builder =
          new ImmutableFeatureSchema.Builder().from(type).sourcePath(Optional.empty());

      for (int i = 0; i < type.getCoalesce().size(); i++) {
        String basePath2 =
            basePath + type.getCoalesce().get(i).getSourcePath().map(p -> p + "/").orElse("");

        for (FeatureSchema prop : type.getCoalesce().get(i).getProperties()) {
          builder.putProperties2(
              i + "_" + prop.getName(),
              new ImmutableFeatureSchema.Builder()
                  .from(prop)
                  .sourcePath(basePath2 + prop.getSourcePath().orElse(""))
                  .path(List.of(i + "_" + prop.getName()))
                  .putAdditionalInfo("coalesceIndex", String.valueOf(i))
                  .putAdditionalInfo(
                      type.getCoalesce().get(i).isArray() ? "coalesceArray" : "coalesceValue",
                      "true")
                  .transformations(List.of())
                  .addTransformations(
                      new ImmutablePropertyTransformation.Builder().rename(prop.getName()).build())
                  .addAllTransformations(prop.getTransformations()));
        }
      }

      if (type.getCoalesce().stream().anyMatch(s -> s.getType() == Type.STRING)) {
        builder.coalesce(
            type.getCoalesce().stream()
                .map(
                    s -> {
                      if (s.getType() == Type.STRING) {
                        return new ImmutableFeatureSchema.Builder()
                            .from(s)
                            .type(type.getType())
                            .build();
                      }
                      return s;
                    })
                .collect(Collectors.toList()));
      }

      return builder.build();
    }

    return type;
  }

  private static boolean hasMerge(FeatureSchema type) {
    return !type.getMerge().isEmpty();
  }

  private static boolean hasConcat(FeatureSchema type) {
    return !type.getConcat().isEmpty();
  }

  private static boolean hasCoalesce(FeatureSchema type) {
    return !type.getCoalesce().isEmpty();
  }
}
