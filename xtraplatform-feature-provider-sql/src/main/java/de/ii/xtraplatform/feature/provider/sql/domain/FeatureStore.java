package de.ii.xtraplatform.feature.provider.sql.domain;

import akka.Done;
import de.ii.xtraplatform.feature.provider.api.FeatureConsumer;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface FeatureStore {

    Map<String, FeatureStoreTypeInfo> getTypes();

    FeatureStoreMultiplicityTracker getMultiplicityTracker(FeatureStoreTypeInfo typeInfo);


    //TODO: to provided
    default CompletionStage<Done> getFeatures(FeatureQuery query, FeatureConsumer consumer) {
        Optional<FeatureStoreTypeInfo> featureSource = Optional.ofNullable(getTypes().get(query.getType()));

        if (!featureSource.isPresent()) {
            CompletableFuture<Done> promise = new CompletableFuture<>();
            promise.completeExceptionally(new IllegalStateException("No features available for type"));
            return promise;
        }

        return streamFeatures(query, consumer, featureSource.get());
    }


    CompletionStage<Done> streamFeatures(FeatureQuery query, FeatureConsumer consumer, FeatureStoreTypeInfo typeInfo);

}
