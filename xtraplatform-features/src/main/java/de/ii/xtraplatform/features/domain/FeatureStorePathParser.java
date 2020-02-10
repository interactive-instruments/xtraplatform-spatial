package de.ii.xtraplatform.features.domain;

import java.util.List;

public interface FeatureStorePathParser {

    interface PathSyntax {}

    List<FeatureStoreInstanceContainer> parse(FeatureType featureType);
}
