/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import de.ii.xtraplatform.crs.domain.BoundingBox;
import java.util.Optional;

public interface TileAccess {
  String CAPABILITY = "access";

  TileResult getTile(TileQuery tileQuery);

  Optional<TilesetMetadata> getMetadata(String tileset);

  default boolean tilesMayBeUnavailable() {
    return false;
  }

  default boolean tilesetHasVectorTiles(String tileset) {
    return getMetadata(tileset).map(TilesetMetadata::isVector).orElse(false);
  }

  default boolean tilesetHasMapTiles(String tileset) {
    return getMetadata(tileset).map(TilesetMetadata::isRaster).orElse(false);
  }

  default Optional<TileResult> validate(TileQuery tile) {
    Optional<TilesetMetadata> metadata = getMetadata(tile.getTileset());

    if (metadata.isEmpty()) {
      return Optional.of(
          TileResult.error(String.format("Tileset '%s' is not supported.", tile.getTileset())));
    }

    if (!metadata.get().getTileMatrixSets().contains(tile.getTileMatrixSet().getId())) {
      return Optional.of(
          TileResult.error(
              String.format(
                  "Tile matrix set '%s' is not supported.", tile.getTileMatrixSet().getId())));
    }

    if (!metadata
        .get()
        .getTmsRanges()
        .get(tile.getTileMatrixSet().getId())
        .contains(tile.getLevel())) {
      return Optional.of(
          TileResult.outsideLimits(
              "The requested tile is outside the zoom levels for this tileset."));
    }

    BoundingBox boundingBox =
        tile.getGenerationParameters()
            .flatMap(TileGenerationParameters::getClipBoundingBox)
            .orElse(tile.getTileMatrixSet().getBoundingBox());
    TileMatrixSetLimits limits = tile.getTileMatrixSet().getLimits(tile.getLevel(), boundingBox);

    if (!limits.contains(tile.getRow(), tile.getCol())) {
      return Optional.of(
          TileResult.outsideLimits(
              "The requested tile is outside of the limits for this zoom level and tileset."));
    }

    return Optional.empty();
  }
}
