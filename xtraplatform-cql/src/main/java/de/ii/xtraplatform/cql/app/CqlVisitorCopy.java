/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.app;

import de.ii.xtraplatform.cql.domain.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CqlVisitorCopy implements CqlVisitor<CqlNode> {
    @Override
    public CqlNode visit(CqlFilter cqlFilter, List<CqlNode> children) {
        return CqlFilter.of(children.get(0));
    }

    @Override
    public CqlNode visit(CqlPredicate cqlPredicate, List<CqlNode> children) {
        return CqlPredicate.of(children.get(0));
    }

    @Override
    public CqlNode visit(LogicalOperation logicalOperation, List<CqlNode> children) {
        if (logicalOperation instanceof And) {
            return And.of(children.stream()
                                  .map(cqlNode -> (CqlPredicate) cqlNode)
                                  .collect(Collectors.toList()));
        } else if (logicalOperation instanceof Or) {
            return Or.of(children.stream()
                                 .map(cqlNode -> (CqlPredicate) cqlNode)
                                 .collect(Collectors.toList()));
        }
        return null;
    }

    @Override
    public CqlNode visit(Not not, List<CqlNode> children) {
        return Not.of((CqlPredicate) children.get(0));
    }

    @Override
    public CqlNode visit(BinaryScalarOperation scalarOperation, List<CqlNode> children) {
        BinaryScalarOperation.Builder<?> builder = null;

        if (scalarOperation instanceof Eq) {
            builder = new ImmutableEq.Builder();
        } else if (scalarOperation instanceof Gt) {
            builder = new ImmutableGt.Builder();
        } else if (scalarOperation instanceof Gte) {
            builder = new ImmutableGte.Builder();
        } else if (scalarOperation instanceof Lt) {
            builder = new ImmutableLt.Builder();
        } else if (scalarOperation instanceof Lte) {
            builder = new ImmutableLte.Builder();
        } else if (scalarOperation instanceof Neq) {
            builder = new ImmutableNeq.Builder();
        }

        if (Objects.nonNull(builder)) {
            return builder.operands(children.stream()
                                            .filter(child -> child instanceof Scalar)
                                            .map(child -> (Scalar) child)
                                            .collect(Collectors.toUnmodifiableList())).build();
        }

        return null;
    }

    @Override
    public CqlNode visit(Between between, List<CqlNode> children) {
        Between.Builder builder = new ImmutableBetween.Builder();

        int i = 0;
        for (CqlNode cqlNode : children) {
            switch (i++) {
                case 0:
                    builder.value((Scalar) cqlNode);
                    break;
                case 1:
                    builder.lower((Scalar) cqlNode);
                    break;
                case 2:
                    builder.upper((Scalar) cqlNode);
                    break;
            }
        }
        return builder.build();
    }

    @Override
    public CqlNode visit(IsNull isNull, List<CqlNode> children) {
        IsNull.Builder builder = new ImmutableIsNull.Builder();
        builder.operand((Scalar) children.get(0));
        return builder.build();
    }

    @Override
    public CqlNode visit(Like like, List<CqlNode> children) {
        Like.Builder builder = new ImmutableLike.Builder();

        // modifiers are set separately
        return builder.operands(children.stream()
                                        .filter(child -> child instanceof Scalar)
                                        .map(child -> (Scalar) child)
                                        .collect(Collectors.toUnmodifiableList())).build();
    }

    @Override
    public CqlNode visit(In in, List<CqlNode> children) {
        ImmutableIn.Builder builder = new ImmutableIn.Builder();

        builder.value((Scalar) children.get(0));
        ArrayList<Scalar> list = new ArrayList<>();
        for (int i = 1; i < children.size(); i++) {
            list.add((Scalar) children.get(i));
        }
        builder.list(list);
        return builder.build();
    }

    @Override
    public CqlNode visit(TemporalOperation temporalOperation, List<CqlNode> children) {
        return new ImmutableTemporalOperation.Builder()
            .operator(temporalOperation.getOperator())
            .operands(children.stream()
                          .filter(child -> child instanceof Temporal)
                          .map(child -> (Temporal) child)
                          .collect(Collectors.toUnmodifiableList()))
            .build();
    }

    @Override
    public CqlNode visit(SpatialOperation spatialOperation, List<CqlNode> children) {
        SpatialOperation.Builder<?> builder = null;

        if (spatialOperation instanceof SContains) {
            builder = new ImmutableSContains.Builder();
        } else if (spatialOperation instanceof SCrosses) {
            builder = new ImmutableSCrosses.Builder();
        } else if (spatialOperation instanceof SDisjoint) {
            builder = new ImmutableSDisjoint.Builder();
        } else if (spatialOperation instanceof SEquals) {
            builder = new ImmutableSEquals.Builder();
        } else if (spatialOperation instanceof SIntersects) {
            builder = new ImmutableSIntersects.Builder();
        } else if (spatialOperation instanceof SOverlaps) {
            builder = new ImmutableSOverlaps.Builder();
        } else if (spatialOperation instanceof STouches) {
            builder = new ImmutableSTouches.Builder();
        } else if (spatialOperation instanceof SWithin) {
            builder = new ImmutableSWithin.Builder();
        }

        if (Objects.nonNull(builder)) {
            return builder.operands(children.stream()
                                            .filter(child -> child instanceof Spatial)
                                            .map(child -> (Spatial) child)
                                            .collect(Collectors.toUnmodifiableList())).build();
        }

        return null;
    }

    @Override
    public CqlNode visit(ArrayOperation arrayOperation, List<CqlNode> children) {
        ArrayOperation.Builder<?> builder = null;

        if (arrayOperation instanceof AContains) {
            builder = new ImmutableAContains.Builder();
        } else if (arrayOperation instanceof AEquals) {
            builder = new ImmutableAEquals.Builder();
        } else if (arrayOperation instanceof AOverlaps) {
            builder = new ImmutableAOverlaps.Builder();
        } else if (arrayOperation instanceof AContainedBy) {
            builder = new ImmutableAContainedBy.Builder();
        }

        if (Objects.nonNull(builder)) {
            return builder.operands(children.stream()
                                            .filter(child -> child instanceof Vector)
                                            .map(child -> (Vector) child)
                                            .collect(Collectors.toUnmodifiableList())).build();
        }

        return null;
    }

    @Override
    public CqlNode visit(ScalarLiteral scalarLiteral, List<CqlNode> children) {
        return scalarLiteral;
    }

    @Override
    public CqlNode visit(TemporalLiteral temporalLiteral, List<CqlNode> children) {
        return temporalLiteral;
    }

    @Override
    public CqlNode visit(ArrayLiteral arrayLiteral, List<CqlNode> children) {
        return arrayLiteral;
    }

    @Override
    public CqlNode visit(SpatialLiteral spatialLiteral, List<CqlNode> children) {
        return spatialLiteral;
    }

    @Override
    public CqlNode visit(Property property, List<CqlNode> children) {
        return property;
    }

    @Override
    public CqlNode visit(Geometry.Coordinate coordinate, List<CqlNode> children) {
        return coordinate;
    }

    @Override
    public CqlNode visit(Geometry.Point point, List<CqlNode> children) {
        return point;
    }

    @Override
    public CqlNode visit(Geometry.LineString lineString, List<CqlNode> children) {
        return lineString;
    }

    @Override
    public CqlNode visit(Geometry.Polygon polygon, List<CqlNode> children) {
        return polygon;
    }

    @Override
    public CqlNode visit(Geometry.MultiPoint multiPoint, List<CqlNode> children) {
        return multiPoint;
    }

    @Override
    public CqlNode visit(Geometry.MultiLineString multiLineString, List<CqlNode> children) {
        return multiLineString;
    }

    @Override
    public CqlNode visit(Geometry.MultiPolygon multiPolygon, List<CqlNode> children) {
        return multiPolygon;
    }

    @Override
    public CqlNode visit(Geometry.Envelope envelope, List<CqlNode> children) {
        return envelope;
    }

    @Override
    public CqlNode visit(Function function, List<CqlNode> children) {
        return function;
    }

}
