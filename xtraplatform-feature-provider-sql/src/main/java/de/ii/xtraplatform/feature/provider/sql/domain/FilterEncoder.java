package de.ii.xtraplatform.feature.provider.sql.domain;

public interface FilterEncoder<T> {

    T encode(String cqlFilter, FeatureStoreInstanceContainer typeInfo);
}
