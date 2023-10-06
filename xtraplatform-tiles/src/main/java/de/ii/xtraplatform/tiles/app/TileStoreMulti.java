/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.app;

import static de.ii.xtraplatform.base.domain.util.LambdaWithException.consumerMayThrow;

import com.google.common.collect.Lists;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.blobs.domain.BlobStore;
import de.ii.xtraplatform.tiles.domain.Cache;
import de.ii.xtraplatform.tiles.domain.Cache.Storage;
import de.ii.xtraplatform.tiles.domain.TileGenerationSchema;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetBase;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetLimits;
import de.ii.xtraplatform.tiles.domain.TileQuery;
import de.ii.xtraplatform.tiles.domain.TileResult;
import de.ii.xtraplatform.tiles.domain.TileStore;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TileStoreMulti implements TileStore, TileStore.Staging {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileStoreMulti.class);
  private static final String STAGING_MARKER = ".staging";

  private final BlobStore cacheStore;
  private final Cache.Storage storage;
  private final String tileSetName;
  private final Map<String, Map<String, TileGenerationSchema>> tileSchemas;
  private final List<Tuple<TileStore, BlobStore>> active;
  private final Map<String, Map<String, List<TileMatrixSetLimits>>> dirty;
  private Tuple<TileStore, BlobStore> staging;

  public TileStoreMulti(
      BlobStore cacheStore,
      Storage storage,
      String tileSetName,
      Map<String, Map<String, TileGenerationSchema>> tileSchemas) {
    this.cacheStore = cacheStore;
    this.storage = storage;
    this.tileSetName = tileSetName;
    this.tileSchemas = tileSchemas;
    this.staging = null;
    this.active = getActive();
    this.dirty = new ConcurrentHashMap<>();
    tileSchemas.keySet().forEach(tileset -> dirty.put(tileset, new ConcurrentHashMap<>()));
  }

  private List<Tuple<TileStore, BlobStore>> getActive() {
    try (Stream<BlobStore> activeLevels = getActiveLevels()) {
      List<Tuple<TileStore, BlobStore>> active =
          activeLevels
              .map(cacheLevel -> Tuple.of(getTileStore(cacheLevel), cacheLevel))
              .collect(Collectors.toList());

      if (LOGGER.isDebugEnabled() && !active.isEmpty()) {
        LOGGER.debug(
            "Active tile cache levels: {}",
            active.stream().map(a -> a.second().getPrefix()).collect(Collectors.toList()));
      }

      return active;
    } catch (IOException e) {
      // ignore
    }

    return List.of();
  }

  @Override
  public boolean has(TileQuery tile) throws IOException {
    for (Tuple<TileStore, BlobStore> store : active) {
      if (store.first().has(tile)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public TileResult get(TileQuery tile) throws IOException {
    for (Tuple<TileStore, BlobStore> store : active) {
      TileResult result = store.first().get(tile);
      if (!result.isNotFound()) {
        return result;
      }
    }

    return TileResult.notFound();
  }

  @Override
  public Optional<Boolean> isEmpty(TileQuery tile) throws IOException {
    for (Tuple<TileStore, BlobStore> store : active) {
      Optional<Boolean> result = store.first().isEmpty(tile);
      if (result.isPresent()) {
        return result;
      }
    }

    return Optional.empty();
  }

  @Override
  public boolean isEmpty() throws IOException {
    return false;
  }

  @Override
  public void walk(Walker walker) {
    throw new NotImplementedException();
  }

  @Override
  public boolean has(String tileset, String tms, int level, int row, int col) throws IOException {
    throw new NotImplementedException();
  }

  @Override
  public void put(TileQuery tile, InputStream content) throws IOException {
    if (!inProgress()) {
      throw new IllegalStateException("Writing is only allowed during staging.");
    }
    staging.first().put(tile, content);
  }

  @Override
  public void delete(TileQuery tile) throws IOException {
    throw new NotImplementedException();
  }

  @Override
  public void delete(
      String tileset, TileMatrixSetBase tileMatrixSet, TileMatrixSetLimits limits, boolean inverse)
      throws IOException {
    if (!inverse) {
      if (!dirty.get(tileset).containsKey(tileMatrixSet.getId())) {
        dirty.get(tileset).put(tileMatrixSet.getId(), new CopyOnWriteArrayList<>());
      }

      dirty.get(tileset).get(tileMatrixSet.getId()).add(limits);

      return;
    }

    try (Stream<BlobStore> activeLevels = getActiveLevels()) {
      activeLevels.forEach(
          consumerMayThrow(
              level -> {
                try (Stream<Path> matchingFiles =
                    level.walk(
                        Path.of(""),
                        5,
                        (path, fileAttributes) ->
                            fileAttributes.isValue()
                                && TileStore.isInsideBounds(
                                    path, tileset, tileMatrixSet.getId(), limits, true))) {
                  matchingFiles.forEach(consumerMayThrow(level::delete));
                }
              }));
    } catch (RuntimeException e) {
      if (e.getCause() instanceof IOException) {
        throw (IOException) e.getCause();
      }
      throw e;
    }
  }

  @Override
  public void delete(String tileset, String tms, int level, int row, int col) throws IOException {
    throw new NotImplementedException();
  }

  @Override
  public boolean isDirty(TileQuery tile) {
    return dirty.containsKey(tile.getTileset())
        && dirty.get(tile.getTileset()).containsKey(tile.getTileMatrixSet().getId())
        && dirty.get(tile.getTileset()).get(tile.getTileMatrixSet().getId()).stream()
            .anyMatch(
                limits ->
                    Objects.equals(limits.getTileMatrix(), String.valueOf(tile.getLevel()))
                        && limits.contains(tile.getRow(), tile.getCol()));
  }

  @Override
  public synchronized boolean inProgress() {
    return Objects.nonNull(staging);
  }

  @Override
  public synchronized boolean init() throws IOException {
    if (inProgress()) {
      return false;
    }
    BlobStore stagingStore = cacheStore.with(String.format("%d", Instant.now().toEpochMilli()));

    stagingStore.put(Path.of(".staging"), new ByteArrayInputStream(new byte[0]));

    TileStore tileStore = getTileStore(stagingStore);

    this.staging = Tuple.of(tileStore, stagingStore);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Staging cache level {}", stagingStore.getPrefix());
    }

    return true;
  }

  private TileStore getTileStore(BlobStore blobStore) {
    return storage == Storage.MBTILES
        ? TileStoreMbTiles.readWrite(blobStore, tileSetName, tileSchemas)
        : new TileStorePlain(blobStore);
  }

  @Override
  public synchronized void promote() throws IOException {
    if (inProgress()) {
      staging.second().delete(Path.of(".staging"));

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Promoting cache level {}", staging.second().getPrefix());
      }

      this.active.add(0, staging);
      this.staging = null;

      for (String tileset : dirty.keySet()) {
        for (String tms : dirty.get(tileset).keySet()) {
          dirty.get(tileset).get(tms).clear();
        }
      }
    }
  }

  @Override
  public synchronized void abort() throws IOException {
    if (inProgress()) {
      this.staging = null;
    }
  }

  @Override
  public synchronized void cleanup() throws IOException {
    if (inProgress()) {
      throw new IllegalStateException("Cleanup is not allowed during staging.");
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Cleaning up cache levels");
    }

    cleanupStaging();

    cleanupDuplicates();

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Cleaned up cache levels");
    }
  }

  private void cleanupStaging() {
    try (Stream<BlobStore> stagingLevels = getStagingLevels()) {
      stagingLevels.forEach(this::deleteCacheLevel);
    } catch (IOException e) {
      LogContext.errorAsDebug(LOGGER, e, "Error during cleanup of staging caches.");
    }
  }

  private void cleanupDuplicates() {
    List<Tuple<TileStore, BlobStore>> reverseLevels = Lists.reverse(active);

    for (int i = 0; i < reverseLevels.size() - 1; i++) {
      TileStore current = reverseLevels.get(i).first();
      List<Tuple<TileStore, BlobStore>> others = reverseLevels.subList(i + 1, reverseLevels.size());

      current.walk(
          ((tileset, tms, level, row, col) -> {
            for (Tuple<TileStore, BlobStore> other : others) {
              try {
                if (other.first().has(tileset, tms, level, row, col)) {
                  try {
                    current.delete(tileset, tms, level, row, col);
                    break;
                  } catch (IOException e) {
                    LogContext.errorAsDebug(
                        LOGGER,
                        e,
                        "Could not delete cache level duplicate {} {}/{}/{}/{}.",
                        tileset,
                        tms,
                        level,
                        row,
                        col);
                  }
                }
              } catch (IOException e) {
                // ignore
              }
            }
          }));

      boolean deleted = deleteCacheLevelIfEmpty(reverseLevels.get(i));
      if (deleted) {
        active.remove(reverseLevels.get(i));
      }
    }
  }

  private void deleteCacheLevel(BlobStore cacheLevel) {
    try (Stream<Path> paths = cacheStore.walk(cacheLevel.getPrefix(), 5, (p, a) -> true)) {
      paths
          .sorted(Comparator.reverseOrder())
          .forEach(
              path -> {
                Path path1 = cacheLevel.getPrefix().resolve(path);
                try {
                  cacheStore.delete(path1);
                } catch (IOException e) {
                  LogContext.errorAsDebug(
                      LOGGER, e, "Could not delete cache level entry {}.", path1);
                }
              });
    } catch (IOException e) {
      LogContext.errorAsDebug(
          LOGGER, e, "Could not delete cache level {}.", cacheLevel.getPrefix());
    }
  }

  private boolean deleteCacheLevelIfEmpty(Tuple<TileStore, BlobStore> cacheLevel) {
    try {
      if (cacheLevel.first().isEmpty()) {
        deleteCacheLevel(cacheLevel.second());
        return true;
      }
    } catch (IOException e) {

    }

    return false;
  }

  private Stream<BlobStore> getCacheLevels() throws IOException {
    return cacheStore
        .walk(Path.of(""), 1, (p, a) -> !a.isValue())
        .skip(1)
        .map(dir -> cacheStore.with(dir.getFileName().toString()))
        .sorted(Comparator.comparing(BlobStore::getPrefix).reversed());
  }

  private Stream<BlobStore> getStagingLevels() throws IOException {
    return getCacheLevels()
        .filter(
            cacheLevel -> {
              try {
                return isStaging(cacheLevel);
              } catch (IOException e) {
                return false;
              }
            });
  }

  private Stream<BlobStore> getActiveLevels() throws IOException {
    return getCacheLevels()
        .filter(
            cacheLevel -> {
              try {
                return !isStaging(cacheLevel);
              } catch (IOException e) {
                return false;
              }
            });
  }

  private boolean isStaging(BlobStore cacheLevel) throws IOException {
    return cacheLevel.has(Path.of(STAGING_MARKER));
  }
}
