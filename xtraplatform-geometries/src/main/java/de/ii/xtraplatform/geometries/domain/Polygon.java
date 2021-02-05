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

import java.util.List;

/**
 *
 * @author fischer
 */
public class Polygon extends Geometry {

    private List<List<List<Double>>> rings;
    
    public Polygon() {
    }

    public Polygon(double[] coordinates, EpsgCrs sr) {
        this();
        this.coordinates = coordinates;
        this.crs = sr;
    }


    public Polygon(String[] coordinates, EpsgCrs sr) {
        this(parseCoordinates(coordinates), sr);
    }

    public Polygon(String[] coordinates, String srs) {
        this(coordinates, EpsgCrs.of(Integer.parseInt(srs)));
    }

    public void setRings(List<List<List<Double>>> rings) {
        this.rings = rings;
    }
    
    public List<List<List<Double>>> getRings() {
        return rings;
    }

    private static double[] parseCoordinates(String[] coordinates) {
        double[] doubleCoordinates = new double[coordinates.length];

        for(int i = 0; i < coordinates.length; i++) {
            doubleCoordinates[i] = Double.parseDouble(coordinates[i]);
        }

        return doubleCoordinates;
    }
}
