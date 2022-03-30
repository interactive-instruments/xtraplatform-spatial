package de.ii.xtraplatform.features.domain;

public interface FeatureChangeHandler {

  void addListener(FeatureChangeListener listener);

  void handle(FeatureChange change);
}
