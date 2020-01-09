package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableIntersects.Builder.class)
public interface Intersects extends SpatialOperation, CqlNode {

    abstract class Builder extends SpatialOperation.Builder<Intersects> {
    }

    @Override
    default String toCqlText() {
        return SpatialOperation.super.toCqlText("INTERSECTS");
    }
}
