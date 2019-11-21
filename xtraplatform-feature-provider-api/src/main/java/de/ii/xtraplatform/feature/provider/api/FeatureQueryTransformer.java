package de.ii.xtraplatform.feature.provider.api;

public interface FeatureQueryTransformer<T> {

    T transformQuery(FeatureQuery featureQuery);
}
