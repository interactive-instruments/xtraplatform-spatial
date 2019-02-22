/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.crs.api;

/**
 *
 * @author zahnen
 */
public interface CrsTransformer {

    EpsgCrs getSourceCrs();

    EpsgCrs getTargetCrs();

    boolean isTargetMetric();

    CoordinateTuple transform(double x, double y);

    CoordinateTuple transform(CoordinateTuple coordinateTuple);

    double[] transform(double[] coordinates, int numberOfPoints);
    
    BoundingBox transformBoundingBox(BoundingBox boundingBox) throws CrsTransformationException;

    double getSourceUnitEquivalentInMeters();

    double getTargetUnitEquivalentInMeters();
}
