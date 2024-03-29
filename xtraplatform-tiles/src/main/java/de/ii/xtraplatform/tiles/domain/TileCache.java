/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import com.google.common.collect.Range;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.services.domain.TaskContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface TileCache {

  Logger LOGGER = LoggerFactory.getLogger(TileCache.class);

  void seed(
      Map<String, TileGenerationParameters> tilesets,
      List<MediaType> mediaTypes,
      boolean reseed,
      String tileSource,
      TaskContext taskContext)
      throws IOException;

  default boolean doSeed(
      Map<String, TileGenerationParameters> tilesets,
      List<MediaType> mediaTypes,
      boolean reseed,
      String tileSourceLabel,
      TaskContext taskContext,
      TileStore tileStore,
      ChainedTileProvider delegate,
      TileWalker tileWalker,
      Map<String, Map<String, Range<Integer>>> tmsRanges)
      throws IOException {
    Map<String, Optional<BoundingBox>> boundingBoxes = getBoundingBoxes(tilesets);

    long numberOfTiles =
        tileWalker.getNumberOfTiles(
            tilesets.keySet(), mediaTypes, tmsRanges, boundingBoxes, taskContext);
    final double[] currentTile = {0.0};
    final boolean[] isEmpty = {true};

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          taskContext.isPartial()
              ? "{}: processing {} tiles with {} [{}/{}]"
              : "{}: processing {} tiles with {}",
          taskContext.getTaskLabel(),
          numberOfTiles,
          tileSourceLabel,
          taskContext.getCurrentPartial(),
          taskContext.getMaxPartials());
    }

    tileWalker.walkTilesetsAndTiles(
        tilesets.keySet(),
        mediaTypes,
        tmsRanges,
        boundingBoxes,
        taskContext,
        (tileset, mediaType, tileMatrixSet, level, row, col) -> {
          TileQuery tile =
              ImmutableTileQuery.builder()
                  .tileset(tileset)
                  .mediaType(mediaType)
                  .tileMatrixSet(tileMatrixSet)
                  .level(level)
                  .row(row)
                  .col(col)
                  .generationParameters(tilesets.get(tileset))
                  .build();

          taskContext.setStatusMessage(
              String.format(
                  "currently processing -> %s, %s/%s/%s/%s, %s",
                  tileset, tileMatrixSet.getId(), level, row, col, mediaType));

          if (reseed || tileStore.isDirty(tile) || !tileStore.has(tile)) {
            TileResult result = delegate.get(tile);

            if (shouldCache(tile) && result.isAvailable()) {
              tileStore.put(tile, new ByteArrayInputStream(result.getContent().get()));
              if (isEmpty[0]) {
                isEmpty[0] = false;
              }
            }

            if (result.isError()) {
              LOGGER.warn(
                  "{}: processing failed -> {}, {}/{}/{}/{}, {} | {}",
                  taskContext.getTaskLabel(),
                  tileset,
                  tileMatrixSet.getId(),
                  level,
                  row,
                  col,
                  mediaType,
                  result.getError().get());
            }
          }

          currentTile[0] += 1;
          taskContext.setCompleteness(currentTile[0] / numberOfTiles);

          return !taskContext.isStopped();
        });

    return isEmpty[0];
  }

  default Map<String, Optional<BoundingBox>> getBoundingBoxes(
      Map<String, TileGenerationParameters> tilesets) {
    return tilesets.entrySet().stream()
        .map(
            entry ->
                new SimpleImmutableEntry<>(entry.getKey(), entry.getValue().getClipBoundingBox()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  default boolean shouldCache(TileQuery tileQuery) {
    return !tileQuery.isTransient();
  }

  default void purge(
      Map<String, TileGenerationParameters> tilesets,
      List<MediaType> mediaTypes,
      boolean reseed,
      String tileSourceLabel,
      TaskContext taskContext)
      throws IOException {}
}
