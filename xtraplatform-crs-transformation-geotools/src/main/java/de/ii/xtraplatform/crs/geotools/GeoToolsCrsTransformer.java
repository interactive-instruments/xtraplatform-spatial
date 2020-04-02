/**
 * Copyright 2020 interactive instruments GmbH
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
import org.opengis.referencing.cs.AxisDirection;
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
    private final boolean needsAxisSwap;

    GeoToolsCrsTransformer(CoordinateReferenceSystem sourceCrs, CoordinateReferenceSystem targetCrs,
                           EpsgCrs origSourceCrs, EpsgCrs origTargetCrs) throws FactoryException {
        this.sourceCrs = new EpsgCrs(sourceCrs.getIdentifiers().iterator().next().toString());
        this.targetCrs = origTargetCrs;
        this.mathTransform = CRS.findMathTransform(sourceCrs, targetCrs, true);

        Unit sourceUnit = CRS.getHorizontalCRS(sourceCrs).getCoordinateSystem().getAxis(0).getUnit();
        Unit targetUnit = CRS.getHorizontalCRS(targetCrs).getCoordinateSystem().getAxis(0).getUnit();
        boolean isSourceMetric = sourceUnit == SI.METER;
        this.isTargetMetric = targetUnit == SI.METER;//targetCrs instanceof ProjectedCRS;
        this.sourceUnitEquivalentInMeters = isSourceMetric ? 1 : (Math.PI/180.00) * CRS.getEllipsoid(sourceCrs).getSemiMajorAxis();
        this.targetUnitEquivalentInMeters = isTargetMetric ? 1 : (Math.PI/180.00) * CRS.getEllipsoid(targetCrs).getSemiMajorAxis();

        AxisDirection sourceDirection = sourceCrs.getCoordinateSystem()
                                           .getAxis(0)
                                           .getDirection();
        AxisDirection sourceOrigDirection = CRS.decode(this.sourceCrs.getAsSimple()).getCoordinateSystem()
                                                 .getAxis(0)
                                                 .getDirection();
        AxisDirection targetDirection = targetCrs.getCoordinateSystem()
                                                 .getAxis(0)
                                                 .getDirection();

        AxisDirection targetOrigDirection = CRS.decode(new EpsgCrs(targetCrs.getIdentifiers().iterator().next().toString()).getAsSimple()).getCoordinateSystem()
                                               .getAxis(0)
                                               .getDirection();
        boolean sourceNeedsAxisSwap = origSourceCrs.isForceLongitudeFirst() && sourceDirection == sourceOrigDirection;
        boolean targetNeedsAxisSwap = origTargetCrs.isForceLongitudeFirst() && targetDirection == targetOrigDirection;
        this.needsAxisSwap = sourceNeedsAxisSwap != targetNeedsAxisSwap;
        //LOGGER.debug("AXIS SWAP: {} {} {} {}, {} {} {}", needsAxisSwap, origSourceCrs.getCode(), sourceNeedsAxisSwap, sourceDirection, origTargetCrs.getCode(), targetNeedsAxisSwap, targetDirection);
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
        CoordinateTuple transformed = transform(new CoordinateTuple(x, y), false);
        //return transformed;
        return needsAxisSwap ? new CoordinateTuple(transformed.getY(), transformed.getX()) : transformed;
    }

    @Override
    public CoordinateTuple transform(CoordinateTuple coordinateTuple, boolean swap) {
        return new CoordinateTupleWithPrecision(transform(coordinateTuple.asArray(), 1, swap && needsAxisSwap), isTargetMetric);
    }

    @Override
    public double[] transform(double[] coordinates, int numberOfPoints, boolean swap) {
        try {
            double[] source = coordinates;
            if (swap && this.needsAxisSwap) {
                source = new double[numberOfPoints * 2];
                for (int i = 0; i < numberOfPoints * 2; i += 2) {
                    source[i] = coordinates[i + 1];
                    source[i + 1] = coordinates[i];
                }
            }
            double[] target = new double[2* numberOfPoints];
            mathTransform.transform(source, 0, target, 0, numberOfPoints);

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

    @Override
    public boolean needsCoordinateSwap() {
        return needsAxisSwap;
    }
}