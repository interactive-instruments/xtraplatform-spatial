/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import dagger.Lazy;
import de.ii.xtraplatform.features.app.LocalSchemaFragmentResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaReferenceResolver implements TypesResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(SchemaReferenceResolver.class);

  private final FeatureProviderDataV2 data;
  private final Lazy<Set<SchemaFragmentResolver>> schemaResolvers;
  private final SchemaFragmentResolver localFragmentResolver;

  public SchemaReferenceResolver(
      FeatureProviderDataV2 data, Lazy<Set<SchemaFragmentResolver>> schemaResolvers) {
    this.data = data;
    this.schemaResolvers = schemaResolvers;
    this.localFragmentResolver = new LocalSchemaFragmentResolver();
  }

  private static boolean hasSchema(FeatureSchema type) {
    return type.getSchema().isPresent();
  }

  private static boolean hasSchema(PartialObjectSchema partial) {
    return partial.getSchema().isPresent();
  }

  private static boolean hasMergeWithSchema(FeatureSchema type) {
    return type.getMerge().stream().anyMatch(SchemaReferenceResolver::hasSchema);
  }

  private static boolean hasConcatWithSchema(FeatureSchema type) {
    return type.getConcat().stream().anyMatch(SchemaReferenceResolver::hasSchema);
  }

  private static boolean hasCoalesceWithSchema(FeatureSchema type) {
    return type.getCoalesce().stream().anyMatch(SchemaReferenceResolver::hasSchema);
  }

  private SchemaFragmentResolver getResolver(String ref) {
    // NOTE: workaround, AutoBinds currently cannot be collected and registered from the same module
    return Stream.concat(Stream.of(localFragmentResolver), schemaResolvers.get().stream())
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
    return hasSchema(type)
        || hasMergeWithSchema(type)
        || hasConcatWithSchema(type)
        || hasCoalesceWithSchema(type);
  }

  @Override
  public boolean needsResolving(PartialObjectSchema partial) {
    return hasSchema(partial);
  }

  @Override
  public FeatureSchema resolve(FeatureSchema type) {
    if (hasSchema(type)) {
      return resolve(type.getSchema().get(), type);
    }

    if (hasMergeWithSchema(type)) {
      List<PartialObjectSchema> partials = new ArrayList<>();

      for (PartialObjectSchema partial : type.getMerge()) {
        if (hasSchema(partial)) {
          PartialObjectSchema resolvedPartial = resolve(partial.getSchema().get(), partial);

          if (Objects.nonNull(resolvedPartial)) {
            partials.add(resolvedPartial);
          }
        } else {
          partials.add(partial);
        }
      }

      return new ImmutableFeatureSchema.Builder().from(type).merge(partials).build();
    }

    if (hasConcatWithSchema(type)) {
      List<FeatureSchema> partials = resolvePartials(type.getConcat());

      return new ImmutableFeatureSchema.Builder()
          .from(type)
          .concat(partials)
          .propertyMap(Map.of())
          .build();
    }

    if (hasCoalesceWithSchema(type)) {
      List<FeatureSchema> partials = resolvePartials(type.getCoalesce());

      return new ImmutableFeatureSchema.Builder()
          .from(type)
          .coalesce(partials)
          .propertyMap(Map.of())
          .build();
    }

    return type;
  }

  private List<FeatureSchema> resolvePartials(List<FeatureSchema> partials) {
    List<FeatureSchema> resolved = new ArrayList<>();

    for (FeatureSchema partial : partials) {
      if (hasSchema(partial)) {
        FeatureSchema resolvedPartial = resolve(partial.getSchema().get(), partial);

        if (Objects.nonNull(resolvedPartial)) {
          resolved.add(resolvedPartial);
        }
      } else {
        resolved.add(partial);
      }
    }
    return resolved;
  }
}
