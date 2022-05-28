/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.primitives.Doubles;
import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.cql.domain.CqlNode;
import de.ii.xtraplatform.cql.domain.Geometry;
import de.ii.xtraplatform.cql.domain.SpatialLiteral;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CoordinateTuple;
import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CqlCoordinateChecker extends CqlVisitorBase<Object> {

    private static final List<String> AXES = ImmutableList.of("first", "second", "third");

    private final CrsInfo crsInfo;
    private final EpsgCrs filterCrs;
    private final Optional<EpsgCrs> nativeCrs;
    private final Optional<CrsTransformer> crsTransformerFilterToNative;
    private final Optional<CrsTransformer> crsTransformerFilterToCrs84;
    private final Optional<BoundingBox> domainOfValidityNative;
    private final Optional<BoundingBox> domainOfValidityFilter;

    public CqlCoordinateChecker(CrsTransformerFactory crsTransformerFactory, CrsInfo crsInfo, EpsgCrs filterCrs, EpsgCrs nativeCrs) {
        this.crsInfo = crsInfo;
        this.filterCrs = filterCrs;
        this.nativeCrs = Optional.ofNullable(nativeCrs);
        this.crsTransformerFilterToNative = this.nativeCrs.isPresent()
            ? crsTransformerFactory.getTransformer(filterCrs, nativeCrs, true)
            : Optional.empty();
        this.crsTransformerFilterToCrs84 = crsTransformerFactory.getTransformer(filterCrs, OgcCrs.CRS84, true);
        this.domainOfValidityFilter = crsInfo.getDomainOfValidity(filterCrs);
        this.domainOfValidityNative = this.nativeCrs.isPresent()
            ? crsInfo.getDomainOfValidity(nativeCrs)
            : Optional.empty();
    }

    @Override
    public Object visit(SpatialLiteral spatialLiteral, List<Object> children) {
        return ((CqlNode) spatialLiteral.getValue()).accept(this);
    }

    @Override
    public Object visit(Geometry.Point point, List<Object> children) {
        point.getCoordinates()
            .forEach(coordinate -> coordinate.accept(this));
        return null;
    }

    @Override
    public Object visit(Geometry.LineString lineString, List<Object> children) {
        lineString.getCoordinates()
            .forEach(coordinate -> coordinate.accept(this));
        return null;
    }

    @Override
    public Object visit(Geometry.Polygon polygon, List<Object> children) {
        polygon.getCoordinates().stream().flatMap(List::stream)
            .forEach(coordinate -> coordinate.accept(this));
        return null;
    }

    @Override
    public Object visit(Geometry.MultiPoint multiPoint, List<Object> children) {
        multiPoint.getCoordinates().stream()
            .forEach(coordinate -> coordinate.accept(this));
        return null;
    }

    @Override
    public Object visit(Geometry.MultiLineString multiLineString, List<Object> children) {
        multiLineString.getCoordinates().stream().map(Geometry::getCoordinates).flatMap(List::stream)
            .forEach(coordinate -> coordinate.accept(this));
        return null;
    }

    @Override
    public Object visit(Geometry.MultiPolygon multiPolygon, List<Object> children) {
        multiPolygon.getCoordinates().stream().map(Geometry::getCoordinates).flatMap(List::stream).flatMap(List::stream)
            .forEach(coordinate -> coordinate.accept(this));
        return null;
    }

    @Override
    public Object visit(Geometry.Envelope envelope, List<Object> children) {
        List<Double> doubles = envelope.getCoordinates();
        Geometry.Coordinate ll;
        Geometry.Coordinate ur;
        if (doubles.size()==4) {
            ll = Geometry.Coordinate.of(doubles.get(0), doubles.get(1));
            ur = Geometry.Coordinate.of(doubles.get(2), doubles.get(3));
        } else {
            ll = Geometry.Coordinate.of(doubles.get(0), doubles.get(1), doubles.get(2));
            ur = Geometry.Coordinate.of(doubles.get(3), doubles.get(4), doubles.get(5));
        }
        visit(ll, ImmutableList.of());
        visit(ur, ImmutableList.of());

        int axisWithWraparound = crsInfo.getAxisWithWraparound(filterCrs).orElse(-1);
        IntStream.range(0, 2)
            .forEach(axis -> {
                if (axisWithWraparound!=axis && ll.get(axis) > ur.get(axis))
                    throw new IllegalArgumentException(String.format("The coordinates of the bounding box [[ %s ], [ %s ]] do not form a valid bounding box for coordinate reference system '%s'. The first value is larger than the second value for the %s axis.", getCoordinatesAsString(ll), getCoordinatesAsString(ur), getCrsText(filterCrs), AXES.get(axis)));
            });

        return null;
    }

    @Override
    public Object visit(Geometry.Coordinate coordinate, List<Object> children) {
        checkCoordinate(coordinate, filterCrs);

        crsTransformerFilterToNative.ifPresent(t -> {
            CoordinateTuple transformed = t.transform(coordinate.get(0), coordinate.get(1));
            if (Objects.isNull(transformed))
                throw new IllegalArgumentException(String.format("Filter is invalid. Coordinate '%s' cannot be transformed to %s.", getCoordinatesAsString(coordinate), getCrsText(nativeCrs.get())));
            checkCoordinate(Geometry.Coordinate.of(transformed.getX(), transformed.getY()), nativeCrs.get());
        });

        Geometry.Coordinate coordinateCrs84 = coordinate;
        if (crsTransformerFilterToCrs84.isPresent()) {
            CoordinateTuple transformed = crsTransformerFilterToCrs84.get().transform(coordinate.get(0), coordinate.get(1));
            if (Objects.nonNull(transformed)) {
                coordinateCrs84 = Geometry.Coordinate.of(transformed.getX(), transformed.getY());
            }
        }
        checkDomainOfValidity(coordinateCrs84, domainOfValidityFilter, getCrsText(filterCrs));
        if (nativeCrs.isPresent())
            checkDomainOfValidity(coordinateCrs84, domainOfValidityNative, getCrsText(nativeCrs.get()));

        return null;
    }

    private void checkCoordinate(Geometry.Coordinate coordinate, EpsgCrs crs) {
        // check each axis against the constraints specified in the CRS definition
        List<Optional<Double>> minimums = crsInfo.getAxisMinimums(crs);
        List<Optional<Double>> maximums = crsInfo.getAxisMaximums(crs);
        IntStream.range(0, coordinate.size())
            .forEach(i -> {
                minimums.get(i).ifPresent(min -> {
                    if (coordinate.get(i) < min)
                        throw new IllegalArgumentException(String.format("The coordinate '%s' in the filter expression is invalid for %s. The value of the %s axis is smaller than the minimum value for the axis: %f.", getCoordinatesAsString(coordinate), getCrsText(crs), AXES.get(i), min));
                });
                maximums.get(i).ifPresent(max -> {
                    if (coordinate.get(i) > max)
                        throw new IllegalArgumentException(String.format("The coordinate '%s' in the filter expression is invalid for %s. The value of the %s axis is larger than the maximum value for the axis: %f.", getCoordinatesAsString(coordinate), getCrsText(crs), AXES.get(i), max));
                });
            });
    }

    private void checkDomainOfValidity(Geometry.Coordinate coordinate, Optional<BoundingBox> domainOfValidity, String crsText) {
        // validate against the domain of validity of the CRS
        domainOfValidity.ifPresent(bboxCrs84 -> {
            if (coordinate.get(0) < bboxCrs84.getXmin() || coordinate.get(0) > bboxCrs84.getXmax() ||
                coordinate.get(1) < bboxCrs84.getYmin() || coordinate.get(1) > bboxCrs84.getYmax()) {
                throw new IllegalArgumentException(String.format("A coordinate in the filter expression is outside the domain of validity of %s. The coordinate converted to WGS 84 longitude/latitude is [ %s ], the domain of validity is [ %s ].", crsText, getCoordinatesAsString(coordinate), getCoordinatesAsString(bboxCrs84)));
            }
        });
    }

    private String getCrsText(EpsgCrs crs) {
        if (crs.equals(filterCrs))
            return String.format("the coordinate reference system '%s' used in the query", crs.toHumanReadableString());
        else if (nativeCrs.isPresent() && crs.equals(nativeCrs.get()))
            return String.format("the native coordinate reference system '%s' of the data", crs.toHumanReadableString());
        return String.format("the coordinate reference system '%s'", crs.toHumanReadableString());
    }

    private String getCoordinatesAsString(Geometry.Coordinate coordinate) {
        return coordinate.stream().map(c -> String.format(Locale.US, "%.2f", c)).collect(Collectors.joining(", "));
    }

    private String getCoordinatesAsString(BoundingBox bbox) {
        return Arrays.stream(bbox.toArray()).mapToObj(c -> String.format(Locale.US, "%.2f", c)).collect(Collectors.joining(", "));
    }
}
