/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.streams.domain.Reactive.Source;
import de.ii.xtraplatform.streams.domain.Reactive.TransformerCustomFuseableIn;
import de.ii.xtraplatform.streams.domain.Reactive.TranformerCustomFuseableOut;
import de.ii.xtraplatform.streams.domain.Reactive.TransformerCustomSource;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class FeatureTokenDecoder<T> implements
    TranformerCustomFuseableOut<T, Object, FeatureEventHandler>,
    TransformerCustomSource<T, Object, FeatureTokenSource>,
    FeatureTokenContext<ModifiableContext> {

  private FeatureEventHandler<ModifiableContext> downstream;

  @Override
  public final Class<? extends FeatureEventHandler> getFusionInterface() {
    return FeatureEventHandler.class;
  }

  @Override
  public final boolean canFuse(
      TransformerCustomFuseableIn<Object, ?, ?> transformerCustomFuseableIn) {
    boolean isTransformerFuseable = TranformerCustomFuseableOut.super.canFuse(
        transformerCustomFuseableIn);

    //TODO: not required here because ModifiableContext is the base context, so enforced by FeatureTokenContext
    // move to FeatureTokenTransformer
    if (isTransformerFuseable && transformerCustomFuseableIn instanceof FeatureTokenContext<?>) {
      if (!ModifiableContext.class.isAssignableFrom(((FeatureTokenContext<?>) transformerCustomFuseableIn).getContextInterface())) {
        throw new IllegalStateException("Cannot fuse FeatureTokenTransformer: " + ((FeatureTokenContext<?>) transformerCustomFuseableIn).getContextInterface() + " does not extend " + this.getContextInterface());
      }
    }

    return isTransformerFuseable;
  }

  @Override
  public final void fuse(
      TransformerCustomFuseableIn<Object, ?, ? extends FeatureEventHandler> transformerCustomFuseableIn) {
    if (!canFuse(transformerCustomFuseableIn)) {
      throw new IllegalArgumentException();
    }
    if (Objects.isNull(downstream)) {
      this.downstream = transformerCustomFuseableIn.fuseableSink();

      transformerCustomFuseableIn.afterInit(this::init);
      //init();
    }
  }

  @Override
  public final void init(Consumer<Object> push) {
    if (Objects.isNull(downstream)) {
      this.downstream = (FeatureTokenEmitter2) push::accept;
      init();
    }
  }

  @Override
  public final void onComplete() {
    cleanup();
  }

  @Override
  public FeatureTokenSource getCustomSource(Source<Object> source) {
    return new FeatureTokenSource(source);
  }

  @Override
  public Class<? extends ModifiableContext> getContextInterface() {
    if (downstream instanceof FeatureTokenContext<?>) {
      return ((FeatureTokenContext<?>) downstream).getContextInterface();
    }

    return ModifiableContext.class;
  }

  @Override
  public final ModifiableContext createContext() {
    if (downstream instanceof FeatureTokenContext<?>) {
      return ((FeatureTokenContext<?>) downstream).createContext();
    }

    return ModifiableGenericContext.create();
  }

  protected final FeatureEventHandler<ModifiableContext> getDownstream() {
    return downstream;
  }

  protected void init() {
  }

  protected void cleanup() {
  }
}
