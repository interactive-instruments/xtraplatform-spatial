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
@JsonDeserialize(as = Lt.class)
public interface Lt extends BinaryScalarOperation, CqlNode {

    @JsonCreator
    static Lt of(List<Operand> operands) {
        return new ImmutableLt.Builder().operands(operands)
                                        .build();
    }

    static Lt of(String property, ScalarLiteral scalarLiteral) {
        return new ImmutableLt.Builder().operands(ImmutableList.of(Property.of(property),scalarLiteral))
                                         .build();
    }

    static Lt of(String property, String property2) {
        return new ImmutableLt.Builder().operands(ImmutableList.of(Property.of(property), Property.of(property2)))
                                         .build();
    }

    static Lt of(Property property, ScalarLiteral scalarLiteral) {
        return new ImmutableLt.Builder().operands(ImmutableList.of(property,scalarLiteral))
                                         .build();
    }

    static Lt of(Property property, Property property2) {
        return new ImmutableLt.Builder().operands(ImmutableList.of(property, property2))
                                         .build();
    }

    static Lt ofFunction(Function function, ScalarLiteral scalarLiteral) {
        return new ImmutableLt.Builder().operands(ImmutableList.of(function, scalarLiteral))
                                         .build();
    }

    abstract class Builder extends BinaryScalarOperation.Builder<Lt> {
    }

}
