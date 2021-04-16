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
@JsonDeserialize(builder = ImmutableTOverlaps.Builder.class)
public interface TOverlaps extends TemporalOperation, CqlNode {

    static TOverlaps of(Temporal temporal1, Temporal temporal2) {
        return new ImmutableTOverlaps.Builder().operand1(temporal1)
                                               .operand2(temporal2)
                                               .build();
    }

    static TOverlaps of(String property, Property property2) {
        return new ImmutableTOverlaps.Builder().property(property)
                .property2(property2)
                .build();
    }

    abstract class Builder extends TemporalOperation.Builder<TOverlaps> {
    }

}
