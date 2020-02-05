package de.ii.xtraplatform.feature.provider.api;

import de.ii.xtraplatform.crs.domain.EpsgCrs;

public interface FeatureCrs {

    //TODO: is there a way to move the whole crs transformation stuff to the provider?
    // as the crs is part of the query, crs transformation should be part of the normalization
    boolean isCrsSupported(EpsgCrs crs);

    default boolean is3dSupported() {
        return false;
    }

    //TODO: let transformer handle swapping again
    @Deprecated
    default boolean shouldSwapCoordinates(EpsgCrs crs) {
        return false;
    }

}
