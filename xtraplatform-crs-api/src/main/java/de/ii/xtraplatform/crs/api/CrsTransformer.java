package de.ii.xtraplatform.crs.api;

/**
 *
 * @author zahnen
 */
public interface CrsTransformer {

    public CoordinateTuple transform(double x, double y);

    public CoordinateTuple transform(CoordinateTuple coord);

    public double[] transform(double[] coord, int numPts);
    
    public BoundingBox transformBoundingBox(BoundingBox bbox, EpsgCrs targetCrs) throws CrsTransformationException;
}
