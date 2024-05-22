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
import de.ii.xtraplatform.entities.domain.maptobuilder.Buildable;
import de.ii.xtraplatform.entities.domain.maptobuilder.BuildableBuilder;
import de.ii.xtraplatform.entities.domain.maptobuilder.BuildableMap;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableTilesetRaster.Builder.class)
public interface TilesetRaster extends TilesetCommonDefaults, Buildable<TilesetRaster> {
  /**
   * @langEn The tileset id.
   * @langDe Die Tileset-Id.
   * @since v4.0
   */
  Optional<String> getPrefix();

  @DocIgnore
  @Override
  BuildableMap<MinMax, ImmutableMinMax.Builder> getLevels();

  @DocIgnore
  @Override
  Optional<LonLat> getCenter();

  /**
   * @langEn List of styles.
   * @langDe Liste der Styles.
   * @since v4.0
   */
  List<String> getStyles();

  @Override
  default ImmutableTilesetRaster.Builder getBuilder() {
    return new ImmutableTilesetRaster.Builder().from(this);
  }

  abstract class Builder implements BuildableBuilder<TilesetRaster> {}
}
