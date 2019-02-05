/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.api;

import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.crs.api.EpsgCrs;

import java.util.Map;
import java.util.Optional;

/**
 * @author zahnen
 */
public interface FeatureProvider<T extends FeatureConsumer> {

    FeatureStream<T> getFeatureStream(FeatureQuery query);

    Optional<String> encodeFeatureQuery(FeatureQuery query, Map<String, String> additionalQueryParameters);

    String getSourceFormat();

    default BoundingBox getSpatialExtent(String featureTypeId) {
        return new BoundingBox(-180.0D, -90.0D, 180.0D, 90.0D, new EpsgCrs(4326));
    }

    interface MetadataAware {
        void getMetadata(FeatureProviderMetadataConsumer metadataConsumer);
    }

    interface DataGenerator<U extends FeatureProviderData> {
        FeatureProviderMetadataConsumer getDataGenerator(U data);
    }

    //TODO
    /*interface Queries {
        FeatureStream<T> getFeatureStream(FeatureQuery query);
        Optional<ListenableFuture<HttpEntity>> getFeatureCount(FeatureQuery query);
    }

    interface Transformations {
        FeatureStream<FeatureTransformer> getFeatureTransformationStream(FeatureQuery query);
    }

    interface Transactions {
        void addFeaturesFromStream(FeatureStream<FeatureTransformer>);
        void updateFeaturesFromStream();
        void deleteFeature(String id);
    }*/
}
