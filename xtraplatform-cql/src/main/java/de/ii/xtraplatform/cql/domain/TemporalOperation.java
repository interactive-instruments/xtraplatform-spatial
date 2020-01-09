package de.ii.xtraplatform.cql.domain;

public interface TemporalOperation extends BinaryOperation<TemporalLiteral>, CqlNode {

    abstract class Builder<T extends TemporalOperation> extends BinaryOperation.Builder<TemporalLiteral, T> {}

}
