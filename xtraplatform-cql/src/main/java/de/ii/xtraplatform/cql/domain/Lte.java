/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonDeserialize(as = Lte.class)
public interface Lte extends BinaryScalarOperation, CqlNode {

    @JsonCreator
    static Lte of(List<Operand> operands) {
        return new ImmutableLte.Builder().operands(operands)
                                        .build();
    }

    static Lte of(String property, ScalarLiteral scalarLiteral) {
        return new ImmutableLte.Builder().operands(ImmutableList.of(Property.of(property),scalarLiteral))
                                         .build();
    }

    static Lte of(String property, String property2) {
        return new ImmutableLte.Builder().operands(ImmutableList.of(Property.of(property), Property.of(property2)))
                                         .build();
    }

    static Lte of(Property property, ScalarLiteral scalarLiteral) {
        return new ImmutableLte.Builder().operands(ImmutableList.of(property,scalarLiteral))
                                         .build();
    }

    static Lte of(Property property, Property property2) {
        return new ImmutableLte.Builder().operands(ImmutableList.of(property, property2))
                                         .build();
    }

    static Lte ofFunction(Function function, ScalarLiteral scalarLiteral) {
        return new ImmutableLte.Builder().operands(ImmutableList.of(function, scalarLiteral))
                                         .build();
    }

    abstract class Builder extends BinaryScalarOperation.Builder<Lte> {
    }

}
