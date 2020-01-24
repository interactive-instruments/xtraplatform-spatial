package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableBefore.Builder.class)
public interface Before extends TemporalOperation, CqlNode {

    abstract class Builder extends TemporalOperation.Builder<Before> {
    }

    @Override
    default String toCqlText() {
        return TemporalOperation.super.toCqlText("BEFORE");
    }

}
