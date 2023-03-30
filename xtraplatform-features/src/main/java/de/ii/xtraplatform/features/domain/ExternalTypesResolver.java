/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableSet;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.store.domain.BlobStore;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.jimblackler.jsonschemafriend.CacheLoader;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalTypesResolver implements TypesResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExternalTypesResolver.class);
  private static final Set<String> SCHEMES = ImmutableSet.of("http", "https");

  private final SchemaStore schemaParser;
  private final BlobStore schemaStore;

  public ExternalTypesResolver(BlobStore schemaStore) {
    this.schemaStore = schemaStore;
    // TODO: custom loader with HttpClient
    this.schemaParser = new SchemaStore(new CacheLoader());
  }

  @Override
  public boolean needsResolving(FeatureSchema type) {
    return type.getSchema().isPresent()
        || type.getAllOf().stream().anyMatch(partial -> partial.getSchema().isPresent());
  }

  @Override
  public FeatureSchema resolve(FeatureSchema type) {
    if (type.getSchema().isPresent()) {
      Optional<net.jimblackler.jsonschemafriend.Schema> schema = parse(type.getSchema().get());

      if (schema.isPresent()) {
        return toFeatureSchema(type.getName(), schema.get(), null, type);
      }

      return null;
    }

    if (type.getAllOf().stream().anyMatch(partial -> partial.getSchema().isPresent())) {
      List<PartialObjectSchema> partials = new ArrayList<>();

      for (PartialObjectSchema partial : type.getAllOf()) {
        if (partial.getSchema().isPresent()) {
          Optional<net.jimblackler.jsonschemafriend.Schema> schema =
              parse(partial.getSchema().get());

          if (schema.isPresent()) {
            PartialObjectSchema resolvedPartial = toPartialSchema(schema.get(), partial);
            if (Objects.nonNull(resolvedPartial)) {
              partials.add(resolvedPartial);
            }
          }
        } else {
          partials.add(partial);
        }
      }

      return new ImmutableFeatureSchema.Builder().from(type).allOf(partials).build();
    }

    return type;
  }

  Optional<net.jimblackler.jsonschemafriend.Schema> parse(String schemaSource) {
    try {
      URI schemaUri = URI.create(schemaSource);

      if (SCHEMES.contains(schemaUri.getScheme())) {
        net.jimblackler.jsonschemafriend.Schema schema = schemaParser.loadSchema(schemaUri);

        return Optional.ofNullable(schema);
      }

      if (Objects.isNull(schemaUri.getScheme())
          && !schemaUri.getSchemeSpecificPart().startsWith("/")) {
        Path path = Path.of(schemaUri.getSchemeSpecificPart());

        if (!schemaStore.has(path)) {
          LOGGER.error("Cannot load schema '{}', not found in 'resources/schemas'.", schemaSource);
          return Optional.empty();
        }

        try (InputStream inputStream = schemaStore.get(path).get()) {
          net.jimblackler.jsonschemafriend.Schema schema = schemaParser.loadSchema(inputStream);

          if (Objects.nonNull(schemaUri.getFragment())) {
            Map<URI, Schema> subSchemas = schema.getSubSchemas();
            URI fullUri =
                URI.create(
                    String.format("%s#%s", schema.getUri().toString(), schemaUri.getFragment()));
            if (Objects.nonNull(subSchemas) && subSchemas.containsKey(fullUri)) {
              schema = subSchemas.get(fullUri);
            } else {
              LOGGER.error(
                  "Cannot load sub-schema '{}', not found in '{}'.", schemaUri.getFragment(), path);
            }
          }

          return Optional.ofNullable(schema);
        }
      }

      LOGGER.error(
          "Cannot load schema '{}', only http/https URLs and relative paths allowed.",
          schemaSource);
    } catch (Throwable e) {
      LogContext.error(LOGGER, e, "Error resolving external schema");
    }

    return Optional.empty();
  }

  private net.jimblackler.jsonschemafriend.Schema resolveComposition(
      net.jimblackler.jsonschemafriend.Schema schema) {
    if (Objects.nonNull(schema.getRef())) {
      return resolveComposition(schema.getRef());
    } else if (Objects.nonNull(schema.getOneOf()) && !schema.getOneOf().isEmpty()) {
      return resolveComposition(schema.getOneOf().iterator().next());
    } else if (Objects.nonNull(schema.getAnyOf()) && !schema.getAnyOf().isEmpty()) {
      return resolveComposition(schema.getAnyOf().iterator().next());
    } else if (Objects.nonNull(schema.getAllOf()) && !schema.getAllOf().isEmpty()) {
      return resolveComposition(schema.getAllOf().iterator().next());
    }
    return schema;
  }

  private PartialObjectSchema toPartialSchema(
      net.jimblackler.jsonschemafriend.Schema schema, PartialObjectSchema original) {
    net.jimblackler.jsonschemafriend.Schema s = resolveComposition(schema);
    Type t = toType(s.getExplicitTypes());

    if (t != Type.OBJECT) {
      LOGGER.error("Schema used in 'allOf' has to be of type 'object' ({})", schema.getUri());
      return null;
    }

    ImmutablePartialObjectSchema.Builder builder =
        new ImmutablePartialObjectSchema.Builder()
            .from(original)
            .schema(Optional.empty())
            .propertyMap(Map.of());

    if (Objects.nonNull(s.getProperties())) {
      s.getProperties()
          .forEach(
              (key, value) -> {
                FeatureSchema featureSchema =
                    toFeatureSchema(key, value, schema, original.getPropertyMap().get(key));
                if (Objects.nonNull(featureSchema)) {
                  builder.putPropertyMap(key, featureSchema);
                }
              });
    }

    return builder.build();
  }

  private FeatureSchema toFeatureSchema(
      String name,
      net.jimblackler.jsonschemafriend.Schema schema,
      @Nullable net.jimblackler.jsonschemafriend.Schema root,
      @Nullable FeatureSchema original) {
    ImmutableFeatureSchema.Builder builder = new ImmutableFeatureSchema.Builder();
    net.jimblackler.jsonschemafriend.Schema s = resolveComposition(schema);

    net.jimblackler.jsonschemafriend.Schema r = Objects.isNull(root) ? schema : root;
    Type t = toType(s.getExplicitTypes());

    if (Objects.equals(s, root)) {
      return null;
    }

    if (Objects.nonNull(original)) {
      if (original.getIgnore()) {
        return null;
      }
      builder.from(original).propertyMap(Map.of());
    }

    builder
        .name(name)
        .label(Optional.ofNullable(schema.getTitle()).or(() -> Optional.ofNullable(s.getTitle())))
        .description(
            Optional.ofNullable(schema.getDescription())
                .or(() -> Optional.ofNullable(s.getDescription())))
        .type(t);

    if (Objects.isNull(original) || original.getSourcePath().isEmpty()) {
      builder.sourcePath((Objects.isNull(root) ? "/" : "") + name);
    }

    if (t == Type.OBJECT) {
      if (Objects.nonNull(s.getProperties())) {
        s.getProperties()
            .forEach(
                (key, value) -> {
                  FeatureSchema featureSchema =
                      toFeatureSchema(
                          key,
                          value,
                          r,
                          Objects.nonNull(original)
                              ? original.getPropertyMap().get(key)
                              : original);
                  if (Objects.nonNull(featureSchema)) {
                    builder.putPropertyMap(key, featureSchema);
                  }
                });
      }
    } else if (t == Type.OBJECT_ARRAY && Objects.nonNull(s.getItems())) {
      net.jimblackler.jsonschemafriend.Schema is =
          Objects.nonNull(s.getItems().getRef()) ? s.getItems().getRef() : s.getItems();
      Type it = toType(is.getExplicitTypes());
      if (isSimple(it)) {
        builder.type(Type.VALUE_ARRAY).valueType(it);
      } else {
        FeatureSchema featureSchema = toFeatureSchema(name, is, r, original);
        if (Objects.nonNull(featureSchema)) {
          // TODO
          builder.path(List.of()).parentPath(List.of()).from(featureSchema).type(Type.OBJECT_ARRAY);
        }
      }
    }

    return builder.build();
  }

  private boolean isSimple(Type type) {
    return type == Type.BOOLEAN
        || type == Type.INTEGER
        || type == Type.FLOAT
        || type == Type.STRING;
  }

  private Type toType(Collection<String> jsonType) {
    if (Objects.isNull(jsonType) || jsonType.isEmpty()) {
      return Type.UNKNOWN;
    }
    switch (jsonType.iterator().next()) {
      case "object":
        return Type.OBJECT;
      case "array":
        return Type.OBJECT_ARRAY;
      case "boolean":
        return Type.BOOLEAN;
      case "integer":
        return Type.INTEGER;
      case "number":
        return Type.FLOAT;
      case "string":
      default:
        return Type.STRING;
    }
  }
}
