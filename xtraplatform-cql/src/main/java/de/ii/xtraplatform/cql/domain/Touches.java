package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableTouches.Builder.class)
public interface Touches extends SpatialOperation, CqlNode {

    abstract class Builder extends SpatialOperation.Builder<Touches> {
    }

    @Override
    default String toCqlText() {
        return SpatialOperation.super.toCqlText("TOUCHES");
    }
}
