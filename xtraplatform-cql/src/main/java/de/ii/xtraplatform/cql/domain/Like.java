/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@JsonDeserialize(builder = ImmutableLike.Builder.class)
public interface Like extends NonBinaryScalarOperation, CqlNode {

    static Like of(String property, ScalarLiteral scalarLiteral) {
        return new ImmutableLike.Builder().operand1(Property.of(property))
                                          .operand2(scalarLiteral)
                                          .build();
    }

    static Like of(String property, ScalarLiteral scalarLiteral, String wildCard, String singlechar, String escapechar, Boolean nocase) {
        return new ImmutableLike.Builder().operand1(Property.of(property))
                                          .operand2(scalarLiteral)
                                          .wildcard(Optional.ofNullable(wildCard))
                                          .singlechar(Optional.ofNullable(singlechar))
                                          .escapechar(Optional.ofNullable(escapechar))
                                          .nocase(Optional.ofNullable(nocase))
                                          .build();
    }

    static Like of(String property, String property2) {
        return new ImmutableLike.Builder().operand1(Property.of(property))
                                          .operand2(Property.of(property2))
                                          .build();
    }

    static Like of(String property, String property2, String wildCard, String singlechar, String escapechar, Boolean nocase) {
        return new ImmutableLike.Builder().operand1(Property.of(property))
                                          .operand2(Property.of(property2))
                                          .wildcard(Optional.ofNullable(wildCard))
                                          .singlechar(Optional.ofNullable(singlechar))
                                          .escapechar(Optional.ofNullable(escapechar))
                                          .nocase(Optional.ofNullable(nocase))
                                          .build();
    }

    static Like ofFunction(Function function, ScalarLiteral scalarLiteral) {
        return new ImmutableLike.Builder().operand1(function)
                                          .operand2(scalarLiteral)
                                          .build();
    }

    static Like ofFunction(Function function, ScalarLiteral scalarLiteral, String wildCard, String singlechar, String escapechar, Boolean nocase) {
        return new ImmutableLike.Builder().operand1(function)
                                          .operand2(scalarLiteral)
                                          .wildcard(Optional.ofNullable(wildCard))
                                          .singlechar(Optional.ofNullable(singlechar))
                                          .escapechar(Optional.ofNullable(escapechar))
                                          .nocase(Optional.ofNullable(nocase))
                                          .build();
    }

    Optional<Scalar> getOperand1();

    Optional<Scalar> getOperand2();

    Optional<String> getWildcard();

    Optional<String> getSinglechar();

    Optional<String> getEscapechar();

    Optional<Boolean> getNocase();

    @Value.Check
    default void check() {
        int count = getOperands().size();
        Preconditions.checkState(count == 2, "a LIKE operation must have exactly two operands, found %s", count);
    }

    @JsonIgnore
    @Value.Derived
    @Value.Auxiliary
    default List<Operand> getOperands() {
        return ImmutableList.of(
                getOperand1(),
                getOperand2()
        )
                            .stream()
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .collect(ImmutableList.toImmutableList());
    }

    abstract class Builder {

        public abstract Like build();

        public abstract Like.Builder operand1(Scalar operand);

        public abstract Like.Builder operand2(Scalar operand);
    }

    @Override
    default <U> U accept(CqlVisitor<U> visitor) {
        U operand1 = getOperands().get(0)
                                  .accept(visitor);
        U operand2 = getOperands().get(1)
                                  .accept(visitor);

        return visitor.visit(this, Lists.newArrayList(operand1, operand2));
    }
}