/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.app;

import com.google.common.collect.Range;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.services.domain.TaskContext;
import de.ii.xtraplatform.tiles.domain.ChainedTileProvider;
import de.ii.xtraplatform.tiles.domain.TileCache;
import de.ii.xtraplatform.tiles.domain.TileGenerationParameters;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetLimits;
import de.ii.xtraplatform.tiles.domain.TileQuery;
import de.ii.xtraplatform.tiles.domain.TileResult;
import de.ii.xtraplatform.tiles.domain.TileStore;
import de.ii.xtraplatform.tiles.domain.TileWalker;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TileCacheDynamic implements ChainedTileProvider, TileCache {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileCacheDynamic.class);

  private final TileWalker tileWalker;
  private final TileStore tileStore;
  private final ChainedTileProvider delegate;
  private final Map<String, Map<String, Range<Integer>>> tmsRanges;
  private final boolean isSeeded;

  public TileCacheDynamic(
      TileWalker tileWalker,
      TileStore tileStore,
      ChainedTileProvider delegate,
      Map<String, Map<String, Range<Integer>>> tmsRanges,
      boolean seeded) {
    this.tileWalker = tileWalker;
    this.tileStore = tileStore;
    this.delegate = delegate;
    this.tmsRanges = tmsRanges;
    this.isSeeded = seeded;
  }

  @Override
  public Map<String, Map<String, Range<Integer>>> getTmsRanges() {
    return tmsRanges;
  }

  @Override
  public Optional<ChainedTileProvider> getDelegate() {
    return Optional.of(delegate);
  }

  @Override
  public TileResult getTile(TileQuery tile) throws IOException {
    if (shouldCache(tile)) {
      return tileStore.get(tile);
    }
    return TileResult.notFound();
  }

  @Override
  public TileResult processDelegateResult(TileQuery tile, TileResult tileResult)
      throws IOException {
    if (shouldCache(tile) && tileResult.isAvailable()) {
      tileStore.put(tile, new ByteArrayInputStream(tileResult.getContent().get()));

      return tileStore.get(tile);
    }

    return tileResult;
  }

  @Override
  public void seed(
      Map<String, TileGenerationParameters> tilesets,
      List<MediaType> mediaTypes,
      boolean reseed,
      String tileSourceLabel,
      TaskContext taskContext)
      throws IOException {
    if (!isSeeded) {
      return;
    }

    doSeed(
        tilesets,
        mediaTypes,
        reseed,
        tileSourceLabel,
        taskContext,
        tileStore,
        delegate,
        tileWalker,
        getTmsRanges());
  }

  @Override
  public void purge(
      Map<String, TileGenerationParameters> tilesets,
      List<MediaType> mediaTypes,
      boolean reseed,
      String tileSourceLabel,
      TaskContext taskContext)
      throws IOException {
    if (!isSeeded) {
      return;
    }

    // NOTE: other partials may start writing before first partial is done with purging, as a
    // workaround they will sleep for 1s
    // TODO: to implement this properly, add preRun and postRun to tasks which are run before/after
    // partials
    if (reseed) {
      if (taskContext.isFirstPartial()) {
        LOGGER.debug("{}: purging cache for {}", taskContext.getTaskLabel(), tileSourceLabel);

        Map<String, Optional<BoundingBox>> boundingBoxes = getBoundingBoxes(tilesets);

        tileWalker.walkTilesetsAndLimits(
            tilesets.keySet(),
            getTmsRanges(),
            boundingBoxes,
            (tileset, tileMatrixSet, limits) -> {
              try {
                // Purge: delete all tiles in the tile matrix
                BoundingBox bbox = tileMatrixSet.getBoundingBox();
                TileMatrixSetLimits purgeLimits =
                    tileMatrixSet.getLimits(Integer.parseInt(limits.getTileMatrix()), bbox);
                tileStore.delete(tileset, tileMatrixSet, purgeLimits, false);
              } catch (IOException e) {
                // ignore
                LOGGER.debug(
                    "{}: Error while purging cached tiles for {}, tileset {}, tile matrix {}. Reason: {}",
                    taskContext.getTaskLabel(),
                    tileSourceLabel,
                    tileset,
                    limits.getTileMatrix(),
                    e.getMessage());
              }
            });

        tileStore.tidyup();

        LOGGER.debug(
            "{}: purged cache successfully for {}", taskContext.getTaskLabel(), tileSourceLabel);
      } else {
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {
          // ignore
        }
      }
    }
  }
}
