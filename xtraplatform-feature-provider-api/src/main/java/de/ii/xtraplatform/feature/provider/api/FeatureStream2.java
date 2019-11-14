package de.ii.xtraplatform.feature.provider.api;

import akka.Done;

import java.util.concurrent.CompletionStage;

public interface FeatureStream2 {

    interface Result {
        boolean isSuccess();
    }

    CompletionStage<Result> runWith(FeatureConsumer consumer);

    CompletionStage<Result> runWith(FeatureTrans consumer);
}
