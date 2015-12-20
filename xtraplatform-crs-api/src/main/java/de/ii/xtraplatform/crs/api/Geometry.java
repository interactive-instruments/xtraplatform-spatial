package de.ii.xtraplatform.crs.api;

/**
 *
 * @author fischer
 */
public abstract class Geometry {

    protected EpsgCrs spatialReference;
    protected double[] coordinates;

    public double[] getCoords() {
        return coordinates;
    }

    public EpsgCrs getSpatialReference() {
        return spatialReference;
    }

    public void setSpatialReference(EpsgCrs spatialReference) {
        this.spatialReference = spatialReference;
    }
}
