package de.ii.xtraplatform.feature.provider.api;

import akka.NotUsed;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

import java.util.concurrent.CompletionStage;

public interface FeatureNormalizer<T> {

    Sink<T, CompletionStage<FeatureStream2.Result>> normalizeAndTransform(FeatureTransformer featureTransformer, FeatureQuery featureQuery);

    Source<Feature, NotUsed> normalize(Source<T, NotUsed> sourceStream);

}
