/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import akka.Done;
import akka.stream.javadsl.RunnableGraph;
import de.ii.xtraplatform.crs.domain.CrsTransformer;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public interface FeatureTransactions {

    List<String> addFeaturesFromStream(String featureType, CrsTransformer crsTransformer, Function<FeatureTransformer, RunnableGraph<CompletionStage<Done>>> stream);

    void updateFeatureFromStream(String featureType, String id, CrsTransformer crsTransformer, Function<FeatureTransformer, RunnableGraph<CompletionStage<Done>>> stream);

    void deleteFeature(String featureType, String id);
}
