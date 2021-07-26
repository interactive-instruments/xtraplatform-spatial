/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.immutables.value.Value;

public interface SchemaBase<T extends SchemaBase<T>> {

    enum Role {
    ID,
    GEOMETRY,
    POINT_IN_TIME,
    PERIOD_START,
    PERIOD_END
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

    String getName();

    Type getType();

    Optional<Role> getRole();

    Optional<Type> getValueType();

    Optional<SimpleFeatureGeometry> getGeometryType();

    List<String> getPath();

    List<String> getParentPath();

    Optional<String> getSourcePath();

    List<T> getProperties();

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default List<T> getAllNestedProperties() {
        return getProperties().stream()
        .flatMap(t -> Stream.concat(Stream.of(t), t.getAllNestedProperties().stream()))
                              .collect(Collectors.toList());
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default Optional<T> getIdProperty() {
        return getProperties().stream()
            .filter(t -> t.getRole().filter(role -> role == Role.ID).isPresent())
            .findFirst();
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default List<String> getFullPath() {
    return new ImmutableList.Builder<String>().addAll(getParentPath()).addAll(getPath()).build();
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default Set<String> getValueNames() {
        return getProperties().stream()
                              .filter(SchemaBase::isValue)
                              .map(SchemaBase::getName)
                              .collect(ImmutableSet.toImmutableSet());
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isObject() {
        return getType() == Type.OBJECT || getType() == Type.OBJECT_ARRAY;
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isArray() {
        return getType() == Type.OBJECT_ARRAY || getType() == Type.VALUE_ARRAY;
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isValue() {
        return !isObject() /*&& getType() != Type.VALUE_ARRAY*/;
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isFeature() {
        return isObject() && getParentPath().isEmpty();
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isGeometry() {
        return getType() == Type.GEOMETRY;
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isId() {
        return getRole().filter(role -> role == Role.ID).isPresent();
    }

    default <U> U accept(SchemaVisitor<T, U> visitor) {
    return visitor.visit(
        (T) this,
        getProperties().stream()
                                                      .map(property -> property.accept(visitor))
            .collect(Collectors.toList()));
  }

  //TODO: replace SchemaVisitor with SchemaVisitorTopDown
  default <U> U accept(SchemaVisitorTopDown<T, U> visitor) {
    return accept(visitor, ImmutableList.of());
  }

  default <U> U accept(SchemaVisitorTopDown<T, U> visitor, List<T> parents) {
    return visitor.visit(
        (T) this,
        parents,
        getProperties().stream()
            .map(
                property ->
                    property.accept(
                        visitor,
                        new ImmutableList.Builder<T>().addAll(parents).add((T) this).build()))
                                                      .collect(Collectors.toList()));
    }
}
