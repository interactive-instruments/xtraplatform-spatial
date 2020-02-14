package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.cql.infra.ObjectVisitor;
import org.immutables.value.Value;
import org.threeten.extra.Interval;

import java.time.Instant;

@Value.Immutable
@JsonDeserialize(builder = ImmutableAfter.Builder.class)
public interface After extends TemporalOperation, CqlNode {

    static After of(String property, Instant instant) {
        return new ImmutableAfter.Builder().property(property).value(TemporalLiteral.of(instant)).build();
    }

    abstract class Builder extends TemporalOperation.Builder<After> {
    }

    @Override
    default String toCqlText() {
        return TemporalOperation.super.toCqlText("AFTER");
    }

    @Override
    default <T> T accept(ObjectVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
