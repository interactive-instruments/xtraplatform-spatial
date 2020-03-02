package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.cql.domain.CqlFilter;

public interface FilterEncoder<T> {

    T encode(CqlFilter cqlFilter, FeatureStoreInstanceContainer typeInfo);
}
