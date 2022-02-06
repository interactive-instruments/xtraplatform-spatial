package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;

public interface FeatureTokenEncoder<T extends ModifiableContext<FeatureSchema, SchemaMapping>> extends
    FeatureTokenEncoderGeneric<FeatureSchema, SchemaMapping, T> {

}
