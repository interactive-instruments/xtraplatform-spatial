package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.streams.domain.Reactive.Source;
import de.ii.xtraplatform.streams.domain.Reactive.TranformerCustomFuseableIn;
import de.ii.xtraplatform.streams.domain.Reactive.TranformerCustomFuseableOut;
import de.ii.xtraplatform.streams.domain.Reactive.TransformerCustomSource;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class FeatureEventDecoder<T> implements
    TranformerCustomFuseableOut<T, Object, FeatureEventConsumer>,
    TransformerCustomSource<T, Object, FeatureTokenSource> {

  private FeatureEventConsumer downstream;

  @Override
  public final Class<FeatureEventConsumer> getFusionInterface() {
    return FeatureEventConsumer.class;
  }

  @Override
  public final boolean canFuse(
      TranformerCustomFuseableIn<Object, ?, ?> tranformerCustomFuseableIn) {
    return TranformerCustomFuseableOut.super.canFuse(tranformerCustomFuseableIn);
  }

  @Override
  public final void fuse(
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
  public final void onComplete() {
    cleanup();
  }

  @Override
  public FeatureTokenSource getCustomSource(Source<Object> source) {
    return new FeatureTokenSource(source);
  }

  protected final FeatureEventConsumer getDownstream() {
    return downstream;
  }

  protected void init() {
  }

  protected void cleanup() {
  }
}
