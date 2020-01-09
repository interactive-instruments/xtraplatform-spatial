package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Geometry.Polygon.class, name = "Polygon")
})
public interface Geometry<T> extends CqlNode {

    //TODO: implement all geometry types

    enum Type {Point, LineString, Polygon, MultiPoint, MultiLineString, MultiPolygon}

    Type getType();

    List<T> getCoordinates();

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

        @Override
        default String toCqlText() {
            return String.format("POLYGON%s", getCoordinates().stream()
                                                              .flatMap(l -> Stream.of(l.stream()
                                                                                       .flatMap(c -> Stream.of(c.toCqlText()))
                                                                                       .collect(Collectors.joining(",", "(", ")"))))
                                                              .collect(Collectors.joining(",", "(", ")")));
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
    }

}
