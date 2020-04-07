package de.ii.xtraplatform.cql.domain;

public interface ScalarOperation extends BinaryOperation<ScalarLiteral>, CqlNode {

    abstract class Builder<T extends ScalarOperation> extends BinaryOperation.Builder<ScalarLiteral, T> {}

}
