package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;
import org.threeten.extra.Interval;

import java.time.Instant;

@Value.Immutable
@JsonDeserialize(builder = ImmutableTEquals.Builder.class)
public interface TEquals extends TemporalOperation, CqlNode {

    static TEquals of(String property, Instant instant) {
        return new ImmutableTEquals.Builder().property(property).value(TemporalLiteral.of(instant)).build();
    }

    abstract class Builder extends TemporalOperation.Builder<TEquals> {
    }

    @Override
    default String toCqlText() {
        return TemporalOperation.super.toCqlText("TEQUALS");
    }

}
