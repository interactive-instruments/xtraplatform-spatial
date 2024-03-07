/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.base.domain.resiliency.OptionalVolatileCapability;
import de.ii.xtraplatform.base.domain.resiliency.VolatileComposed;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry.ChangeHandler;
import de.ii.xtraplatform.entities.domain.PersistentEntity;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public interface FeatureProvider2 extends PersistentEntity, VolatileComposed {

  String PROVIDER_TYPE = "FEATURE";

  @Override
  default String getType() {
    return ProviderData.ENTITY_TYPE;
  }

  @Override
  FeatureProviderDataV2 getData();

  // TODO: FeatureChanges?
  FeatureChangeHandler getChangeHandler();

  default OptionalVolatileCapability<FeatureQueries> queries() {
    return new FeatureVolatileCapability<>(FeatureQueries.class, FeatureQueries.CAPABILITY, this);
  }

  default OptionalVolatileCapability<FeatureExtents> extents() {
    return new FeatureVolatileCapability<>(FeatureExtents.class, FeatureExtents.CAPABILITY, this);
  }

  default OptionalVolatileCapability<FeatureQueriesPassThrough> passThrough() {
    return new FeatureVolatileCapability<>(
        FeatureQueriesPassThrough.class, FeatureQueriesPassThrough.CAPABILITY, this);
  }

  default boolean supportsMutationsInternal() {
    return false;
  }

  default OptionalVolatileCapability<FeatureTransactions> mutations() {
    return new FeatureVolatileCapability<>(
        FeatureTransactions.class,
        FeatureTransactions.CAPABILITY,
        this,
        this::supportsMutationsInternal);
  }

  default boolean supportsCrsInternal() {
    return getData().getNativeCrs().isPresent();
  }

  default OptionalVolatileCapability<FeatureCrs> crs() {
    return new FeatureVolatileCapability<>(
        FeatureCrs.class, FeatureCrs.CAPABILITY, this, this::supportsCrsInternal);
  }

  default OptionalVolatileCapability<FeatureMetadata> metadata() {
    return new FeatureVolatileCapability<>(FeatureMetadata.class, FeatureMetadata.CAPABILITY, this);
  }

  default OptionalVolatileCapability<MultiFeatureQueries> multiQueries() {
    return new FeatureVolatileCapability<>(
        MultiFeatureQueries.class, MultiFeatureQueries.CAPABILITY, this);
  }

  // TODO: to QueryCapabilities
  default boolean supportsSorting() {
    return false;
  }

  // TODO: to QueryCapabilities
  default boolean supportsHighLoad() {
    return false;
  }

  default String getCapabilityKey(String subKey) {
    return String.format("%s/%s", getUniqueKey(), subKey);
  }

  class FeatureVolatileCapability<T> implements OptionalVolatileCapability<T> {

    private final Class<T> clazz;
    private final String key;
    private final VolatileComposed composed;
    private final Supplier<Boolean> onlyIf;

    public FeatureVolatileCapability(Class<T> clazz, String key, VolatileComposed composed) {
      this(clazz, key, composed, null);
    }

    public FeatureVolatileCapability(
        Class<T> clazz, String key, VolatileComposed composed, Supplier<Boolean> onlyIf) {
      this.clazz = clazz;
      this.key = key;
      this.composed = composed;
      this.onlyIf = onlyIf;
    }

    @Override
    public State getState() {
      return composed.getState(key);
    }

    @Override
    public Optional<String> getMessage() {
      return composed.getMessage(key);
    }

    @Override
    public Runnable onStateChange(ChangeHandler handler, boolean initialCall) {
      return composed.onStateChange(key, handler, initialCall);
    }

    @Override
    public boolean isSupported() {
      return clazz.isAssignableFrom(composed.getClass())
          && (Objects.isNull(onlyIf) || onlyIf.get());
    }

    @Override
    public boolean isAvailable() {
      return isSupported() && OptionalVolatileCapability.super.isAvailable();
    }

    @Override
    public T get() {
      if (!isSupported()) {
        throw new UnsupportedOperationException(key + " not supported");
      }
      return clazz.cast(composed);
    }
  }
}
