/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import akka.Done;
import akka.stream.javadsl.Sink;

import java.util.concurrent.CompletionStage;

public interface FeatureStream2 {

    interface Result {
        boolean isSuccess();
    }

    CompletionStage<Result> runWith(FeatureTransformer2 transformer);

    CompletionStage<Result> runWith(Sink<Feature<?>, CompletionStage<Done>> transformer);

    default <T extends Property<?>,U extends Feature<T>> CompletionStage<Result> runWith(FeatureTransformer3<T,U> transformer) {
        return null;
    }
}
