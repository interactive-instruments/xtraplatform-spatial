/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import de.ii.xtraplatform.base.domain.resiliency.OptionalVolatileCapability;
import de.ii.xtraplatform.base.domain.resiliency.VolatileComposed;
import de.ii.xtraplatform.entities.domain.PersistentEntity;
import de.ii.xtraplatform.features.domain.FeatureProvider.FeatureVolatileCapability;

public interface TileProvider extends PersistentEntity, VolatileComposed {

  @Override
  TileProviderData getData();

  @Override
  default String getType() {
    return TileProviderData.ENTITY_TYPE;
  }

  default OptionalVolatileCapability<TileAccess> access() {
    return new FeatureVolatileCapability<>(TileAccess.class, TileAccess.CAPABILITY, this);
  }

  default OptionalVolatileCapability<TileGenerator> generator() {
    return OptionalVolatileCapability.unsupported(TileGenerator.class);
  }

  default OptionalVolatileCapability<TileSeeding> seeding() {
    return new FeatureVolatileCapability<>(
        TileSeeding.class, TileSeeding.CAPABILITY, this, () -> generator().isSupported());
  }
}
