package de.ii.xtraplatform.feature.provider.api;

import akka.NotUsed;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public interface FeatureNormalizer<T> {

    Sink<T, CompletionStage<FeatureStream2.Result>> normalizeAndTransform(FeatureTransformer2 featureTransformer, FeatureQuery featureQuery);

    <U extends Property<?>,V extends Feature<U>> Source<V, CompletionStage<FeatureStream2.Result>> normalize(Source<T, NotUsed> sourceStream, FeatureQuery featureQuery, Supplier<V> featureCreator, Supplier<U> propertyCreator);

}
