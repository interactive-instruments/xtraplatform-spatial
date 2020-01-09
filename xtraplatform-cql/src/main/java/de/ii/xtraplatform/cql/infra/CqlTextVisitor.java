package de.ii.xtraplatform.cql.infra;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.cql.domain.And;
import de.ii.xtraplatform.cql.domain.ComparisonOperator;
import de.ii.xtraplatform.cql.domain.CqlNode;
import de.ii.xtraplatform.cql.domain.CqlParseException;
import de.ii.xtraplatform.cql.domain.CqlPredicate;
import de.ii.xtraplatform.cql.domain.During;
import de.ii.xtraplatform.cql.domain.Eq;
import de.ii.xtraplatform.cql.domain.Geometry;
import de.ii.xtraplatform.cql.domain.Gt;
import de.ii.xtraplatform.cql.domain.ImmutableCqlPredicate;
import de.ii.xtraplatform.cql.domain.ImmutableDuring;
import de.ii.xtraplatform.cql.domain.ImmutableEq;
import de.ii.xtraplatform.cql.domain.ImmutableGt;
import de.ii.xtraplatform.cql.domain.ImmutableIntersects;
import de.ii.xtraplatform.cql.domain.ImmutableLineString;
import de.ii.xtraplatform.cql.domain.ImmutablePolygon;
import de.ii.xtraplatform.cql.domain.Intersects;
import de.ii.xtraplatform.cql.domain.Property;
import de.ii.xtraplatform.cql.domain.Scalar;
import de.ii.xtraplatform.cql.domain.ScalarLiteral;
import de.ii.xtraplatform.cql.domain.ScalarOperation;
import de.ii.xtraplatform.cql.domain.Spatial;
import de.ii.xtraplatform.cql.domain.SpatialLiteral;
import de.ii.xtraplatform.cql.domain.SpatialOperation;
import de.ii.xtraplatform.cql.domain.SpatialOperator;
import de.ii.xtraplatform.cql.domain.Temporal;
import de.ii.xtraplatform.cql.domain.TemporalLiteral;
import de.ii.xtraplatform.cql.domain.TemporalOperation;
import de.ii.xtraplatform.cql.domain.TemporalOperator;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CqlTextVisitor extends CqlParserBaseVisitor<CqlNode> implements CqlParserVisitor<CqlNode> {

    private CqlPredicate wrapInPredicate(CqlNode node) {
        ImmutableCqlPredicate.Builder builder = new ImmutableCqlPredicate.Builder();

        //TODO: add all expressions when implemented
        if (node instanceof And) {
            builder.and((And) node);
        } else if (node instanceof Eq) {
            builder.eq((Eq) node);
        } else if (node instanceof Gt) {
            builder.gt((Gt) node);
        } else if (node instanceof During) {
            builder.during((During) node);
        } else if (node instanceof Intersects) {
            builder.intersects((Intersects) node);
        }

        return builder.build();
    }

    @Override
    public CqlNode visitCqlFilter(CqlParser.CqlFilterContext ctx) {
        CqlNode booleanValueExpression = ctx.booleanValueExpression()
                                            .accept(this);

        return wrapInPredicate(booleanValueExpression);
    }

    @Override
    public CqlNode visitBooleanValueExpression(CqlParser.BooleanValueExpressionContext ctx) {
        CqlNode booleanTerm = ctx.booleanTerm()
                                 .accept(this);

        //TODO: create Or
        if (Objects.nonNull(ctx.OR())) {

        }

        return booleanTerm;
    }

    @Override
    public CqlNode visitBooleanTerm(CqlParser.BooleanTermContext ctx) {
        CqlNode booleanFactor = ctx.booleanFactor()
                                   .accept(this);

        // create And
        if (Objects.nonNull(ctx.AND())) {
            CqlPredicate predicate1 = wrapInPredicate(ctx.booleanTerm()
                                                         .accept(this));
            CqlPredicate predicate2 = wrapInPredicate(booleanFactor);

            return And.of(ImmutableList.of(predicate1, predicate2));
        }

        return booleanFactor;
    }

    @Override
    public CqlNode visitBooleanFactor(CqlParser.BooleanFactorContext ctx) {
        CqlNode booleanPrimary = ctx.booleanPrimary()
                                    .accept(this);

        //TODO: create Not
        if (Objects.nonNull(ctx.NOT())) {

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

        ScalarOperation.Builder<? extends ScalarOperation> builder;

        //TODO: add all comparison/scalar expressions when implemented

        switch (comparisonOperator) {
            case GT:
                builder = new ImmutableGt.Builder();
                break;
            case EQ:
                builder = new ImmutableEq.Builder();
                break;
            default:
                throw new IllegalStateException("unknown comparison operator: " + comparisonOperator);
        }

        return builder.operand1(scalar1)
                      .operand2(scalar2)
                      .build();
    }

    @Override
    public CqlNode visitPropertyIsLikePredicate(CqlParser.PropertyIsLikePredicateContext ctx) {

        //TODO

        return null;
    }

    @Override
    public CqlNode visitPropertyIsBetweenPredicate(CqlParser.PropertyIsBetweenPredicateContext ctx) {

        //TODO

        return null;
    }

    @Override
    public CqlNode visitPropertyIsNullPredicate(CqlParser.PropertyIsNullPredicateContext ctx) {

        //TODO

        return null;
    }

    @Override
    public CqlNode visitTemporalPredicate(CqlParser.TemporalPredicateContext ctx) {

        Temporal temporal1 = (Temporal) ctx.temporalExpression(0)
                                           .accept(this);
        Temporal temporal2 = (Temporal) ctx.temporalExpression(1)
                                           .accept(this);
        TemporalOperator temporalOperator = TemporalOperator.valueOf(ctx.TemporalOperator()
                                                                        .getText());

        TemporalOperation.Builder<? extends TemporalOperation> builder;

        //TODO: add all temporal expressions when implemented

        switch (temporalOperator) {
            case DURING:
                builder = new ImmutableDuring.Builder();
                break;
            default:
                throw new IllegalStateException("unknown temporal operator: " + temporalOperator);
        }

        return builder.operand1(temporal1)
                      .operand2(temporal2)
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
                                                                     .getText());

        SpatialOperation.Builder<? extends SpatialOperation> builder;

        //TODO: add all spatial expressions when implemented

        switch (spatialOperator) {
            case INTERSECTS:
                builder = new ImmutableIntersects.Builder();
                break;
            default:
                throw new IllegalStateException("unknown spatial operator: " + spatialOperator);
        }

        return builder.operand1(spatial1)
                      .operand2(spatial2)
                      .build();
    }

    @Override
    public CqlNode visitExistencePredicate(CqlParser.ExistencePredicateContext ctx) {

        //TODO

        return null;
    }

    @Override
    public CqlNode visitInPredicate(CqlParser.InPredicateContext ctx) {

        //TODO

        return null;
    }

    @Override
    public CqlNode visitPropertyName(CqlParser.PropertyNameContext ctx) {
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
        return SpatialLiteral.of((Geometry<?>) ctx.getChild(0)
                                                  .accept(this));
    }

    @Override
    public CqlNode visitPolygon(CqlParser.PolygonContext ctx) {
        List<List<Geometry.Coordinate>> coordinates = ctx.linestringDef()
                                                         .stream()
                                                         .map(linestringContext -> ((Geometry.LineString) linestringContext.accept(this)).getCoordinates())
                                                         .collect(Collectors.toList());

        return new ImmutablePolygon.Builder().coordinates(coordinates)
                                             .build();
    }

    @Override
    public CqlNode visitLinestringDef(CqlParser.LinestringDefContext ctx) {
        List<Geometry.Coordinate> coordinates = ctx.coordinate()
                                                   .stream()
                                                   .map(pointContext -> (Geometry.Coordinate) pointContext.accept(this))
                                                   .collect(Collectors.toList());

        return new ImmutableLineString.Builder().coordinates(coordinates)
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

}
