/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;

public interface Decoder extends AutoCloseable {

  void decode(byte[] data, Pipeline pipeline);

  default void reset() {
    reset(true);
  }

  default void reset(boolean full) {}

  interface Pipeline {
    ModifiableContext<FeatureSchema, SchemaMapping> context();

    FeatureEventHandler<
            FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>>
        downstream();
  }
}
