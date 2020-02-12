package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
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

    @Override
    default String toCqlText() {
        //TODO: remove after visitor upgrade
        if (getValue().isPresent() && getValue().get().getValue() instanceof Geometry.Envelope) {
            Geometry.Envelope envelope = (Geometry.Envelope) getValue().get().getValue();
            return String.format(Locale.US, "BBOX(%s, %f, %f, %f, %f, '%s')", getProperty().get().toCqlText(), envelope.getCoordinates().get(0), envelope.getCoordinates().get(1), envelope.getCoordinates().get(2), envelope.getCoordinates().get(3), envelope.getCrs().get().toSimpleString());
        }

        return SpatialOperation.super.toCqlText("INTERSECTS");
    }
}
