package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import java.util.Objects;

public interface FeatureTokenEmitter2<T extends ModifiableContext> extends FeatureEventHandler<T> {

  void push(Object token);

  @Override
  default void onStart(T context) {
    push(FeatureTokenType.INPUT);

    if (context.metadata().isSingleFeature()) {
      push(true);
    } else {
      if (context.metadata().getNumberReturned().isPresent()) {
        push(context.metadata().getNumberReturned().getAsLong());
      }
      if (context.metadata().getNumberMatched().isPresent()) {
        push(context.metadata().getNumberMatched().getAsLong());
      }
    }
  }

  @Override
  default void onEnd(T context) {
    push(FeatureTokenType.INPUT_END);
  }

  @Override
  default void onFeatureStart(T context) {
    push(FeatureTokenType.FEATURE);
    if (!context.path().isEmpty()) {
      push(context.path());
    }
  }

  @Override
  default void onFeatureEnd(T context) {
    push(FeatureTokenType.FEATURE_END);
  }

  @Override
  default void onObjectStart(T context) {
    push(FeatureTokenType.OBJECT);
    push(context.path());
    if (context.geometryType().isPresent()) {
      push(context.geometryType().get());
    }
  }

  @Override
  default void onObjectEnd(T context) {
    push(FeatureTokenType.OBJECT_END);
  }

  @Override
  default void onArrayStart(T context) {
    push(FeatureTokenType.ARRAY);
    push(context.path());
  }

  @Override
  default void onArrayEnd(T context) {
    push(FeatureTokenType.ARRAY_END);
  }

  @Override
  default void onValue(T context) {
    push(FeatureTokenType.VALUE);
    push(context.path());
    if (Objects.nonNull(context.value())) {
      push(context.value());
      push(context.valueType());
    }
  }
}
