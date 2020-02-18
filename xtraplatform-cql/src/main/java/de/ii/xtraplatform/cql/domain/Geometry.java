package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.immutables.value.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", defaultImpl = Geometry.Envelope.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Geometry.Point.class, name = "Point"),
        @JsonSubTypes.Type(value = Geometry.LineString.class, name = "LineString"),
        @JsonSubTypes.Type(value = Geometry.Polygon.class, name = "Polygon"),
        @JsonSubTypes.Type(value = Geometry.MultiPoint.class, name = "MultiPoint"),
        @JsonSubTypes.Type(value = Geometry.MultiLineString.class, name = "MultiLineString"),
        @JsonSubTypes.Type(value = Geometry.MultiPolygon.class, name = "MultiPolygon")
})
public interface Geometry<T> extends CqlNode {

    enum Type {Point, LineString, Polygon, MultiPoint, MultiLineString, MultiPolygon, Envelope}

    Type getType();

    List<T> getCoordinates();

    Optional<EpsgCrs> getCrs();

    @Value.Immutable
    @JsonDeserialize(builder = ImmutablePoint.Builder.class)
    interface Point extends Geometry<Coordinate> {

        @Value.Check
        default void check() {
            Preconditions.checkState(getCoordinates().size() == 1, "a point must have only one coordinate", getCoordinates().size());
        }

        @Override
        default Type getType() {
            return Type.Point;
        }

    }

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableLineString.Builder.class)
    interface LineString extends Geometry<Coordinate> {

        @Override
        default Type getType() {
            return Type.LineString;
        }

    }

    @Value.Immutable
    @JsonDeserialize(builder = ImmutablePolygon.Builder.class)
    interface Polygon extends Geometry<List<Coordinate>> {

        @Override
        default Type getType() {
            return Type.Polygon;
        }

    }

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableMultiPoint.Builder.class)
    interface MultiPoint extends Geometry<Point> {

        @Override
        default Type getType() {
            return Type.MultiPoint;
        }

    }

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableMultiLineString.Builder.class)
    interface MultiLineString extends Geometry<LineString> {

        @Override
        default Type getType() {
            return Type.MultiLineString;
        }

    }

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableMultiPolygon.Builder.class)
    interface MultiPolygon extends Geometry<Polygon> {

        @Override
        default Type getType() {
            return Type.MultiPolygon;
        }

    }

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableEnvelope.Builder.class)
    interface Envelope extends Geometry<Double> {

        static Envelope of(BoundingBox boundingBox) {
            return new ImmutableEnvelope.Builder().addCoordinates(boundingBox.getXmin(), boundingBox.getYmin(), boundingBox.getXmax(), boundingBox.getYmax()).crs(boundingBox.getEpsgCrs()).build();
        }

        @JsonIgnore
        @Override
        default Type getType() {
            return Type.Envelope;
        }

        @JsonProperty("bbox")
        @Override
        List<Double> getCoordinates();

    }

    class Coordinate extends ArrayList<Double> implements CqlNode {
        public Coordinate(Double x, Double y) {
            super();
            add(x);
            add(y);
        }

        public Coordinate(Double x, Double y, Double z) {
            this(x,y);
            add(z);
        }

        public Coordinate() {
            super();
        }

    }

}
