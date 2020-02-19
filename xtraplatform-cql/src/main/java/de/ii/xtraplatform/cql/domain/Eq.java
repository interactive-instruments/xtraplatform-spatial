package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableEq.Builder.class)
public interface Eq extends ScalarOperation, CqlNode {

    static Eq of(String property, ScalarLiteral scalarLiteral) {
        return new ImmutableEq.Builder().property(property)
                                        .value(scalarLiteral)
                                        .build();
    }

    abstract class Builder extends ScalarOperation.Builder<Eq> {
    }

}
