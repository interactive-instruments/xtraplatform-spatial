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
@JsonDeserialize(builder = ImmutableCrosses.Builder.class)
public interface Crosses extends SpatialOperation, CqlNode {

    static Crosses of(String property, SpatialLiteral spatialLiteral) {
        return new ImmutableCrosses.Builder().operand1(Property.of(property))
                                              .operand2(spatialLiteral)
                                              .build();
    }

    static Crosses of(String property, String property2) {
        return new ImmutableCrosses.Builder().operand1(Property.of(property))
                                              .operand2(Property.of(property2))
                                              .build();
    }

    abstract class Builder extends SpatialOperation.Builder<Crosses> {
    }

}
