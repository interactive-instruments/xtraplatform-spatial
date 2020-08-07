/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilder;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueInstance;
import org.immutables.value.Value;

import java.util.Map;
import java.util.Optional;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true, builder = "new", attributeBuilderDetection = true)
@JsonDeserialize(builder = ImmutableFeatureProperty.Builder.class)
public interface FeatureProperty extends ValueInstance {

    //TODO: Role with ID, SPATIAL, TEMPORAL, REFERENCE, REFERENCE_EMBED
    //TODO: more specific types, in addition or instead of Type
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
        UNKNOWN
    }

    abstract class Builder implements ValueBuilder<FeatureProperty> {
    }

    @Override
    default ImmutableFeatureProperty.Builder toBuilder() {
        return new ImmutableFeatureProperty.Builder().from(this);
    }

    //@Nullable
    @JsonIgnore
    String getName();

    String getPath();

    @Value.Default
    default Type getType() {
        return Type.STRING;
    }

    Optional<Role> getRole();

    Optional<String> getConstantValue();

    Map<String, String> getAdditionalInfo();

    //TODO
    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isId() {
        return getRole().filter(role -> role == Role.ID).isPresent();
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

    //TODO: implement double col support as provider transformer and remove this
    @Deprecated
    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean hasDoubleColumn() {
        return getPath().indexOf(":") > getPath().lastIndexOf("/");
    }
}
