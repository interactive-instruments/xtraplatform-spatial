package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.cql.domain.CqlFilter;

import java.util.Optional;
import java.util.stream.Stream;

public interface FeatureStoreQueryGenerator<T> {

    T getMetaQuery(FeatureStoreInstanceContainer instanceContainer, int limit, int offset, Optional<CqlFilter> cqlFilter, boolean computeNumberMatched);

    Stream<T> getInstanceQueries(FeatureStoreInstanceContainer instanceContainer, Optional<CqlFilter> cqlFilter, long minKey, long maxKey);

    T getExtentQuery(FeatureStoreAttributesContainer attributesContainer);
}
