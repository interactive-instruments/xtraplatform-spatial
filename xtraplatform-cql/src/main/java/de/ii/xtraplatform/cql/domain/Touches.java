package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableTouches.Builder.class)
public interface Touches extends SpatialOperation, CqlNode {

    static Touches of(String property, SpatialLiteral spatialLiteral) {
        return new ImmutableTouches.Builder().property(property)
                                             .value(spatialLiteral)
                                             .build();
    }

    abstract class Builder extends SpatialOperation.Builder<Touches> {
    }

}
