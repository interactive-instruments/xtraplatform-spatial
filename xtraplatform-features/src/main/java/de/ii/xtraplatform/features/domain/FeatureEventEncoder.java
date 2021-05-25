package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.streams.domain.Reactive.TranformerCustomFuseableIn;
import java.util.function.Consumer;

public abstract class FeatureEventEncoder<T> implements
    TranformerCustomFuseableIn<Object, T, FeatureEventConsumer>,
    FeatureEventConsumer {

  private final FeatureTokenReader tokenReader;
  private Consumer<T> downstream;

  protected FeatureEventEncoder() {
    this.tokenReader = new FeatureTokenReader(this);
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
  public FeatureEventConsumer fuseableSink() {
    return this;
  }

  protected final void push(T t) {
    downstream.accept(t);
  }

  protected void init() {
  }

  protected void cleanup() {
  }
}
