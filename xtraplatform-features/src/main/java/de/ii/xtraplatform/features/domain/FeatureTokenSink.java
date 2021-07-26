package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.streams.domain.Reactive.Sink;

public class FeatureTokenSink implements Sink<Object, Void> {

  private final Sink<Object, Void> delegate;

  public FeatureTokenSink(Sink<Object, Void> delegate) {
    this.delegate = delegate;
  }

}
