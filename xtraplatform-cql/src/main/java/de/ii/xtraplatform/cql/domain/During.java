/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import org.immutables.value.Value;
import org.threeten.extra.Interval;

import java.time.Instant;
import java.util.Objects;

@Value.Immutable
@JsonDeserialize(builder = ImmutableDuring.Builder.class)
public interface During extends TemporalOperation, CqlNode {

    static During of(String property, TemporalLiteral temporalLiteral) {
        if (!Objects.equals(temporalLiteral.getType(), Interval.class)) {
            throw new IllegalArgumentException(String.format("not a valid interval: %s", temporalLiteral));
        }
        return new ImmutableDuring.Builder().operand1(Property.of(property))
                                            .operand2(temporalLiteral)
                                            .build();
    }

    static During of(String property, String property2) {
        return new ImmutableDuring.Builder().operand1(Property.of(property))
                                            .operand2(Property.of(property2))
                                            .build();
    }

    abstract class Builder extends TemporalOperation.Builder<During> {
    }

    @Value.Check
    @Override
    default void check() {
        TemporalOperation.super.check();
        Preconditions.checkState( getOperand1().get() instanceof Property ||
                                          (getOperand1().get() instanceof TemporalLiteral &&
                                                  Objects.equals(((TemporalLiteral) getOperand1().get()).getType(), Instant.class)),
                                 "The left hand side of DURING must be a property or time instant, found %s",
                                  getOperand1().get() instanceof Property
                                          ? ((Property) getOperand1().get()).getName()
                                          : ((TemporalLiteral) getOperand1().get()).getValue());
        Preconditions.checkState( getOperand2().get() instanceof TemporalLiteral &&
                                         Objects.equals(((TemporalLiteral) getOperand2().get()).getType(), Interval.class),
                                 "The right hand side of DURING must be a time interval, found %s",
                                  getOperand2().get() instanceof Property
                                          ? ((Property) getOperand2().get()).getName()
                                          : ((TemporalLiteral) getOperand2().get()).getValue());
    }
}
