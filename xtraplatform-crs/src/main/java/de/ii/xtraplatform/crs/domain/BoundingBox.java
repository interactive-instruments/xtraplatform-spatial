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
package de.ii.xtraplatform.crs.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 *
 * @author fischer
 */
//TODO: immutable
public class BoundingBox {
    private double xmin;
    private double ymin;
    private double xmax;
    private double ymax;
    private EpsgCrs crs;

    public BoundingBox() {
        this(-180.0, -90.0, 180.0, 90.0, EpsgCrs.of(4326));
    }

    public BoundingBox(double xmin, double ymin, double xmax, double ymax, EpsgCrs crs) {
        this.xmin = xmin;
        this.xmax = xmax;
        this.ymin = ymin;
        this.ymax = ymax;

        this.crs = crs;
    }
    
    public BoundingBox(double[] coords, EpsgCrs crs) {
        this.xmin = coords[0];
        this.ymin = coords[1];
        this.xmax = coords[2];
        this.ymax = coords[3];

        this.crs = crs;
    }
    
    @JsonIgnore
    public double [] getCoords(){
        double [] out = {xmin, ymin, xmax, ymax};
        return out;
    }

    public double getXmin() {
        return xmin;
    }

    public void setXmin(double xmin) {
        this.xmin = xmin;
    }

    public double getYmin() {
        return ymin;
    }

    public void setYmin(double ymin) {
        this.ymin = ymin;
    }

    public double getXmax() {
        return xmax;
    }

    public void setXmax(double xmax) {
        this.xmax = xmax;
    }

    public double getYmax() {
         return ymax;
    }

    public void setYmax(double ymax) {
        this.ymax = ymax;
    }

    public EpsgCrs getEpsgCrs() {
        return crs;
    }

    public void setEpsgCrs(EpsgCrs crs) {
        this.crs = crs;
    }

    @Override
    public String toString() {
        return "(" + xmin + ", " + ymin + ", " + xmax + ", " + ymax + ", " + crs.toSimpleString() + ')';
    }
    
    
}
