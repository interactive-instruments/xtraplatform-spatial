package de.ii.xtraplatform.cql.domain;

public interface SpatialOperation extends BinaryOperation<SpatialLiteral>, CqlNode {

    abstract class Builder<T extends SpatialOperation> extends BinaryOperation.Builder<SpatialLiteral, T> {}

}
