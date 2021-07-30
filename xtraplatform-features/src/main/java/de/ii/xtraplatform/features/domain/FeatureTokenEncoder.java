/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.streams.domain.Reactive.TransformerCustomFuseableIn;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class FeatureTokenEncoder<U extends ModifiableContext> implements
    TransformerCustomFuseableIn<Object, byte[], FeatureEventHandler<U>>,
    //TODO: TransformerCustomSink<Object, byte[], FeatureTokenSinkReduced<?>>,
    FeatureEventHandler<U>,
    FeatureTokenContext<U> {

  private final FeatureTokenReader tokenReader;
  private Consumer<byte[]> downstream;
  private Runnable afterInit;

  protected FeatureTokenEncoder() {
    this.tokenReader = new FeatureTokenReader(this, null);
  }

  @Override
  public final void init(Consumer<byte[]> push) {
    this.downstream = push;
    init();
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
  public FeatureEventHandler<U> fuseableSink() {
    return this;
  }

  protected final void push(byte[] t) {
    downstream.accept(t);
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
}
