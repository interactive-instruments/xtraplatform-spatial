package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import de.ii.xtraplatform.cql.infra.ObjectVisitor;
import org.immutables.value.Value;

import java.util.ArrayList;
import java.util.List;
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

        @Override
        default String toCqlText() {
            return String.format("POINT(%s)", getCoordinates().get(0).toCqlText());
        }

        @Override
        default <T> T accept(ObjectVisitor<T> visitor) {
            return visitor.visit(this);
        }

    }

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableLineString.Builder.class)
    interface LineString extends Geometry<Coordinate> {

        @Override
        default Type getType() {
            return Type.LineString;
        }

        @Override
        default String toCqlText() {
            return String.format("LINESTRING%s", getCoordinates().stream()
                    .map(Coordinate::toCqlText)
                    .collect(Collectors.joining(",", "(", ")")));
        }

        @Override
        default <T> T accept(ObjectVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    @Value.Immutable
    @JsonDeserialize(builder = ImmutablePolygon.Builder.class)
    interface Polygon extends Geometry<List<Coordinate>> {

        @Override
        default Type getType() {
            return Type.Polygon;
        }

        @Override
        default String toCqlText() {
            return String.format("POLYGON%s", getCoordinates().stream()
                                                              .flatMap(l -> Stream.of(l.stream()
                                                                                       .flatMap(c -> Stream.of(c.toCqlText()))
                                                                                       .collect(Collectors.joining(",", "(", ")"))))
                                                              .collect(Collectors.joining(",", "(", ")")));
        }

        @Override
        default <T> T accept(ObjectVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableMultiPoint.Builder.class)
    interface MultiPoint extends Geometry<Point> {

        @Override
        default Type getType() {
            return Type.MultiPoint;
        }

        @Override
        default String toCqlText() {
            return String.format("MULTIPOINT%s", getCoordinates().stream()
                    .flatMap(point -> point.getCoordinates().stream())
                    .map(Coordinate::toCqlText)
                    .collect(Collectors.joining(",", "(", ")")));
        }

        @Override
        default <T> T accept(ObjectVisitor<T> visitor) {
            return visitor.visit(this);
        }

    }

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableMultiLineString.Builder.class)
    interface MultiLineString extends Geometry<LineString> {

        @Override
        default Type getType() {
            return Type.MultiLineString;
        }

        @Override
        default String toCqlText() {
            return String.format("MULTILINESTRING%s", getCoordinates().stream()
                    .flatMap(ls -> Stream.of(ls.getCoordinates()
                            .stream()
                            .map(Coordinate::toCqlText)
                            .collect(Collectors.joining(",", "(", ")"))))
                    .collect(Collectors.joining(",", "(", ")")));
        }

        @Override
        default <T> T accept(ObjectVisitor<T> visitor) {
            return visitor.visit(this);
        }

    }

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableMultiPolygon.Builder.class)
    interface MultiPolygon extends Geometry<Polygon> {

        @Override
        default Type getType() {
            return Type.MultiPolygon;
        }

        @Override
        default String toCqlText() {
            return String.format("MULTIPOLYGON%s", getCoordinates().stream()
                    .flatMap(p -> Stream.of(p.getCoordinates().stream()
                            .flatMap(l -> Stream.of(l.stream()
                                    .flatMap(c -> Stream.of(c.toCqlText()))
                                    .collect(Collectors.joining(",", "(", ")"))))
                            .collect(Collectors.joining(",", "(", ")"))))
                    .collect(Collectors.joining(",", "(", ")")));
        }

        @Override
        default <T> T accept(ObjectVisitor<T> visitor) {
            return visitor.visit(this);
        }

    }

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableEnvelope.Builder.class)
    interface Envelope extends Geometry<Double> {

        @JsonIgnore
        @Override
        default Type getType() {
            return Type.Envelope;
        }

        @JsonProperty("bbox")
        @Override
        List<Double> getCoordinates();

        @Override
        default String toCqlText() {
            return String.format("ENVELOPE%s", getCoordinates().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",", "(", ")")));
        }

        @Override
        default <T> T accept(ObjectVisitor<T> visitor) {
            return visitor.visit(this);
        }
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

        @Override
        public String toCqlText() {
            return stream().map(Object::toString).collect(Collectors.joining(" "));
        }

        @Override
        public <T> T accept(ObjectVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

}
