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
@JsonDeserialize(as = AContains.class)
public interface AContains extends ArrayOperation, CqlNode {

    @JsonCreator
    static AContains of(List<Operand> operands) {
        return new ImmutableAContains.Builder().operands(operands)
                                               .build();
    }

    static AContains of(String property, ArrayLiteral arrayLiteral) {
        return new ImmutableAContains.Builder().operands(ImmutableList.of(Property.of(property),arrayLiteral))
                                               .build();
    }

    static AContains of(Property property, ArrayLiteral arrayLiteral) {
        return new ImmutableAContains.Builder().operands(ImmutableList.of(property,arrayLiteral))
                .build();
    }

    static AContains of(String property, String property2) {
        return new ImmutableAContains.Builder().operands(ImmutableList.of(Property.of(property),Property.of(property2)))
                                               .build();
    }

    abstract class Builder extends ArrayOperation.Builder<AContains> {
    }

}
