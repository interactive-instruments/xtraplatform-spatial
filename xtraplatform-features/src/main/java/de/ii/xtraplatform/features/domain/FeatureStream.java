/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.streams.domain.Reactive.Sink;
import de.ii.xtraplatform.streams.domain.Reactive.SinkReduced;
import de.ii.xtraplatform.streams.domain.Reactive.SinkReducedTransformed;
import de.ii.xtraplatform.streams.domain.Reactive.SinkTransformed;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.immutables.value.Value;

public interface FeatureStream {

  @Value.Immutable
  interface Result extends ResultBase {

    abstract class Builder extends ResultBase.Builder<Result, Builder> {

    }
  }

  @Value.Immutable
  interface ResultReduced<T> extends ResultBase {

    abstract class Builder<T> extends ResultBase.Builder<ResultReduced<T>, Builder<T>> {

      public abstract Builder<T> reduced(T bytes);
    }

    T reduced();
  }

  interface ResultBase {

    abstract class Builder<T extends ResultBase, U extends Builder<T, U>> {

      public abstract U isEmpty(boolean isEmpty);

      public abstract U error(Throwable error);

      public abstract T build();
    }

    @Value.Derived
    default boolean isSuccess() {
      return getError().isEmpty();
    }

    boolean isEmpty();

    Optional<Throwable> getError();
  }

  CompletionStage<Result> runWith(Sink<Object> sink, Optional<PropertyTransformations> propertyTransformations);

  <X> CompletionStage<ResultReduced<X>> runWith(SinkReduced<Object, X> sink, Optional<PropertyTransformations> propertyTransformations);

  //CompletionStage<Result> runWith(SinkTransformed<Object, byte[]> sink, Optional<PropertyTransformations> propertyTransformations);

  //CompletionStage<ResultReduced<byte[]>> runWith(SinkReducedTransformed<Object, byte[], byte[]> sink, Optional<PropertyTransformations> propertyTransformations);
}
