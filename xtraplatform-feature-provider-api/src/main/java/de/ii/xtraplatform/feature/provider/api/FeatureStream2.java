package de.ii.xtraplatform.feature.provider.api;

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
