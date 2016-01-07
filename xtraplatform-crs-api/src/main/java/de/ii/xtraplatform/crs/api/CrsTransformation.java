package de.ii.xtraplatform.crs.api;

/**
 *
 * @author zahnen
 */
public interface CrsTransformation {

    CrsTransformer getTransformer(String sourceCrs, String targetCrs);

    CrsTransformer getTransformer(EpsgCrs sourceCrs, EpsgCrs targetCrs);

    boolean isCrsAxisOrderEastNorth(String crs);

    boolean isCrsSupported(String crs);
    
    double getUnitEquivalentInMeter(String crs);
    
}
