package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.streams.domain.Reactive.Sink;
import de.ii.xtraplatform.streams.domain.Reactive.TranformerCustomFuseableIn;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class FeatureTokenEncoder<T, U extends ModifiableContext> implements
    TranformerCustomFuseableIn<Object, T, FeatureEventHandler<U>>,
    FeatureEventHandler<U>,
    FeatureTokenContext<U> {

  private final FeatureTokenReader tokenReader;
  private Consumer<T> downstream;
  private Runnable afterInit;

  protected FeatureTokenEncoder() {
    this.tokenReader = new FeatureTokenReader(this, null);
  }

  @Override
  public final void init(Consumer<T> push) {
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

  protected final void push(T t) {
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
