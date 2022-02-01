/**
 * Copyright 2022 interactive instruments GmbH
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

import java.util.Objects;

@Value.Immutable
@JsonDeserialize(as = TStartedBy.class)
public interface TStartedBy extends TemporalOperation, CqlNode {

    abstract class Builder extends TemporalOperation.Builder<TStartedBy> {
    }

    @Value.Check
    @Override
    default void check() {
        TemporalOperation.super.check();
        Preconditions.checkState( getOperands().get(0) instanceof Property ||
                        (getOperands().get(0) instanceof TemporalLiteral &&
                                Objects.equals(((TemporalLiteral) getOperands().get(0)).getType(), ImmutableCqlInterval.class)),
                "The first argument of T_STARTEDBY must be a property or a time interval, found %s",
                getOperands().get(0) instanceof Property
                        ? ((Property) getOperands().get(0)).getName()
                        : ((TemporalLiteral) getOperands().get(0)).getValue());
        Preconditions.checkState( getOperands().get(0) instanceof Property ||
                        (getOperands().get(1) instanceof TemporalLiteral &&
                                Objects.equals(((TemporalLiteral) getOperands().get(1)).getType(), ImmutableCqlInterval.class)),
                "The second argument of T_STARTEDBY must be a property or a time interval, found %s",
                getOperands().get(1) instanceof Property
                        ? ((Property) getOperands().get(1)).getName()
                        : ((TemporalLiteral) getOperands().get(1)).getValue());
    }

}
