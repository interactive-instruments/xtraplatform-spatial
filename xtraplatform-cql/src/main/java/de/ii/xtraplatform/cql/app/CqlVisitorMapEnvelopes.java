/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.app;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.cql.domain.CqlNode;
import de.ii.xtraplatform.cql.domain.Geometry;
import de.ii.xtraplatform.cql.domain.ImmutableMultiPolygon;
import de.ii.xtraplatform.cql.domain.ImmutablePolygon;
import de.ii.xtraplatform.cql.domain.SpatialLiteral;
import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;

import java.util.List;
import java.util.Objects;

public class CqlVisitorMapEnvelopes extends CqlVisitorCopy {

    private final CrsInfo crsInfo;

    public CqlVisitorMapEnvelopes(CrsInfo crsInfo) {
        this.crsInfo = crsInfo;
    }

    @Override
    public CqlNode visit(SpatialLiteral spatialLiteral, List<CqlNode> children) {
        if (spatialLiteral.getType() == Geometry.class && spatialLiteral.getValue() instanceof Geometry.Envelope)
            return SpatialLiteral.of((Geometry<?>) visit((Geometry.Envelope) spatialLiteral.getValue(), children));

        return super.visit(spatialLiteral, children);
    }

    @Override
    public CqlNode visit(Geometry.Envelope envelope, List<CqlNode> children) {
        List<Double> c = envelope.getCoordinates();

        // if the bbox crosses the antimeridian, we create a MultiPolygon with a polygon
        // on each side of the antimeridian
        EpsgCrs crs = envelope.getCrs().orElse(OgcCrs.CRS84);
        if (Objects.nonNull(crsInfo)) {
            int axisWithWraparaound = crsInfo.getAxisWithWraparound(crs).orElse(-1);
            if (axisWithWraparaound==0 && c.get(0)>c.get(2)) {
                // x axis is longitude
                List<Geometry.Coordinate> coordinates1 = ImmutableList.of(
                    Geometry.Coordinate.of(c.get(0), c.get(1)),
                    Geometry.Coordinate.of(180.0, c.get(1)),
                    Geometry.Coordinate.of(180.0, c.get(3)),
                    Geometry.Coordinate.of(c.get(0), c.get(3)),
                    Geometry.Coordinate.of(c.get(0), c.get(1))
                );
                List<Geometry.Coordinate> coordinates2 = ImmutableList.of(
                    Geometry.Coordinate.of(-180, c.get(1)),
                    Geometry.Coordinate.of(c.get(2), c.get(1)),
                    Geometry.Coordinate.of(c.get(2), c.get(3)),
                    Geometry.Coordinate.of(-180, c.get(3)),
                    Geometry.Coordinate.of(-180, c.get(1))
                );
                return createMultiPolygon(coordinates1, coordinates2, crs);
            } else if (axisWithWraparaound==1 && c.get(1)>c.get(3)) {
                // y axis is longitude
                List<Geometry.Coordinate> coordinates1 = ImmutableList.of(
                    Geometry.Coordinate.of(c.get(0), c.get(1)),
                    Geometry.Coordinate.of(c.get(2), c.get(1)),
                    Geometry.Coordinate.of(c.get(2), 180),
                    Geometry.Coordinate.of(c.get(0), 180),
                    Geometry.Coordinate.of(c.get(0), c.get(1))
                );
                List<Geometry.Coordinate> coordinates2 = ImmutableList.of(
                    Geometry.Coordinate.of(c.get(0), -180),
                    Geometry.Coordinate.of(c.get(2), -180),
                    Geometry.Coordinate.of(c.get(2), c.get(3)),
                    Geometry.Coordinate.of(c.get(0), c.get(3)),
                    Geometry.Coordinate.of(c.get(0), -180)
                );
                return createMultiPolygon(coordinates1, coordinates2, crs);
            }
        }

        // standard case, nothing to do
        return super.visit(envelope, children);
    }

    private Geometry.MultiPolygon createMultiPolygon(List<Geometry.Coordinate> coordinates1, List<Geometry.Coordinate> coordinates2, EpsgCrs crs) {
        Geometry.Polygon polygon1 = new ImmutablePolygon.Builder().addCoordinates(coordinates1)
            .crs(crs)
            .build();
        Geometry.Polygon polygon2 = new ImmutablePolygon.Builder().addCoordinates(coordinates2)
            .crs(crs)
            .build();
        return new ImmutableMultiPolygon.Builder().addCoordinates(polygon1, polygon2)
            .crs(crs)
            .build();
    }
}
