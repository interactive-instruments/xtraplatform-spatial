/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.github.azahnen.dagger.annotations.AutoMultiBind;
import java.util.function.BiConsumer;

@AutoMultiBind
public interface FeatureQueriesExtension {

  enum LIFECYCLE_HOOK {STARTED}

  enum QUERY_HOOK {BEFORE, AFTER}

  boolean isSupported(FeatureProviderConnector<?,?,?> connector);

  void on(LIFECYCLE_HOOK hook, FeatureProviderDataV2 data, FeatureProviderConnector<?, ?, ?> connector);

  void on(QUERY_HOOK hook, FeatureProviderDataV2 data, FeatureProviderConnector<?, ?, ?> connector,
      FeatureQuery query,
      BiConsumer<String, String> aliasResolver);

}
