/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.app;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.cql.domain.*;
import org.threeten.extra.Interval;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

public class CqlVisitorMapTemporalOperators extends CqlVisitorCopy {

    private final Set<TemporalOperator> supportedOperators;

    public CqlVisitorMapTemporalOperators(Set<TemporalOperator> supportedOperators) {
        this.supportedOperators = supportedOperators;
    }

    @Override
    public CqlNode visit(TemporalOperation temporalOperation, List<CqlNode> children) {

        Temporal temporal1 = (Temporal) children.get(0);
        Temporal temporal2 = (Temporal) children.get(1);

        TemporalOperator temporalOperator = temporalOperation.getOperator();

        // if the next visitor supports a temporal operator, we keep it
        if (supportedOperators.contains(temporalOperator)) {
            return new ImmutableTemporalOperation.Builder()
                .operator(temporalOperator)
                .operands(ImmutableList.of(temporal1,temporal2))
                .build();
        }

        // otherwise we simplify the predicate to scalar comparisons
        switch (temporalOperator) {
            case T_AFTER:
                // start1 > end2
                return Gt.of(ImmutableList.of(getStart(temporal1), getEnd(temporal2)));
            case T_BEFORE:
                // end1 < start2
                return Lt.of(ImmutableList.of(getEnd(temporal1), getStart(temporal2)));
            case T_DURING:
                // start1 > start2 AND end1 < end2
                return And.of(
                    CqlPredicate.of(Gt.of(ImmutableList.of(getStart(temporal1), getStart(temporal2)))),
                    CqlPredicate.of(Lt.of(ImmutableList.of(getEnd(temporal1), getEnd(temporal2)))));
            case T_EQUALS:
                // start1 = start2 AND end1 = end2
                return And.of(
                    CqlPredicate.of(Eq.of(ImmutableList.of(getStart(temporal1), getStart(temporal2)))),
                    CqlPredicate.of(Eq.of(ImmutableList.of(getEnd(temporal1), getEnd(temporal2)))));
            case T_STARTS:
                // start1 = start2 AND end1 < end2
                return And.of(
                    CqlPredicate.of(Eq.of(ImmutableList.of(getStart(temporal1), getStart(temporal2)))),
                    CqlPredicate.of(Lt.of(ImmutableList.of(getEnd(temporal1), getEnd(temporal2)))));
            case T_STARTEDBY:
                // start1 = start2 AND end1 > end2
                return And.of(
                    CqlPredicate.of(Eq.of(ImmutableList.of(getStart(temporal1), getStart(temporal2)))),
                    CqlPredicate.of(Gt.of(ImmutableList.of(getEnd(temporal1), getEnd(temporal2)))));
            case T_CONTAINS:
                // start1 < start2 AND end1 > end2
                return And.of(
                    CqlPredicate.of(Lt.of(ImmutableList.of(getStart(temporal1), getStart(temporal2)))),
                    CqlPredicate.of(Gt.of(ImmutableList.of(getEnd(temporal1), getEnd(temporal2)))));
            case T_DISJOINT:
                // end1 < start2 OR start1 > end2
                return Or.of(
                    CqlPredicate.of(Lt.of(ImmutableList.of(getEnd(temporal1), getStart(temporal2)))),
                    CqlPredicate.of(Gt.of(ImmutableList.of(getStart(temporal1), getEnd(temporal2)))));
            case T_INTERSECTS:
                // NOT (end1 < start2 OR start1 > end2)
                return Not.of(
                    CqlPredicate.of(Or.of(
                        CqlPredicate.of(Lt.of(ImmutableList.of(getEnd(temporal1), getStart(temporal2)))),
                        CqlPredicate.of(Gt.of(ImmutableList.of(getStart(temporal1), getEnd(temporal2)))))));
            case T_FINISHES:
                // start1 > start2 AND end1 = end2
                return And.of(
                    CqlPredicate.of(Gt.of(ImmutableList.of(getStart(temporal1), getStart(temporal2)))),
                    CqlPredicate.of(Eq.of(ImmutableList.of(getEnd(temporal1), getEnd(temporal2)))));
            case T_FINISHEDBY:
                // start1 < start2 AND end1 = end2
                return And.of(
                    CqlPredicate.of(Lt.of(ImmutableList.of(getStart(temporal1), getStart(temporal2)))),
                    CqlPredicate.of(Eq.of(ImmutableList.of(getEnd(temporal1), getEnd(temporal2)))));
            case T_MEETS:
                // end1 = start2
                return new ImmutableEq.Builder()
                    .addOperands(getEnd(temporal1), getStart(temporal2))
                    .build();
            case T_METBY:
                // start1 = end2
                return new ImmutableEq.Builder()
                    .addOperands(getStart(temporal1), getEnd(temporal2))
                    .build();
            case T_OVERLAPS:
                // start1 < start2 AND end1 > start2 AND end1 < end2
                return And.of(
                    CqlPredicate.of(Lt.of(ImmutableList.of(getStart(temporal1), getStart(temporal2)))),
                    CqlPredicate.of(Gt.of(ImmutableList.of(getEnd(temporal1), getStart(temporal2)))),
                    CqlPredicate.of(Lt.of(ImmutableList.of(getEnd(temporal1), getEnd(temporal2)))));
            case T_OVERLAPPEDBY:
                // start1 > start2 AND start1 < end2 AND end1 > end2
                return And.of(
                    CqlPredicate.of(Gt.of(ImmutableList.of(getStart(temporal1), getStart(temporal2)))),
                    CqlPredicate.of(Lt.of(ImmutableList.of(getStart(temporal1), getEnd(temporal2)))),
                    CqlPredicate.of(Gt.of(ImmutableList.of(getEnd(temporal1), getEnd(temporal2)))));
        }

        throw new IllegalStateException("unknown temporal operator: " + temporalOperator);
    }

