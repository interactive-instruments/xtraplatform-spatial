/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import java.util.Objects;

public abstract class FeatureTokenTransformer extends
    FeatureTokenTransformerBase<ModifiableContext> {

  private ModifiableContext context;

  @Override
  public Class<? extends ModifiableContext> getContextInterface() {
    if (getDownstream() instanceof FeatureTokenContext<?>) {
      return ((FeatureTokenContext<?>) getDownstream()).getContextInterface();
    }

    return ModifiableContext.class;
  }

  @Override
  public final ModifiableContext createContext() {
    ModifiableContext context = getDownstream() instanceof FeatureTokenContext<?>
        ? ((FeatureTokenContext<?>) getDownstream()).createContext()
        : ModifiableGenericContext.create();

    if (Objects.isNull(this.context)) {
      this.context = context;
    }

    return context;
  }

  protected final ModifiableContext getContext() {
    if (Objects.isNull(context)) {
      return createContext();
    }

    return context;
  }
}