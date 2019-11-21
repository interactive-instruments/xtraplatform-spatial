package de.ii.xtraplatform.feature.provider.api;

import de.ii.xtraplatform.crs.api.EpsgCrs;

import javax.ws.rs.core.MediaType;
import java.util.Optional;

public interface FeatureProvider2 {

    //TODO: FeatureCrs???
    //TODO: is there a way to move the whole crs transformation stuff to the provider?
    // as the crs is part of the query, crs transformation should be part of the normalization
    boolean supportsCrs(EpsgCrs crs);

    //TODO: let transformer handle swapping again
    @Deprecated
    default boolean shouldSwapCoordinates(EpsgCrs crs) {
        return false;
    }



    default boolean supportsQueries() {
        return this instanceof FeatureQueries;
    }

    default FeatureQueries queries() {
        if (!supportsQueries()) {
            throw new UnsupportedOperationException();
        }
        return (FeatureQueries) this;
    }

    default boolean supportsExtents() {
        return this instanceof FeatureExtents;
    }

    default FeatureExtents extents() {
        if (!supportsExtents()) {
            throw new UnsupportedOperationException();
        }
        return (FeatureExtents) this;
    }

    default boolean supportsPassThrough() {
        return this instanceof FeatureQueriesPassThrough;
    }

    default FeatureQueriesPassThrough passThrough() {
        if (!supportsQueries()) {
            throw new UnsupportedOperationException();
        }
        return (FeatureQueriesPassThrough) this;
    }

    default boolean supportsTransactions() {
        return this instanceof FeatureTransactions;
    }

    default FeatureTransactions transactions() {
        if (!supportsTransactions()) {
            throw new UnsupportedOperationException();
        }
        return (FeatureTransactions) this;
    }
}
