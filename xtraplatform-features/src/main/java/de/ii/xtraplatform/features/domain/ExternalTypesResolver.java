/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.schemas.domain.JsonSchemaParser;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalTypesResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExternalTypesResolver.class);

  public Map<String, FeatureSchema> resolve(Map<String, FeatureSchemaExt> externalTypes) {
    Map<String, FeatureSchema> types = new LinkedHashMap<>();

    if (!externalTypes.isEmpty()) {
      ExternalTypesResolver resolver = new ExternalTypesResolver();

      externalTypes.forEach(
          (key, value) -> {
            Optional<FeatureSchema> featureSchema = resolver.resolve(key, value);
            if (featureSchema.isPresent()) {
              types.put(key, featureSchema.get());
            }
          });
    }

    return types;
  }

  public Optional<FeatureSchema> resolve(String key, FeatureSchemaExt value) {
    if (value.getSchema().isPresent()) {
      try {
        // TODO: rel path from blob store
        net.jimblackler.jsonschemafriend.Schema schema =
            new JsonSchemaParser().parseUri(URI.create(value.getSchema().get()));

        FeatureSchema featureSchema = toFeatureSchema(key, schema, null, value);

        if (Objects.nonNull(featureSchema)) {
          return Optional.of(featureSchema);
        }
      } catch (Throwable e) {
        LogContext.error(LOGGER, e, "Error parsing json schema");
      }
    }
    return Optional.empty();
  }

  // TODO: constrains, arrays, oneOf/anyOf/allOf etc
  private FeatureSchema toFeatureSchema(
      String name,
      net.jimblackler.jsonschemafriend.Schema schema,
      @Nullable net.jimblackler.jsonschemafriend.Schema root,
      @Nullable FeatureSchemaExt original) {

    /*if (Objects.equals(name, "isGeregistreerdMet")) {
      return null;
    }*/
    ImmutableFeatureSchema.Builder builder = new ImmutableFeatureSchema.Builder();
    net.jimblackler.jsonschemafriend.Schema s =
        Objects.nonNull(schema.getRef()) ? schema.getRef() : schema;
    net.jimblackler.jsonschemafriend.Schema r = Objects.isNull(root) ? schema : root;
    Type t = toType(s.getExplicitTypes());

    if (Objects.equals(s, root)) {
      return null;
    }

    if (Objects.nonNull(original)) {
      if (original.getIgnore()) {
        LOGGER.debug("IGNORE {}", name);
        return null;
      }
      builderFrom(builder, original).propertyMap(Map.of());
    }

    builder
        .name(name)
        .label(Optional.ofNullable(s.getTitle()))
        .description(Optional.ofNullable(s.getDescription()))
        .sourcePath((Objects.isNull(root) ? "/" : "") + name)
        .type(t);

    // TODO: name vs objectType (from def key)
    if (t == Type.OBJECT && Objects.nonNull(s.getProperties())) {
      s.getProperties()
          .forEach(
              (key, value) -> {
                FeatureSchema featureSchema =
                    toFeatureSchema(
                        key,
                        value,
                        r,
                        Objects.nonNull(original) ? original.getPropertyMap().get(key) : original);
                if (Objects.nonNull(featureSchema)) {
                  builder.putPropertyMap(key, featureSchema);
                }
              });
    } else if (t == Type.OBJECT_ARRAY && Objects.nonNull(s.getItems())) {
      net.jimblackler.jsonschemafriend.Schema is =
          Objects.nonNull(s.getItems().getRef()) ? s.getItems().getRef() : s.getItems();
      Type it = toType(is.getExplicitTypes());
      if (isSimple(it)) {
        builder.type(Type.VALUE_ARRAY).valueType(it);
      } else {
        FeatureSchema featureSchema = toFeatureSchema(name, is, r, original);
        if (Objects.nonNull(featureSchema)) {
          builder.from(featureSchema).type(Type.OBJECT_ARRAY);
        }
      }
    }

    ImmutableFeatureSchema build = builder.build();
    LOGGER.debug("{} {}", name, build);

    return build;
  }

  private ImmutableFeatureSchema.Builder builderFrom(
      ImmutableFeatureSchema.Builder builder, FeatureSchemaExt instance) {
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
