/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.fasterxml.jackson.annotation.JacksonInject;
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

    @JsonIgnore
    @JacksonInject("filterCrs")
    Optional<EpsgCrs> getCrs();

    @Value.Immutable
    @JsonDeserialize(builder = ImmutablePoint.Builder.class)
    interface Point extends Geometry<Coordinate> {

        static Point of(double x, double y) {
            return new ImmutablePoint.Builder().addCoordinates(Coordinate.of(x, y))
                                               .build();
        }

        static Point of(Coordinate coordinate) {
            return new ImmutablePoint.Builder().addCoordinates(coordinate)
                                               .build();
        }

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

        static LineString of(Coordinate... coordinates) {
            return new ImmutableLineString.Builder().addCoordinates(coordinates)
                                                    .build();
        }

        @Override
        default Type getType() {
            return Type.LineString;
        }

    }

    @Value.Immutable
    @JsonDeserialize(builder = ImmutablePolygon.Builder.class)
    interface Polygon extends Geometry<List<Coordinate>> {

        static Polygon of(List<Coordinate>... coordinates) {
            return new ImmutablePolygon.Builder().addCoordinates(coordinates)
                                                 .build();
        }

        static Polygon of(EpsgCrs crs, List<Coordinate>... coordinates) {
            return new ImmutablePolygon.Builder().crs(crs)
                                                 .addCoordinates(coordinates)
                                                 .build();
        }

        @Override
        default Type getType() {
            return Type.Polygon;
        }

    }

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableMultiPoint.Builder.class)
    interface MultiPoint extends Geometry<Point> {

        static MultiPoint of(Point... points) {
            return new ImmutableMultiPoint.Builder().addCoordinates(points)
                                                    .build();
        }

        @Override
        default Type getType() {
            return Type.MultiPoint;
        }

    }

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableMultiLineString.Builder.class)
    interface MultiLineString extends Geometry<LineString> {

        static MultiLineString of(LineString... lineStrings) {
            return new ImmutableMultiLineString.Builder().addCoordinates(lineStrings)
                                                         .build();
        }

        @Override
        default Type getType() {
            return Type.MultiLineString;
        }

    }

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableMultiPolygon.Builder.class)
    interface MultiPolygon extends Geometry<Polygon> {

        static MultiPolygon of(Polygon... polygons) {
            return new ImmutableMultiPolygon.Builder().addCoordinates(polygons)
                                                      .build();
        }

        @Override
        default Type getType() {
            return Type.MultiPolygon;
        }

    }

    @Value.Immutable
    @JsonDeserialize(builder = ImmutableEnvelope.Builder.class)
    interface Envelope extends Geometry<Double> {

        static Envelope of(BoundingBox boundingBox) {
            return new ImmutableEnvelope.Builder().addCoordinates(boundingBox.getXmin(), boundingBox.getYmin(), boundingBox.getXmax(), boundingBox.getYmax())
                                                  .crs(boundingBox.getEpsgCrs())
                                                  .build();
        }

        static Envelope of(double xmin, double ymin, double xmax, double ymax) {
            return new ImmutableEnvelope.Builder().addCoordinates(xmin, ymin, xmax, ymax)
                                                  .build();
        }

        static Envelope of(double xmin, double ymin, double xmax, double ymax, EpsgCrs crs) {
            return new ImmutableEnvelope.Builder().addCoordinates(xmin, ymin, xmax, ymax)
                                                  .crs(crs)
                                                  .build();
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

        public static Coordinate of(double x, double y) {
            return new Coordinate(x, y);
        }

        static Coordinate of(double x, double y, double z) {
            return new Coordinate(x, y, z);
        }

        public Coordinate(Double x, Double y) {
            super();
            add(x);
            add(y);
        }

        public Coordinate(Double x, Double y, Double z) {
            this(x, y);
            add(z);
        }

        public Coordinate() {
            super();
        }

    }

}
