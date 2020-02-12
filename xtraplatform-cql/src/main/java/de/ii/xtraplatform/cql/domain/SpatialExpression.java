package de.ii.xtraplatform.cql.domain;

import java.util.Optional;

public interface SpatialExpression {

    Optional<Equals> getEquals();

    Optional<Disjoint> getDisjoint();

    Optional<Touches> getTouches();

    Optional<Within> getWithin();

    Optional<Overlaps> getOverlaps();

    Optional<Crosses> getCrosses();

    Optional<Intersects> getIntersects();

    Optional<Contains> getContains();

}
