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
