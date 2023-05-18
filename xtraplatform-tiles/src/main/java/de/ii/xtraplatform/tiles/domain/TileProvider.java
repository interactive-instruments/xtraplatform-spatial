/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.store.domain.entities.PersistentEntity;
import java.util.Optional;

public interface TileProvider extends PersistentEntity {

  @Override
  TileProviderData getData();

  @Override
  default String getType() {
    return TileProviderData.ENTITY_TYPE;
  }

  TileResult getTile(TileQuery tileQuery);

  default void deleteFromCache(
      String tileset, TileMatrixSetBase tileMatrixSet, TileMatrixSetLimits limits) {}

  Optional<TilesetMetadata> metadata(String tileset);

  default boolean supportsGeneration() {
    return this instanceof TileGenerator;
  }

  default TileGenerator generator() {
    if (!supportsGeneration()) {
      throw new UnsupportedOperationException("Generation not supported");
    }
    return (TileGenerator) this;
  }

  default boolean supportsSeeding() {
    return this instanceof TileSeeding;
  }

  default TileSeeding seeding() {
    if (!supportsGeneration()) {
      throw new UnsupportedOperationException("Seeding not supported");
    }
    return (TileSeeding) this;
  }

  default Optional<TileResult> validate(TileQuery tile) {
    Optional<TilesetMetadata> metadata = metadata(tile.getTileset());

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
