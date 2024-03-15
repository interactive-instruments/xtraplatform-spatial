/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.entities.domain.PersistentEntity;

public interface FeatureProviderEntity extends FeatureProvider, PersistentEntity {

  @Override
  default String getId() {
    return PersistentEntity.super.getId();
  }

  @Override
  FeatureProviderDataV2 getData();
}
