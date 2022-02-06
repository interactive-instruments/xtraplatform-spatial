package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.streams.domain.Reactive.TransformerCustomFuseableIn;

public interface FeatureTokenEncoderGeneric<T extends SchemaBase<T>, U extends SchemaMappingBase<T>, V extends ModifiableContext<T, U>> extends
    TransformerCustomFuseableIn<Object, byte[], FeatureEventHandler<T, U, V>>,
    //TODO: TransformerCustomSink<Object, byte[], FeatureTokenSinkReduced<?>>,
    FeatureEventHandler<T, U, V>,
    FeatureTokenContext<V>{

}
