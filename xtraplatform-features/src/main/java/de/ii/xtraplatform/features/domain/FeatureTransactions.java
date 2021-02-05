/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import akka.Done;
import akka.stream.javadsl.RunnableGraph;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public interface FeatureTransactions {

    @Value.Immutable
    interface MutationResult {

        @Value.Derived
        default boolean isSuccess() {
            return !getError().isPresent();
        }

        List<String> getIds();

        Optional<Throwable> getError();
    }

    List<String> addFeaturesFromStream(String featureType, CrsTransformer crsTransformer, Function<FeatureTransformer, RunnableGraph<CompletionStage<Done>>> stream);

    MutationResult createFeatures(String featureType, FeatureDecoder.WithSource featureSource);

    MutationResult updateFeature(String featureType, FeatureDecoder.WithSource featureSource, String id);

    void updateFeatureFromStream(String featureType, String id, CrsTransformer crsTransformer, Function<FeatureTransformer, RunnableGraph<CompletionStage<Done>>> stream);

    MutationResult deleteFeature(String featureType, String id);
}
