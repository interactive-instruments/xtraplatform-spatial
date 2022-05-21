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
        return ctx.booleanExpression().accept(this);
    }

    @Override
    public CqlNode visitNestedCqlFilter(CqlParser.NestedCqlFilterContext ctx) {
        return ctx.booleanExpression().accept(this);
    }

    @Override
    public CqlNode visitBooleanExpression(CqlParser.BooleanExpressionContext ctx) {
        CqlNode booleanTerm = ctx.booleanTerm()
                                 .accept(this);

        if (Objects.nonNull(ctx.OR())) {
            Cql2Predicate predicate1 = (Cql2Predicate) ctx.booleanExpression()
                .accept(this);
            Cql2Predicate predicate2 = (Cql2Predicate) booleanTerm;

            if (predicate1 instanceof Or) {
                List<Cql2Predicate> predicates = ((Or) predicate1).getArgs();

                return Or.of(new ImmutableList.Builder<Cql2Predicate>()
                        .addAll(predicates)
                        .add(predicate2)
                        .build());
            }

            return Or.of(ImmutableList.of(predicate1, predicate2));
        }

        return booleanTerm;
    }

    @Override
    public CqlNode visitBooleanTerm(CqlParser.BooleanTermContext ctx) {
        CqlNode booleanFactor = ctx.booleanFactor()
                                   .accept(this);

        // create And
        if (Objects.nonNull(ctx.AND())) {
            Cql2Predicate predicate1 = (Cql2Predicate) ctx.booleanTerm()
                                                         .accept(this);
            Cql2Predicate predicate2 = (Cql2Predicate) booleanFactor;

            if (predicate1 instanceof And) {
                List<Cql2Predicate> predicates = ((And) predicate1).getArgs();

                return And.of(new ImmutableList.Builder<Cql2Predicate>()
                        .addAll(predicates)
                        .add(predicate2)
                        .build());
            }

            return And.of(ImmutableList.of(predicate1, predicate2));
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
                                .booleanExpression()
                                .accept(this);
        }
        if (Objects.nonNull(ctx.NOT())) {
            return Not.of((Cql2Predicate) booleanPrimary);
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

        return builder.args(ImmutableList.of(scalar1,scalar2))
                      .build();
    }

    @Override
    public CqlNode visitIsLikePredicate(CqlParser.IsLikePredicateContext ctx) {

        if (Objects.nonNull(ctx.LIKE())) {

            Scalar scalar1 = (Scalar) ctx.characterExpression()
                                         .accept(this);
            Scalar scalar2 = (Scalar) ctx.patternExpression()
                                         .accept(this);

            Like like = new ImmutableLike.Builder()
                    .args(ImmutableList.of(scalar1,scalar2))
                    .build();

            if (Objects.nonNull(ctx.NOT())) {
                return Not.of(like);
            }

            return like;
        }
        return null;
    }

    @Override
    public CqlNode visitIsBetweenPredicate(CqlParser.IsBetweenPredicateContext ctx) {

        if (Objects.nonNull(ctx.BETWEEN())) {

            Scalar scalar = (Scalar) ctx.numericExpression(0)
                                         .accept(this);

            Scalar numeric1 = (Scalar) ctx.numericExpression(1)
                                         .accept(this);
            Scalar numeric2 = (Scalar) ctx.numericExpression(2)
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
    public CqlNode visitIsInListPredicate(CqlParser.IsInListPredicateContext ctx) {
        In in;
        List<Scalar> values = ImmutableList.of(ctx.scalarExpression().subList(1, ctx.scalarExpression().size()))
            .stream()
            .flatMap(Collection::stream)
            .map(v -> (Scalar) v.accept(this))
            .collect(Collectors.toList());

        // TODO IN currently requires a property on the left side and literals on the right side
        in = new ImmutableIn.Builder()
            .addArgs((Scalar) ctx.scalarExpression(0).accept(this))
            .addArgs(ArrayLiteral.of(values))
            .build();
        if (Objects.nonNull(ctx.NOT())) {
            return Not.of(in);
        }
        return in;
    }

    @Override
    public CqlNode visitIsNullPredicate(CqlParser.IsNullPredicateContext ctx) {

        if (Objects.nonNull(ctx.IS())) {
            Scalar scalar1 = (Scalar) ctx.scalarExpression()
                                         .accept(this);

            IsNull isNull = new ImmutableIsNull.Builder()
                    .addArgs(scalar1)
                    .build();
            if (Objects.nonNull(ctx.NOT())) {
                return Not.of(isNull);
            }

            return isNull;

        }
        return null;

    }

    @Override
    public CqlNode visitTemporalPredicate(CqlParser.TemporalPredicateContext ctx) {

        if (Objects.isNull(ctx.TemporalOperator()))
            throw new IllegalStateException("unknown temporal predicate: " + ctx.getText());

        TemporalOperator temporalOperator = TemporalOperator.valueOf(ctx.TemporalOperator()
                                                                         .getText()
                                                                         .toUpperCase());

        Temporal temporal1 = (Temporal) ctx.temporalExpression(0)
            .accept(this);
        Temporal temporal2 = (Temporal) ctx.temporalExpression(1)
            .accept(this);

        return BinaryTemporalOperation.of(temporalOperator, temporal1, temporal2);
    }

    @Override
    public CqlNode visitSpatialPredicate(CqlParser.SpatialPredicateContext ctx) {

        if (Objects.isNull(ctx.SpatialOperator()))
            throw new IllegalStateException("unknown spatial operator: " + ctx.getText());

        SpatialOperator spatialOperator = SpatialOperator.valueOf(ctx.SpatialOperator()
                                                                     .getText()
                                                                     .toUpperCase());

        Spatial spatial1 = (Spatial) ctx.geomExpression()
            .get(0)
            .accept(this);
        Spatial spatial2 = (Spatial) ctx.geomExpression()
            .get(1)
            .accept(this);

        return new ImmutableSpatialOperation.Builder()
            .operator(spatialOperator)
            .operands(ImmutableList.of(spatial1,spatial2))
            .build();
    }

    @Override
    public CqlNode visitArrayPredicate(CqlParser.ArrayPredicateContext ctx) {

        if (Objects.isNull(ctx.ArrayOperator()))
            throw new IllegalStateException("unknown array operator: " + ctx.getText());

        ArrayOperator arrayOperator = ArrayOperator.valueOf(ctx.ArrayOperator()
                                                                .getText()
                                                                .toUpperCase());

        Vector vector1 = (Vector) ctx.arrayExpression().get(0).accept(this);
        Vector vector2 = (Vector) ctx.arrayExpression().get(1).accept(this);

        return new ImmutableArrayOperation.Builder()
            .operator(arrayOperator)
            .operands(ImmutableList.of(vector1, vector2))
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
    public CqlNode visitPropertyName(CqlParser.PropertyNameContext ctx) {
        if (!ctx.nestedCqlFilter().isEmpty()) {
            Map<String, CqlFilter> nestedFilters = new HashMap<>();
            for (int i = 0; i < ctx.nestedCqlFilter().size(); i++) {
                CqlFilter nestedFilter = CqlFilter.of(ctx.nestedCqlFilter(i)
                                               .accept(this));
                CqlVisitorPropertyPrefix prefix = new CqlVisitorPropertyPrefix(stripDoubleQuotes(ctx.Identifier(i).getText()));
                nestedFilter = (CqlFilter) nestedFilter.accept(prefix);
                nestedFilters.put(stripDoubleQuotes(ctx.Identifier(i).getText()), nestedFilter);
            }
            String path = ctx.Identifier().stream()
                .map(ParseTree::getText)
                .map(this::stripDoubleQuotes)
                .collect(Collectors.joining("."));
            return Property.of(path, nestedFilters);
        }
        return Property.of(ctx.Identifier().stream()
                               .map(ParseTree::getText)
                               .map(this::stripDoubleQuotes)
                               .collect(Collectors.joining(".")));
    }

    private String stripDoubleQuotes(String identifier) {
        if (identifier.startsWith("\"") && identifier.endsWith("\"")) {
            return identifier.substring(1, identifier.length()-1);
        }
        return identifier;
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

        if (arg1 instanceof Temporal && arg2 instanceof Temporal) {
            return TemporalLiteral.interval((Temporal) arg1, (Temporal) arg2);
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
