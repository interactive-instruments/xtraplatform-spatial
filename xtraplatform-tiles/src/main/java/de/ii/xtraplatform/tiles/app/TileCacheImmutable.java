/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.app;

import com.google.common.collect.Range;
import de.ii.xtraplatform.services.domain.TaskContext;
import de.ii.xtraplatform.tiles.domain.ChainedTileProvider;
import de.ii.xtraplatform.tiles.domain.TileCache;
import de.ii.xtraplatform.tiles.domain.TileGenerationParameters;
import de.ii.xtraplatform.tiles.domain.TileQuery;
import de.ii.xtraplatform.tiles.domain.TileResult;
import de.ii.xtraplatform.tiles.domain.TileStore;
import de.ii.xtraplatform.tiles.domain.TileWalker;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TileCacheImmutable implements ChainedTileProvider, TileCache {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileCacheImmutable.class);

  private final TileWalker tileWalker;
  private final TileStore tileStore;
  private final ChainedTileProvider delegate;
  private final Map<String, Map<String, Range<Integer>>> tmsRanges;

  public TileCacheImmutable(
      TileWalker tileWalker,
      TileStore tileStore,
      ChainedTileProvider delegate,
      Map<String, Map<String, Range<Integer>>> tmsRanges) {
    this.tileWalker = tileWalker;
    this.tileStore = tileStore;
    this.delegate = delegate;
    this.tmsRanges = tmsRanges;
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
  public void seed(
      Map<String, TileGenerationParameters> tilesets,
      List<MediaType> mediaTypes,
      boolean reseed,
      String tileSourceLabel,
      TaskContext taskContext)
      throws IOException {

    tileStore.staging().init();

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

    if (taskContext.getActivePartials() == 1) {
      tileStore.staging().promote();

      tileStore.staging().cleanup();

      tileStore.tidyup();
    }
  }
}
