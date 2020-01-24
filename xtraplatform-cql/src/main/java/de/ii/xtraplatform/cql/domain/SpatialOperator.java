package de.ii.xtraplatform.cql.domain;

public enum SpatialOperator implements CqlNode {
    EQUALS,
    DISJOINT,
    TOUCHES,
    WITHIN,
    OVERLAPS,
    CROSSES,
    INTERSECTS,
    CONTAINS
}
