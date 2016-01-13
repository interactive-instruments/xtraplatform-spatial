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
 * @author zahnen
 */
public class LazyStringCoordinateTuple extends CoordinateTuple {
    
    private String[] cs;
    private boolean resolved;

    public LazyStringCoordinateTuple() {
        super();
        this.cs = new String[2];
        this.resolved = false;
    }
    
    public void setX(String x) {
        this.resolved = false;
        this.cs[0] = x;
        this.used[0] = true;
    }
    
    public void setY(String y) {
        this.resolved = false;
        this.cs[1] = y;
        this.used[1] = true;
    }
    
    public void appendX(String x) {
        this.cs[0] = this.cs[0].concat(x);
        this.used[0] = true;
    }
    
    public void appendY(String y) {
        this.cs[1] = this.cs[1].concat(y);
        this.used[1] = true;
    }
    
    @Override
    public double[] asArray() {
        if (!resolved) {
            resolve();
        }
        return super.asArray();
    }

    @Override
    public double getX() {
        if (!resolved) {
            resolve();
        }
        return super.getX();
    }

    @Override
    public double getY() {
        if (!resolved) {
            resolve();
        }
        return super.getY();
    }
    
    @Override
    public String getXasString() {
        return cs[0];
    }
    
    @Override
    public String getYasString() {
        return cs[1];
    }
    
    private void resolve() {
        if (cs[0] != null)
        this.c[0] = Double.parseDouble(cs[0]);
        if (cs[1] != null)
        this.c[1] = Double.parseDouble(cs[1]);
        this.resolved = true;
    }
}
