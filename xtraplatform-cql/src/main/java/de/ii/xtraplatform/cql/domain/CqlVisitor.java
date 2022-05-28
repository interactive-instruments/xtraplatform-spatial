/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import java.util.List;

public interface CqlVisitor<T> {

    default T postProcess(CqlNode node, T t) {
        return t;
    }

    default T visit(CqlNode node, List<T> children) {
        if (node instanceof Not) {
            return visit((Not) node, children);
        } else if (node instanceof LogicalOperation) {
            return visit((LogicalOperation) node, children);
        } else if (node instanceof Between) {
            return visit((Between) node, children);
        } else if (node instanceof Like) {
            return visit((Like) node, children);
        } else if (node instanceof In) {
            return visit((In) node, children);
        } else if (node instanceof IsNull) {
            return visit((IsNull) node, children);
        } else if (node instanceof Casei) {
            return visit((Casei) node, children);
        } else if (node instanceof Accenti) {
            return visit((Accenti) node, children);
        } else if (node instanceof Interval) {
            return visit((Interval) node, children);
        } else if (node instanceof BinaryScalarOperation) {
            return visit((BinaryScalarOperation) node, children);
        } else if (node instanceof BinaryTemporalOperation) {
            return visit((BinaryTemporalOperation) node, children);
        } else if (node instanceof BinarySpatialOperation) {
            return visit((BinarySpatialOperation) node, children);
        } else if (node instanceof BinaryArrayOperation) {
            return visit((BinaryArrayOperation) node, children);
        } else if (node instanceof Property) {
            return visit((Property) node, children);
        } else if (node instanceof ScalarLiteral) {
            return visit((ScalarLiteral) node, children);
        } else if (node instanceof TemporalLiteral) {
            return visit((TemporalLiteral) node, children);
        } else if (node instanceof ArrayLiteral) {
            return visit((ArrayLiteral) node, children);
        } else if (node instanceof Geometry.Coordinate) {
            return visit((Geometry.Coordinate) node, children);
        } else if (node instanceof Geometry.Point) {
            return visit((Geometry.Point) node, children);
        } else if (node instanceof Geometry.LineString) {
            return visit((Geometry.LineString) node, children);
        } else if (node instanceof Geometry.Polygon) {
            return visit((Geometry.Polygon) node, children);
        } else if (node instanceof Geometry.MultiPoint) {
            return visit((Geometry.MultiPoint) node, children);
        } else if (node instanceof Geometry.MultiLineString) {
            return visit((Geometry.MultiLineString) node, children);
        } else if (node instanceof Geometry.MultiPolygon) {
            return visit((Geometry.MultiPolygon) node, children);
        } else if (node instanceof Geometry.Envelope) {
            return visit((Geometry.Envelope) node, children);
        } else if (node instanceof SpatialLiteral) {
            return visit((SpatialLiteral) node, children);
        } else if (node instanceof Function) {
            return visit((Function) node, children);
        } else if (node instanceof BooleanValue2) {
            return visit((BooleanValue2) node, children);
        }

        throw new IllegalStateException();
    }

    T visit(Not not, List<T> children);

    T visit(LogicalOperation logicalOperation, List<T> children);

    T visit(BinaryScalarOperation scalarOperation, List<T> children);

    T visit(Between between, List<T> children);

    T visit(Like like, List<T> children);

    T visit(In in, List<T> children);

    T visit(IsNull isNull, List<T> children);

    T visit(Casei casei, List<T> children);

    T visit(Accenti accenti, List<T> children);

    T visit(Interval interval, List<T> children);

    T visit(BinaryTemporalOperation temporalOperation, List<T> children);

    T visit(BinarySpatialOperation spatialOperation, List<T> children);

    T visit(BinaryArrayOperation arrayOperation, List<T> children);

    T visit(Property property, List<T> children);

    T visit(ScalarLiteral scalarLiteral, List<T> children);

    T visit(TemporalLiteral temporalLiteral, List<T> children);

    T visit(ArrayLiteral arrayLiteral, List<T> children);

    T visit(SpatialLiteral spatialLiteral, List<T> children);

    T visit(Geometry.Coordinate coordinate, List<T> children);

    T visit(Geometry.Point point, List<T> children);

    T visit(Geometry.LineString lineString, List<T> children);

    T visit(Geometry.Polygon polygon, List<T> children);

    T visit(Geometry.MultiPoint multiPoint, List<T> children);

    T visit(Geometry.MultiLineString multiLineString, List<T> children);

    T visit(Geometry.MultiPolygon multiPolygon, List<T> children);

    T visit(Geometry.Envelope envelope, List<T> children);

    T visit(Function function, List<T> children);

    T visit(BooleanValue2 booleanValue, List<T> children);
}
