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
@JsonDeserialize(as = TBefore.class)
public interface TBefore extends TemporalOperation, CqlNode {

    @JsonCreator
    static TBefore of(List<Operand> operands) {
        return new ImmutableTBefore.Builder().operands(operands)
                                               .build();
    }

    static TBefore of(String property, TemporalLiteral temporalLiteral) {
        return new ImmutableTBefore.Builder().operands(ImmutableList.of(Property.of(property), temporalLiteral))
                                            .build();
    }

    static TBefore of(String property, String property2) {
        return new ImmutableTBefore.Builder().operands(ImmutableList.of(Property.of(property),Property.of(property2)))
                                            .build();
    }

    abstract class Builder extends TemporalOperation.Builder<TBefore> {
    }

}
