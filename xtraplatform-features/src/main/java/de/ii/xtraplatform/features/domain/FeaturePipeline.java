/**
 * Copyright 2021 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import de.ii.xtraplatform.streams.domain.LogContextStream;
import de.ii.xtraplatform.streams.domain.RunnableGraphWrapper;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;
import org.immutables.value.Value;

@Value.Immutable
public interface FeaturePipeline<U extends PropertyBase<U, W>, V extends FeatureBase<U, W>, W extends SchemaBase<W>, X, Y> extends
    RunnableGraphWrapper<Y> {

  @Value.Auxiliary
  FeatureDecoder.WithSource getDecoderWithSource();

  @Value.Auxiliary
  SchemaMapping<W> getMapping();

  @Value.Auxiliary
  Supplier<V> getFeatureCreator();

  @Value.Auxiliary
  Supplier<U> getPropertyCreator();

  @Value.Auxiliary
  FeatureEncoder<X, U, V, W> getEncoder();

  @Value.Auxiliary
  Sink<X, CompletionStage<Y>> getSink();

  @Override
  Function<Throwable, Y> getExceptionHandler();

  @Override
  @Value.Lazy
  default RunnableGraph<CompletionStage<Y>> getGraph() {
    Source<V, ?> featureSource = getDecoderWithSource()
        .decode(getMapping(), getFeatureCreator(), getPropertyCreator());

    Flow<V, X, ?> encoderFlow = getEncoder().flow(getMapping().getTargetSchema());

    Sink<V, CompletionStage<Y>> encoderSink = encoderFlow.toMat(getSink(), Keep.right());

    return LogContextStream.graphWithMdc(featureSource, encoderSink).getGraph();
  }
}
