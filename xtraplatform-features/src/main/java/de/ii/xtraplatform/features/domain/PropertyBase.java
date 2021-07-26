/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public interface PropertyBase<T extends PropertyBase<T,U>, U extends SchemaBase<U>> {

    enum Type {
        VALUE, ARRAY, OBJECT
    }

    Type getType();

    Optional<U> getSchema();

    @Value.Default
    default String getName() {
        return getSchema().map(U::getName).orElse("");
    }

    @Nullable
    String getValue();

    @Value.Auxiliary
    Optional<T> getParent();

    List<T> getNestedProperties();

    List<String> getPropertyPath();

    @Value.Derived
    default boolean isValue() {
        return getType() == Type.VALUE;
    }

    @Value.Derived
    default boolean isObject() {
        return getType() == Type.OBJECT;
    }

    @Value.Derived
    default boolean isArray() {
        return getType() == Type.ARRAY;
    }



    PropertyBase<T,U> schema(Optional<U> schema);

    PropertyBase<T,U> schema(U schema);

    PropertyBase<T,U> name(String name);

    PropertyBase<T,U> type(Type type);

    PropertyBase<T,U> value(String value);

    PropertyBase<T,U> parent(T parent);

    PropertyBase<T,U> addNestedProperties(T element);

    PropertyBase<T,U> propertyPath(Iterable<String> path);

}
