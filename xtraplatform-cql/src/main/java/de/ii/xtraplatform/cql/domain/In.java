/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Value.Immutable
@JsonDeserialize(builder = ImmutableIn.Builder.class)
public interface In extends CqlNode, NonBinaryScalarOperation {

    String ID_PLACEHOLDER = "_ID_";

    static In of(String property, List<ScalarLiteral> values) {
        return new ImmutableIn.Builder().value(Property.of(property))
                                        .list(values)
                                        .build();
    }

    static In of(String property, ScalarLiteral... values) {
        return new ImmutableIn.Builder().value(Property.of(property))
                                        .list(Arrays.asList(values))
                                        .build();
    }

    static In of(ScalarLiteral... values) {
        return new ImmutableIn.Builder().value(Property.of(ID_PLACEHOLDER))
                                        .addList(values)
                                        .build();
    }

    static In of(List<ScalarLiteral> values) {
        return new ImmutableIn.Builder().value(Property.of(ID_PLACEHOLDER))
                                        .list(values)
                                        .build();
    }

    @Value.Check
    default void check() {
        int count = getList().size();
        Preconditions.checkState(count > 0, "an IN operation must have at least one value, found %s", count);
        Preconditions.checkState(getValue().isPresent(), "an IN operation must have exactly one operand, found 0");
    }

    abstract class Builder {

        public abstract In build();

        public abstract In.Builder value(Scalar property);
    }

    /* TODO not yet supported
    @Value.Default
    default Boolean getNocase() {
        return Boolean.TRUE;
    }
     */

    Optional<Scalar> getValue();

    List<Scalar> getList();

    @Override
    default <T> T accept(CqlVisitor<T> visitor) {
        List<T> children = Stream.concat(Stream.of(getValue().get()), getList().stream())
                                 .map(value -> value.accept(visitor))
                                 .collect(Collectors.toList());

        return visitor.visit(this, children);
    }
}
