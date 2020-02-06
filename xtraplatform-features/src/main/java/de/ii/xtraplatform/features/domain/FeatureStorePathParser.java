package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureStoreInstanceContainer;

import java.util.List;

public interface FeatureStorePathParser {

    interface PathSyntax {}

    List<FeatureStoreInstanceContainer> parse(List<String> paths);
}
