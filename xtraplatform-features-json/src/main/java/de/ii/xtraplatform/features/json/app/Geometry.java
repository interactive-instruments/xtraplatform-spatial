/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.json.app;

import com.google.common.base.Preconditions;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.json.domain.GeoJsonGeometryType;
import org.immutables.value.Value;

import java.util.ArrayList;
import java.util.List;

public interface Geometry<T> {

    GeoJsonGeometryType getType();

    List<T> getCoordinates();

    @Value.Default
    default EpsgCrs getCrs() {
        return OgcCrs.CRS84;
    }

    @Value.Modifiable
    interface Point extends Geometry<String> {

        @Value.Check
        default void check() {
            Preconditions.checkState(getCoordinates().size() == 1, "a point must have only one coordinate", getCoordinates().size());
        }

        @Value.Derived
        @Override
        default GeoJsonGeometryType getType() {
            return GeoJsonGeometryType.POINT;
        }

    }

    @Value.Modifiable
    interface LineString extends Geometry<String> {

        @Value.Derived
        @Override
        default GeoJsonGeometryType getType() {
            return GeoJsonGeometryType.LINE_STRING;
        }

    }

    @Value.Modifiable
    interface Polygon extends Geometry<List<String>> {

        @Value.Derived
        @Override
        default GeoJsonGeometryType getType() {
            return GeoJsonGeometryType.POLYGON;
        }

    }

    @Value.Modifiable
    interface MultiPoint extends Geometry<Point> {

        @Value.Derived
        @Override
        default GeoJsonGeometryType getType() {
            return GeoJsonGeometryType.MULTI_POINT;
        }

    }

    @Value.Modifiable
    interface MultiLineString extends Geometry<LineString> {

        @Value.Derived
        @Override
        default GeoJsonGeometryType getType() {
            return GeoJsonGeometryType.MULTI_LINE_STRING;
        }

    }

    @Value.Modifiable
    interface MultiPolygon extends Geometry<Polygon> {

        @Value.Derived
        @Override
        default GeoJsonGeometryType getType() {
            return GeoJsonGeometryType.MULTI_POLYGON;
        }

    }

    class Coordinate extends ArrayList<Double> {

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
