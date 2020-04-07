package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableTOverlaps.Builder.class)
public interface TOverlaps extends TemporalOperation, CqlNode {

    static TOverlaps of(Temporal temporal1, Temporal temporal2) {
        return new ImmutableTOverlaps.Builder().operand1(temporal1)
                                               .operand2(temporal2)
                                               .build();
    }

    abstract class Builder extends TemporalOperation.Builder<TOverlaps> {
    }

}
