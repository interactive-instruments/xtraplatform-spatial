/**
 * Copyright 2021 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureStream.ResultBase;
import java.util.List;
import org.immutables.value.Value;

public interface FeatureTransactions {

  @Value.Immutable
  interface MutationResult extends FeatureStream.ResultBase {

    abstract class Builder extends ResultBase.Builder<MutationResult, MutationResult.Builder> {
      public abstract Builder addIds(String... ids);
    }

    List<String> getIds();
  }

  MutationResult createFeatures(String featureType, FeatureTokenSource featureTokenSource);

  MutationResult updateFeature(String type, String id, FeatureTokenSource featureTokenSource);

  MutationResult deleteFeature(String featureType, String id);
}
