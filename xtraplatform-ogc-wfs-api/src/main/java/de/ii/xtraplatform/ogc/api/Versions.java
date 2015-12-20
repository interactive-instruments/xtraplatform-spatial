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
