package de.ii.xtraplatform.features.domain;

import java.util.List;

public interface FeatureStoreMultiplicityTracker {
    void reset();

    void track(List<String> path, List<Long> ids);

    List<Integer> getMultiplicitiesForPath(List<String> path);
}
