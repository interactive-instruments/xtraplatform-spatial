/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.query.api;

import com.google.common.util.concurrent.ListenableFuture;
import org.apache.http.HttpEntity;

import java.util.Optional;

/**
 * @author zahnen
 */
public interface FeatureProvider<T extends FeatureConsumer> {
    FeatureStream<T> getFeatureStream(FeatureQuery query);
    Optional<ListenableFuture<HttpEntity>> getFeatureCount(FeatureQuery query);
    Optional<String> encodeFeatureQuery(FeatureQuery query);
    String getSourceFormat();

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
