/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.Buildable;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableBuilder;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableMap;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.encoding.BuildableMapEncodingEnabled;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import org.immutables.value.Value;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new", attributeBuilderDetection = true)
@BuildableMapEncodingEnabled
@JsonDeserialize(builder = ImmutableFeatureSchema.Builder.class)
@JsonPropertyOrder({"sourcePath", "type", "role", "valueType", "geometryType", "objectType", "label", "description", "transformations", "constraints", "properties"})
public interface FeatureSchema extends SchemaBase<FeatureSchema>, Buildable<FeatureSchema> {

    @JsonIgnore
    @Override
    String getName();

    @JsonIgnore
    @Override
    List<String> getPath();

    @JsonIgnore
    @Override
    List<String> getParentPath();

    @JsonAlias("path")
    @Override
    Optional<String> getSourcePath();

    @Value.Default
    default List<String> getSourcePaths() {
        return getSourcePath().map(ImmutableList::of).orElse(ImmutableList.of());
    }

    @Value.Default
    @Override
    default Type getType() {
        return getPropertyMap().isEmpty() ? Type.STRING : Type.OBJECT;
    }

    @Override
    Optional<Role> getRole();

    @Override
    Optional<Type> getValueType();

    @Override
    Optional<SimpleFeatureGeometry> getGeometryType();

    Optional<String> getObjectType();

    Optional<String> getLabel();

    Optional<String> getDescription();

    Optional<String> getConstantValue();

    Optional<PropertyTransformation> getTransformations();

    Optional<SchemaConstraints> getConstraints();

    //behaves exactly like Map<String, FeaturePropertyV2>, but supports mergeable builder deserialization
    // (immutables attributeBuilder does not work with maps yet)
    @JsonProperty(value = "properties")
    BuildableMap<FeatureSchema, ImmutableFeatureSchema.Builder> getPropertyMap();

    // custom builder to automatically use keys of properties as name
    abstract class Builder implements BuildableBuilder<FeatureSchema> {

        public abstract ImmutableFeatureSchema.Builder putPropertyMap(String key,
                                                                      ImmutableFeatureSchema.Builder builder);

        //@JsonMerge
        @JsonProperty(value = "properties")
        public ImmutableFeatureSchema.Builder putProperties2(Map<String, ImmutableFeatureSchema.Builder> builderMap) {
            ImmutableFeatureSchema.Builder builder1 = null;
            for (Map.Entry<String, ImmutableFeatureSchema.Builder> entry : builderMap.entrySet()) {
                String key = entry.getKey();
                ImmutableFeatureSchema.Builder builder = entry.getValue();
                builder1 = putPropertyMap(key, builder.name(key));
            }
            return builder1;
            //return putPropertyMap(key, builder.name(key));
        }

        //@JsonProperty(value = "properties")
        //@JsonAnySetter
        public ImmutableFeatureSchema.Builder putProperties2(String key, ImmutableFeatureSchema.Builder builder) {
            return putPropertyMap(key, builder.name(key));
        }
    }

    @Override
    default ImmutableFeatureSchema.Builder getBuilder() {
        return new ImmutableFeatureSchema.Builder().from(this);
    }

    Map<String, String> getAdditionalInfo();

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    @Override
    default List<FeatureSchema> getProperties() {
        return getPropertyMap().values()
                               .stream()
                               .map(featureSchema -> new ImmutableFeatureSchema.Builder().from(featureSchema)
                                                                                         .addPath(featureSchema.getName())
                                                                                         .parentPath(getFullPath())
                                                                                         .build())
                               .collect(Collectors.toList());
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    @Override
    default List<String> getFullPath() {
        return SchemaBase.super.getFullPath();
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    @Override
    default Set<String> getValueNames() {
        return SchemaBase.super.getValueNames();
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    @Override
    default boolean isObject() {
        return SchemaBase.super.isObject();
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    @Override
    default boolean isValue() {
        return SchemaBase.super.isValue();
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    @Override
    default boolean isFeature() {
        return SchemaBase.super.isFeature();
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isId() {
        return getRole().filter(role -> role == Role.ID)
                        .isPresent();
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isType() {
        return getRole().filter(role -> role == Role.TYPE)
            .isPresent();
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isSpatial() {
        return getType() == Type.GEOMETRY;
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isTemporal() {
        return getType() == Type.DATETIME;
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isConstant() {
        return (isValue() && getConstantValue().isPresent()) || (isObject() && getProperties().stream().allMatch(FeatureSchema::isConstant));
    }

    //TODO
    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isReference() {
        return false;
    }

    //TODO
    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isReferenceEmbed() {
        return false;
    }

    //TODO
    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isForceReversePolygon() {
        return false;
    }

    //TODO
    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isRequired() {
        return getConstraints().filter(SchemaConstraints::isRequired).isPresent();
    }

    @Value.Check
    default FeatureSchema normalizeConstants() {
        if (!getPropertyMap().isEmpty() && getPropertyMap().values()
                                                           .stream()
                                                           .anyMatch(property -> property.getConstantValue()
                                                                                         .isPresent() && !property.getSourcePath()
                                                                                                                  .isPresent())) {
            final int[] constantCounter = {0};

            Map<String, FeatureSchema> properties = getPropertyMap().entrySet()
                                                                                     .stream()
                                                                                     .map(entry -> {
                                                                                         if (entry.getValue().getConstantValue().isPresent() && !entry.getValue().getSourcePath().isPresent()) {
                                                                                             String constantValue = entry.getValue().getType() == Type.STRING ? String.format("'%s'", entry.getValue()
                                                                                                                                                                                           .getConstantValue()
                                                                                                                                                                                           .get()) : entry.getValue().getConstantValue().get();
                                                                                             String constantSourcePath = String.format(
                                                                                                 "constant_%s_%d{constant=%s}",
                                                                                                 getName(),
                                                                                                 constantCounter[0]++,
                                                                                                 constantValue);
                                                                                             return new AbstractMap.SimpleEntry<>(entry.getKey(), new ImmutableFeatureSchema.Builder().from(entry.getValue())
                                                                                                                                                                                      .sourcePath(constantSourcePath)
                                                                                                                                                                                        .addSourcePaths(constantSourcePath)
                                                                                                                                                                                      .build());
                                                                                         }
                                                                                         return entry;
                                                                                     })
                                                                                     .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

            return new ImmutableFeatureSchema.Builder().from(this)
                                                       .propertyMap(properties)
                                                       .build();
        }

        return this;
    }

    @Value.Check
    default FeatureSchema backwardsCompatibility() {
        // migrate double column syntax to multiple sourcePaths
        if (getSourcePath().filter(path -> path.lastIndexOf(':') > path.lastIndexOf('/')).isPresent()) {
            String path1 = getSourcePath().get().substring(0, getSourcePath().get().lastIndexOf(':'));
            String path2 = path1.substring(0, path1.lastIndexOf('/') + 1) + getSourcePath().get().substring(getSourcePath().get().lastIndexOf(':') + 1);

            return new ImmutableFeatureSchema.Builder().from(this)
                //TODO
                .sourcePath(path1)
                .addSourcePaths(path1)
                .addSourcePaths(path2)
                .build();
        }

        return this;
    }
}
