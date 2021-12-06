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
    private final int sourceDimension;
    private final int targetDimension;
    private final CoordinateOperation operation;

    CrsTransformerProj(CoordinateReferenceSystem sourceCrs, CoordinateReferenceSystem targetCrs,
        EpsgCrs origSourceCrs, EpsgCrs origTargetCrs, int sourceDimension,
        int targetDimension, CoordinateOperation coordinateOperation) throws FactoryException {
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
        this.isTargetMetric = targetUnit == Units.METRE;

        SingleCRS horizontalSourceCrs = getHorizontalCrs(sourceCrs);
        SingleCRS horizontalTargetCrs = getHorizontalCrs(targetCrs);

        this.sourceUnitEquivalentInMeters = getUnitEquivalentInMeters(horizontalSourceCrs);
        this.targetUnitEquivalentInMeters = getUnitEquivalentInMeters(horizontalTargetCrs);

        this.sourceDimension = sourceDimension;
        this.targetDimension = targetDimension;

        operation  = coordinateOperation;
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
        return new CoordinateTupleWithPrecision(transform(coordinateTuple.asArray(), 1, 2), isTargetMetric);
    }

    @Override
    public double[] transform(double[] coordinates, int numberOfPoints, int dimension) {
        try {
            double[] target = new double[dimension * numberOfPoints];

            operation.getMathTransform().transform(coordinates, 0, target, 0, numberOfPoints);

            return target;
        } catch (MismatchedDimensionException | TransformException ex) {
            LogContext.errorAsDebug(LOGGER, ex, "PROJ");
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
