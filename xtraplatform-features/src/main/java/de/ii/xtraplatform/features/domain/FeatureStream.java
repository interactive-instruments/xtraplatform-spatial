/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.streams.domain.Reactive.Sink;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.immutables.value.Value;

public interface FeatureStream {

  @Value.Immutable
  interface Result extends ResultBase {

    abstract class Builder extends ResultBase.Builder<Result, Builder> {

    }
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

  <X> CompletionStage<Result> runWith(FeatureTokenEncoder<X, ?> encoder, Sink<X, ?> sink,
      Optional<PropertyTransformations> propertyTransformations);
}
