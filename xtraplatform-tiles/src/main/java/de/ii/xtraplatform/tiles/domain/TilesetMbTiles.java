/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;
import de.ii.xtraplatform.docs.DocIgnore;
import de.ii.xtraplatform.entities.domain.maptobuilder.BuildableMap;
import de.ii.xtraplatform.tiles.domain.ImmutableMinMax.Builder;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableTilesetMbTiles.Builder.class)
public interface TilesetMbTiles extends TilesetCommon {

  @DocIgnore
  @Override
  BuildableMap<MinMax, Builder> getLevels();

  @DocIgnore
  @Override
  Optional<LonLat> getCenter();

  /**
   * @langEn Filename of the MBTiles file in the `api-resources/tiles` directory.
   * @langDe Dateiname der MBTiles-Datei im Verzeichnis `api-resources/tiles`.
   * @default null
   * @since v3.4
   */
  String getSource();

  /**
   * @langEn Tile Matrix Set of the tiles in the MBTiles file.
   * @langDe Kachelschema der Kacheln in der MBTiles-Datei.
   * @default WebMercatorQuad
   * @since v3.6
   */
  @Value.Default
  default String getTileMatrixSet() {
    return "WebMercatorQuad";
  }

  @Value.Check
  default void checkSingleTileMatrixSet() {
    Preconditions.checkState(
        getLevels().size() <= 1,
        "There must be no more than one tile matrix set associated with an MBTiles file. Found: %s.",
        getLevels().size());
  }
}
