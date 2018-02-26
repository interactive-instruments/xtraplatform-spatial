/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
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
