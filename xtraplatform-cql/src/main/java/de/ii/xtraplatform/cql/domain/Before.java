package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.time.Instant;

@Value.Immutable
@JsonDeserialize(builder = ImmutableBefore.Builder.class)
public interface Before extends TemporalOperation, CqlNode {

    static Before of(String property, Instant instant) {
        return new ImmutableBefore.Builder().property(property).value(TemporalLiteral.of(instant)).build();
    }

    abstract class Builder extends TemporalOperation.Builder<Before> {
    }

    @Override
    default String toCqlText() {
        return TemporalOperation.super.toCqlText("BEFORE");
    }

}
