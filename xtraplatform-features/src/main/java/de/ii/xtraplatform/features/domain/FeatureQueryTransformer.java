package de.ii.xtraplatform.features.domain;

import java.util.Map;

public interface FeatureQueryTransformer<T> {

    String PROPERTY_NOT_AVAILABLE = "PROPERTY_NOT_AVAILABLE";

    T transformQuery(FeatureQuery featureQuery,
                     Map<String, String> additionalQueryParameters);
}
