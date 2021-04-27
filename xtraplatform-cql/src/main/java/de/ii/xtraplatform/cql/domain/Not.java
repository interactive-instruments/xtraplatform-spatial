/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Value.Immutable
@JsonDeserialize(as = Not.class)
public interface Not extends CqlNode {

    @JsonValue
    Optional<CqlPredicate> getPredicate();

    @JsonCreator
    static Not of(CqlPredicate predicate) {
        return new ImmutableNot.Builder()
                .predicate(predicate)
                .build();
    }

    static Not of(BinaryOperation<?> binaryOperation) {
        return new ImmutableNot.Builder()
                .predicate(CqlPredicate.of(binaryOperation))
                .build();
    }

    static Not of(NonBinaryScalarOperation scalarOperation) {
        return new ImmutableNot.Builder()
                .predicate(CqlPredicate.of(scalarOperation))
                .build();
    }

    @Value.Check
    default void check() {
        Preconditions.checkState(getPredicate().isPresent(), "a NOT operation must have one child, found 0");
    }

    @Override
    default <T> T accept(CqlVisitor<T> visitor) {
        T expression = getPredicate().get()
                                     .accept(visitor);

        return visitor.visit(this, Lists.newArrayList(expression));
    }
}
