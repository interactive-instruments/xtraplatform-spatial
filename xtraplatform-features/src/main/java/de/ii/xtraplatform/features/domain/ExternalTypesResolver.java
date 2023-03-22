/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema.Builder;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.store.domain.BlobStore;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.jimblackler.jsonschemafriend.CacheLoader;
import net.jimblackler.jsonschemafriend.Schema;
import net.jimblackler.jsonschemafriend.SchemaStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalTypesResolver implements SchemaVisitorTopDown<FeatureSchema, FeatureSchema> {

  class AllOfResolver implements SchemaVisitorTopDown<FeatureSchema, FeatureSchema> {
    @Override
    public FeatureSchema visit(
        FeatureSchema schema, List<FeatureSchema> parents, List<FeatureSchema> visitedProperties) {
      boolean ignoreProperties = false;
      FeatureSchema resolved = schema;

      if (!resolved.getAllOf().isEmpty()) {
        resolved = resolveAllOf(resolved);
        // LOGGER.debug("ALLOF {}", resolved);
        ignoreProperties = true;
      }

      if (ignoreProperties) {
        return resolved;
      }

      Map<String, FeatureSchema> visitedPropertiesMap =
          visitedProperties.stream()
              .filter(Objects::nonNull)
              .map(
                  featureSchema ->
                      new SimpleImmutableEntry<>(featureSchema.getName(), featureSchema))
              .collect(
                  ImmutableMap.toImmutableMap(
                      Entry::getKey, Entry::getValue, (first, second) -> second));

      return new ImmutableFeatureSchema.Builder()
          .from(schema)
          .propertyMap(visitedPropertiesMap)
          .build();
    }
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(ExternalTypesResolver.class);
  public static final String IGNORE_OBJECT = "_IGNORE_";

  private final SchemaStore schemaParser;
  private final BlobStore schemaStore;

  public ExternalTypesResolver(BlobStore schemaStore) {
    this.schemaStore = schemaStore;
    // TODO: custom loader with HttpClient
    this.schemaParser = new SchemaStore(new CacheLoader());
  }

  public boolean needsResolving(Map<String, FeatureSchema> types) {
    return types.values().stream()
        .flatMap(type -> Stream.concat(Stream.of(type), type.getAllNestedProperties().stream()))
        .anyMatch(def -> def.getSchema().isPresent() || !def.getAllOf().isEmpty());
  }

  @Override
  public FeatureSchema visit(
      FeatureSchema schema, List<FeatureSchema> parents, List<FeatureSchema> visitedProperties) {
    boolean ignoreProperties = false;
    FeatureSchema resolved = schema;

    /*if (!resolved.getAllOf().isEmpty()) {
      resolved = resolveAllOf(resolved);
      // LOGGER.debug("ALLOF {}", resolved);
      ignoreProperties = true;
    }*/

    if (resolved.getSchema().isPresent()) {
      resolved = resolve(resolved.getSchema().get(), resolved).orElse(null);
      ignoreProperties = true;
    }

    if (ignoreProperties) {
      return resolved;
    }

    Map<String, FeatureSchema> visitedPropertiesMap =
        visitedProperties.stream()
            .filter(Objects::nonNull)
            .map(
                featureSchema -> new SimpleImmutableEntry<>(featureSchema.getName(), featureSchema))
            .collect(
                ImmutableMap.toImmutableMap(
                    Entry::getKey, Entry::getValue, (first, second) -> second));

    return new ImmutableFeatureSchema.Builder()
        .from(schema)
        .propertyMap(visitedPropertiesMap)
        .build();
  }

  public Map<String, FeatureSchema> resolve(Map<String, FeatureSchema> externalTypes) {
    Map<String, FeatureSchema> types = new LinkedHashMap<>();
    AllOfResolver allOfResolver = new AllOfResolver();

    externalTypes.forEach(
        (key, value) -> {
          FeatureSchema resolved = value.accept(allOfResolver);
          resolved = resolved.accept(this);
          if (Objects.nonNull(resolved)) {
            types.put(key, resolved);
          }
        });

    return types;
  }

  public FeatureSchema resolveAllOf(FeatureSchema original) {

    if (!original.getAllOf().isEmpty()) {
      Map<String, FeatureSchema> props = new LinkedHashMap<>();

      original
          .getAllOf()
          .forEach(
              partial -> {
                PartialObjectSchema partial1 = partial;

                if (partial1.getSchema().isPresent()) {
                  ImmutableFeatureSchema partial2 =
                      new Builder()
                          .name(IGNORE_OBJECT)
                          .sourcePath(partial1.getSourcePath())
                          .schema(partial1.getSchema())
                          .propertyMap(partial1.getPropertyMap())
                          .build();
                  Optional<FeatureSchema> resolved = resolve(partial1.getSchema().get(), partial2);
                  if (resolved.isPresent()) {
                    partial1 =
                        new ImmutablePartialObjectSchema.Builder()
                            .putPropertyMap(partial2.getName(), partial2)
                            .build();
                  }
                }
                // LOGGER.debug("PARTIAL {}", partial);
                if (partial1.getSourcePath().isPresent()) {
                  PartialObjectSchema finalPartial = partial1;
                  partial1
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
                                                        finalPartial.getSourcePath().get(),
                                                        sourcePath)))
                                    .sourcePaths(
                                        schema.getSourcePaths().stream()
                                            .map(
                                                sourcePath ->
                                                    String.format(
                                                        "%s/%s",
                                                        finalPartial.getSourcePath().get(),
                                                        sourcePath))
                                            .collect(Collectors.toList()))
                                    .build());
                          });
                } else {
                  props.putAll(partial1.getPropertyMap());
                }
              });

      return new ImmutableFeatureSchema.Builder()
          .from(original)
          .allOf(List.of())
          .propertyMap(props)
          .build();
    }

    return original;
  }

  private static final Set<String> SCHEMES = ImmutableSet.of("http", "https");

  public Optional<FeatureSchema> resolve(String schemaSource, FeatureSchema original) {
    // LOGGER.debug("RESOLVING {}", schemaSource);
    try {
      URI schemaUri = URI.create(schemaSource);

      if (SCHEMES.contains(schemaUri.getScheme())) {
        net.jimblackler.jsonschemafriend.Schema schema = schemaParser.loadSchema(schemaUri);

        FeatureSchema featureSchema = toFeatureSchema(original.getName(), schema, null, original);

        return Optional.ofNullable(featureSchema);
      } else if (Objects.isNull(schemaUri.getScheme())
          && !schemaUri.getSchemeSpecificPart().startsWith("/")) {
        Path path = Path.of(schemaUri.getSchemeSpecificPart());
        if (schemaStore.has(path)) {
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
                    "Cannot load sub-schema '{}', not found in '{}'.",
                    schemaUri.getFragment(),
                    path);
              }
            }

            FeatureSchema featureSchema =
                toFeatureSchema(original.getName(), schema, null, original);

            return Optional.ofNullable(featureSchema);
          }
        } else {
          LOGGER.error("Cannot load schema '{}', not found in 'resources/schemas'.", schemaSource);
        }
      } else {
        LOGGER.error(
            "Cannot load schema '{}', only http/https URLs and relative paths allowed.",
            schemaSource);
      }
    } catch (Throwable e) {
      LogContext.error(LOGGER, e, "Error resolving external schema");
    }

    return Optional.empty();
  }

  net.jimblackler.jsonschemafriend.Schema resolve2(net.jimblackler.jsonschemafriend.Schema schema) {
    if (Objects.nonNull(schema.getRef())) {
      return resolve2(schema.getRef());
    } else if (Objects.nonNull(schema.getOneOf()) && !schema.getOneOf().isEmpty()) {
      return resolve2(schema.getOneOf().iterator().next());
    } else if (Objects.nonNull(schema.getAnyOf()) && !schema.getAnyOf().isEmpty()) {
      return resolve2(schema.getAnyOf().iterator().next());
    } else if (Objects.nonNull(schema.getAllOf()) && !schema.getAllOf().isEmpty()) {
      return resolve2(schema.getAllOf().iterator().next());
    }
    return schema;
  }

  // TODO: configurable
  private static final Map<String, SimpleFeatureGeometry> GEO_DEFS =
      ImmutableMap.of(
          "#/$defs/polygonGeoJSON",
          SimpleFeatureGeometry.POLYGON,
          "#/$defs/multipolygonGeoJSON",
          SimpleFeatureGeometry.MULTI_POLYGON);

  // TODO: constrains, arrays, oneOf/anyOf/allOf etc
  private FeatureSchema toFeatureSchema(
      String name,
      net.jimblackler.jsonschemafriend.Schema schema,
      @Nullable net.jimblackler.jsonschemafriend.Schema root,
      @Nullable FeatureSchema original) {
    ImmutableFeatureSchema.Builder builder = new ImmutableFeatureSchema.Builder();
    net.jimblackler.jsonschemafriend.Schema s = resolve2(schema);

    net.jimblackler.jsonschemafriend.Schema r = Objects.isNull(root) ? schema : root;
    Type t = toType(s.getExplicitTypes());

    if (Objects.equals(s, root)) {
      return null;
    }

    if (Objects.nonNull(original)) {
      if (original.getIgnore()) {
        // LOGGER.debug("IGNORE {}", name);
        return null;
      }
      // builderFrom(builder, original).propertyMap(Map.of());
      builder.from(original).propertyMap(Map.of());
    }

    builder
        .name(name)
        .label(Optional.ofNullable(s.getTitle()))
        .description(Optional.ofNullable(s.getDescription()))
        .type(t);

    if (Objects.isNull(original) || original.getSourcePath().isEmpty()) {
      builder.sourcePath((Objects.isNull(root) ? "/" : "") + name);
    }

    if (t == Type.OBJECT) {
      Optional<String> geoType =
          GEO_DEFS.keySet().stream().filter(def -> s.getUri().toString().endsWith(def)).findFirst();

      if (geoType.isPresent()) {
        // builder.type(Type.STRING);
        builder.type(Type.GEOMETRY).geometryType(GEO_DEFS.get(geoType.get()));
        // TODO: configurable
        builder.sourcePath(
            Optional.ofNullable(original).flatMap(o -> o.getSourcePath()).orElse(name) + "/asWKT");
      }
      // TODO: name vs objectType (from def key)
      else if (Objects.nonNull(s.getProperties())) {
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

    ImmutableFeatureSchema build = builder.build();
    // LOGGER.debug("{} {}", name, build);

    return build;
  }

  private ImmutableFeatureSchema.Builder builderFrom(
      ImmutableFeatureSchema.Builder builder, FeatureSchema instance) {
    Objects.requireNonNull(instance, "instance");
    builder.name(instance.getName());
    builder.addAllPath(instance.getPath());
    builder.addAllParentPath(instance.getParentPath());
    Optional<String> sourcePathOptional = instance.getSourcePath();
    if (sourcePathOptional.isPresent()) {
      builder.sourcePath(sourcePathOptional);
    }
    builder.addAllSourcePaths(instance.getSourcePaths());
    builder.type(instance.getType());
    Optional<SchemaBase.Role> roleOptional = instance.getRole();
    if (roleOptional.isPresent()) {
      builder.role(roleOptional);
    }
    Optional<SchemaBase.Type> valueTypeOptional = instance.getValueType();
    if (valueTypeOptional.isPresent()) {
      builder.valueType(valueTypeOptional);
    }
    Optional<SimpleFeatureGeometry> geometryTypeOptional = instance.getGeometryType();
    if (geometryTypeOptional.isPresent()) {
      builder.geometryType(geometryTypeOptional);
    }
    Optional<String> objectTypeOptional = instance.getObjectType();
    if (objectTypeOptional.isPresent()) {
      builder.objectType(objectTypeOptional);
    }
    /*Optional<String> labelOptional = instance.getLabel();
    if (labelOptional.isPresent()) {
      builder.label(labelOptional);
    }
    Optional<String> descriptionOptional = instance.getDescription();
    if (descriptionOptional.isPresent()) {
      builder.description(descriptionOptional);
    }*/
    Optional<String> unitOptional = instance.getUnit();
    if (unitOptional.isPresent()) {
      builder.unit(unitOptional);
    }
    Optional<String> constantValueOptional = instance.getConstantValue();
    if (constantValueOptional.isPresent()) {
      builder.constantValue(constantValueOptional);
    }
    Optional<FeatureSchemaBase.Scope> scopeOptional = instance.getScope();
    if (scopeOptional.isPresent()) {
      builder.scope(scopeOptional);
    }
    builder.addAllTransformations(instance.getTransformations());
    Optional<SchemaConstraints> constraintsOptional = instance.getConstraints();
    if (constraintsOptional.isPresent()) {
      builder.constraints(constraintsOptional);
    }
    Optional<Boolean> forcePolygonCCWOptional = instance.getForcePolygonCCW();
    if (forcePolygonCCWOptional.isPresent()) {
      builder.forcePolygonCCW(forcePolygonCCWOptional);
    }
    // builder.propertyMap(instance.getPropertyMap());
    // builder.putAllAdditionalInfo(instance.getAdditionalInfo());
    return builder;
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
