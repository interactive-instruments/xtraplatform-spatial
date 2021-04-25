/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableEq.Builder.class)
public interface Eq extends BinaryScalarOperation, CqlNode {

    static Eq of(String property, ScalarLiteral scalarLiteral) {
        return new ImmutableEq.Builder().operand1(Property.of(property))
                                        .operand2(scalarLiteral)
                                        .build();
    }

    static Eq of(String property, String property2) {
        return new ImmutableEq.Builder().operand1(Property.of(property))
                                        .operand2(Property.of(property2))
                                        .build();
    }

    static Eq of(Property property, ScalarLiteral scalarLiteral) {
        return new ImmutableEq.Builder().operand1(property)
                                        .operand2(scalarLiteral)
                                        .build();
    }

    static Eq of(Property property, Property property2) {
        return new ImmutableEq.Builder().operand1(property)
                                        .operand2(property2)
                                        .build();
    }

    static Eq ofFunction(Function function, ScalarLiteral scalarLiteral) {
        return new ImmutableEq.Builder().operand1(function)
                                        .operand2(scalarLiteral)
                                        .build();
    }

    abstract class Builder extends BinaryScalarOperation.Builder<Eq> {
    }

}
