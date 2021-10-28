package de.ii.xtraplatform.features.domain;

import java.util.function.BiConsumer;

public interface FeatureQueriesExtension {

  enum LIFECYCLE_HOOK {STARTED}

  enum QUERY_HOOK {BEFORE, AFTER}

  boolean isSupported(FeatureProviderConnector<?,?,?> connector);

  void on(LIFECYCLE_HOOK hook, FeatureProviderDataV2 data, FeatureProviderConnector<?, ?, ?> connector);

  void on(QUERY_HOOK hook, FeatureProviderDataV2 data, FeatureProviderConnector<?, ?, ?> connector,
      FeatureQuery query,
      BiConsumer<String, String> aliasResolver);

}
