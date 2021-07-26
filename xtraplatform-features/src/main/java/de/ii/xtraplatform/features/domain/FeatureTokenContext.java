package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;

public interface FeatureTokenContext<T extends ModifiableContext> {

  Class<? extends T> getContextInterface();

  T createContext();

}
