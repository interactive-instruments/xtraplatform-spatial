package de.ii.xtraplatform.features.domain;

import java.util.Map;

public interface FeatureQueryTransformer<T> {

    T transformQuery(FeatureQuery featureQuery,
                     Map<String, String> additionalQueryParameters);
}
