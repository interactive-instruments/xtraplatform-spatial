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
import de.ii.xtraplatform.features.domain.SchemaBase.Role;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** FIXME add documentation, or include in FeatureRefResolver */
public class FeatureRefEmbedder implements SchemaVisitorTopDown<FeatureSchema, FeatureSchema> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureRefEmbedder.class);

  private final Map<String, FeatureSchema> types;
  private final WithoutRoles withoutRoles;

  public FeatureRefEmbedder(Map<String, FeatureSchema> types) {
    this.types = types;
    this.withoutRoles = new WithoutRoles();
  }

  @Override
  public FeatureSchema visit(
      FeatureSchema schema, List<FeatureSchema> parents, List<FeatureSchema> visitedProperties) {
    if (schema.isEmbed()) {

      if (!schema.getConcat().isEmpty()) {
        return getBuilder(schema, types)
            .map(
                b -> {
                  schema
                      .getConcat()
                      .forEach(
                          concat -> {
                            getBuilder(concat, types)
                                .ifPresent(b2 -> b.addConcat(b2.build().accept(withoutRoles)));
                          });
                  return b.build();
                })
            .orElse(null);
      }

      if (!schema.getCoalesce().isEmpty()) {
        return getBuilder(schema, types)
            .map(
                b -> {
                  schema
                      .getCoalesce()
                      .forEach(
                          coalesce -> {
                            getBuilder(coalesce, types)
                                .ifPresent(b2 -> b.addCoalesce(b2.build().accept(withoutRoles)));
                          });
                  return b.build();
                })
            .orElse(null);
      }

      return getBuilder(schema, types).map(b -> b.build().accept(withoutRoles)).orElse(null);
    }

    Map<String, FeatureSchema> visitedPropertiesMap =
        asMap(visitedProperties, FeatureSchema::getFullPathAsString);

    return new ImmutableFeatureSchema.Builder()
        .from(schema)
        .propertyMap(visitedPropertiesMap)
        .build();
  }

  private static Optional<Builder> getBuilder(
      FeatureSchema schema, Map<String, FeatureSchema> types) {
    String ref =
        schema
            .getRefType()
            .or(
                () ->
                    schema.getProperties().stream()
                        .filter(p -> "type".equals(p.getName()))
                        .findFirst()
                        .flatMap(FeatureSchema::getConstantValue))
            .orElse(null);
    if (Objects.isNull(ref) && schema.getConcat().isEmpty() && schema.getCoalesce().isEmpty()) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(
            "The referenced type of a feature reference cannot be determined. Property: {}.",
            schema.getFullPathAsString());
      }
      return Optional.empty();
    }

    FeatureSchema refSchema = Objects.nonNull(ref) ? types.get(ref) : null;
    if (Objects.isNull(refSchema)
        && schema.getConcat().isEmpty()
        && schema.getCoalesce().isEmpty()) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(
            "The schema of a referenced type of a feature reference cannot be determined. Property: {}.",
            schema.getFullPathAsString());
      }
      return Optional.empty();
    }

    Optional<String> objectType =
        Objects.nonNull(refSchema) ? refSchema.getObjectType() : Optional.ofNullable(ref);

    Builder builder =
        new Builder()
            .from(schema)
            .type(schema.isArray() ? Type.OBJECT_ARRAY : Type.OBJECT)
            .objectType(objectType)
            .refType(Optional.empty())
            .embed(Optional.empty())
            .role(Optional.of(Role.EMBEDDED_FEATURE))
            .concat(List.of())
            .coalesce(List.of());

    if (Objects.nonNull(refSchema)) {
      builder.propertyMap(refSchema.getPropertyMap());
    } else {
      builder.propertyMap(Map.of());
    }

    return Optional.of(builder);
  }
}
