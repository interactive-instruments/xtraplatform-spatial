/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import akka.Done;
import akka.NotUsed;
import akka.japi.function.Function2;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import org.immutables.value.Value;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

@Value.Immutable
public interface FeatureMutationPipeline<U extends PropertyBase<U,W>, V extends FeatureBase<U,W>, W extends SchemaBase<W>> extends FeaturePipeline<U, V, W, String, CompletionStage<FeatureTransactions.MutationResult>> {

    @Value.Derived
    @Value.Auxiliary
    @Override
    default Sink<String, CompletionStage<FeatureTransactions.MutationResult>> getSink() {
        return Flow.of(String.class)
                   .watchTermination((Function2<NotUsed, CompletionStage<Done>, CompletionStage<ImmutableMutationResult.Builder>>) (notUsed, completionStage) -> completionStage.handle((done, throwable) -> {
                       return ImmutableMutationResult.builder()
                                                     .error(Optional.ofNullable(throwable));
                   }))
                   .toMat(Sink.seq(), (resultStage, idsStage) -> resultStage.thenCombine(idsStage, (result, ids) -> result.ids(ids)
                                                                                                                          .build()));
    }

}
