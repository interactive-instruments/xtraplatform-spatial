/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.infra;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.cql.app.CqlVisitorPropertyPrefix;
import de.ii.xtraplatform.cql.domain.*;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.antlr.v4.runtime.tree.ParseTree;
import org.threeten.extra.Interval;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CqlTextVisitor extends CqlParserBaseVisitor<CqlNode> implements CqlParserVisitor<CqlNode> {

    private final EpsgCrs defaultCrs;

    public CqlTextVisitor(EpsgCrs defaultCrs) {
        this.defaultCrs = defaultCrs;
    }

    @Override
    public CqlNode visitCqlFilter(CqlParser.CqlFilterContext ctx) {
        CqlNode booleanValueExpression = ctx.booleanValueExpression()
                                            .accept(this);

        return CqlFilter.of(booleanValueExpression);
    }

    @Override
    public CqlNode visitNestedCqlFilter(CqlParser.NestedCqlFilterContext ctx) {
        return CqlFilter.of(ctx.booleanValueExpression().accept(this));
    }

    @Override
    public CqlNode visitBooleanValueExpression(CqlParser.BooleanValueExpressionContext ctx) {
        CqlNode booleanTerm = ctx.booleanTerm()
                                 .accept(this);

        if (Objects.nonNull(ctx.OR())) {
            CqlPredicate predicate1 = CqlPredicate.of(ctx.booleanValueExpression()
                                                         .accept(this));
            CqlPredicate predicate2 = CqlPredicate.of(booleanTerm);

            Or result;
            if (predicate1.getOr()
                          .isPresent()) {
                List<CqlPredicate> predicates = predicate1.getOr()
                                                          .get()
                                                          .getPredicates();
                result = Or.of(new ImmutableList.Builder<CqlPredicate>()
                        .addAll(predicates)
                        .add(predicate2)
                        .build());
            } else {
                result = Or.of(ImmutableList.of(predicate1, predicate2));
            }
            return result;
        }

        return booleanTerm;
    }

    @Override
    public CqlNode visitBooleanTerm(CqlParser.BooleanTermContext ctx) {
        CqlNode booleanFactor = ctx.booleanFactor()
                                   .accept(this);

        // create And
        if (Objects.nonNull(ctx.AND())) {
            CqlPredicate predicate1 = CqlPredicate.of(ctx.booleanTerm()
                                                         .accept(this));
            CqlPredicate predicate2 = CqlPredicate.of(booleanFactor);

            And result;
            if (predicate1.getAnd()
                          .isPresent()) {
                List<CqlPredicate> predicates = predicate1.getAnd()
                                                          .get()
                                                          .getPredicates();
                result = And.of(new ImmutableList.Builder<CqlPredicate>()
                        .addAll(predicates)
                        .add(predicate2)
                        .build());
            } else {
                result = And.of(ImmutableList.of(predicate1, predicate2));
            }

            return result;
        }

        return booleanFactor;
    }

    @Override
    public CqlNode visitBooleanFactor(CqlParser.BooleanFactorContext ctx) {
        CqlNode booleanPrimary = ctx.booleanPrimary()
                                    .accept(this);
        if (Objects.nonNull(ctx.booleanPrimary()
                               .LEFTPAREN())) {
            booleanPrimary = ctx.booleanPrimary()
                                .booleanValueExpression()
                                .accept(this);
        }
        if (Objects.nonNull(ctx.NOT())) {
            return Not.of(CqlPredicate.of(booleanPrimary));
        }

        return booleanPrimary;
    }

    @Override
    public CqlNode visitBinaryComparisonPredicate(CqlParser.BinaryComparisonPredicateContext ctx) {

        ComparisonOperator comparisonOperator = ComparisonOperator.valueOfCqlText(ctx.ComparisonOperator()
                .getText());

        Scalar scalar1 = (Scalar) ctx.scalarExpression(0)
                                     .accept(this);
        Scalar scalar2 = (Scalar) ctx.scalarExpression(1)
                                     .accept(this);

        BinaryScalarOperation.Builder<? extends BinaryScalarOperation> builder;

        switch (comparisonOperator) {
            case EQ:
                builder = new ImmutableEq.Builder();
                break;
            case NEQ:
                builder = new ImmutableNeq.Builder();
                break;
            case GT:
                builder = new ImmutableGt.Builder();
                break;
            case GTEQ:
                builder = new ImmutableGte.Builder();
                break;
            case LT:
                builder = new ImmutableLt.Builder();
                break;
            case LTEQ:
                builder = new ImmutableLte.Builder();
                break;
            default:
                throw new IllegalStateException("unknown comparison operator: " + comparisonOperator);
        }

        return builder.operands(ImmutableList.of(scalar1,scalar2))
                      .build();
    }

    @Override
    public CqlNode visitPropertyIsLikePredicate(CqlParser.PropertyIsLikePredicateContext ctx) {

        if (Objects.nonNull(ctx.LIKE())) {

            Scalar scalar1 = (Scalar) ctx.characterExpression()
                                         .accept(this);
            Scalar scalar2 = (Scalar) ctx.scalarExpression()
                                         .accept(this);

            Like like = new ImmutableLike.Builder()
                    .operands(ImmutableList.of(scalar1,scalar2))
                    .build();

            if (Objects.nonNull(ctx.NOT())) {
                return Not.of(like);
            }

            return like;
        }
        return null;
    }

    @Override
    public CqlNode visitPropertyIsBetweenPredicate(CqlParser.PropertyIsBetweenPredicateContext ctx) {

        if (Objects.nonNull(ctx.BETWEEN())) {

            Scalar scalar = (Scalar) ctx.scalarExpression()
                                         .accept(this);

            Scalar numeric1 = (Scalar) ctx.numericExpression(0)
                                         .accept(this);
            Scalar numeric2 = (Scalar) ctx.numericExpression(1)
                                         .accept(this);

            Between between = new ImmutableBetween.Builder()
                    .value(scalar)
                    .lower(numeric1)
                    .upper(numeric2)
                    .build();

            if (Objects.nonNull(ctx.NOT())) {
                return Not.of(between);
            }
            return between;
        }
        return null;
    }

    @Override
    public CqlNode visitPropertyIsNullPredicate(CqlParser.PropertyIsNullPredicateContext ctx) {

        if (Objects.nonNull(ctx.IS())) {
            Scalar scalar1 = (Scalar) ctx.scalarExpression()
                                         .accept(this);

            IsNull isNull = new ImmutableIsNull.Builder()
                    .operand(scalar1)
                    .build();
            if (Objects.nonNull(ctx.NOT())) {
                return Not.of(CqlPredicate.of(isNull));
            }

            return isNull;

        }
        return null;

    }

    @Override
    public CqlNode visitTemporalPredicate(CqlParser.TemporalPredicateContext ctx) {

        Temporal temporal1 = (Temporal) ctx.temporalExpression(0)
            .accept(this);
        Temporal temporal2 = (Temporal) ctx.temporalExpression(1)
            .accept(this);

        assert !(temporal1 instanceof Function) || ((Function) temporal1).isInterval();
        assert !(temporal2 instanceof Function) || ((Function) temporal2).isInterval();

        if (Objects.nonNull(ctx.TemporalOperator())) {
            TemporalOperator temporalOperator = TemporalOperator.valueOf(ctx.TemporalOperator()
                                                                             .getText()
                                                                             .toUpperCase());

            switch (temporalOperator) {
                case T_AFTER:
                    // start1 > end2
                    return new ImmutableGt.Builder()
                        .addOperands(getStart(temporal1), getEnd(temporal2))
                        .build();
                case T_BEFORE:
                    // end1 < start2
                    return new ImmutableLt.Builder()
                        .addOperands(getEnd(temporal1), getStart(temporal2))
                        .build();
                case T_DURING:
                    // start1 > start2 AND end1 < end2
                    return new ImmutableAnd.Builder()
                        .addPredicates(
                            CqlPredicate.of(new ImmutableGt.Builder().addOperands(getStart(temporal1), getStart(temporal2)).build()),
                            CqlPredicate.of(new ImmutableLt.Builder().addOperands(getEnd(temporal1), getEnd(temporal2)).build()))
                        .build();
                case T_EQUALS:
                    // start1 = start2 AND end1 = end2
                    return new ImmutableAnd.Builder()
                        .addPredicates(
                            CqlPredicate.of(new ImmutableEq.Builder().addOperands(getStart(temporal1), getStart(temporal2)).build()),
                            CqlPredicate.of(new ImmutableEq.Builder().addOperands(getEnd(temporal1), getEnd(temporal2)).build()))
                        .build();
                case T_STARTS:
                    // start1 = start2 AND end1 < end2
                    return new ImmutableAnd.Builder()
                        .addPredicates(
                            CqlPredicate.of(new ImmutableEq.Builder().addOperands(getStart(temporal1), getStart(temporal2)).build()),
                            CqlPredicate.of(new ImmutableLt.Builder().addOperands(getEnd(temporal1), getEnd(temporal2)).build()))
                        .build();
                case T_STARTEDBY:
                    // start1 = start2 AND end1 > end2
                    return new ImmutableAnd.Builder()
                        .addPredicates(
                            CqlPredicate.of(new ImmutableEq.Builder().addOperands(getStart(temporal1), getStart(temporal2)).build()),
                            CqlPredicate.of(new ImmutableGt.Builder().addOperands(getEnd(temporal1), getEnd(temporal2)).build()))
                        .build();
                case T_CONTAINS:
                    // start1 < start2 AND end1 > end2
                    return new ImmutableAnd.Builder()
                        .addPredicates(
                            CqlPredicate.of(new ImmutableLt.Builder().addOperands(getStart(temporal1), getStart(temporal2)).build()),
                            CqlPredicate.of(new ImmutableGt.Builder().addOperands(getEnd(temporal1), getEnd(temporal2)).build()))
                        .build();
                case T_DISJOINT:
                    return new ImmutableNot.Builder()
                        .predicate(
                            CqlPredicate.of(new ImmutableTIntersects.Builder()
                                            .operands(ImmutableList.of(temporal1,temporal2))
                                            .build()))
                        .build();
                case T_INTERSECTS:
                    return new ImmutableTIntersects.Builder()
                        .operands(ImmutableList.of(temporal1,temporal2))
                        .build();
                case T_FINISHES:
                    // start1 > start2 AND end1 = end2
                    return new ImmutableAnd.Builder()
                        .addPredicates(
                            CqlPredicate.of(new ImmutableGt.Builder().addOperands(getStart(temporal1), getStart(temporal2)).build()),
                            CqlPredicate.of(new ImmutableEq.Builder().addOperands(getEnd(temporal1), getEnd(temporal2)).build()))
                        .build();
                case T_FINISHEDBY:
                    // start1 < start2 AND end1 = end2
                    return new ImmutableAnd.Builder()
                        .addPredicates(
                            CqlPredicate.of(new ImmutableLt.Builder().addOperands(getStart(temporal1), getStart(temporal2)).build()),
                            CqlPredicate.of(new ImmutableEq.Builder().addOperands(getEnd(temporal1), getEnd(temporal2)).build()))
                        .build();
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
                    return new ImmutableAnd.Builder()
                        .addPredicates(
                            CqlPredicate.of(new ImmutableLt.Builder().addOperands(getStart(temporal1), getStart(temporal2)).build()),
                            CqlPredicate.of(new ImmutableGt.Builder().addOperands(getEnd(temporal1), getStart(temporal2)).build()),
                            CqlPredicate.of(new ImmutableLt.Builder().addOperands(getEnd(temporal1), getEnd(temporal2)).build()))
                        .build();
                case T_OVERLAPPEDBY:
                    // start1 > start2 AND start1 < end2 AND end1 > end2
                    return new ImmutableAnd.Builder()
                        .addPredicates(
                            CqlPredicate.of(new ImmutableGt.Builder().addOperands(getStart(temporal1), getStart(temporal2)).build()),
                            CqlPredicate.of(new ImmutableLt.Builder().addOperands(getStart(temporal1), getEnd(temporal2)).build()),
                            CqlPredicate.of(new ImmutableGt.Builder().addOperands(getEnd(temporal1), getEnd(temporal2)).build()))
                        .build();
                default:
                    throw new IllegalStateException("unknown temporal operator: " + temporalOperator);
            }
        }
        throw new IllegalStateException("unknown temporal predicate: " + ctx.getText());
    }

    private Temporal getStart(Temporal temporal) {
        if (temporal instanceof Property) {
            return temporal;
        } else if (temporal instanceof TemporalLiteral) {
            if (((TemporalLiteral) temporal).getType() == Interval.class) {
                return TemporalLiteral.of(((Interval) ((TemporalLiteral) temporal).getValue()).getStart());
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

    @Override
    public CqlNode visitSpatialPredicate(CqlParser.SpatialPredicateContext ctx) {

        Spatial spatial1 = (Spatial) ctx.geomExpression()
                                        .get(0)
                                        .accept(this);
        Spatial spatial2 = (Spatial) ctx.geomExpression()
                                        .get(1)
                                        .accept(this);
        SpatialOperator spatialOperator = SpatialOperator.valueOf(ctx.SpatialOperator()
                                                                     .getText()
                                                                     .toUpperCase());

        SpatialOperation.Builder<? extends SpatialOperation> builder;

        switch (spatialOperator) {
            case S_EQUALS:
                builder = new ImmutableSEquals.Builder();
                break;
            case S_DISJOINT:
                builder = new ImmutableSDisjoint.Builder();
                break;
            case S_TOUCHES:
                builder = new ImmutableSTouches.Builder();
                break;
            case S_WITHIN:
                builder = new ImmutableSWithin.Builder();
                break;
            case S_OVERLAPS:
                builder = new ImmutableSOverlaps.Builder();
                break;
            case S_CROSSES:
                builder = new ImmutableSCrosses.Builder();
                break;
            case S_INTERSECTS:
                builder = new ImmutableSIntersects.Builder();
                break;
            case S_CONTAINS:
                builder = new ImmutableSContains.Builder();
                break;
            default:
                throw new IllegalStateException("unknown spatial operator: " + spatialOperator);
        }

        return builder.operands(ImmutableList.of(spatial1,spatial2))
                      .build();
    }

    @Override
    public CqlNode visitArrayPredicate(CqlParser.ArrayPredicateContext ctx) {

        Vector vector1 = (Vector) ctx.arrayExpression().get(0).accept(this);
        Vector vector2 = (Vector) ctx.arrayExpression().get(1).accept(this);

        ArrayOperator arrayOperator = ArrayOperator.valueOf(ctx.ArrayOperator()
                .getText()
                .toUpperCase());

        ArrayOperation.Builder<? extends ArrayOperation> builder;

        switch (arrayOperator) {
            case A_CONTAINS:
                builder = new ImmutableAContains.Builder();
                break;
            case A_EQUALS:
                builder = new ImmutableAEquals.Builder();
                break;
            case A_OVERLAPS:
                builder = new ImmutableAOverlaps.Builder();
                break;
            case A_CONTAINEDBY:
                builder = new ImmutableAContainedBy.Builder();
                break;
            default:
                throw new IllegalStateException("unknown array operator: " + arrayOperator);
        }

        return builder.operands(ImmutableList.of(vector1, vector2))
                      .build();
    }

    @Override
    public CqlNode visitArrayClause(CqlParser.ArrayClauseContext ctx) {
        try {
            List<Scalar> values = ctx.arrayElement()
                    .stream()
                    .map(e -> (ScalarLiteral) e.accept(this))
                    .collect(Collectors.toList());
            return ArrayLiteral.of(values);
        } catch (CqlParseException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @Override
    public CqlNode visitInPredicate(CqlParser.InPredicateContext ctx) {
        In in;
        List<Scalar> values = ImmutableList.of(ctx.scalarExpression().subList(1, ctx.scalarExpression().size()))
                                                  .stream()
                                                  .flatMap(Collection::stream)
                                                  .map(v -> (Scalar) v.accept(this))
                                                  .collect(Collectors.toList());

        // TODO IN currently requires a property on the left side and literals on the right side
        in = new ImmutableIn.Builder()
                .value((Scalar) ctx.scalarExpression(0).accept(this))
                .list(values)
                .build();
        if (Objects.nonNull(ctx.NOT())) {
            return Not.of(in);
        }
        return in;
    }

    @Override
    public CqlNode visitPropertyName(CqlParser.PropertyNameContext ctx) {
        if (!ctx.nestedCqlFilter().isEmpty()) {
            Map<String, CqlFilter> nestedFilters = new HashMap<>();
            for (int i = 0; i < ctx.nestedCqlFilter().size(); i++) {
                CqlFilter nestedFilter = CqlFilter.of(ctx.nestedCqlFilter(i)
                                               .accept(this));
                CqlVisitorPropertyPrefix prefix = new CqlVisitorPropertyPrefix(ctx.Identifier(i)
                                                                                                    .getText());
                nestedFilter = (CqlFilter) nestedFilter.accept(prefix);
                nestedFilters.put(ctx.Identifier(i).getText(), nestedFilter);
            }
            String path = ctx.Identifier().stream()
                    .map(ParseTree::getText)
                    .collect(Collectors.joining("."));
            return Property.of(path, nestedFilters);
        }
        return Property.of(ctx.getText());
    }

    @Override
    public CqlNode visitNumericLiteral(CqlParser.NumericLiteralContext ctx) {
        return ScalarLiteral.of(ctx.getText(), true);
    }

    @Override
    public CqlNode visitBooleanLiteral(CqlParser.BooleanLiteralContext ctx) {
        return ScalarLiteral.of(ctx.getText(), true);
    }

    @Override
    public CqlNode visitTemporalClause(CqlParser.TemporalClauseContext ctx) {
        try {
            if (Objects.nonNull(ctx.interval())) {
                return ctx.interval().accept(this);
            }
        } catch (CqlParseException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        return ctx.instantLiteral().accept(this);
    }

    @Override
    public CqlNode visitInterval(CqlParser.IntervalContext ctx) {
        CqlNode arg1 = ctx.intervalParameter(0)
            .accept(this);
        CqlNode arg2 = ctx.intervalParameter(1)
            .accept(this);

        // if at least one parameter is a property, we create a function, otherwise a fixed interval
        if (arg1 instanceof Property && arg2 instanceof Property) {
            return Function.of("interval", ImmutableList.of((Property) arg1, (Property) arg2));
        } else if (arg1 instanceof Property &&  arg2 instanceof TemporalLiteral) {
            return Function.of("interval", ImmutableList.of((Property) arg1, (TemporalLiteral) arg2));
        } else if (arg1 instanceof TemporalLiteral &&  arg2 instanceof Property) {
            return Function.of("interval", ImmutableList.of((TemporalLiteral) arg1, (Property) arg2));
        } else if (arg1 instanceof TemporalLiteral &&  arg2 instanceof TemporalLiteral) {
            return TemporalLiteral.of((TemporalLiteral) arg1, (TemporalLiteral) arg2);
        }

        throw new IllegalStateException("unsupported interval value: " + ctx.getText());
    }

    @Override
    public CqlNode visitIntervalParameter(CqlParser.IntervalParameterContext ctx) {
        if (Objects.nonNull(ctx.NOW())) {
            return TemporalLiteral.of(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        } else if (Objects.nonNull(ctx.DotDotString())) {
            return TemporalLiteral.of("..");
        } else if (Objects.nonNull(ctx.propertyName())) {
            return ctx.propertyName().accept(this);
        } else if (Objects.nonNull(ctx.DateString())) {
            String s = ctx.DateString().getText();
            return TemporalLiteral.of(s.substring(1, s.length()-1));
        } else if (Objects.nonNull(ctx.TimestampString())) {
            String s = ctx.TimestampString().getText();
            return TemporalLiteral.of(s.substring(1, s.length()-1));
        }
        throw new IllegalStateException("unsupported interval parameter: " + ctx.getText());
    }

    @Override
    public CqlNode visitInstantLiteral(CqlParser.InstantLiteralContext ctx) {
        if (Objects.nonNull(ctx.NOW())) {
            return TemporalLiteral.of(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        }

        String s = Objects.nonNull(ctx.DATE()) ? ctx.DateString().getText() : ctx.TimestampString().getText();
        return TemporalLiteral.of(s.substring(1, s.length()-1));
    }

    @Override
    public CqlNode visitCharacterClause(CqlParser.CharacterClauseContext ctx) {
        if (Objects.nonNull(ctx.CASEI())) {
            Scalar scalar = (Scalar) ctx.characterExpression()
                .accept(this);

            return Function.of("CASEI", ImmutableList.of(scalar));
        } else if (Objects.nonNull(ctx.ACCENTI())) {
            Scalar scalar = (Scalar) ctx.characterExpression()
                .accept(this);

            return Function.of("ACCENTI", ImmutableList.of(scalar));
        } else if (Objects.nonNull(ctx.LOWER())) {
            Scalar scalar = (Scalar) ctx.characterExpression()
                .accept(this);

            return Function.of("LOWER", ImmutableList.of(scalar));
        } else if (Objects.nonNull(ctx.UPPER())) {
            Scalar scalar = (Scalar) ctx.characterExpression()
                .accept(this);

            return Function.of("UPPER", ImmutableList.of(scalar));
        }

        return ctx.characterLiteral().accept(this);
    }

    @Override
    public CqlNode visitCharacterLiteral(CqlParser.CharacterLiteralContext ctx) {
        return getScalarLiteralFromText(ctx.getText());
    }

    @Override
    public CqlNode visitGeomLiteral(CqlParser.GeomLiteralContext ctx) {
        CqlNode geomLiteral = ctx.getChild(0)
                                 .accept(this);
        return SpatialLiteral.of((Geometry<?>) geomLiteral);
    }

    @Override
    public CqlNode visitPoint(CqlParser.PointContext ctx) {
        Geometry.Coordinate coordinate = (Geometry.Coordinate) ctx.coordinate()
                                                                  .accept(this);

        return new ImmutablePoint.Builder().addCoordinates(coordinate)
                                           .crs(defaultCrs)
                                           .build();
    }

    @Override
    public CqlNode visitMultiPoint(CqlParser.MultiPointContext ctx) {
        List<Geometry.Point> points = ctx.coordinate()
                                                   .stream()
                                                   .map(coordinateContext -> Geometry.Point.of((Geometry.Coordinate) coordinateContext.accept(this)))
                                                   .collect(Collectors.toList());

        return new ImmutableMultiPoint.Builder().coordinates(points)
                                                .crs(defaultCrs)
                                                     .build();
    }

    @Override
    public CqlNode visitMultiLinestring(CqlParser.MultiLinestringContext ctx) {
        List<Geometry.LineString> lineStrings = ctx.linestringDef()
                                                         .stream()
                                                         .map(linestringContext -> ((Geometry.LineString) linestringContext.accept(this)))
                                                         .collect(Collectors.toList());

        return new ImmutableMultiLineString.Builder().coordinates(lineStrings)
                                                     .crs(defaultCrs)
                                             .build();
    }

    @Override
    public CqlNode visitMultiPolygon(CqlParser.MultiPolygonContext ctx) {
        List<Geometry.Polygon> polygons = ctx.polygonDef()
                                                   .stream()
                                                   .map(polygonDefContext -> ((Geometry.Polygon) polygonDefContext.accept(this)))
                                                   .collect(Collectors.toList());

        return new ImmutableMultiPolygon.Builder().coordinates(polygons)
                                                  .crs(defaultCrs)
                                                     .build();
    }

    @Override
    public CqlNode visitPolygonDef(CqlParser.PolygonDefContext ctx) {
        List<List<Geometry.Coordinate>> coordinates = ctx.linestringDef()
                                                         .stream()
                                                         .map(linestringContext -> ((Geometry.LineString) linestringContext.accept(this)).getCoordinates())
                                                         .collect(Collectors.toList());

        return new ImmutablePolygon.Builder().coordinates(coordinates)
                                             .crs(defaultCrs)
                                             .build();
    }

    @Override
    public CqlNode visitEnvelope(CqlParser.EnvelopeContext ctx) {
        List<Double> coordinates;
        Double eastBoundLon = Double.valueOf(ctx.eastBoundLon()
                                                .NumericLiteral()
                                                .getText());
        Double westBoundLon = Double.valueOf(ctx.westBoundLon()
                                                .NumericLiteral()
                                                .getText());
        Double northBoundLat = Double.valueOf(ctx.northBoundLat()
                                                 .NumericLiteral()
                                                 .getText());
        Double southBoundLat = Double.valueOf(ctx.southBoundLat()
                                                 .NumericLiteral()
                                                 .getText());

        if (Objects.nonNull(ctx.minElev()) && Objects.nonNull(ctx.maxElev())) {
            Double minElev = Double.valueOf(ctx.minElev()
                                               .NumericLiteral()
                                               .getText());
            Double maxElev = Double.valueOf(ctx.maxElev()
                                               .NumericLiteral()
                                               .getText());
            coordinates = ImmutableList.of(southBoundLat, westBoundLon, minElev, northBoundLat, eastBoundLon, maxElev);
        } else {
            coordinates = ImmutableList.of(westBoundLon, southBoundLat, eastBoundLon, northBoundLat);
        }

        return new ImmutableEnvelope.Builder()
                .coordinates(coordinates)
                .crs(defaultCrs)
                .build();
    }

    @Override
    public CqlNode visitLinestringDef(CqlParser.LinestringDefContext ctx) {
        List<Geometry.Coordinate> coordinates = ctx.coordinate()
                                                   .stream()
                                                   .map(pointContext -> (Geometry.Coordinate) pointContext.accept(this))
                                                   .collect(Collectors.toList());

        return new ImmutableLineString.Builder().coordinates(coordinates)
                                                .crs(defaultCrs)
                                                .build();
    }

    @Override
    public CqlNode visitCoordinate(CqlParser.CoordinateContext ctx) {
        Double x = Double.valueOf(ctx.xCoord()
                                     .getText());
        Double y = Double.valueOf(ctx.yCoord()
                                     .getText());

        if (Objects.nonNull(ctx.zCoord())) {
            Double z = Double.valueOf(ctx.zCoord()
                                         .getText());
            return new Geometry.Coordinate(x, y, z);
        }

        return new Geometry.Coordinate(x, y);
    }

    @Override
    public CqlNode visitFunction(CqlParser.FunctionContext ctx) {
        String functionName = ctx.Identifier().getText();
        if (Objects.isNull(ctx.argumentList().positionalArgument())) {
            return Function.of(functionName, ImmutableList.of());
        }

        List<Operand> args = ctx.argumentList().positionalArgument()
                .argument()
                .stream()
                .map(arg -> (Operand) arg.accept(this))
                .collect(Collectors.toList());
        return Function.of(functionName, args);
    }

    private ScalarLiteral getScalarLiteralFromText(String cqlText) {
        return ScalarLiteral.of(cqlText.substring(1, cqlText.length() - 1)
                .replaceAll("''", "'"));
    }

}
