package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableIntersects.Builder.class)
public interface Intersects extends SpatialOperation, CqlNode {

    static Intersects of(String property, SpatialLiteral spatialLiteral) {
        return new ImmutableIntersects.Builder().property(property)
                                                .value(spatialLiteral)
                                                .build();
    }

    static Intersects of(String property, BoundingBox boundingBox) {
        return new ImmutableIntersects.Builder().property(property)
                                                .value(SpatialLiteral.of(Geometry.Envelope.of(boundingBox)))
                                                .build();
    }

    abstract class Builder extends SpatialOperation.Builder<Intersects> {
    }

}
