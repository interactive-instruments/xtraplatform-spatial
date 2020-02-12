package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.cql.domain.CqlPredicate;

public interface FilterEncoder<T> {

    T encode(CqlPredicate cqlFilter, FeatureStoreInstanceContainer typeInfo);
}
