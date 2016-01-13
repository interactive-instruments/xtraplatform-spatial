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
package de.ii.xtraplatform.ogc.api;

/**
 *
 * @author fischer
 */
public class Versions {
    GML.VERSION gmlVersion;
    WFS.VERSION wfsVersion;

    public Versions() {
    }
    
    public Versions(WFS.VERSION wfsVersion) {
        this.wfsVersion = wfsVersion;
    }
    
    public Versions(WFS.VERSION wfsVersion, GML.VERSION gmlVersion) {
        this.wfsVersion = wfsVersion;
        this.gmlVersion = gmlVersion;
    }

    public GML.VERSION getGmlVersion() {
        return gmlVersion;
    }

    public void setGmlVersion(GML.VERSION gmlVersion) {
        this.gmlVersion = gmlVersion;
    }

    public WFS.VERSION getWfsVersion() {
        return wfsVersion;
    }

    public void setWfsVersion(WFS.VERSION wfsVersion) {
        this.wfsVersion = wfsVersion;
    }
    
    
}
