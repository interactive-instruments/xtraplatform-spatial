package de.ii.xtraplatform.cql.infra;

import de.ii.xtraplatform.cql.domain.*;

import java.util.List;

public interface CqlObjectVisitor<T> {

    T visit(CqlNode node);

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

    T visit(Geometry geometry);

}