    private Temporal getStart(Temporal temporal) {
        if (temporal instanceof Property) {
            return temporal;
        } else if (temporal instanceof TemporalLiteral) {
            if (((TemporalLiteral) temporal).getType() == Interval.class) {
                return TemporalLiteral.of(((Interval) ((TemporalLiteral) temporal).getValue()).getStart());
            } else if (((TemporalLiteral) temporal).getType() == TemporalLiteral.OPEN.class) {
                return TemporalLiteral.of(Instant.MIN);
            } else if (((TemporalLiteral) temporal).getType() == Function.class) {
                Function function = (Function) ((TemporalLiteral) temporal).getValue();
                return getStart((Temporal) function.getArguments().get(0));
            }
            return temporal;
        } else if (temporal instanceof Function) {
            Temporal start = (Temporal) ((Function) temporal).getArguments().get(0);
            if (start instanceof TemporalLiteral && ((TemporalLiteral) start).getType() == TemporalLiteral.OPEN.class) {
                return TemporalLiteral.of(Instant.MIN);
            }
            return start;
        }

        throw new IllegalStateException("unknown temporal type: " + temporal.getClass().getSimpleName());
    }

    private Temporal getEnd(Temporal temporal) {
        if (temporal instanceof Property) {
            return temporal;
        } else if (temporal instanceof TemporalLiteral) {
            if (((TemporalLiteral) temporal).getType() == Interval.class) {
                Instant end = ((Interval) ((TemporalLiteral) temporal).getValue()).getEnd();
                if (end==Instant.MAX)
                    return TemporalLiteral.of(Instant.MAX);
                return TemporalLiteral.of(end.minusSeconds(1));
            } else if (((TemporalLiteral) temporal).getType() == TemporalLiteral.OPEN.class) {
                return TemporalLiteral.of(Instant.MAX);
            } else if (((TemporalLiteral) temporal).getType() == Function.class) {
                Function function = (Function) ((TemporalLiteral) temporal).getValue();
                return getEnd((Temporal) function.getArguments().get(1));
            } else if (((TemporalLiteral) temporal).getType() == LocalDate.class) {
                // TODO convert to last second of the day, since we currently do not know, if we are
                //      comparing against a timestamp or a date. In case of a comparison between
                //      a timestamp and a date the date is cast to midnight and the following
                //      will be true, which is not intuitive: TIMESTAMP('1969-07-16T05:32:00Z') > DATE('1969-07-16')
                //      If we would know the data of the other operand this could be improved.
                return TemporalLiteral.of(((LocalDate)((TemporalLiteral) temporal).getValue())
                                              .atTime(23,59,59)
                                              .atZone(ZoneOffset.UTC)
                                              .toInstant());
            }
            return temporal;
        } else if (temporal instanceof Function) {
            Temporal end = (Temporal) ((Function) temporal).getArguments().get(1);
            if (end instanceof TemporalLiteral && ((TemporalLiteral) end).getType() == TemporalLiteral.OPEN.class) {
                return TemporalLiteral.of(Instant.MAX);
            }
            return end;
        }

        throw new IllegalStateException("unknown temporal type: " + temporal.getClass().getSimpleName());
    }
}
