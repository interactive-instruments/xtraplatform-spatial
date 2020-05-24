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
import org.immutables.value.Value;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface FeatureStream2 {

    @Value.Immutable
    interface Result {

        @Value.Derived
        default boolean isSuccess() {
            return !getError().isPresent();
        }

        boolean isEmpty();

        Optional<Throwable> getError();
    }

    CompletionStage<Result> runWith(FeatureTransformer2 transformer);

    CompletionStage<Result> runWith(Sink<Feature, CompletionStage<Done>> transformer);

    default <V extends PropertyBase<V,X>, W extends FeatureBase<V,X>, X extends SchemaBase<X>> CompletionStage<Result> runWith(
            FeatureProcessor<V,W,X> transformer) {
        return null;
    }
}
