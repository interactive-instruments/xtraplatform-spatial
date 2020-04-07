package de.ii.xtraplatform.features.domain;

import javax.ws.rs.core.MediaType;

public interface FeatureQueriesPassThrough<T> {

    MediaType getMediaType();

    FeatureSourceStream<T> getFeatureSourceStream(FeatureQuery query);

}
