/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.crs.geotools;

import de.ii.xtraplatform.crs.api.BoundingBoxTransformer;
import de.ii.xtraplatform.crs.api.CoordinateTuple;
import de.ii.xtraplatform.crs.api.CoordinateTupleWithPrecision;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import org.geotools.referencing.CRS;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.measure.unit.SI;
import javax.measure.unit.Unit;

/**
 *
 * @author zahnen
 */
public class GeoToolsCrsTransformer extends BoundingBoxTransformer implements CrsTransformer  {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoToolsCrsTransformer.class);

    private final EpsgCrs sourceCrs;
    private final EpsgCrs targetCrs;
    private final boolean isTargetMetric;
    private final MathTransform mathTransform;
    private final double sourceUnitEquivalentInMeters;
    private final double targetUnitEquivalentInMeters;

    GeoToolsCrsTransformer(CoordinateReferenceSystem sourceCrs, CoordinateReferenceSystem targetCrs, EpsgCrs origTargetCrs) throws FactoryException {
        this.sourceCrs = new EpsgCrs(sourceCrs.getIdentifiers().iterator().next().toString());
        this.targetCrs = origTargetCrs;
        this.mathTransform = CRS.findMathTransform(sourceCrs, targetCrs, true);

        Unit sourceUnit = CRS.getHorizontalCRS(sourceCrs).getCoordinateSystem().getAxis(0).getUnit();
        Unit targetUnit = CRS.getHorizontalCRS(targetCrs).getCoordinateSystem().getAxis(0).getUnit();
        boolean isSourceMetric = sourceUnit == SI.METER;
        this.isTargetMetric = targetUnit == SI.METER;//targetCrs instanceof ProjectedCRS;
        this.sourceUnitEquivalentInMeters = isSourceMetric ? 1 : (Math.PI/180.00) * CRS.getEllipsoid(sourceCrs).getSemiMajorAxis();
        this.targetUnitEquivalentInMeters = isTargetMetric ? 1 : (Math.PI/180.00) * CRS.getEllipsoid(targetCrs).getSemiMajorAxis();
    }

    @Override
    public EpsgCrs getSourceCrs() {
        return sourceCrs;
    }

    @Override
    public EpsgCrs getTargetCrs() {
        return targetCrs;
    }

    @Override
    public boolean isTargetMetric() {
        return isTargetMetric;
    }

    @Override
    public CoordinateTuple transform(double x, double y) {
        return transform(new CoordinateTuple(x, y));
    }

    @Override
    public CoordinateTuple transform(CoordinateTuple coordinateTuple) {
        return new CoordinateTupleWithPrecision(transform(coordinateTuple.asArray(), 1), isTargetMetric);
    }

    @Override
    public double[] transform(double[] coordinates, int numberOfPoints) {
        try {
            double[] target = new double[2* numberOfPoints];
            mathTransform.transform(coordinates, 0, target, 0, numberOfPoints);

            return target;
        } catch (MismatchedDimensionException | TransformException ex) {
            LOGGER.error("GeoTools error", ex);
        }

        return null;
    }

    @Override
    public double getSourceUnitEquivalentInMeters() {
        return sourceUnitEquivalentInMeters;
    }

    @Override
    public double getTargetUnitEquivalentInMeters() {
        return targetUnitEquivalentInMeters;
    }
}