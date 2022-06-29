/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.FeatureEventHandlerGeneric.GenericContext;
import java.util.Objects;

public abstract class FeatureTokenTransformer
    extends FeatureTokenTransformerBase<
        FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>> {

  private ModifiableContext<FeatureSchema, SchemaMapping> context;

  @Override
  public Class<? extends ModifiableContext<FeatureSchema, SchemaMapping>> getContextInterface() {
    if (getDownstream() instanceof FeatureTokenContext<?>) {
      return ((FeatureTokenContext<ModifiableContext<FeatureSchema, SchemaMapping>>)
              getDownstream())
          .getContextInterface();
    }

    return GenericContext.class;
  }

  @Override
  public final ModifiableContext<FeatureSchema, SchemaMapping> createContext() {
    ModifiableContext<FeatureSchema, SchemaMapping> context =
        getDownstream() instanceof FeatureTokenContext<?>
            ? ((FeatureTokenContext<ModifiableContext<FeatureSchema, SchemaMapping>>)
                    getDownstream())
                .createContext()
            : ModifiableGenericContext.create();

    if (Objects.isNull(this.context)) {
      this.context = context;
    }

    return context;
  }

  protected final ModifiableContext<FeatureSchema, SchemaMapping> getContext() {
    if (Objects.isNull(context)) {
      return createContext();
    }

    return context;
  }
}
