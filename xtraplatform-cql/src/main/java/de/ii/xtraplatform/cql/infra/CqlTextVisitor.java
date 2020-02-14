package de.ii.xtraplatform.cql.infra;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.cql.domain.*;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CqlTextVisitor extends CqlParserBaseVisitor<CqlNode> implements CqlParserVisitor<CqlNode> {

    private CqlPredicate wrapInPredicate(CqlNode node) {
        ImmutableCqlPredicate.Builder builder = new ImmutableCqlPredicate.Builder();

        if (node instanceof And) {
            builder.and((And) node);
        } else if (node instanceof Or) {
            builder.or((Or) node);
        }  else if (node instanceof Not) {
            builder.not((Not) node);
        }  else if (node instanceof Eq) {
            builder.eq((Eq) node);
        } else if (node instanceof Neq) {
            builder.neq((Neq) node);
        } else if (node instanceof Gt) {
            builder.gt((Gt) node);
        } else if (node instanceof Gte) {
            builder.gte((Gte) node);
        } else if (node instanceof Lt) {
            builder.lt((Lt) node);
        } else if (node instanceof Lte) {
            builder.lte((Lte) node);
        } else if (node instanceof Between) {
            builder.between((Between) node);
        } else if (node instanceof In) {
            builder.inOperator((In) node);
        } else if (node instanceof Like) {
            builder.like((Like) node);
        } else if (node instanceof IsNull) {
            builder.isNull((IsNull) node);
        } else if (node instanceof Exists) {
            builder.exists((Exists) node);
        } else if (node instanceof After) {
            builder.after((After) node);
        } else if (node instanceof Before) {
            builder.before((Before) node);
        } else if (node instanceof Begins) {
            builder.begins((Begins) node);
        } else if (node instanceof BegunBy) {
            builder.begunBy((BegunBy) node);
        } else if (node instanceof TContains) {
            builder.tContains((TContains) node);
        } else if (node instanceof During) {
            builder.during((During) node);
        } else if (node instanceof EndedBy) {
            builder.endedBy((EndedBy) node);
        } else if (node instanceof Ends) {
            builder.ends((Ends) node);
        } else if (node instanceof TEquals) {
            builder.tEquals((TEquals) node);
        } else if (node instanceof Meets) {
            builder.meets((Meets) node);
        } else if (node instanceof MetBy) {
            builder.metBy((MetBy) node);
        } else if (node instanceof TOverlaps) {
            builder.tOverlaps((TOverlaps) node);
        } else if (node instanceof OverlappedBy) {
            builder.overlappedBy((OverlappedBy) node);
        } else if (node instanceof Equals) {
            builder.within((Within) node);
        } else if (node instanceof Disjoint) {
            builder.disjoint((Disjoint) node);
        } else if (node instanceof Touches) {
            builder.touches((Touches) node);
        } else if (node instanceof Within) {
            builder.within((Within) node);
        } else if (node instanceof Overlaps) {
            builder.overlaps((Overlaps) node);
        } else if (node instanceof Crosses) {
            builder.crosses((Crosses) node);
        } else if (node instanceof Intersects) {
            builder.intersects((Intersects) node);
        } else if (node instanceof Contains) {
            builder.contains((Contains) node);
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

        if (Objects.nonNull(ctx.OR())) {
            CqlPredicate predicate1 = wrapInPredicate(ctx.booleanValueExpression().accept(this));
            CqlPredicate predicate2 = wrapInPredicate(booleanTerm);

            Or result;
            if (predicate1.getOr().isPresent()) {
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
            CqlPredicate predicate1 = wrapInPredicate(ctx.booleanTerm()
                                                         .accept(this));
            CqlPredicate predicate2 = wrapInPredicate(booleanFactor);

            And result;
            if (predicate1.getAnd().isPresent()) {
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
        if (Objects.nonNull(ctx.booleanPrimary().LEFTPAREN())) {
            booleanPrimary = ctx.booleanPrimary()
                    .booleanValueExpression()
                    .accept(this);
        }
        if (Objects.nonNull(ctx.NOT())) {
            CqlPredicate predicate = wrapInPredicate(booleanPrimary);
            return Not.of(ImmutableList.of(predicate));
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

        return builder.operand1(scalar1)
                      .operand2(scalar2)
                      .build();
    }

    @Override
    public CqlNode visitPropertyIsLikePredicate(CqlParser.PropertyIsLikePredicateContext ctx) {

        if (Objects.nonNull(ctx.LIKE())) {

            Scalar scalar1 = (Scalar) ctx.scalarExpression()
                    .accept(this);
            Scalar scalar2 = (Scalar) ctx.regularExpression()
                    .accept(this);

            CqlNode like = new ImmutableLike.Builder()
                    .operand1(scalar1)
                    .operand2(scalar2)
                    .build();

            if (Objects.nonNull(ctx.NOT()) ) {
                return Not.of(ImmutableList.of(wrapInPredicate(like)));
            }

            return like;
        }
        return null;
    }

    @Override
    public CqlNode visitPropertyIsBetweenPredicate(CqlParser.PropertyIsBetweenPredicateContext ctx) {

        Property property = (Property) ctx.scalarExpression(0).accept(this);
        ScalarLiteral lowerValue = (ScalarLiteral) ctx.scalarExpression(1).accept(this);
        ScalarLiteral upperValue = (ScalarLiteral) ctx.scalarExpression(2).accept(this);
        return new ImmutableBetween.Builder()
                .property(property.getName())
                .lower(lowerValue)
                .upper(upperValue)
                .build();
    }

    @Override
    public CqlNode visitPropertyIsNullPredicate(CqlParser.PropertyIsNullPredicateContext ctx) {

        if (Objects.nonNull(ctx.IS())) {
            Property property = (Property) ctx.scalarExpression().accept(this);

            IsNull isNull = new ImmutableIsNull.Builder()
                    .property(property.getName())
                    .build();
            if (Objects.nonNull(ctx.NOT())) {
                return Not.of(ImmutableList.of(new ImmutableCqlPredicate.Builder()
                        .isNull(isNull)
                        .build()));
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
        TemporalOperator temporalOperator = TemporalOperator.valueOf(ctx.TemporalOperator()
                                                                        .getText());

        TemporalOperation.Builder<? extends TemporalOperation> builder;

        switch (temporalOperator) {
            case AFTER:
                builder = new ImmutableAfter.Builder();
                break;
            case BEFORE:
                builder = new ImmutableBefore.Builder();
                break;
            case BEGINS:
                builder = new ImmutableBegins.Builder();
                break;
            case BEGUNBY:
                builder = new ImmutableBegunBy.Builder();
                break;
            case TCONTAINS:
                builder = new ImmutableTContains.Builder();
                break;
            case DURING:
                builder = new ImmutableDuring.Builder();
                break;
            case ENDEDBY:
                builder = new ImmutableEndedBy.Builder();
                break;
            case ENDS:
                builder = new ImmutableEnds.Builder();
                break;
            case TEQUALS:
                builder = new ImmutableTEquals.Builder();
                break;
            case MEETS:
                builder = new ImmutableMeets.Builder();
                break;
            case METBY:
                builder = new ImmutableMetBy.Builder();
                break;
            case TOVERLAPS:
                builder = new ImmutableTOverlaps.Builder();
                break;
            case OVERLAPPEDBY:
                builder = new ImmutableOverlappedBy.Builder();
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

        return builder.operand1(spatial1)
                      .operand2(spatial2)
                      .build();
    }

    @Override
    public CqlNode visitExistencePredicate(CqlParser.ExistencePredicateContext ctx) {

        if (Objects.nonNull(ctx.EXISTS())) {
            return new ImmutableExists.Builder()
                    .property(ctx.PropertyName().getText())
                    .build();
        } else if (Objects.nonNull(ctx.DOES()) && Objects.nonNull(ctx.NOT()) && Objects.nonNull(ctx.EXIST())) {
            return Not.of(ImmutableList.of(new ImmutableCqlPredicate.Builder()
                    .exists(new ImmutableExists.Builder()
                            .property(ctx.PropertyName().getText())
                            .build())
                    .build()));
        }

        return null;
    }

    @Override
    public CqlNode visitInPredicate(CqlParser.InPredicateContext ctx) {

        List<ScalarLiteral> values = ImmutableList.of(ctx.characterLiteral(), ctx.numericLiteral())
                .stream()
                .flatMap(Collection::stream)
                .map(v -> (ScalarLiteral) v.accept(this))
                .collect(Collectors.toList());
        return new ImmutableIn.Builder()
                .property(ctx.PropertyName().getText())
                .values(values)
                .build();
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
        CqlNode geomLiteral = ctx.getChild(0).accept(this);
        return SpatialLiteral.of((Geometry<?>) geomLiteral);
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
    public CqlNode visitEnvelope(CqlParser.EnvelopeContext ctx) {
        List<Double> coordinates;
        Double eastBoundLon = Double.valueOf(ctx.eastBoundLon().NumericLiteral().getText());
        Double westBoundLon = Double.valueOf(ctx.westBoundLon().NumericLiteral().getText());
        Double northBoundLat = Double.valueOf(ctx.northBoundLat().NumericLiteral().getText());
        Double southBoundLat = Double.valueOf(ctx.southBoundLat().NumericLiteral().getText());

        if (Objects.nonNull(ctx.minElev()) && Objects.nonNull(ctx.maxElev())) {
            Double minElev = Double.valueOf(ctx.minElev().NumericLiteral().getText());
            Double maxElev = Double.valueOf(ctx.maxElev().NumericLiteral().getText());
            coordinates = ImmutableList.of(southBoundLat, westBoundLon, minElev, northBoundLat, eastBoundLon, maxElev);
        } else {
            coordinates = ImmutableList.of(westBoundLon, eastBoundLon, northBoundLat, southBoundLat);
        }

        return new ImmutableEnvelope.Builder()
                .coordinates(coordinates)
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
