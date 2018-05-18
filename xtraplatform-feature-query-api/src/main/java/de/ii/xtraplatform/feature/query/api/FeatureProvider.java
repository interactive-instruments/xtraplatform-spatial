package de.ii.xtraplatform.feature.query.api;

import com.google.common.util.concurrent.ListenableFuture;
import org.apache.http.HttpEntity;

import java.util.Optional;

/**
 * @author zahnen
 */
public interface FeatureProvider {
    Optional<ListenableFuture<HttpEntity>> getFeatureStream(FeatureQuery query);
    Optional<ListenableFuture<HttpEntity>> getFeatureCount(FeatureQuery query);
}
