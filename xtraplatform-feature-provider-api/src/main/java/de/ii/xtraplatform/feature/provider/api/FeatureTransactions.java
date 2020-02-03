package de.ii.xtraplatform.feature.provider.api;

import akka.Done;
import akka.stream.javadsl.RunnableGraph;
import de.ii.xtraplatform.geometries.domain.CrsTransformer;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public interface FeatureTransactions {

    List<String> addFeaturesFromStream(String featureType, CrsTransformer crsTransformer, Function<FeatureTransformer, RunnableGraph<CompletionStage<Done>>> stream);

    void updateFeatureFromStream(String featureType, String id, CrsTransformer crsTransformer, Function<FeatureTransformer, RunnableGraph<CompletionStage<Done>>> stream);

    void deleteFeature(String featureType, String id);
}
