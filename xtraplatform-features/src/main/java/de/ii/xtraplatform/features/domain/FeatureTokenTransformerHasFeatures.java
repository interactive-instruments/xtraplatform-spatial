/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.ImmutableResult.Builder;
import java.util.function.Consumer;

public class FeatureTokenTransformerHasFeatures extends FeatureTokenTransformer {

  private final Consumer<Boolean> setter;
  private boolean done;

  public FeatureTokenTransformerHasFeatures(Builder resultBuilder) {
    this.setter = resultBuilder::hasFeatures;
    this.done = false;
  }

  public <X> FeatureTokenTransformerHasFeatures(ImmutableResultReduced.Builder<X> resultBuilder) {
    this.setter = resultBuilder::hasFeatures;
    this.done = false;
  }

  @Override
  public void onFeatureStart(ModifiableContext<FeatureSchema, SchemaMapping> context) {
    if (!done) {
      this.done = true;
      setter.accept(true);
    }

    super.onFeatureStart(context);
  }
}
