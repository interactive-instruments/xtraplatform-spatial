package de.ii.xtraplatform.cql.domain;

import java.util.Optional;

public interface SpatialExpression {

    //TODO: implement missing operations, see Intersects

    //Optional<Equals> getEquals();

    //Optional<Disjoint> getDisjoint();

    //Optional<Touches> getTouches();

    //Optional<Within> getWithin();

    //Optional<Overlaps> getOverlaps();

    //Optional<Crosses> getCrosses();

    Optional<Intersects> getIntersects();

    //Optional<Contains> getContains();

}
