/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilder;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilderMap;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueInstance;
import de.ii.xtraplatform.entity.api.maptobuilder.encoding.ValueBuilderMapEncodingEnabled;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new", attributeBuilderDetection = true)
@ValueBuilderMapEncodingEnabled
@JsonDeserialize(builder = ImmutableFeaturePropertyV2.Builder.class)
public interface FeaturePropertyV2 extends ValueInstance {

    enum Role {
        ID
    }

    enum Type {
        INTEGER,
        FLOAT,
        STRING,
        BOOLEAN,
        DATETIME,
        GEOMETRY,
        OBJECT,
        VALUE_ARRAY,
        OBJECT_ARRAY,
        UNKNOWN
    }

    @JsonIgnore
    String getName();

    String getPath();

    @Value.Default
    default Type getType() {
        return Type.STRING;
    }

    Optional<String> getObjectType();

    Optional<Type> getValueType();

    Optional<Role> getRole();

    Optional<String> getLabel();

    Optional<String> getDescription();

    Optional<SimpleFeatureGeometry> getGeometryType();

    Optional<Map<String, String>> getTransformers();

    Map<String, List<String>> getConstraints();

    //behaves exactly like Map<String, FeaturePropertyV2>, but supports mergeable builder deserialization
    // (immutables attributeBuilder does not work with maps yet)
    @JsonMerge
    ValueBuilderMap<FeaturePropertyV2, ImmutableFeaturePropertyV2.Builder> getProperties();

    Map<String, String> getAdditionalInfo();

    // custom builder to automatically use keys of types as name of nested FeaturePropertyV2
    abstract class Builder implements ValueBuilder<FeaturePropertyV2> {

        public abstract ImmutableFeaturePropertyV2.Builder putProperties(String key,
                                                                     ImmutableFeaturePropertyV2.Builder builder);

        @JsonProperty(value = "properties")
        public ImmutableFeaturePropertyV2.Builder putProperties2(String key, ImmutableFeaturePropertyV2.Builder builder) {
            return putProperties(key, builder.name(key));
        }
    }

    @Override
    default ImmutableFeaturePropertyV2.Builder toBuilder() {
        return new ImmutableFeaturePropertyV2.Builder().from(this);
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
