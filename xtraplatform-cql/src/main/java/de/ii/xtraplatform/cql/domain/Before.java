package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableBefore.Builder.class)
public interface Before extends TemporalOperation, CqlNode {

    static Before of(String property, TemporalLiteral temporalLiteral) {
        return new ImmutableBefore.Builder().property(property)
                                            .value(temporalLiteral)
                                            .build();
    }

    abstract class Builder extends TemporalOperation.Builder<Before> {
    }

}
