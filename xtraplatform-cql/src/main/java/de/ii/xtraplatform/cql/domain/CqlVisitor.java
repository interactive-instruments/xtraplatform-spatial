/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import java.util.List;

public interface CqlVisitor<T> {

    default T visit(CqlNode node, List<T> children) {
        if (node instanceof CqlFilter) {
            return visit((CqlFilter) node, children);
        } else if (node instanceof CqlPredicate) {
            return visit((CqlPredicate) node, children);
        } else if (node instanceof LogicalOperation) {
            return visit((LogicalOperation) node, children);
        } else if (node instanceof Like) {
            return visit((Like) node, children);
        } else if (node instanceof ScalarOperation) {
            return visit((ScalarOperation) node, children);
        } else if (node instanceof TemporalOperation) {
            return visit((TemporalOperation) node, children);
        } else if (node instanceof SpatialOperation) {
            return visit((SpatialOperation) node, children);
        } else if (node instanceof ArrayOperation) {
            return visit((ArrayOperation) node, children);
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
        }

        throw new IllegalStateException();
    }

    T visit(CqlFilter cqlFilter, List<T> children);

    T visit(CqlPredicate cqlPredicate, List<T> children);

    T visit(LogicalOperation logicalOperation, List<T> children);

    T visit(ScalarOperation scalarOperation, List<T> children);

    T visit(TemporalOperation temporalOperation, List<T> children);

    T visit(SpatialOperation spatialOperation, List<T> children);

    T visit(ArrayOperation arrayOperation, List<T> children);

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

    T visit(Like like, List<T> children);
}
