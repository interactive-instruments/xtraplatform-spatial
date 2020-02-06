package de.ii.xtraplatform.features.domain;

public interface FeatureQueryTransformer<T> {

    T transformQuery(FeatureQuery featureQuery);
}
