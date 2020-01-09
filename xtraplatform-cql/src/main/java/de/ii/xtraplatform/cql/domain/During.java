package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableDuring.Builder.class)
public interface During extends TemporalOperation, CqlNode {

    abstract class Builder extends TemporalOperation.Builder<During> {
    }

    @Override
    default String toCqlText() {
        return TemporalOperation.super.toCqlText("DURING");
    }

}
