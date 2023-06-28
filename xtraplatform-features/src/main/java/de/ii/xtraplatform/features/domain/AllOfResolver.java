/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AllOfResolver implements TypesResolver {

  @Override
  public boolean needsResolving(FeatureSchema type) {
    return !type.getMerge().isEmpty();
  }

  @Override
  public FeatureSchema resolve(FeatureSchema type) {
    if (needsResolving(type)) {
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

    return type;
  }
}
