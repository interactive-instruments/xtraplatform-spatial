package de.ii.xtraplatform.feature.provider.api;

import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.entity.api.PersistentEntity;

public interface FeatureProvider2 extends PersistentEntity {

    String ENTITY_TYPE = "providers";

    @Override
    default String getType() {
        return ENTITY_TYPE;
    }

    @Override
    FeatureProviderDataV1 getData();

/*
    default String getProviderType() {
        return getData().getProviderType();
    }

    default String getFeatureProviderType() {
        return getData().getFeatureProviderType();
    }
*/



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

    default boolean supportsCrs() {
        return this instanceof FeatureCrs;
    }

    default FeatureCrs crs() {
        if (!supportsCrs()) {
            throw new UnsupportedOperationException();
        }
        return (FeatureCrs) this;
    }
}
