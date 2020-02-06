package de.ii.xtraplatform.features.domain;

import java.util.stream.Stream;

public interface FeatureStoreQueryGenerator<T> {

    T getMetaQuery(FeatureStoreInstanceContainer instanceContainer, int limit, int offset, String filter, boolean computeNumberMatched);

    Stream<T> getInstanceQueries(FeatureStoreInstanceContainer instanceContainer, String featureFilter, long minKey, long maxKey);

    T getExtentQuery(FeatureStoreAttributesContainer attributesContainer);
}
