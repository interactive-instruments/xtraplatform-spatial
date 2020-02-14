package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.cql.infra.ObjectVisitor;
import org.immutables.value.Value;
import org.threeten.extra.Interval;

@Value.Immutable
@JsonDeserialize(builder = ImmutableDuring.Builder.class)
public interface During extends TemporalOperation, CqlNode {

    static During of(String property, Interval interval) {
        return new ImmutableDuring.Builder().property(property).value(TemporalLiteral.of(interval)).build();
    }

    abstract class Builder extends TemporalOperation.Builder<During> {
    }

    @Override
    default String toCqlText() {
        return TemporalOperation.super.toCqlText("DURING");
    }

    @Override
    default <T> T accept(ObjectVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
