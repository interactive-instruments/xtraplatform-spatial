package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.streams.domain.Reactive.TranformerCustomFuseable;
import de.ii.xtraplatform.streams.domain.Reactive.TranformerCustomFuseableIn;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class FeatureEventTransformer implements
    TranformerCustomFuseable<Object, FeatureEventConsumer>,
    FeatureEventConsumer {

  private final FeatureTokenReader tokenReader;
  private FeatureEventConsumer downstream;

  protected FeatureEventTransformer() {
    this.tokenReader = new FeatureTokenReader(this);
  }

  @Override
  public Class<FeatureEventConsumer> getFusionInterface() {
    return FeatureEventConsumer.class;
  }

  @Override
  public void fuse(
      TranformerCustomFuseableIn<Object, ?, FeatureEventConsumer> tranformerCustomFuseableIn) {
    if (!canFuse(tranformerCustomFuseableIn)) {
      throw new IllegalArgumentException();
    }
    if (Objects.isNull(downstream)) {
      this.downstream = tranformerCustomFuseableIn.fuseableSink();
      init();
    }
  }

  @Override
  public final void init(Consumer<Object> push) {
    if (Objects.isNull(downstream)) {
      this.downstream = (FeatureTokenEmitter) push::accept;
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
  public FeatureEventConsumer fuseableSink() {
    return this;
  }

  protected final FeatureEventConsumer getDownstream() {
    return downstream;
  }

  protected void init() {
  }

  protected void cleanup() {
  }

}
