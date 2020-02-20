package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import org.immutables.value.Value;
import org.threeten.extra.Interval;

import java.util.Objects;

@Value.Immutable
@JsonDeserialize(builder = ImmutableDuring.Builder.class)
public interface During extends TemporalOperation, CqlNode {

    static During of(String property, TemporalLiteral temporalLiteral) {
        if (!Objects.equals(temporalLiteral.getType(), Interval.class)) {
            throw new IllegalArgumentException(String.format("not a valid interval: %s", temporalLiteral));
        }

        return new ImmutableDuring.Builder().property(property)
                                            .value(temporalLiteral)
                                            .build();
    }

    abstract class Builder extends TemporalOperation.Builder<During> {
    }

    @Value.Check
    default void check() {
        Preconditions.checkState(getValue().isPresent() && Objects.equals(getValue().get().getType(), Interval.class), "not a valid interval: %s", getValue().get().getValue());
    }

}
