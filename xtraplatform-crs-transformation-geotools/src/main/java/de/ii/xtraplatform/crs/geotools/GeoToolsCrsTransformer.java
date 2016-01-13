/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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