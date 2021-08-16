/**
 * Copyright 2021 interactive instruments GmbH
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

        Scalar scalar1 = (Scalar) ctx.scalarExpression(0)
                                     .accept(this);
        Scalar scalar2 = (Scalar) ctx.scalarExpression(1)
                                     .accept(this);
        ComparisonOperator comparisonOperator = ComparisonOperator.valueOfCqlText(ctx.ComparisonOperator()
                                                                                     .getText());

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

            Scalar scalar1 = (Scalar) ctx.scalarExpression().get(0)
                                         .accept(this);
            Scalar scalar2 = (Scalar) ctx.scalarExpression().get(1)
                                         .accept(this);

            Like like = new ImmutableLike.Builder()
                    .operands(ImmutableList.of(scalar1,scalar2))
                    .build();

            if (Objects.nonNull(ctx.NOT())) {
                return Not.of(like);
            }

            List<CqlParser.LikeModifierContext> likeModifiers = ctx.likeModifier();

            for (CqlParser.LikeModifierContext likeModifier : likeModifiers) {
                if (Objects.nonNull(likeModifier.wildcard())) {
                    ScalarLiteral wildcard = (ScalarLiteral) likeModifier.wildcard().accept(this);
                    like = new ImmutableLike.Builder().from(like).wildcard((String) wildcard.getValue()).build();
                }

                if (Objects.nonNull(likeModifier.singlechar())) {
                    ScalarLiteral singlechar = (ScalarLiteral) likeModifier.singlechar().accept(this);
                    like = new ImmutableLike.Builder().from(like).singleChar((String) singlechar.getValue()).build();
                }

                if (Objects.nonNull(likeModifier.escapechar())) {
                    ScalarLiteral escapechar = (ScalarLiteral) likeModifier.escapechar().accept(this);
                    like = new ImmutableLike.Builder().from(like).escapeChar((String) escapechar.getValue()).build();
                }

                if (Objects.nonNull(likeModifier.nocase())) {
                    ScalarLiteral nocase = (ScalarLiteral) likeModifier.nocase().accept(this);
                    like = new ImmutableLike.Builder().from(like).nocase((Boolean) nocase.getValue()).build();
                }
            }

            return like;
        }
        return null;
    }

    @Override
    public CqlNode visitPropertyIsBetweenPredicate(CqlParser.PropertyIsBetweenPredicateContext ctx) {

        if (Objects.nonNull(ctx.BETWEEN())) {

            Scalar scalar1 = (Scalar) ctx.scalarExpression().get(0)
                                         .accept(this);

            if (!ctx.temporalExpression().isEmpty()) {
                TemporalLiteral temporalLiteral = TemporalLiteral.of(String.format("%s/%s", ctx.temporalExpression(0).getText(), ctx.temporalExpression(1).getText()));

                During during = new ImmutableDuring.Builder()
                        .operands(ImmutableList.of(scalar1, temporalLiteral))
                        .build();

                if (Objects.nonNull(ctx.NOT())) {
                    return Not.of(during);
                }
                return during;

            } else {
                Scalar scalar2 = (Scalar) ctx.scalarExpression().get(1)
                        .accept(this);
                Scalar scalar3 = (Scalar) ctx.scalarExpression().get(2)
                        .accept(this);

                Between between = new ImmutableBetween.Builder()
                        .value(scalar1)
                        .lower(scalar2)
                        .upper(scalar3)
                        .build();

                if (Objects.nonNull(ctx.NOT())) {
                    return Not.of(between);
                }
                return between;
            }

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

        TemporalOperation.Builder<? extends TemporalOperation> builder = null;

        if (Objects.nonNull(ctx.ComparisonOperator())) {
            ComparisonOperator comparisonOperator = ComparisonOperator.valueOfCqlText(ctx.ComparisonOperator()
                    .getText());
            switch (comparisonOperator) {
                case EQ:
                    builder = new ImmutableTEquals.Builder();
                    break;
                case NEQ:
                    TEquals tEquals = new ImmutableTEquals.Builder()
                            .operands(ImmutableList.of(temporal1, temporal2))
                            .build();
                    return Not.of(tEquals);
                case GT:
                    builder = new ImmutableAfter.Builder();
                    break;
                case GTEQ:
                    After after = new ImmutableAfter.Builder().operands(ImmutableList.of(temporal1, temporal2)).build();
                    tEquals = new ImmutableTEquals.Builder().operands(ImmutableList.of(temporal1, temporal2)).build();
                    return Or.of(CqlPredicate.of(after), CqlPredicate.of(tEquals));
                case LT:
                    builder = new ImmutableBefore.Builder();
                    break;
                case LTEQ:
                    Before before = new ImmutableBefore.Builder().operands(ImmutableList.of(temporal1, temporal2)).build();
                    tEquals = new ImmutableTEquals.Builder().operands(ImmutableList.of(temporal1, temporal2)).build();
                    return Or.of(CqlPredicate.of(before), CqlPredicate.of(tEquals));
                default:
                    throw new IllegalStateException("unsupported temporal comparison operator: " + comparisonOperator);
            }
        } else if (Objects.nonNull(ctx.TemporalOperator())) {
            TemporalOperator temporalOperator = TemporalOperator.valueOf(ctx.TemporalOperator()
                    .getText()
                    .toUpperCase());

            switch (temporalOperator) {
                case AFTER:
                    builder = new ImmutableAfter.Builder();
                    break;
                case BEFORE:
                    builder = new ImmutableBefore.Builder();
                    break;
                case DURING:
                    builder = new ImmutableDuring.Builder();
                    break;
                case TEQUALS:
                    builder = new ImmutableTEquals.Builder();
                    break;
                case ANYINTERACTS:
                    builder = new ImmutableAnyInteracts.Builder();
                    break;
                case BEGINS:
                case BEGUNBY:
                case TCONTAINS:
                case ENDEDBY:
                case ENDS:
                case MEETS:
                case METBY:
                case TOVERLAPS:
                case OVERLAPPEDBY:
                    throw new IllegalArgumentException(String.format("unsupported temporal operator (%s)", temporalOperator));
                default:
                    throw new IllegalStateException("unknown temporal operator: " + temporalOperator);
            }
        }

        return builder.operands(ImmutableList.of(temporal1,temporal2))
                      .build();
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
            case EQUALS:
                builder = new ImmutableEquals.Builder();
                break;
            case DISJOINT:
                builder = new ImmutableDisjoint.Builder();
                break;
            case TOUCHES:
                builder = new ImmutableTouches.Builder();
                break;
            case WITHIN:
                builder = new ImmutableWithin.Builder();
                break;
            case OVERLAPS:
                builder = new ImmutableOverlaps.Builder();
                break;
            case CROSSES:
                builder = new ImmutableCrosses.Builder();
                break;
            case INTERSECTS:
                builder = new ImmutableIntersects.Builder();
                break;
            case CONTAINS:
                builder = new ImmutableContains.Builder();
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
            case ACONTAINS:
                builder = new ImmutableAContains.Builder();
                break;
            case AEQUALS:
                builder = new ImmutableAEquals.Builder();
                break;
            case AOVERLAPS:
                builder = new ImmutableAOverlaps.Builder();
                break;
            case CONTAINEDBY:
                builder = new ImmutableContainedBy.Builder();
                break;
            default:
                throw new IllegalStateException("unknown array operator: " + arrayOperator);
        }

        return builder.operands(ImmutableList.of(vector1, vector2))
                      .build();
    }

    @Override
    public CqlNode visitArrayLiteral(CqlParser.ArrayLiteralContext ctx) {
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
        List<Scalar> values = ImmutableList.of(ctx.characterLiteral(), ctx.numericLiteral(), ctx.temporalLiteral())
                                                  .stream()
                                                  .flatMap(Collection::stream)
                                                  .map(v -> (Scalar) v.accept(this))
                                                  .collect(Collectors.toList());

        if (Objects.nonNull(ctx.function())) {
            in = new ImmutableIn.Builder()
                    .value((Function) ctx.function().accept(this))
                    .list(values)
                    .build();
        } else if (Objects.isNull(ctx.propertyName())) {
            in = In.of(values);
        } else {
            // TODO IN currently requires a property on the left side and literals on the right side
            in = new ImmutableIn.Builder()
                    .value((Property) ctx.propertyName().accept(this))
                    .list(values)
                    .build();
        }
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
        } else {
            return Property.of(ctx.getText());
        }
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
    public CqlNode visitTemporalLiteral(CqlParser.TemporalLiteralContext ctx) {
        try {
            return TemporalLiteral.of(ctx.getText());
        } catch (CqlParseException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @Override
    public CqlNode visitCharacterLiteral(CqlParser.CharacterLiteralContext ctx) {
        return ScalarLiteral.of(ctx.getText()
                                   .substring(1, ctx.getText()
                                                    .length() - 1)
                                   .replaceAll("''", "'"));
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

    @Override
    public CqlNode visitWildcard(CqlParser.WildcardContext ctx) {
        return ctx.characterLiteral().accept(this);
    }

    @Override
    public CqlNode visitSinglechar(CqlParser.SinglecharContext ctx) {
        return ctx.characterLiteral().accept(this);
    }

    @Override
    public CqlNode visitEscapechar(CqlParser.EscapecharContext ctx) {
        return ctx.characterLiteral().accept(this);
    }

    @Override
    public CqlNode visitNocase(CqlParser.NocaseContext ctx) {
        return ctx.booleanLiteral().accept(this);
    }

}
