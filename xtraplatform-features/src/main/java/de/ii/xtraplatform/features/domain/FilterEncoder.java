package de.ii.xtraplatform.features.domain;

public interface FilterEncoder<T> {

    T encode(String cqlFilter, FeatureStoreInstanceContainer typeInfo);
}
