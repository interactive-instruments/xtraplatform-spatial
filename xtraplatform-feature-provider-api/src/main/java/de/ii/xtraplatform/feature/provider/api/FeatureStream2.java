package de.ii.xtraplatform.feature.provider.api;

import akka.stream.javadsl.Sink;

import java.util.concurrent.CompletionStage;

public interface FeatureStream2 {

    interface Result {
        boolean isSuccess();
    }

    CompletionStage<Result> runWith(FeatureTransformer2 transformer);

    CompletionStage<Result> runWith(Sink<Feature, CompletionStage<Result>> transformer);
}
