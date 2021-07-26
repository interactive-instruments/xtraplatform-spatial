/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.streams.domain.Reactive.Source;
import de.ii.xtraplatform.streams.domain.Reactive.TranformerCustomFuseable;
import de.ii.xtraplatform.streams.domain.Reactive.TranformerCustomFuseableIn;
import de.ii.xtraplatform.streams.domain.Reactive.TransformerCustomSource;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class FeatureTokenTransformerBase<T extends ModifiableContext> implements
    TranformerCustomFuseable<Object, FeatureEventHandler>,
    TransformerCustomSource<Object, Object, FeatureTokenSource>,
    FeatureEventHandler<T>,
    FeatureTokenContext<T> {

  private FeatureEventHandler<T> downstream;
  private FeatureTokenReader<T> tokenReader;
  private Runnable afterInit;

  @Override
  public Class<? extends FeatureEventHandler> getFusionInterface() {
    return FeatureEventHandler.class;
  }

  @Override
  public final boolean canFuse(
      TranformerCustomFuseableIn<Object, ?, ?> tranformerCustomFuseableIn) {
    boolean isTransformerFuseable = TranformerCustomFuseable.super.canFuse(tranformerCustomFuseableIn);

    if (isTransformerFuseable && tranformerCustomFuseableIn instanceof FeatureTokenContext<?>) {
      if (!ModifiableContext.class.isAssignableFrom(((FeatureTokenContext<?>) tranformerCustomFuseableIn).getContextInterface())) {
        throw new IllegalStateException("Cannot fuse FeatureTokenTransformer: " + ((FeatureTokenContext<?>) tranformerCustomFuseableIn).getContextInterface() + " does not extend " + this.getContextInterface());
      }
    }

    return isTransformerFuseable;
  }

  @Override
  public final void fuse(
      TranformerCustomFuseableIn<Object, ?, ? extends FeatureEventHandler> tranformerCustomFuseableIn) {
    if (!canFuse(tranformerCustomFuseableIn)) {
      throw new IllegalArgumentException();
    }
    if (Objects.isNull(downstream)) {
      this.downstream = tranformerCustomFuseableIn.fuseableSink();
      //TODO this.tokenReader = new FeatureTokenReader<>(this, createContext());

      tranformerCustomFuseableIn.afterInit(this::init);
      //init();
    }
  }

  @Override
  public final void init(Consumer<Object> push) {
    if (Objects.isNull(downstream)) {
      this.downstream = (FeatureTokenEmitter2<T>) push::accept;
      //TODO this.tokenReader = new FeatureTokenReader<>(this, createContext());
      init();
    }
  }

  @Override
  public final void onPush(Object token) {
    tokenReader.onToken(token);
  }

  @Override
  public final void onComplete() {
    cleanup();
  }

  @Override
  public final FeatureEventHandler<T> fuseableSink() {
    return this;
  }

  @Override
  public final FeatureTokenSource getCustomSource(Source<Object> source) {
    return new FeatureTokenSource(source);
  }

  protected final FeatureEventHandler<T> getDownstream() {
    return downstream;
  }

  @Override
  public void afterInit(Runnable runnable) {
    this.afterInit = runnable;
  }

  protected void init() {
    if (Objects.nonNull(afterInit)) {
      afterInit.run();
    }
  }

  protected void cleanup() {
  }

  @Override
  public void onStart(T context) {
    getDownstream().onStart(context);
  }

  @Override
  public void onEnd(T context) {
    getDownstream().onEnd(context);
  }

  @Override
  public void onFeatureStart(T context) {
    getDownstream().onFeatureStart(context);
  }

  @Override
  public void onFeatureEnd(T context) {
    getDownstream().onFeatureEnd(context);
  }

  @Override
  public void onObjectStart(T context) {
    getDownstream().onObjectStart(context);
  }

  @Override
  public void onObjectEnd(T context) {
    getDownstream().onObjectEnd(context);
  }

  @Override
  public void onArrayStart(T context) {
    getDownstream().onArrayStart(context);
  }

  @Override
  public void onArrayEnd(T context) {
    getDownstream().onArrayEnd(context);
  }

  @Override
  public void onValue(T context) {
    getDownstream().onValue(context);
  }
}
