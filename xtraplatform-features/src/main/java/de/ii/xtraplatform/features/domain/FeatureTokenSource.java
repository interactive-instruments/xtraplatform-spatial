/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.streams.domain.Reactive.BasicStream;
import de.ii.xtraplatform.streams.domain.Reactive.Sink;
import de.ii.xtraplatform.streams.domain.Reactive.Source;
import de.ii.xtraplatform.streams.domain.Reactive.Transformer;

public class FeatureTokenSource implements Source<Object> {

  private final Source<Object> delegate;

  public FeatureTokenSource(Source<Object> delegate) {
    this.delegate = delegate;
  }

  @Override
  public <U> Source<U> via(Transformer<Object, U> transformer) {
    return delegate.via(transformer);
  }

  @Override
  public <V> BasicStream<Object, V> to(Sink<Object, V> sink) {
    return delegate.to(sink);
  }
}
