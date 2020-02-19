package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableTEquals.Builder.class)
public interface TEquals extends TemporalOperation, CqlNode {

    static TEquals of(String property, TemporalLiteral temporalLiteral) {
        return new ImmutableTEquals.Builder().property(property)
                                             .value(temporalLiteral)
                                             .build();
    }

    abstract class Builder extends TemporalOperation.Builder<TEquals> {
    }

}
