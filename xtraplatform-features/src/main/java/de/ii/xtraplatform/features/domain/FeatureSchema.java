/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilder;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilderMap;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueInstance;
import de.ii.xtraplatform.entity.api.maptobuilder.encoding.ValueBuilderMapEncodingEnabled;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new", attributeBuilderDetection = true)
@ValueBuilderMapEncodingEnabled
@JsonDeserialize(builder = ImmutableFeatureSchema.Builder.class)
@JsonPropertyOrder({"sourcePath", "type", "role", "valueType", "geometryType", "objectType", "label", "description", "transformers", "constraints", "properties"})
public interface FeatureSchema extends SchemaBase<FeatureSchema>, ValueInstance {

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

    Map<String, String> getTransformers();

    Optional<SchemaConstraints> getConstraints();

    //behaves exactly like Map<String, FeaturePropertyV2>, but supports mergeable builder deserialization
    // (immutables attributeBuilder does not work with maps yet)
    @JsonProperty(value = "properties")
    ValueBuilderMap<FeatureSchema, ImmutableFeatureSchema.Builder> getPropertyMap();

    // custom builder to automatically use keys of properties as name
    abstract class Builder implements ValueBuilder<FeatureSchema> {

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
    default ImmutableFeatureSchema.Builder toBuilder() {
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
    default boolean isSpatial() {
        return getType() == Type.GEOMETRY;
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isTemporal() {
        return getType() == Type.DATETIME;
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
}
