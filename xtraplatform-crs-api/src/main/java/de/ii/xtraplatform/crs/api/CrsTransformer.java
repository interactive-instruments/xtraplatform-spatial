package de.ii.xtraplatform.crs.api;

/**
 *
 * @author zahnen
 */
public interface CrsTransformer {

    CoordinateTuple transform(double x, double y);

    CoordinateTuple transform(CoordinateTuple coordinateTuple);

    double[] transform(double[] coordinates, int numberOfPoints);
    
    BoundingBox transformBoundingBox(BoundingBox boundingBox, EpsgCrs targetCrs) throws CrsTransformationException;
}
