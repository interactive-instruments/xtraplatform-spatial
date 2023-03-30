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
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureQueriesExtension;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutablePartialObjectSchema;
import de.ii.xtraplatform.features.domain.ImmutableSchemaConstraints;
import de.ii.xtraplatform.features.domain.PartialObjectSchema;
import de.ii.xtraplatform.features.domain.Query;
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
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.jimblackler.jsonschemafriend.CacheLoader;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @title JSON Schema Resolver
 * @langEn Derive [schema definitions](../#schema-definitions) from external JSON schema files.
 * @langDe [Schema-Definitionen](../#schema-definitions) aus externen JSON Schema Dateien ableiten.
 * @scopeEn When this extension is enabled `schema` references can point to JSON schema files in
 *     addition to local fragments. The reference can be either a URL or a relative path to a file
 *     in `resources/schemas`. It also supports referencing sub-schemas in `$defs`. Examples: <code>
 *  - `https://example.com/buildings.json`
 *  - `https://example.com/buildings.json#/$defs/address`
 *  - `buildings.json`
 *  - `buildings.json#/$defs/address`
 *  </code>
 * @limitationsEn Compositions are only supported to some extent:<code>
 *  - **oneOf** Only the first variant is used.
 *  - **anyOf** Only the first variant is used.
 *  - **allOf** The first variant is used as main schema. Properties from additonal variants are merged into the main schema.
 *  </code>
 * @scopeDe Wenn diese Erweiterung aktiviert ist können `schema` Referenzen auch auf JSON Schema
 *     Dateien zeigen. Die Referenz kann entweder eine URL oder ein relativer Pfad zu einer Datei in
 *     `resources/schemas` sein. Unterstützt auch das Referenzieren von Sub-Schemas in `$defs`.
 *     Beispiele: <code>
 * - `https://example.com/buildings.json`
 * - `https://example.com/buildings.json#/$defs/address`
 * - `buildings.json`
 * - `buildings.json#/$defs/address`
 * </code>
 * @limitationsDe Kompositionen werden nur teilweise unterstützt:<code>
 * - **oneOf** Nur die erste Variante wird verwendet.
 * - **anyOf** Nur die erste Variante wird verwendet.
 * - **allOf** Die erste Variante wird als Haupt-Schema verwendet. Properties aus den weiteren Varianten werden ins Haupt-Schema gemergt.
 * </code>
 * @ref:propertyTable {@link de.ii.xtraplatform.schemas.ext.domain.ImmutableJsonSchemaConfiguration}
 * @ref:example {@link de.ii.xtraplatform.schemas.ext.domain.JsonSchemaConfiguration}
 */
@Singleton
@AutoBind
public class JsonSchemaResolver implements SchemaFragmentResolver, FeatureQueriesExtension {

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
      return toFeatureSchema(original.getName(), schema.get(), null, original, data, false);
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

  private List<Schema> resolveComposition(Schema schema) {
    if (Objects.nonNull(schema.getRef())) {
      return resolveComposition(schema.getRef());
    } else if (Objects.nonNull(schema.getOneOf()) && !schema.getOneOf().isEmpty()) {
      return resolveComposition(schema.getOneOf().iterator().next());
    } else if (Objects.nonNull(schema.getAnyOf()) && !schema.getAnyOf().isEmpty()) {
      return resolveComposition(schema.getAnyOf().iterator().next());
    } else if (Objects.nonNull(schema.getAllOf()) && !schema.getAllOf().isEmpty()) {
      return schema.getAllOf().stream()
          .flatMap(s -> resolveComposition(s).stream())
          .collect(Collectors.toList());
    }
    return List.of(schema);
  }

  private PartialObjectSchema toPartialSchema(
      Schema schema, PartialObjectSchema original, FeatureProviderDataV2 data) {
    List<Schema> resolved = resolveComposition(schema);
    Schema s = resolved.get(0);
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
      resolveProperties(
          s.getProperties(),
          Objects.nonNull(original) ? original.getPropertyMap() : null,
          schema,
          builder::putPropertyMap,
          data,
          s.getRequiredProperties());
      s.getProperties()
          .forEach(
              (key, value) -> {
                FeatureSchema featureSchema =
                    toFeatureSchema(
                        key, value, schema, original.getPropertyMap().get(key), data, false);
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
      FeatureProviderDataV2 data,
      boolean isRequired) {
    ImmutableFeatureSchema.Builder builder = new ImmutableFeatureSchema.Builder();
    List<Schema> resolved = resolveComposition(schema);
    Schema s = resolved.get(0);

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

    ImmutableSchemaConstraints.Builder constraintsBuilder = builder.constraintsBuilder();
    boolean constrained = false;
    if (isRequired) {
      constraintsBuilder.required(true);
      constrained = true;
    }

    if (t == Type.OBJECT) {
      applyObjectType(s.getUri().toString(), builder, data);

      if (Objects.nonNull(s.getProperties()) || resolved.size() > 1) {
        if (Objects.nonNull(s.getProperties())) {
          resolveProperties(
              s.getProperties(),
              Objects.nonNull(original) ? original.getPropertyMap() : null,
              r,
              builder::putPropertyMap,
              data,
              s.getRequiredProperties());
        }
        if (resolved.size() > 1) {
          for (int i = 1; i < resolved.size(); i++) {
            Schema add = resolved.get(i);
            if (Objects.nonNull(add.getProperties())) {
              resolveProperties(
                  add.getProperties(),
                  Objects.nonNull(original) ? original.getPropertyMap() : null,
                  r,
                  builder::putPropertyMap,
                  data,
                  s.getRequiredProperties());
            }
          }
        }
      }
    } else if (t == Type.OBJECT_ARRAY && Objects.nonNull(s.getItems())) {
      Schema is = Objects.nonNull(s.getItems().getRef()) ? s.getItems().getRef() : s.getItems();
      Type it = toType(is.getExplicitTypes());
      if (isSimple(it)) {
        builder.type(Type.VALUE_ARRAY).valueType(it);
      } else {
        FeatureSchema featureSchema = toFeatureSchema(name, is, r, original, data, false);
        if (Objects.nonNull(featureSchema)) {
          builder.path(List.of()).parentPath(List.of()).from(featureSchema).type(Type.OBJECT_ARRAY);
        }
        applyObjectType(s.getUri().toString(), builder, data);

        if (s.getMinItems() != null) {
          constraintsBuilder.minOccurrence(s.getMinItems().intValue());
          constrained = true;
        }
        if (s.getMaxItems() != null) {
          constraintsBuilder.maxOccurrence(s.getMaxItems().intValue());
          constrained = true;
        }
      }
    }

    if (t == Type.STRING || t == Type.INTEGER || t == Type.FLOAT) {
      if (s.getConst() != null) {
        constraintsBuilder.addEnumValues(s.getConst().toString());
        constrained = true;
      } else if (s.getEnums() != null && !s.getEnums().isEmpty()) {
        constraintsBuilder.addAllEnumValues(
            s.getEnums().stream().map(Object::toString).collect(Collectors.toList()));
        constrained = true;
      }
      if (t == Type.STRING) {
        if (s.getPattern() != null) {
          constraintsBuilder.regex(s.getPattern());
          constrained = true;
        }
      }
      if (t == Type.INTEGER || t == Type.FLOAT) {
        if (s.getMinimum() != null) {
          constraintsBuilder.min(s.getMinimum().doubleValue());
          constrained = true;
        }
        if (s.getMaximum() != null) {
          constraintsBuilder.max(s.getMaximum().doubleValue());
          constrained = true;
        }
      }
    }

    if (constrained) {
      builder.constraints(constraintsBuilder.build());
    }

    return builder.build();
  }

  private void resolveProperties(
      Map<String, Schema> properties,
      Map<String, FeatureSchema> original,
      Schema root,
      BiConsumer<String, FeatureSchema> consumer,
      FeatureProviderDataV2 data,
      Collection<String> requiredProperties) {
    properties.forEach(
        (key, value) -> {
          FeatureSchema featureSchema =
              toFeatureSchema(
                  key,
                  value,
                  root,
                  Objects.nonNull(original) ? original.get(key) : null,
                  data,
                  Objects.nonNull(requiredProperties) && requiredProperties.contains(key));
          if (Objects.nonNull(featureSchema)) {
            consumer.accept(key, featureSchema);
          }
        });
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

  @Override
  public boolean isSupported(FeatureProviderConnector<?, ?, ?> connector) {
    return true;
  }

  @Override
  public void on(
      LIFECYCLE_HOOK hook,
      FeatureProvider2 provider,
      FeatureProviderConnector<?, ?, ?> connector) {}

  @Override
  public void on(
      QUERY_HOOK hook,
      FeatureProviderDataV2 data,
      FeatureProviderConnector<?, ?, ?> connector,
      Query query,
      BiConsumer<String, String> aliasResolver) {}
}
