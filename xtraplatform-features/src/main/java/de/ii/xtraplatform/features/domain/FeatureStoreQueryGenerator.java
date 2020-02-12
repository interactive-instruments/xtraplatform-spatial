package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.cql.domain.CqlPredicate;

import java.util.Optional;
import java.util.stream.Stream;

public interface FeatureStoreQueryGenerator<T> {

    T getMetaQuery(FeatureStoreInstanceContainer instanceContainer, int limit, int offset, Optional<CqlPredicate> filter, boolean computeNumberMatched);

    Stream<T> getInstanceQueries(FeatureStoreInstanceContainer instanceContainer, Optional<CqlPredicate> cqlFilter, long minKey, long maxKey);

    T getExtentQuery(FeatureStoreAttributesContainer attributesContainer);
}
