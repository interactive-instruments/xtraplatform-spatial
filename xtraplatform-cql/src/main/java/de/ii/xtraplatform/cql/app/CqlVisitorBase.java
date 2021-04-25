/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.app;

import de.ii.xtraplatform.cql.domain.ArrayLiteral;
import de.ii.xtraplatform.cql.domain.ArrayOperation;
import de.ii.xtraplatform.cql.domain.Between;
import de.ii.xtraplatform.cql.domain.BinaryScalarOperation;
import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.cql.domain.CqlPredicate;
import de.ii.xtraplatform.cql.domain.CqlVisitor;
import de.ii.xtraplatform.cql.domain.Function;
import de.ii.xtraplatform.cql.domain.Geometry;
import de.ii.xtraplatform.cql.domain.In;
import de.ii.xtraplatform.cql.domain.IsNull;
import de.ii.xtraplatform.cql.domain.Like;
import de.ii.xtraplatform.cql.domain.LogicalOperation;
import de.ii.xtraplatform.cql.domain.Property;
import de.ii.xtraplatform.cql.domain.ScalarLiteral;
import de.ii.xtraplatform.cql.domain.SpatialLiteral;
import de.ii.xtraplatform.cql.domain.SpatialOperation;
import de.ii.xtraplatform.cql.domain.TemporalLiteral;
import de.ii.xtraplatform.cql.domain.TemporalOperation;

import java.util.List;

public class CqlVisitorBase<T> implements CqlVisitor<T> {
    @Override
    public T visit(CqlFilter cqlFilter, List<T> children) {
        return null;
    }

    @Override
    public T visit(CqlPredicate cqlPredicate, List<T> children) {
        return null;
    }

    @Override
    public T visit(LogicalOperation logicalOperation, List<T> children) {
        return null;
    }

    @Override
    public T visit(BinaryScalarOperation scalarOperation, List<T> children) { return null; }

    @Override
    public T visit(Between between, List<T> children) { return null; }

    @Override
    public T visit(Like like, List<T> children) { return null; }

    @Override
    public T visit(In in, List<T> children) { return null; }

    @Override
    public T visit(IsNull isNull, List<T> children) { return null; }

    @Override
    public T visit(TemporalOperation temporalOperation, List<T> children) {
        return null;
    }

    @Override
    public T visit(SpatialOperation spatialOperation, List<T> children) {
        return null;
    }

    @Override
    public T visit(ArrayOperation arrayOperation, List<T> children) {
        return null;
    }

    @Override
    public T visit(ScalarLiteral scalarLiteral, List<T> children) {
        return null;
    }

    @Override
    public T visit(TemporalLiteral temporalLiteral, List<T> children) {
        return null;
    }

    @Override
    public T visit(ArrayLiteral arrayLiteral, List<T> children) {
        return null;
    }

    @Override
    public T visit(SpatialLiteral spatialLiteral, List<T> children) {
        return null;
    }

    @Override
    public T visit(Property property, List<T> children) {
        return null;
    }

    @Override
    public T visit(Geometry.Coordinate coordinate, List<T> children) {
        return null;
    }

    @Override
    public T visit(Geometry.Point point, List<T> children) {
        return null;
    }

    @Override
    public T visit(Geometry.LineString lineString, List<T> children) {
        return null;
    }

    @Override
    public T visit(Geometry.Polygon polygon, List<T> children) {
        return null;
    }

    @Override
    public T visit(Geometry.MultiPoint multiPoint, List<T> children) {
        return null;
    }

    @Override
    public T visit(Geometry.MultiLineString multiLineString, List<T> children) {
        return null;
    }

    @Override
    public T visit(Geometry.MultiPolygon multiPolygon, List<T> children) {
        return null;
    }

    @Override
    public T visit(Geometry.Envelope envelope, List<T> children) {
        return null;
    }

    @Override
    public T visit(Function function, List<T> children) {
        return null;
    }

}
