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

import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.crs.api.BoundingBoxTransformer;
import de.ii.xtraplatform.crs.api.CoordinateTuple;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.geotools.referencing.CRS;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author zahnen
 */
public class GeoToolsCrsTransformer extends BoundingBoxTransformer implements CrsTransformer  {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(GeoToolsCrsTransformer.class);
    private final MathTransform mathTransform;

    GeoToolsCrsTransformer(CoordinateReferenceSystem sourceCrs, CoordinateReferenceSystem targetCrs) throws FactoryException {
        this.mathTransform = CRS.findMathTransform(sourceCrs, targetCrs, true);
    }

    @Override
    public CoordinateTuple transform(double x, double y) {
        return transform(new CoordinateTuple(x, y));
    }

    @Override
    public CoordinateTuple transform(CoordinateTuple coordinateTuple) {
        return new CoordinateTuple(transform(coordinateTuple.asArray(), 1));
    }

    @Override
    public double[] transform(double[] coordinates, int numberOfPoints) {
        try {
            double[] target = new double[2* numberOfPoints];
            mathTransform.transform(coordinates, 0, target, 0, numberOfPoints);

            return target;
        } catch (MismatchedDimensionException | TransformException ex) {
            LOGGER.getLogger().error("GeoTools error", ex);
        }

        return null;
    }
}