/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import dagger.Lazy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaReferenceResolver implements TypesResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(SchemaReferenceResolver.class);

  private final FeatureProviderDataV2 data;
  private final Lazy<Set<SchemaFragmentResolver>> schemaResolvers;

  public SchemaReferenceResolver(
      FeatureProviderDataV2 data, Lazy<Set<SchemaFragmentResolver>> schemaResolvers) {
    this.data = data;
    this.schemaResolvers = schemaResolvers;
  }

  private static boolean hasSchema(FeatureSchema type) {
    return type.getSchema().isPresent();
  }

  private static boolean hasSchema(PartialObjectSchema partial) {
    return partial.getSchema().isPresent();
  }

  private static boolean hasNestedSchema(PartialObjectSchema partial) {
    return partial.getAllNestedProperties().stream().anyMatch(SchemaReferenceResolver::hasSchema);
  }

  private static boolean hasAllOfWithSchema(FeatureSchema type) {
    return type.getAllOf().stream().anyMatch(SchemaReferenceResolver::hasSchema);
  }

  private static boolean hasAllOfWithNestedSchema(FeatureSchema type) {
    return type.getAllOf().stream().anyMatch(SchemaReferenceResolver::hasNestedSchema);
  }

  private SchemaFragmentResolver getResolver(String ref) {
    return schemaResolvers.get().stream()
        .filter(resolver -> resolver.canResolve(ref, data))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("No resolver found for 'schema: " + ref + "'"));
  }

  private FeatureSchema resolve(String ref, FeatureSchema original) {
    SchemaFragmentResolver resolver = getResolver(ref);

    return resolver.resolve(ref, original, data);
  }

  private PartialObjectSchema resolve(String ref, PartialObjectSchema original) {
    SchemaFragmentResolver resolver = getResolver(ref);

    return resolver.resolve(ref, original, data);
  }

  @Override
  public boolean needsResolving(FeatureSchema type) {
    return hasSchema(type) || hasAllOfWithSchema(type) || hasAllOfWithNestedSchema(type);
  }

  @Override
  public FeatureSchema resolve(FeatureSchema type) {
    if (hasSchema(type)) {
      return resolve(type.getSchema().get(), type);
    }

    if (hasAllOfWithSchema(type) || hasAllOfWithNestedSchema(type)) {
      List<PartialObjectSchema> partials = new ArrayList<>();

      for (PartialObjectSchema partial : type.getAllOf()) {
        if (hasSchema(partial)) {
          PartialObjectSchema resolvedPartial = resolve(partial.getSchema().get(), partial);

          if (Objects.nonNull(resolvedPartial)) {
            partials.add(resolvedPartial);
          }
        } else if (hasNestedSchema(partial)) {
          ImmutablePartialObjectSchema.Builder builder =
              new ImmutablePartialObjectSchema.Builder().from(partial).propertyMap(Map.of());

          partial
              .getPropertyMap()
              .forEach(
                  (key, value) -> {
                    FeatureSchema accept = value.accept(this);
                    if (Objects.nonNull(accept)) {
                      builder.putPropertyMap(key, accept);
                    }
                  });

          partials.add(builder.build());
        } else {
          partials.add(partial);
        }
      }

      return new ImmutableFeatureSchema.Builder().from(type).allOf(partials).build();
    }

    return type;
  }
}
