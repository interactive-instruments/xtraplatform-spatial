package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.FeatureEventHandlerGeneric.GenericContext;
import org.immutables.value.Value;
import org.immutables.value.Value.Modifiable;

//TODO: more comfortable variant of consumer, use in encoder/transformer
public interface FeatureEventHandlerGeneric extends FeatureEventHandler<ModifiableContext>, FeatureTokenContext<GenericContext> {

  @Modifiable
  @Value.Style(deepImmutablesDetection = true)
  interface GenericContext extends ModifiableContext {}

  @Override
  default GenericContext createContext() {
    return ModifiableGenericContext.create();
  }
}
