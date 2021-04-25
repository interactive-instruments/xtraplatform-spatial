/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.app;

import de.ii.xtraplatform.cql.domain.*;

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
        } else if (logicalOperation instanceof Not) {
            return Not.of(children.stream()
                                  .map(cqlNode -> (CqlPredicate) cqlNode)
                                  .collect(Collectors.toList()));
        }
        return null;
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
            int i = 0;
            for (CqlNode cqlNode : children) {
                switch (i++) {
                    case 0:
                        builder.operand1((Operand) cqlNode);
                        break;
                    case 1:
                        builder.operand2((Operand) cqlNode);
                        break;
                }
            }
            return builder.build();
        }

        return null;
    }

    @Override
    public CqlNode visit(Between between, List<CqlNode> children) {
        Between.Builder builder = null;

        if (Objects.nonNull(builder)) {
            int i = 0;
            for (CqlNode cqlNode : children) {
                switch (i++) {
                    case 0:
                        builder.operand((Scalar) cqlNode);
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

        return null;
    }

    @Override
    public CqlNode visit(IsNull isNull, List<CqlNode> children) {
        Between.Builder builder = null;

        if (Objects.nonNull(builder)) {
            int i = 0;
            for (CqlNode cqlNode : children) {
                switch (i++) {
                    case 0:
                        builder.operand((Scalar) cqlNode);
                        break;
                }
            }
            return builder.build();
        }

        return null;
    }

    @Override
    public CqlNode visit(Like like, List<CqlNode> children) {
        Like.Builder builder = null;

        if (Objects.nonNull(builder)) {
            int i = 0;
            for (CqlNode cqlNode : children) {
                switch (i++) {
                    case 0:
                        builder.operand1((Scalar) cqlNode);
                        break;
                    case 1:
                        builder.operand2((Scalar) cqlNode);
                        break;
                    // modifiers are set separately
                }
            }
            return builder.build();
        }

        return null;
    }

    @Override
    public CqlNode visit(In in, List<CqlNode> children) {
        In.Builder builder = null;

        if (Objects.nonNull(builder)) {
            int i = 0;
            for (CqlNode cqlNode : children) {
                switch (i++) {
                    case 0:
                        builder.operand((Scalar) cqlNode);
                        break;
                    // values are set separately
                }
            }
            return builder.build();
        }

        return null;
    }

    @Override
    public CqlNode visit(TemporalOperation temporalOperation, List<CqlNode> children) {
        TemporalOperation.Builder<?> builder = null;

        if (temporalOperation instanceof After) {
            builder = new ImmutableAfter.Builder();
        } else if (temporalOperation instanceof Before) {
            builder = new ImmutableBefore.Builder();
        } else if (temporalOperation instanceof Begins) {
            builder = new ImmutableBegins.Builder();
        } else if (temporalOperation instanceof BegunBy) {
            builder = new ImmutableBegunBy.Builder();
        } else if (temporalOperation instanceof During) {
            builder = new ImmutableDuring.Builder();
        } else if (temporalOperation instanceof EndedBy) {
            builder = new ImmutableEndedBy.Builder();
        } else if (temporalOperation instanceof Ends) {
            builder = new ImmutableEnds.Builder();
        } else if (temporalOperation instanceof Meets) {
            builder = new ImmutableMeets.Builder();
        } else if (temporalOperation instanceof MetBy) {
            builder = new ImmutableMetBy.Builder();
        } else if (temporalOperation instanceof OverlappedBy) {
            builder = new ImmutableOverlappedBy.Builder();
        } else if (temporalOperation instanceof TContains) {
            builder = new ImmutableTContains.Builder();
        } else if (temporalOperation instanceof TEquals) {
            builder = new ImmutableTEquals.Builder();
        } else if (temporalOperation instanceof TOverlaps) {
            builder = new ImmutableTOverlaps.Builder();
        } else if (temporalOperation instanceof AnyInteracts) {
            builder = new ImmutableAnyInteracts.Builder();
        }

        if (Objects.nonNull(builder)) {
            int i = 0;
            for (CqlNode cqlNode : children) {
                switch (i++) {
                    case 0:
                        builder.operand1((Operand) cqlNode);
                        break;
                    case 1:
                        builder.operand2((Operand) cqlNode);
                        break;
                }
            }
            return builder.build();
        }

        return null;
    }

    @Override
    public CqlNode visit(SpatialOperation spatialOperation, List<CqlNode> children) {
        SpatialOperation.Builder<?> builder = null;

        if (spatialOperation instanceof Contains) {
            builder = new ImmutableContains.Builder();
        } else if (spatialOperation instanceof Crosses) {
            builder = new ImmutableCrosses.Builder();
        } else if (spatialOperation instanceof Disjoint) {
            builder = new ImmutableDisjoint.Builder();
        } else if (spatialOperation instanceof Equals) {
            builder = new ImmutableEquals.Builder();
        } else if (spatialOperation instanceof Intersects) {
            builder = new ImmutableIntersects.Builder();
        } else if (spatialOperation instanceof Overlaps) {
            builder = new ImmutableOverlaps.Builder();
        } else if (spatialOperation instanceof Touches) {
            builder = new ImmutableTouches.Builder();
        } else if (spatialOperation instanceof Within) {
            builder = new ImmutableWithin.Builder();
        }

        if (Objects.nonNull(builder)) {
            int i = 0;
            for (CqlNode cqlNode : children) {
                switch (i++) {
                    case 0:
                        builder.operand1((Operand) cqlNode);
                        break;
                    case 1:
                        builder.operand2((Operand) cqlNode);
                        break;
                }
            }
            return builder.build();
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
        } else if (arrayOperation instanceof ContainedBy) {
            builder = new ImmutableContainedBy.Builder();
        }

        if (Objects.nonNull(builder)) {
            int i = 0;
            for (CqlNode cqlNode : children) {
                switch (i++) {
                    case 0:
                        builder.operand1((Operand) cqlNode);
                        break;
                    case 1:
                        builder.operand2((Operand) cqlNode);
                        break;
                }
            }
            return builder.build();
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
