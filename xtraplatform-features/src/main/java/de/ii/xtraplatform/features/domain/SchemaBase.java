package de.ii.xtraplatform.features.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.ii.xtraplatform.cql.domain.CqlVisitor;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface SchemaBase<T extends SchemaBase<T>> {

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

    String getName();

    Type getType();

    Optional<Role> getRole();

    Optional<Type> getValueType();

    Optional<SimpleFeatureGeometry> getGeometryType();

    List<String> getPath();

    List<String> getParentPath();

    Optional<String> getSourcePath();

    List<T> getProperties();

    @Value.Derived
    @Value.Auxiliary
    default List<T> getAllNestedProperties() {
        return getProperties().stream()
                              .flatMap(t -> Stream.concat(
                                      Stream.of(t),
                                      t.getAllNestedProperties()
                                       .stream()
                              ))
                              .collect(Collectors.toList());
    }

    @Value.Derived
    @Value.Auxiliary
    default List<String> getFullPath() {
        return new ImmutableList.Builder<String>().addAll(getParentPath())
                                                  .addAll(getPath())
                                                  .build();
    }

    @Value.Derived
    @Value.Auxiliary
    default Set<String> getValueNames() {
        return getProperties().stream()
                              .filter(SchemaBase::isValue)
                              .map(SchemaBase::getName)
                              .collect(ImmutableSet.toImmutableSet());
    }

    @Value.Derived
    @Value.Auxiliary
    default boolean isObject() {
        return getType() == Type.OBJECT || getType() == Type.OBJECT_ARRAY;
    }

    @Value.Derived
    @Value.Auxiliary
    default boolean isArray() {
        return getType() == Type.OBJECT_ARRAY || getType() == Type.VALUE_ARRAY;
    }

    @Value.Derived
    @Value.Auxiliary
    default boolean isValue() {
        return !isObject() /*&& getType() != Type.VALUE_ARRAY*/;
    }

    @Value.Derived
    @Value.Auxiliary
    default boolean isFeature() {
        return isObject() && getParentPath().isEmpty();
    }

    default <U> U accept(SchemaVisitor<T, U> visitor) {
        return visitor.visit((T) this, getProperties().stream()
                                                      .map(property -> property.accept(visitor))
                                                      .collect(Collectors.toList()));
    }
}
