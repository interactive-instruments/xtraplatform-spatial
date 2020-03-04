package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableNeq.Builder.class)
public interface Neq extends ScalarOperation, CqlNode {

    static Neq of(Property property, ScalarLiteral scalarLiteral) {
        return new ImmutableNeq.Builder().property(property)
                                         .value(scalarLiteral)
                                         .build();
    }

    static Neq ofFunction(Function function, ScalarLiteral scalarLiteral) {
        return new ImmutableNeq.Builder().function(function)
                .value(scalarLiteral)
                .build();
    }

    abstract class Builder extends ScalarOperation.Builder<Neq> {
    }

}
