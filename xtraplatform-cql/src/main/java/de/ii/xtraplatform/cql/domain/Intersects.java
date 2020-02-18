package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import org.immutables.value.Value;

import java.util.Locale;

@Value.Immutable
@JsonDeserialize(builder = ImmutableIntersects.Builder.class)
public interface Intersects extends SpatialOperation, CqlNode {

    static Intersects of(String property, BoundingBox boundingBox) {
        return new ImmutableIntersects.Builder().property(property).value(SpatialLiteral.of(Geometry.Envelope.of(boundingBox))).build();
    }

    abstract class Builder extends SpatialOperation.Builder<Intersects> {
    }

}
