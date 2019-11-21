package de.ii.xtraplatform.feature.provider.api;

public interface FeatureQueries {

    FeatureStream2 getFeatureStream2(FeatureQuery query);

    long getFeatureCount(FeatureQuery featureQuery);

}
