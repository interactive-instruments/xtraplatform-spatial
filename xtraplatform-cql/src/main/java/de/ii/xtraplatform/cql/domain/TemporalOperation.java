/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.immutables.value.Value;
import org.threeten.extra.Interval;

@Value.Immutable
@JsonDeserialize(as = TemporalOperation.class)
public interface TemporalOperation extends BinaryOperation<TemporalLiteral>, CqlNode {

    @JsonValue
    TemporalOperator getOperator();

    static TemporalOperation of(TemporalOperator operator, List<Operand> operands) {
        return new ImmutableTemporalOperation.Builder()
            .operator(operator)
            .operands(operands)
            .build();
    }

    static TemporalOperation of(TemporalOperator operator, Temporal temporal1, Temporal temporal2) {
        return new ImmutableTemporalOperation.Builder()
            .operator(operator)
            .operands(ImmutableList.of(temporal1,temporal2))
            .build();
    }

    static TemporalOperation of(TemporalOperator operator, String property, TemporalLiteral temporalLiteral) {
        return new ImmutableTemporalOperation.Builder()
            .operator(operator)
            .operands(ImmutableList.of(Property.of(property),temporalLiteral))
            .build();
    }

    static TemporalOperation of(TemporalOperator operator, String property, String property2) {
        return new ImmutableTemporalOperation.Builder()
            .operator(operator)
            .operands(ImmutableList.of(Property.of(property), Property.of(property2)))
            .build();
    }

    List<TemporalOperator> INTERVAL_ONLY = ImmutableList.of(
        TemporalOperator.T_CONTAINS,
        TemporalOperator.T_DURING,
        TemporalOperator.T_FINISHEDBY,
        TemporalOperator.T_FINISHES,
        TemporalOperator.T_MEETS,
        TemporalOperator.T_METBY,
        TemporalOperator.T_OVERLAPPEDBY,
        TemporalOperator.T_OVERLAPS,
        TemporalOperator.T_STARTEDBY,
        TemporalOperator.T_STARTS
    );

    @Value.Check
    @Override
    default void check() {
        BinaryOperation.super.check();
        getOperands().forEach(operand -> {
            Preconditions.checkState(operand instanceof Temporal, "a temporal operation must have temporal operands, found %s", operand.getClass().getSimpleName());
            if (operand instanceof Function)
                Preconditions.checkState(((Function) operand).isInterval(), "The arguments of %s must be an INTERVAL(). Found: a function '%s' of type '%s'.", getOperator().toString(), ((Function) operand).getName(), ((Function) operand).getType());
            if (INTERVAL_ONLY.contains(getOperator())) {
                if (operand instanceof Property)
                    Preconditions.checkState(true, "The arguments of %s must be an INTERVAL(). Found: a property ('%s').", getOperator().toString(), ((Property) operand).getName());
                if (operand instanceof TemporalLiteral)
                    Preconditions.checkState( ((TemporalLiteral) operand).getType() == Interval.class ||
                                                  (((TemporalLiteral) operand).getType() == Function.class && ((Function)((TemporalLiteral) operand).getValue()).isInterval()), "The arguments of %s must be an INTERVAL(). Found: a temporal literal '%s' of type '%s'.", getOperator().toString(), ((TemporalLiteral) operand).getValue(), ((TemporalLiteral) operand).getType());
            }
        });
    }

    abstract class Builder extends BinaryOperation.Builder<TemporalLiteral, TemporalOperation> {
    }
}
