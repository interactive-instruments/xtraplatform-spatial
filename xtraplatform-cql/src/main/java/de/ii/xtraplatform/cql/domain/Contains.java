package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableContains.Builder.class)
public interface Contains extends SpatialOperation, CqlNode {

    static Contains of(String property, SpatialLiteral spatialLiteral) {
        return new ImmutableContains.Builder()
                .property(property)
                .value(spatialLiteral)
                .build();
    }

    abstract class Builder extends SpatialOperation.Builder<Contains> {
    }
}
