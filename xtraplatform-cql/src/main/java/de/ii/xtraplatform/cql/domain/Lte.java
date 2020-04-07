package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableLte.Builder.class)
public interface Lte extends ScalarOperation, CqlNode {

    static Lte of(String property, ScalarLiteral scalarLiteral) {
        return new ImmutableLte.Builder().property(property)
                                         .value(scalarLiteral)
                                         .build();
    }

    static Lte of(Property property, ScalarLiteral scalarLiteral) {
        return new ImmutableLte.Builder().property(property)
                .value(scalarLiteral)
                .build();
    }

    static Lte ofFunction(Function function, ScalarLiteral scalarLiteral) {
        return new ImmutableLte.Builder().function(function)
                .value(scalarLiteral)
                .build();
    }

    abstract class Builder extends ScalarOperation.Builder<Lte> {
    }

}
