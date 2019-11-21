package de.ii.xtraplatform.feature.provider.api;

import javax.ws.rs.core.MediaType;

public interface FeatureQueriesPassThrough<T> {

    MediaType getMediaType();

    FeatureSourceStream<T> getFeatureSourceStream(FeatureQuery query);

}
