package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableTEquals.Builder.class)
public interface TEquals extends TemporalOperation, CqlNode {

    abstract class Builder extends TemporalOperation.Builder<TEquals> {
    }

    @Override
    default String toCqlText() {
        return TemporalOperation.super.toCqlText("TEQUALS");
    }

}
