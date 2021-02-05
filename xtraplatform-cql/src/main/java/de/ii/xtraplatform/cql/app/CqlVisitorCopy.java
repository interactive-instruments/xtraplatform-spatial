/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.app;

import de.ii.xtraplatform.cql.domain.After;
import de.ii.xtraplatform.cql.domain.And;
import de.ii.xtraplatform.cql.domain.Before;
import de.ii.xtraplatform.cql.domain.Begins;
import de.ii.xtraplatform.cql.domain.BegunBy;
import de.ii.xtraplatform.cql.domain.Between;
import de.ii.xtraplatform.cql.domain.Contains;
import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.cql.domain.CqlNode;
import de.ii.xtraplatform.cql.domain.CqlPredicate;
import de.ii.xtraplatform.cql.domain.CqlVisitor;
import de.ii.xtraplatform.cql.domain.Crosses;
import de.ii.xtraplatform.cql.domain.Disjoint;
import de.ii.xtraplatform.cql.domain.During;
import de.ii.xtraplatform.cql.domain.EndedBy;
import de.ii.xtraplatform.cql.domain.Ends;
import de.ii.xtraplatform.cql.domain.Eq;
import de.ii.xtraplatform.cql.domain.Equals;
import de.ii.xtraplatform.cql.domain.Exists;
import de.ii.xtraplatform.cql.domain.Function;
import de.ii.xtraplatform.cql.domain.Geometry;
import de.ii.xtraplatform.cql.domain.Gt;
import de.ii.xtraplatform.cql.domain.Gte;
import de.ii.xtraplatform.cql.domain.ImmutableAfter;
import de.ii.xtraplatform.cql.domain.ImmutableBefore;
import de.ii.xtraplatform.cql.domain.ImmutableBegins;
import de.ii.xtraplatform.cql.domain.ImmutableBegunBy;
import de.ii.xtraplatform.cql.domain.ImmutableBetween;
import de.ii.xtraplatform.cql.domain.ImmutableContains;
import de.ii.xtraplatform.cql.domain.ImmutableCrosses;
import de.ii.xtraplatform.cql.domain.ImmutableDisjoint;
import de.ii.xtraplatform.cql.domain.ImmutableDuring;
import de.ii.xtraplatform.cql.domain.ImmutableEndedBy;
import de.ii.xtraplatform.cql.domain.ImmutableEnds;
import de.ii.xtraplatform.cql.domain.ImmutableEq;
import de.ii.xtraplatform.cql.domain.ImmutableEquals;
import de.ii.xtraplatform.cql.domain.ImmutableExists;
import de.ii.xtraplatform.cql.domain.ImmutableGt;
import de.ii.xtraplatform.cql.domain.ImmutableGte;
import de.ii.xtraplatform.cql.domain.ImmutableIn;
import de.ii.xtraplatform.cql.domain.ImmutableIntersects;
import de.ii.xtraplatform.cql.domain.ImmutableIsNull;
import de.ii.xtraplatform.cql.domain.ImmutableLike;
import de.ii.xtraplatform.cql.domain.ImmutableLt;
import de.ii.xtraplatform.cql.domain.ImmutableLte;
import de.ii.xtraplatform.cql.domain.ImmutableMeets;
import de.ii.xtraplatform.cql.domain.ImmutableMetBy;
import de.ii.xtraplatform.cql.domain.ImmutableNeq;
import de.ii.xtraplatform.cql.domain.ImmutableOverlappedBy;
import de.ii.xtraplatform.cql.domain.ImmutableOverlaps;
import de.ii.xtraplatform.cql.domain.ImmutableTContains;
import de.ii.xtraplatform.cql.domain.ImmutableTEquals;
import de.ii.xtraplatform.cql.domain.ImmutableTOverlaps;
import de.ii.xtraplatform.cql.domain.ImmutableTouches;
import de.ii.xtraplatform.cql.domain.ImmutableWithin;
import de.ii.xtraplatform.cql.domain.In;
import de.ii.xtraplatform.cql.domain.Intersects;
import de.ii.xtraplatform.cql.domain.IsNull;
import de.ii.xtraplatform.cql.domain.Like;
import de.ii.xtraplatform.cql.domain.LogicalOperation;
import de.ii.xtraplatform.cql.domain.Lt;
import de.ii.xtraplatform.cql.domain.Lte;
import de.ii.xtraplatform.cql.domain.Meets;
import de.ii.xtraplatform.cql.domain.MetBy;
import de.ii.xtraplatform.cql.domain.Neq;
import de.ii.xtraplatform.cql.domain.Not;
import de.ii.xtraplatform.cql.domain.Operand;
import de.ii.xtraplatform.cql.domain.Or;
import de.ii.xtraplatform.cql.domain.OverlappedBy;
import de.ii.xtraplatform.cql.domain.Overlaps;
import de.ii.xtraplatform.cql.domain.Property;
import de.ii.xtraplatform.cql.domain.ScalarLiteral;
import de.ii.xtraplatform.cql.domain.ScalarOperation;
import de.ii.xtraplatform.cql.domain.SpatialLiteral;
import de.ii.xtraplatform.cql.domain.SpatialOperation;
import de.ii.xtraplatform.cql.domain.TContains;
import de.ii.xtraplatform.cql.domain.TEquals;
import de.ii.xtraplatform.cql.domain.TOverlaps;
import de.ii.xtraplatform.cql.domain.TemporalLiteral;
import de.ii.xtraplatform.cql.domain.TemporalOperation;
import de.ii.xtraplatform.cql.domain.Touches;
import de.ii.xtraplatform.cql.domain.Within;

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
    public CqlNode visit(ScalarOperation scalarOperation, List<CqlNode> children) {
        ScalarOperation.Builder<?> builder = null;

        if (scalarOperation instanceof Between) {
            builder = new ImmutableBetween.Builder();
        } else if (scalarOperation instanceof Eq) {
            builder = new ImmutableEq.Builder();
        } else if (scalarOperation instanceof Exists) {
            builder = new ImmutableExists.Builder();
        } else if (scalarOperation instanceof Gt) {
            builder = new ImmutableGt.Builder();
        } else if (scalarOperation instanceof Gte) {
            builder = new ImmutableGte.Builder();
        } else if (scalarOperation instanceof In) {
            builder = new ImmutableIn.Builder();
        } else if (scalarOperation instanceof IsNull) {
            builder = new ImmutableIsNull.Builder();
        } else if (scalarOperation instanceof Like) {
            builder = new ImmutableLike.Builder();
        } else if (scalarOperation instanceof Lt) {
            builder = new ImmutableLt.Builder();
        } else if (scalarOperation instanceof Lte) {
            builder = new ImmutableLte.Builder();
        } else if (scalarOperation instanceof Neq) {
            builder = new ImmutableNeq.Builder();
        }

        if (Objects.nonNull(builder)) {
            for (CqlNode cqlNode : children) {
                builder.addOperand((Operand) cqlNode);
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
        }

        if (Objects.nonNull(builder)) {
            for (CqlNode cqlNode : children) {
                builder.addOperand((Operand) cqlNode);
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
            for (CqlNode cqlNode : children) {
                builder.addOperand((Operand) cqlNode);
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
