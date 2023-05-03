/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import java.util.Optional;

public interface WithFeatureProvider {
  /**
   * @langEn The id of the feature provider. By default the tile provider id without `-tiles` is
   *     used.
   * @langDe Die Id des Feature-Providers. Standardmäßig wird die Tile-Provider-Id ohne `-tiles`
   *     verwendet.
   * @default null
   * @since v3.4
   */
  Optional<String> getFeatureProvider();
}
