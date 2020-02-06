package de.ii.xtraplatform.features.domain;

public interface FeatureQueries {

    FeatureStream2 getFeatureStream2(FeatureQuery query);

    long getFeatureCount(FeatureQuery featureQuery);

}
