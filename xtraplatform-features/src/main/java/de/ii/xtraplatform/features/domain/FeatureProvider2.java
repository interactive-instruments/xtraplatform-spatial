/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;


import de.ii.xtraplatform.entities.domain.PersistentEntity;

public interface FeatureProvider2 extends PersistentEntity {

    String ENTITY_TYPE = "providers";
    String PROVIDER_TYPE_KEY = "providerType";

    @Override
    default String getType() {
        return ENTITY_TYPE;
    }

    @Override
    FeatureProviderDataV2 getData();

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
            throw new UnsupportedOperationException("Queries not supported");
        }
        return (FeatureQueries) this;
    }

    default boolean supportsExtents() {
        return this instanceof FeatureExtents && supportsCrs();
    }

    default FeatureExtents extents() {
        if (!supportsExtents()) {
            throw new UnsupportedOperationException("Extents not supported");
        }
        return (FeatureExtents) this;
    }

    default boolean supportsPassThrough() {
        return this instanceof FeatureQueriesPassThrough;
    }

    default FeatureQueriesPassThrough passThrough() {
        if (!supportsQueries()) {
            throw new UnsupportedOperationException("Queries not supported");
        }
        return (FeatureQueriesPassThrough) this;
    }

    default boolean supportsTransactions() {
        return this instanceof FeatureTransactions;
    }

    default FeatureTransactions transactions() {
        if (!supportsTransactions()) {
            throw new UnsupportedOperationException("Transactions not supported");
        }
        return (FeatureTransactions) this;
    }

    default boolean supportsCrs() {
        return this instanceof FeatureCrs;
    }

    default FeatureCrs crs() {
        if (!supportsCrs()) {
            throw new UnsupportedOperationException("CRS not supported");
        }
        return (FeatureCrs) this;
    }

    default boolean supportsMetadata() {
        return this instanceof FeatureMetadata;
    }

    default FeatureMetadata metadata() {
        if (!supportsMetadata()) {
            throw new UnsupportedOperationException("Metadata not supported");
        }
        return (FeatureMetadata) this;
    }
}
