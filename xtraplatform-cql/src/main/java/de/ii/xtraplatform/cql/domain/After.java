package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableAfter.Builder.class)
public interface After extends TemporalOperation, CqlNode {

    abstract class Builder extends TemporalOperation.Builder<After> {
    }

    @Override
    default String toCqlText() {
        return TemporalOperation.super.toCqlText("AFTER");
    }

}
