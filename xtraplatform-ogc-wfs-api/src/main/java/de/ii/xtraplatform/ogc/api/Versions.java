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
package de.ii.xtraplatform.ogc.api;

import java.util.Objects;

/**
 *
 * @author fischer
 */
public class Versions {
    GML.VERSION gmlVersion = null;
    WFS.VERSION wfsVersion = null;

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
        if (Objects.nonNull(gmlVersion)) {
            return gmlVersion;
        }
        if (Objects.nonNull(wfsVersion)) {
            return wfsVersion.getGmlVersion();
        }
        return null;
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
