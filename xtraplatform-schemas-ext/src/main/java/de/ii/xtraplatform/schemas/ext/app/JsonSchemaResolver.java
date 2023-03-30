/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.schemas.ext.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableSet;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.features.domain.ExtensionConfiguration;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutablePartialObjectSchema;
import de.ii.xtraplatform.features.domain.PartialObjectSchema;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaFragmentResolver;
import de.ii.xtraplatform.schemas.ext.domain.JsonSchemaConfiguration;
import de.ii.xtraplatform.store.domain.BlobStore;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.jimblackler.jsonschemafriend.CacheLoader;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class JsonSchemaResolver implements SchemaFragmentResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(JsonSchemaResolver.class);
  private static final Set<String> SCHEMES = ImmutableSet.of("http", "https");

  private final SchemaStore schemaParser;
  private final BlobStore schemaStore;

  @Inject
  JsonSchemaResolver(BlobStore blobStore) {
    this.schemaStore = blobStore.with("schemas");
    // TODO: custom loader with HttpClient
    this.schemaParser = new SchemaStore(new CacheLoader());
  }

  private Optional<JsonSchemaConfiguration> getConfiguration(FeatureProviderDataV2 data) {
    return data.getExtensions().stream()
        .filter(extension -> extension.isEnabled() && extension instanceof JsonSchemaConfiguration)
        .map(extension -> (JsonSchemaConfiguration) extension)
        .findFirst();
  }

  private boolean isEnabled(FeatureProviderDataV2 data) {
    return getConfiguration(data).filter(ExtensionConfiguration::isEnabled).isPresent();
  }

  @Override
  public boolean canResolve(String ref, FeatureProviderDataV2 data) {
    if (!isEnabled(data)) {
      return false;
    }

    try {
      URI schemaUri = URI.create(ref);

      if (SCHEMES.contains(schemaUri.getScheme())
          || ((Objects.isNull(schemaUri.getScheme())
              && !schemaUri.getSchemeSpecificPart().isBlank()
              && !schemaUri.getSchemeSpecificPart().startsWith("/")))) {
        return true;
      }
    } catch (Throwable e) {
      // ignore
    }
    return false;
  }

  @Override
  public FeatureSchema resolve(String ref, FeatureSchema original, FeatureProviderDataV2 data) {
    Optional<Schema> schema = parse(ref);

    if (schema.isPresent()) {
      return toFeatureSchema(original.getName(), schema.get(), null, original, data);
    }

    return null;
  }

  @Override
  public PartialObjectSchema resolve(
      String ref, PartialObjectSchema original, FeatureProviderDataV2 data) {
    Optional<Schema> schema = parse(ref);

    if (schema.isPresent()) {
      return toPartialSchema(schema.get(), original, data);
    }

    return null;
  }

  Optional<Schema> parse(String schemaSource) {
    try {
      URI schemaUri = URI.create(schemaSource);

      if (SCHEMES.contains(schemaUri.getScheme())) {
        Schema schema = schemaParser.loadSchema(schemaUri);

        return Optional.ofNullable(schema);
      }

      if (Objects.isNull(schemaUri.getScheme())
          && !schemaUri.getSchemeSpecificPart().isBlank()
          && !schemaUri.getSchemeSpecificPart().startsWith("/")) {
        Path path = Path.of(schemaUri.getSchemeSpecificPart());

        if (!schemaStore.has(path)) {
          LOGGER.error("Cannot load schema '{}', not found in 'resources/schemas'.", schemaSource);
          return Optional.empty();
        }

        try (InputStream inputStream = schemaStore.get(path).get()) {
          Schema schema = schemaParser.loadSchema(inputStream);

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

  private Schema resolveComposition(Schema schema) {
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
      Schema schema, PartialObjectSchema original, FeatureProviderDataV2 data) {
    Schema s = resolveComposition(schema);
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
                    toFeatureSchema(key, value, schema, original.getPropertyMap().get(key), data);
                if (Objects.nonNull(featureSchema)) {
                  builder.putPropertyMap(key, featureSchema);
                }
              });
    }

    return builder.build();
  }

  private FeatureSchema toFeatureSchema(
      String name,
      Schema schema,
      @Nullable Schema root,
      @Nullable FeatureSchema original,
      FeatureProviderDataV2 data) {
    ImmutableFeatureSchema.Builder builder = new ImmutableFeatureSchema.Builder();
    Schema s = resolveComposition(schema);

    Schema r = Objects.isNull(root) ? schema : root;
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
      applyObjectType(s.getUri().toString(), builder, data);

      if (Objects.nonNull(s.getProperties())) {
        s.getProperties()
            .forEach(
                (key, value) -> {
                  FeatureSchema featureSchema =
                      toFeatureSchema(
                          key,
                          value,
                          r,
                          Objects.nonNull(original) ? original.getPropertyMap().get(key) : original,
                          data);
                  if (Objects.nonNull(featureSchema)) {
                    builder.putPropertyMap(key, featureSchema);
                  }
                });
      }
    } else if (t == Type.OBJECT_ARRAY && Objects.nonNull(s.getItems())) {
      Schema is = Objects.nonNull(s.getItems().getRef()) ? s.getItems().getRef() : s.getItems();
      Type it = toType(is.getExplicitTypes());
      if (isSimple(it)) {
        builder.type(Type.VALUE_ARRAY).valueType(it);
      } else {
        FeatureSchema featureSchema = toFeatureSchema(name, is, r, original, data);
        if (Objects.nonNull(featureSchema)) {
          builder.path(List.of()).parentPath(List.of()).from(featureSchema).type(Type.OBJECT_ARRAY);
        }
        applyObjectType(s.getUri().toString(), builder, data);
      }
    }

    return builder.build();
  }

  private void applyObjectType(
      String uri, ImmutableFeatureSchema.Builder builder, FeatureProviderDataV2 data) {
    getConfiguration(data)
        .ifPresent(
            cfg -> {
              Optional<String> objectType =
                  cfg.getObjectTypeRefs().keySet().stream().filter(uri::endsWith).findFirst();

              if (objectType.isPresent()) {
                builder.objectType(cfg.getObjectTypeRefs().get(objectType.get()));
              }
            });
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
