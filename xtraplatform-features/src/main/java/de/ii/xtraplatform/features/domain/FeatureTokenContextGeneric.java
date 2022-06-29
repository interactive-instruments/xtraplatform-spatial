/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;

public class FeatureTokenContextGeneric implements FeatureTokenContext<ModifiableContext> {

  @Override
  public Class<? extends ModifiableContext> getContextInterface() {
    return ModifiableContext.class;
  }

  @Override
  public ModifiableContext createContext() {
    ModifiableGenericContext context = ModifiableGenericContext.create();
    ModifiableCollectionMetadata collectionMetadata = ModifiableCollectionMetadata.create();
    context.setMetadata(collectionMetadata);

    return context;
  }
}
