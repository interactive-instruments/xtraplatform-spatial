package de.ii.xtraplatform.cql.infra;

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
import de.ii.xtraplatform.cql.domain.Crosses;
import de.ii.xtraplatform.cql.domain.Disjoint;
import de.ii.xtraplatform.cql.domain.During;
import de.ii.xtraplatform.cql.domain.EndedBy;
import de.ii.xtraplatform.cql.domain.Ends;
import de.ii.xtraplatform.cql.domain.Eq;
import de.ii.xtraplatform.cql.domain.Equals;
import de.ii.xtraplatform.cql.domain.Exists;
import de.ii.xtraplatform.cql.domain.Geometry;
import de.ii.xtraplatform.cql.domain.Gt;
import de.ii.xtraplatform.cql.domain.Gte;
import de.ii.xtraplatform.cql.domain.In;
import de.ii.xtraplatform.cql.domain.Intersects;
import de.ii.xtraplatform.cql.domain.IsNull;
import de.ii.xtraplatform.cql.domain.Like;
import de.ii.xtraplatform.cql.domain.Literal;
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
import de.ii.xtraplatform.cql.domain.SpatialLiteral;
import de.ii.xtraplatform.cql.domain.TContains;
import de.ii.xtraplatform.cql.domain.TEquals;
import de.ii.xtraplatform.cql.domain.TOverlaps;
import de.ii.xtraplatform.cql.domain.Touches;
import de.ii.xtraplatform.cql.domain.Within;

import java.util.List;

public interface CqlVisitor<T> {

    default T visit(CqlNode node) {
        if (node instanceof CqlFilter) {
            return visit((CqlFilter) node);
        } else if (node instanceof CqlPredicate) {
            return visit((CqlPredicate) node);
        } else if (node instanceof And) {
            return visit((And) node);
        } else if (node instanceof Or) {
            return visit((Or) node);
        } else if (node instanceof Not) {
            return visit((Not) node);
        } else if (node instanceof Eq) {
            return visit((Eq) node);
        } else if (node instanceof Neq) {
            return visit((Neq) node);
        } else if (node instanceof Gt) {
            return visit((Gt) node);
        } else if (node instanceof Gte) {
            return visit((Gte) node);
        } else if (node instanceof Lt) {
            return visit((Lt) node);
        } else if (node instanceof Lte) {
            return visit((Lte) node);
        } else if (node instanceof Between) {
            return visit((Between) node);
        } else if (node instanceof In) {
            return visit((In) node);
        } else if (node instanceof Like) {
            return visit((Like) node);
        } else if (node instanceof IsNull) {
            return visit((IsNull) node);
        } else if (node instanceof Exists) {
            return visit((Exists) node);
        } else if (node instanceof After) {
            return visit((After) node);
        } else if (node instanceof Before) {
            return visit((Before) node);
        } else if (node instanceof Begins) {
            return visit((Begins) node);
        } else if (node instanceof BegunBy) {
            return visit((BegunBy) node);
        } else if (node instanceof TContains) {
            return visit((TContains) node);
        } else if (node instanceof During) {
            return visit((During) node);
        } else if (node instanceof EndedBy) {
            return visit((EndedBy) node);
        } else if (node instanceof Ends) {
            return visit((Ends) node);
        } else if (node instanceof TEquals) {
            return visit((TEquals) node);
        } else if (node instanceof Meets) {
            return visit((Meets) node);
        } else if (node instanceof MetBy) {
            return visit((MetBy) node);
        } else if (node instanceof TOverlaps) {
            return visit((TOverlaps) node);
        } else if (node instanceof OverlappedBy) {
            return visit((OverlappedBy) node);
        } else if (node instanceof Equals) {
            return visit((Within) node);
        } else if (node instanceof Disjoint) {
            return visit((Disjoint) node);
        } else if (node instanceof Touches) {
            return visit((Touches) node);
        } else if (node instanceof Within) {
            return visit((Within) node);
        } else if (node instanceof Overlaps) {
            return visit((Overlaps) node);
        } else if (node instanceof Crosses) {
            return visit((Crosses) node);
        } else if (node instanceof Intersects) {
            return visit((Intersects) node);
        } else if (node instanceof Contains) {
            return visit((Contains) node);
        }

        throw new IllegalStateException();
    }

    T visit(CqlFilter cqlFilter);

    T visit(CqlPredicate cqlPredicate);

    T visit(And and, List<T> children);

    T visit(Or or, List<T> children);

    T visit(Not not);

    T visit(Eq eq);

    T visit(Neq neq);

    T visit(Gt gt);

    T visit(Gte gte);

    T visit(Lt lt);

    T visit(Lte lte);

    T visit(Like like);

    T visit(Between between);

    T visit(In in);

    T visit(IsNull isNull);

    T visit(Exists exists);

    T visit(After after);

    T visit(Before before);

    T visit(Begins begins);

    T visit(BegunBy begunBy);

    T visit(TContains tContains);

    T visit(During during);

    T visit(EndedBy endedBy);

    T visit(Ends ends);

    T visit(TEquals tEquals);

    T visit(Meets meets);

    T visit(MetBy metBy);

    T visit(TOverlaps tOverlaps);

    T visit(OverlappedBy overlappedBy);

    T visit(Equals equals);

    T visit(Disjoint disjoint);

    T visit(Touches touches);

    T visit(Within within);

    T visit(Overlaps overlaps);

    T visit(Crosses crosses);

    T visit(Intersects intersects);

    T visit(Contains contains);

    T visit(Geometry.Coordinate coordinate);

    T visit(Geometry.Point point);

    T visit(Geometry.LineString lineString);

    T visit(Geometry.Polygon polygon);

    T visit(Geometry.MultiPoint multiPoint);

    T visit(Geometry.MultiLineString multiLineString);

    T visit(Geometry.MultiPolygon multiPolygon);

    T visit(Geometry.Envelope envelope);

    T visit(Literal literal);

    T visit(SpatialLiteral spatialLiteral);

    T visit(Operand operand);

    T visit(Property property);

}
