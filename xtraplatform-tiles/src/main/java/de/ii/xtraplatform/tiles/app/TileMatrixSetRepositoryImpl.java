/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import de.ii.xtraplatform.base.domain.AppLifeCycle;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.blobs.domain.BlobStore;
import de.ii.xtraplatform.tiles.domain.TileMatrixSet;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetRepository;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Access to the cache for tile files. */
@Singleton
@AutoBind
public class TileMatrixSetRepositoryImpl implements TileMatrixSetRepository, AppLifeCycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileMatrixSetRepositoryImpl.class);
  private static final String STORE_RESOURCE_TYPE = "tile-matrix-sets";
  private final BlobStore customTileMatrixSetsStore;
  private final Map<String, TileMatrixSet> tileMatrixSets;

  /** set data directory */
  @Inject
  public TileMatrixSetRepositoryImpl(BlobStore blobStore) {
    this.customTileMatrixSetsStore = blobStore.with(STORE_RESOURCE_TYPE);
    this.tileMatrixSets = new HashMap<>();
  }

  @Override
  public void onStart() {
    if (tileMatrixSets.isEmpty()) initCache();
  }

  @Override
  public Optional<TileMatrixSet> get(String tileMatrixSetId) {
    if (tileMatrixSets.isEmpty()) initCache();
    return Optional.ofNullable(tileMatrixSets.get(tileMatrixSetId));
  }

  @Override
  public Map<String, TileMatrixSet> getAll() {
    if (tileMatrixSets.isEmpty()) initCache();
    return new ImmutableMap.Builder<String, TileMatrixSet>().putAll(tileMatrixSets).build();
  }

  private void initCache() {
    PREDEFINED_TILE_MATRIX_SETS.forEach(
        tileMatrixSetId ->
            TileMatrixSet.fromWellKnownId(tileMatrixSetId)
                .ifPresent(tms -> tileMatrixSets.put(tileMatrixSetId, tms)));

    try (Stream<Path> fileStream =
        customTileMatrixSetsStore.walk(
            Path.of(""),
            1,
            (path, attributes) ->
                attributes.isValue()
                    && !attributes.isHidden()
                    && com.google.common.io.Files.getFileExtension(path.getFileName().toString())
                        .equals("json"))) {
      fileStream.forEach(
          path -> {
            String tileMatrixSetId = Files.getNameWithoutExtension(path.getFileName().toString());
            try {
              TileMatrixSet.fromInputStream(
                      customTileMatrixSetsStore.content(path).get(), tileMatrixSetId)
                  .ifPresent(tms -> tileMatrixSets.put(tileMatrixSetId, tms));
            } catch (IOException e) {
              LOGGER.debug("Tile matrix set '{}' not found: {}", tileMatrixSetId, e.getMessage());
            }
          });
    } catch (IOException e) {
      LogContext.error(LOGGER, e, "Could not parse tile matrix sets");
    }
  }
}
