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
package de.ii.xtraplatform.crs.api;

/**
 *
 * @author fischer
 */
public class BoundingBox {
    private double xmin;
    private double ymin;
    private double xmax;
    private double ymax;
    private EpsgCrs spatialReference;

    public BoundingBox() {
        this(-180.0, -90.0, 180.0, 90.0, new EpsgCrs(4326));
    }

    public BoundingBox(double xmin, double ymin, double xmax, double ymax, EpsgCrs sr) {
        this.xmin = xmin;
        this.xmax = xmax;
        this.ymin = ymin;
        this.ymax = ymax;

        this.spatialReference = sr;
    }
    
    public BoundingBox(double [] coords, EpsgCrs sr) {
        this.xmin = coords[0];
        this.ymin = coords[1];
        this.xmax = coords[2];
        this.ymax = coords[3];

        this.spatialReference = sr;
    }
    
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
        return spatialReference;
    }

    public void setEpsgCrs(EpsgCrs spatialReference) {
        this.spatialReference = spatialReference;
    }

    @Override
    public String toString() {
        return "(" + xmin + ", " + ymin + ", " + xmax + ", " + ymax + ", " + spatialReference.getAsSimple() + ')';
    }
    
    
}
