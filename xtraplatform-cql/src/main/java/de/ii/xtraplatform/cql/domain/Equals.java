/**
 * Copyright 2022 interactive instruments GmbH
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
@JsonDeserialize(as = Equals.class)
public interface Equals extends SpatialOperation, CqlNode {

    @JsonCreator
    static Equals of(List<Operand> operands) {
        return new ImmutableEquals.Builder().operands(operands)
                                        .build();
    }

    static Equals of(String property, SpatialLiteral spatialLiteral) {
        return new ImmutableEquals.Builder().operands(ImmutableList.of(Property.of(property),spatialLiteral))
                                            .build();
    }

    static Equals of(String property, String property2) {
        return new ImmutableEquals.Builder().operands(ImmutableList.of(Property.of(property), Property.of(property2)))
                                            .build();
    }

    abstract class Builder extends SpatialOperation.Builder<Equals> {
    }

}
