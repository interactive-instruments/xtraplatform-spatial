/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.docs.DocIgnore;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableMap;
import de.ii.xtraplatform.tiles.domain.ImmutableMinMax.Builder;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableTilesetMbTilesDefaults.Builder.class)
public interface TilesetMbTilesDefaults extends TilesetCommonDefaults {

  @DocIgnore
  @Override
  Optional<LonLat> getCenter();

  @DocIgnore
  @Override
  BuildableMap<MinMax, Builder> getLevels();
}
