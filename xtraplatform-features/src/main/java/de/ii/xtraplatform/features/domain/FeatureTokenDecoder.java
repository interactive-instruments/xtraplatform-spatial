package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.streams.domain.Reactive.Source;
import de.ii.xtraplatform.streams.domain.Reactive.TranformerCustomFuseableIn;
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
      TranformerCustomFuseableIn<Object, ?, ?> tranformerCustomFuseableIn) {
    boolean isTransformerFuseable = TranformerCustomFuseableOut.super.canFuse(tranformerCustomFuseableIn);

    //TODO: not required here because ModifiableContext is the base context, so enforced by FeatureTokenContext
    // move to FeatureTokenTransformer
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

      tranformerCustomFuseableIn.afterInit(this::init);
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
