package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableOverlappedBy.Builder.class)
public interface OverlappedBy extends TemporalOperation, CqlNode {

    abstract class Builder extends TemporalOperation.Builder<OverlappedBy> {
    }

    @Override
    default String toCqlText() {
        return TemporalOperation.super.toCqlText("OVERLAPPEDBY");
    }

}
