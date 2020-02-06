package de.ii.xtraplatform.features.domain;

import akka.stream.javadsl.Sink;

import java.util.concurrent.CompletionStage;

public interface FeatureSourceStream<T> {

    CompletionStage<FeatureStream2.Result> runWith(FeatureConsumer consumer);

    CompletionStage<FeatureStream2.Result> runWith2(Sink<T, CompletionStage<FeatureStream2.Result>> consumer);

}
