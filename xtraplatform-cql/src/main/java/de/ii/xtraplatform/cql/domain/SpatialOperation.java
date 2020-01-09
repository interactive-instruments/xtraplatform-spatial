package de.ii.xtraplatform.cql.domain;

import java.util.function.Function;

public interface SpatialOperation extends BinaryOperation<SpatialLiteral>, CqlNode {

    abstract class Builder<T extends SpatialOperation> extends BinaryOperation.Builder<SpatialLiteral, T> {}

    @Override
    default String toCqlText(String operator) {
        return String.format("%s(%s, %s)", operator, getOperands().get(0)
                                                      .toCqlText(), getOperands().get(1)
                                                                                           .toCqlText());
    }
}
