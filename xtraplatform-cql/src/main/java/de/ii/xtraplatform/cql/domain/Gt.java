package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableGt.Builder.class)
public interface Gt extends ScalarOperation, CqlNode {

    static Gt of(String property, ScalarLiteral scalarLiteral) {
        return new ImmutableGt.Builder().property(property)
                                        .value(scalarLiteral)
                                        .build();
    }

    static Gt of(Property property, ScalarLiteral scalarLiteral) {
        return new ImmutableGt.Builder().property(property)
                .value(scalarLiteral)
                .build();
    }

    static Gt ofFunction(Function function, ScalarLiteral scalarLiteral) {
        return new ImmutableGt.Builder().function(function)
                .value(scalarLiteral)
                .build();
    }

    abstract class Builder extends ScalarOperation.Builder<Gt> {
    }

}
