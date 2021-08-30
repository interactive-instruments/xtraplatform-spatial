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
import org.geotools.referencing.CRS;
import org.kortforsyningen.proj.Proj;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationFactory;
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
    private final double sourceUnitEquivalentInMeters;
    private final double targetUnitEquivalentInMeters;
    private final boolean needsAxisSwap;
    //TODO
    private final int sourceDimension;
    private final int targetDimension;
    private final boolean preserveHeight;
    private final CRSAuthorityFactory factory;
    private final CoordinateOperationFactory regops;
    private final CoordinateOperation operation;

    GeoToolsCrsTransformer(CoordinateReferenceSystem sourceCrs, CoordinateReferenceSystem targetCrs,
                           EpsgCrs origSourceCrs, EpsgCrs origTargetCrs, int sourceDimension,
                           int targetDimension) throws FactoryException {
        this.sourceCrs = origSourceCrs;
        this.targetCrs = origTargetCrs;

        Unit<?> sourceUnit = CRS.getHorizontalCRS(sourceCrs)
                                .getCoordinateSystem()
                                .getAxis(0)
                                .getUnit();
        Unit<?> targetUnit = CRS.getHorizontalCRS(targetCrs)
                                .getCoordinateSystem()
                                .getAxis(0)
                                .getUnit();
        //TODO: test if METRE is really returned
        boolean isSourceMetric = sourceUnit == SI.METER;
        this.isTargetMetric = targetUnit == SI.METER;//targetCrs instanceof ProjectedCRS;
        this.sourceUnitEquivalentInMeters = isSourceMetric ? 1 : (Math.PI / 180.00) * CRS.getEllipsoid(sourceCrs)
                                                                                         .getSemiMajorAxis();
        this.targetUnitEquivalentInMeters = isTargetMetric ? 1 : (Math.PI / 180.00) * CRS.getEllipsoid(targetCrs)
                                                                                         .getSemiMajorAxis();
/*
        AxisDirection sourceDirection = sourceCrs.getCoordinateSystem()
                                           .getAxis(0)
                                           .getDirection();
        AxisDirection sourceOrigDirection = CRS.decode(this.sourceCrs.toSimpleString())
                                               .getCoordinateSystem()
                                                 .getAxis(0)
                                                 .getDirection();
        AxisDirection targetDirection = targetCrs.getCoordinateSystem()
                                                 .getAxis(0)
                                                 .getDirection();

        AxisDirection targetOrigDirection = CRS.decode(EpsgCrs.fromString(targetCrs.getIdentifiers()
                                                                                   .iterator()
                                                                                   .next()
                                                                                   .toString())
                                                              .toSimpleString())
                                               .getCoordinateSystem()
                                               .getAxis(0)
                                               .getDirection();
        boolean sourceNeedsAxisSwap = origSourceCrs.getForceLonLat() && sourceDirection == sourceOrigDirection;
        boolean targetNeedsAxisSwap = origTargetCrs.getForceLonLat() && targetDirection == targetOrigDirection;

 */
        //TODO
        this.needsAxisSwap = false; //sourceNeedsAxisSwap != targetNeedsAxisSwap;
        this.sourceDimension = sourceDimension;
        this.targetDimension = targetDimension;
        this.preserveHeight = sourceDimension == 3 && targetDimension == 3;

        //LOGGER.debug("AXIS SWAP: {} {} {} {}, {} {} {}", needsAxisSwap, origSourceCrs.getCode(), sourceNeedsAxisSwap, sourceDirection, origTargetCrs.getCode(), targetNeedsAxisSwap, targetDirection);

        factory = Proj.getAuthorityFactory("EPSG");
        regops  = Proj.getOperationFactory(null);
        operation = regops.createOperation(sourceCrs, targetCrs);
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
            LOGGER.error("GeoTools error", ex);
        }

        return null;
    }

    @Override
    public double[] transform3d(double[] coordinates, int numberOfPoints, boolean swap) {
        try {
            double[] coordinates2D = new double[numberOfPoints * 2];
            int j = 0;
            for (int i = 0; i < numberOfPoints * 3; i += 3) {
                if (swap && this.needsAxisSwap) {
                    coordinates2D[j++] = coordinates[i + 1];
                    coordinates2D[j++] = coordinates[i];
                } else {
                    coordinates2D[j++] = coordinates[i];
                    coordinates2D[j++] = coordinates[i + 1];
                }
            }
            operation.getMathTransform().transform(coordinates2D, 0, coordinates2D, 0, numberOfPoints);

            j = 0;
            for (int i = 0; i < numberOfPoints * 3; i += 3) {
                coordinates[i] = coordinates2D[j++];
                coordinates[i + 1] = coordinates2D[j++];
            }

            return coordinates;
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

    @Override
    public int getSourceDimension() {
        return sourceDimension;
    }

    @Override
    public int getTargetDimension() {
        return targetDimension;
    }
}
