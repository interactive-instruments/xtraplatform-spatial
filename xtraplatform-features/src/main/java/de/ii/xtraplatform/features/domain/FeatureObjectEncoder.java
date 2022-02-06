package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.FeatureEventHandlerGeneric.GenericContext;

public abstract class FeatureObjectEncoder<T extends PropertyBase<T, FeatureSchema>, U extends FeatureBase<T, FeatureSchema>> extends
    FeatureObjectEncoderBase<FeatureSchema, SchemaMapping, T, U> implements FeatureTokenEncoder<ModifiableContext<FeatureSchema, SchemaMapping>> {


  @Override
  public Class<? extends ModifiableContext<FeatureSchema, SchemaMapping>> getContextInterface() {
    return GenericContext.class;
  }

  @Override
  public ModifiableContext<FeatureSchema, SchemaMapping> createContext() {
    return ModifiableGenericContext.create();
  }

}
