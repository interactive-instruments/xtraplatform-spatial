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
        this.spatialReference = sr;
    }


    public Polygon(String[] coordinates, EpsgCrs sr) {
        this(parseCoordinates(coordinates), sr);
    }

    public Polygon(String[] coordinates, String srs) {
        this(coordinates, new EpsgCrs(Integer.parseInt(srs)));
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
