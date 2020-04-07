package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableCrosses.Builder.class)
public interface Crosses extends SpatialOperation, CqlNode {

    static Crosses of(String property, SpatialLiteral spatialLiteral) {
        return new ImmutableCrosses.Builder().property(property)
                                             .value(spatialLiteral)
                                             .build();
    }

    abstract class Builder extends SpatialOperation.Builder<Crosses> {
    }

}
