package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableDuring.Builder.class)
public interface During extends TemporalOperation, CqlNode {

    static During of(String property, TemporalLiteral temporalLiteral) {
        return new ImmutableDuring.Builder().property(property)
                                            .value(temporalLiteral)
                                            .build();
    }

    abstract class Builder extends TemporalOperation.Builder<During> {
    }

}
