package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableGte.Builder.class)
public interface Gte extends ScalarOperation, CqlNode {

    static Gte of(Property property, ScalarLiteral scalarLiteral) {
        return new ImmutableGte.Builder().property(property)
                                         .value(scalarLiteral)
                                         .build();
    }

    static Gte ofFunction(Function function, ScalarLiteral scalarLiteral) {
        return new ImmutableGte.Builder().function(function)
                .value(scalarLiteral)
                .build();
    }

    abstract class Builder extends ScalarOperation.Builder<Gte> {
    }

}
