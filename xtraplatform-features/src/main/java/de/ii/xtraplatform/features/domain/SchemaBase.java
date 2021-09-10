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
    TYPE,
    PRIMARY_GEOMETRY,
    PRIMARY_INSTANT,
    PRIMARY_INTERVAL_START,
    PRIMARY_INTERVAL_END
    }

    enum Type {
        INTEGER,
        FLOAT,
        STRING,
        BOOLEAN,
        DATETIME,
        DATE,
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

    @Value.Default
    default List<String> getSourcePaths() {
        return getSourcePath().map(ImmutableList::of).orElse(ImmutableList.of());
    }

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
    default Optional<T> getPrimaryGeometry() {
        return getProperties().stream()
            .filter(t -> t.getRole().filter(role -> role == Role.PRIMARY_GEOMETRY).isPresent())
            .findFirst()
            .or(() -> getProperties().stream()
                .filter(SchemaBase::isGeometry)
                .findFirst());
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default Optional<T> getPrimaryInstant() {
        return getProperties().stream()
            .filter(t -> t.getRole().filter(role -> role == Role.PRIMARY_INSTANT).isPresent())
            .findFirst()
            .or(() -> getProperties().stream()
                .filter(SchemaBase::isTemporal)
                .findFirst());
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default Optional<Tuple<T,T>> getPrimaryInterval() {
        Optional<T> start = getProperties().stream()
            .filter(
                t -> t.getRole().filter(role -> role == Role.PRIMARY_INTERVAL_START).isPresent())
            .findFirst();
        Optional<T> end = getProperties().stream()
            .filter(
                t -> t.getRole().filter(role -> role == Role.PRIMARY_INTERVAL_END).isPresent())
            .findFirst();

        return start.isPresent() && end.isPresent()
            ? Optional.of(Tuple.of(start.get(), end.get()))
            : Optional.empty();
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
    default String getFullPathAsString() {
        return String.join(".", getFullPath());
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
        return isObject() && (!getSourcePaths().isEmpty() && getSourcePaths().get(0).startsWith("/"));
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
    default boolean isTemporal() {
        return getType() == Type.DATETIME || getType() == Type.DATE;
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isId() {
        return getRole().filter(role -> role == Role.ID).isPresent();
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isPrimaryGeometry() {
        return getRole().filter(role -> role == Role.PRIMARY_GEOMETRY).isPresent();
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isPrimaryInstant() {
        return getRole().filter(role -> role == Role.PRIMARY_INSTANT).isPresent();
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isPrimaryIntervalStart() {
        return getRole().filter(role -> role == Role.PRIMARY_INTERVAL_START).isPresent();
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isPrimaryIntervalEnd() {
        return getRole().filter(role -> role == Role.PRIMARY_INTERVAL_END).isPresent();
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default boolean isType() {
        return getRole().filter(role -> role == Role.TYPE).isPresent();
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
