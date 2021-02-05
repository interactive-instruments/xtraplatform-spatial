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
package de.ii.xtraplatform.geometries.domain;

import de.ii.xtraplatform.crs.domain.EpsgCrs;

/**
 *
 * @author fischer
 */
public abstract class Geometry {

    protected EpsgCrs crs;
    protected double[] coordinates;

    public double[] getCoordinates() {
        return coordinates;
    }

    public EpsgCrs getCrs() {
        return crs;
    }

    public void setCrs(EpsgCrs crs) {
        this.crs = crs;
    }
}
