package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;

public abstract class FeatureTokenEncoderDefault<T extends ModifiableContext<FeatureSchema, SchemaMapping>> extends
    FeatureTokenEncoderBase<FeatureSchema, SchemaMapping, T> implements FeatureTokenEncoder<T> {

}
