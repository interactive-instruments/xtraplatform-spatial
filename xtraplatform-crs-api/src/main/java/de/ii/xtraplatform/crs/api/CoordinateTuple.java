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
public class CoordinateTuple {

    protected double[] c;
    protected boolean[] used;

    public CoordinateTuple() {
        this.c = new double[2];
        this.used = new boolean[2];
    }

    public CoordinateTuple(double x, double y) {
        this();
        setX(x);
        setY(y);
    }

    public CoordinateTuple(String x, String y) {
        this(Double.parseDouble(x), Double.parseDouble(y));
    }

    public CoordinateTuple(double[] c) {
        this();
        this.c = c;
        this.used[0] = true;
        this.used[1] = true;
    }

    public double[] asArray() {
        return c;
    }

    public double getX() {
        return c[0];
    }

    public double getY() {
        return c[1];
    }

    public void setX(double x) {
        this.c[0] = x;
        this.used[0] = true;
    }

    public void setY(double y) {
        this.c[1] = y;
        this.used[1] = true;
    }

    public String getXasString() {
        return String.valueOf(c[0]);
    }

    public String getYasString() {
        return String.valueOf(c[1]);
    }

    public boolean hasX() {
        return used[0];
    }

    public boolean hasY() {
        return used[1];
    }
    
    public boolean isNull() {
        return c == null;
    }
}
