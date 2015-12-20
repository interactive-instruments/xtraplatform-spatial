package de.ii.xtraplatform.crs.api;

/**
 *
 * @author zahnen
 */
public interface CrsTransformation {

    CrsTransformer getTransformer(String sourceCrs, String targetCrs);

    CrsTransformer getTransformer(EpsgCrs sourceCrs, EpsgCrs targetCrs);

    boolean isSRSAxisOrderEastNorth(String epsg);

    boolean isSRSSupported(String epsg);
    
    double getUnitEquivalentInMeter(String epsg);
    
}
