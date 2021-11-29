/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.crs.infra;

import de.ii.xtraplatform.crs.domain.BoundingBoxTransformer;
import de.ii.xtraplatform.crs.domain.CoordinateTuple;
import de.ii.xtraplatform.crs.domain.CoordinateTupleWithPrecision;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.runtime.domain.LogContext;
import javax.measure.Unit;
import org.kortforsyningen.proj.Proj;
import org.kortforsyningen.proj.Units;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CompoundCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author zahnen
 */
public class CrsTransformerProj extends BoundingBoxTransformer implements CrsTransformer  {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrsTransformerProj.class);

    private final EpsgCrs sourceCrs;
    private final EpsgCrs targetCrs;
    private final boolean isSourceMetric;
    private final boolean isTargetMetric;
    private final double sourceUnitEquivalentInMeters;
    private final double targetUnitEquivalentInMeters;
    private final boolean needsAxisSwap;
    //TODO
    private final int sourceDimension;
    private final int targetDimension;
    private final boolean preserveHeight;
    private final CoordinateOperation operation;

    CrsTransformerProj(CoordinateReferenceSystem sourceCrs, CoordinateReferenceSystem targetCrs,
                           EpsgCrs origSourceCrs, EpsgCrs origTargetCrs, int sourceDimension,
                           int targetDimension) throws FactoryException {
        this.sourceCrs = origSourceCrs;
        this.targetCrs = origTargetCrs;

        Unit<?> sourceUnit = sourceCrs
                                .getCoordinateSystem()
                                .getAxis(0)
                                .getUnit();
        Unit<?> targetUnit = targetCrs
                                .getCoordinateSystem()
                                .getAxis(0)
                                .getUnit();

        this.isSourceMetric = sourceUnit == Units.METRE;
        this.isTargetMetric = targetUnit == Units.METRE;//targetCrs instanceof ProjectedCRS;

        SingleCRS horizontalSourceCrs = getHorizontalCrs(sourceCrs);
        SingleCRS horizontalTargetCrs = getHorizontalCrs(targetCrs);

        this.sourceUnitEquivalentInMeters = getUnitEquivalentInMeters(horizontalSourceCrs);

        this.targetUnitEquivalentInMeters = getUnitEquivalentInMeters(horizontalTargetCrs);

        //TODO
        this.needsAxisSwap = false; //sourceNeedsAxisSwap != targetNeedsAxisSwap;
        this.sourceDimension = sourceDimension;
        this.targetDimension = targetDimension;
        this.preserveHeight = sourceDimension == 3 && targetDimension == 3;

        //LOGGER.debug("AXIS SWAP: {} {} {} {}, {} {} {}", needsAxisSwap, origSourceCrs.getCode(), sourceNeedsAxisSwap, sourceDirection, origTargetCrs.getCode(), targetNeedsAxisSwap, targetDirection);

        operation  = Proj.createCoordinateOperation(sourceCrs, targetCrs, null);
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
            operation.getMathTransform().transform(source, 0, target, 0, numberOfPoints);

            return target;
        } catch (MismatchedDimensionException | TransformException ex) {
            LogContext.errorAsDebug(LOGGER, ex, "Proj error");
        }

        return null;
    }

    @Override
    public double[] transform3d(double[] coordinates, int numberOfPoints, boolean swap) {
        try {
            double[] source = coordinates;
            if (swap && this.needsAxisSwap) {
                source = new double[numberOfPoints * 3];
                for (int i = 0; i < numberOfPoints * 3; i += 3) {
                    source[i] = coordinates[i + 1];
                    source[i + 1] = coordinates[i];
                }
            }
            double[] target = new double[3* numberOfPoints];
            operation.getMathTransform().transform(source, 0, target, 0, numberOfPoints);

            return target;
        } catch (MismatchedDimensionException | TransformException ex) {
            LogContext.errorAsDebug(LOGGER, ex, "Proj error");
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

    @Override
    public int getSourceDimension() {
        return sourceDimension;
    }

    @Override
    public int getTargetDimension() {
        return targetDimension;
    }

    private double getUnitEquivalentInMeters(SingleCRS horizontalCrs) {
        return isSourceMetric || !(horizontalCrs.getDatum() instanceof GeodeticDatum)
            ? 1
            : (Math.PI / 180.00) * ((GeodeticDatum) horizontalCrs.getDatum()).getEllipsoid()
                .getSemiMajorAxis();
    }

    private SingleCRS getHorizontalCrs(CoordinateReferenceSystem crs) {
        return crs instanceof CompoundCRS
            ? (SingleCRS) ((CompoundCRS) crs).getComponents().get(0)
            : (SingleCRS) crs;
    }
}
