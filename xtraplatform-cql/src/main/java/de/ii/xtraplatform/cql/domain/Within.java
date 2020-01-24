package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableWithin.Builder.class)
public interface Within extends SpatialOperation, CqlNode {

    abstract class Builder extends SpatialOperation.Builder<Within> {
    }

    @Override
    default String toCqlText() {
        return SpatialOperation.super.toCqlText("WITHIN");
    }
}
