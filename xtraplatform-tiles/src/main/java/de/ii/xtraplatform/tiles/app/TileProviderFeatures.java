/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.app;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.services.domain.TaskContext;
import de.ii.xtraplatform.store.domain.BlobStore;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.tiles.domain.Cache;
import de.ii.xtraplatform.tiles.domain.Cache.Storage;
import de.ii.xtraplatform.tiles.domain.Cache.Type;
import de.ii.xtraplatform.tiles.domain.ChainedTileProvider;
import de.ii.xtraplatform.tiles.domain.TileCache;
import de.ii.xtraplatform.tiles.domain.TileGenerationParameters;
import de.ii.xtraplatform.tiles.domain.TileGenerationSchema;
import de.ii.xtraplatform.tiles.domain.TileGenerator;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetBase;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetLimits;
import de.ii.xtraplatform.tiles.domain.TileProvider;
import de.ii.xtraplatform.tiles.domain.TileProviderFeaturesData;
import de.ii.xtraplatform.tiles.domain.TileQuery;
import de.ii.xtraplatform.tiles.domain.TileResult;
import de.ii.xtraplatform.tiles.domain.TileSeeding;
import de.ii.xtraplatform.tiles.domain.TileStore;
import de.ii.xtraplatform.tiles.domain.TileWalker;
import de.ii.xtraplatform.tiles.domain.TilesetFeatures;
import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TileProviderFeatures extends AbstractTileProvider<TileProviderFeaturesData>
    implements TileProvider, TileSeeding {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileProviderFeatures.class);
  private static final String TILES_DIR_NAME = "tiles";

  private final TileGeneratorFeatures tileGenerator;
  private final TileEncoders tileEncoders;
  private final ChainedTileProvider generatorProviderChain;
  private final ChainedTileProvider combinerProviderChain;
  private final Map<Type, Map<Storage, TileStore>> tileStores;
  private final List<TileCache> generatorCaches;
  private final List<TileCache> combinerCaches;
  private final BlobStore tilesStore;

  @AssistedInject
  public TileProviderFeatures(
      CrsInfo crsInfo,
      CrsTransformerFactory crsTransformerFactory,
      EntityRegistry entityRegistry,
      AppContext appContext,
      Cql cql,
      BlobStore blobStore,
      TileWalker tileWalker,
      @Assisted TileProviderFeaturesData data) {
    super(data);

    this.tileGenerator =
        new TileGeneratorFeatures(data, crsInfo, crsTransformerFactory, entityRegistry, cql);
    this.tileStores =
        new ConcurrentHashMap<>(
            Map.of(
                Type.DYNAMIC,
                new ConcurrentHashMap<>(),
                Type.IMMUTABLE,
                new ConcurrentHashMap<>()));
    this.generatorCaches = new ArrayList<>();
    this.combinerCaches = new ArrayList<>();

    this.tilesStore = blobStore.with(TILES_DIR_NAME, clean(data.getId()));
    ChainedTileProvider current = tileGenerator;

    for (int i = 0; i < data.getCaches().size(); i++) {
      Cache cache = data.getCaches().get(i);
      BlobStore cacheStore =
          tilesStore.with(String.format("cache_%s", cache.getType().getSuffix()));
      TileStore tileStore = getTileStore(cache, cacheStore, data.getId(), data.getTilesets());

      if (cache.getType() == Type.DYNAMIC) {
        current =
            new TileCacheDynamic(
                tileWalker, tileStore, current, getCacheRanges(cache), cache.getSeeded());
        generatorCaches.add((TileCache) current);
      } else if (cache.getType() == Type.IMMUTABLE) {
        current = new TileCacheImmutable(tileWalker, tileStore, current, getCacheRanges(cache));
        generatorCaches.add((TileCache) current);
      }
    }

    this.generatorProviderChain = current;

    this.tileEncoders = new TileEncoders(data, generatorProviderChain);
    current = tileEncoders;

    for (int i = 0; i < data.getCaches().size(); i++) {
      Cache cache = data.getCaches().get(i);
      BlobStore cacheStore =
          tilesStore.with(String.format("cache_%s", cache.getType().getSuffix()));
      TileStore tileStore = getTileStore(cache, cacheStore, data.getId(), data.getTilesets());

      if (cache.getType() == Type.DYNAMIC) {
        current =
            new TileCacheDynamic(
                tileWalker, tileStore, current, getCacheRanges(cache), cache.getSeeded());
        combinerCaches.add((TileCache) current);
      } else if (cache.getType() == Type.IMMUTABLE) {
        current = new TileCacheImmutable(tileWalker, tileStore, current, getCacheRanges(cache));
        combinerCaches.add((TileCache) current);
      }
    }

    this.combinerProviderChain = current;
  }

  private TileStore getTileStore(
      Cache cache, BlobStore cacheStore, String id, Map<String, TilesetFeatures> layers) {
    return tileStores
        .get(cache.getType())
        .computeIfAbsent(
            cache.getStorage(),
            storage -> {
              if (cache.getType() == Type.IMMUTABLE) {
                return new TileStoreMulti(
                    cacheStore, cache.getStorage(), id, getTileSchemas(tileGenerator, layers));
              }

              return storage == Storage.MBTILES
                  ? TileStoreMbTiles.readWrite(
                      cacheStore, id, getTileSchemas(tileGenerator, layers))
                  : new TileStorePlain(cacheStore);
            });
  }

  private Map<String, Map<String, Range<Integer>>> getCacheRanges(Cache cache) {
    return getData().getTilesets().entrySet().stream()
        .map(
            entry ->
                new SimpleImmutableEntry<>(
                    entry.getKey(),
                    mergeCacheRanges(
                        cache.getTmsRanges(), cache.getLayerTmsRanges().get(entry.getKey()))))
        .collect(MapStreams.toMap());
  }

  private Map<String, Range<Integer>> mergeCacheRanges(
      Map<String, Range<Integer>> defaults, Map<String, Range<Integer>> layer) {
    if (Objects.isNull(layer)) {
      return defaults;
    }
    Map<String, Range<Integer>> merged = new LinkedHashMap<>();

    merged.putAll(defaults);
    merged.putAll(layer);

    return merged;
  }

  interface MapStreams {
    static <T, U> Collector<Map.Entry<T, U>, ?, Map<T, U>> toMap() {
      return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    static <T, U> Collector<Map.Entry<T, U>, ?, Map<T, U>> toUnmodifiableMap() {
      return Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    static <T, U> Collector<Map.Entry<T, U>, ?, ImmutableMap<T, U>> toImmutableMap() {
      return ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue);
    }
  }

  static String clean(String id) {
    return id.replace("-tiles", "").replace("tiles-", "");
  }

  private static Map<String, Map<String, TileGenerationSchema>> getTileSchemas(
      TileGeneratorFeatures tileGenerator, Map<String, TilesetFeatures> layers) {
    return layers.values().stream()
        .map(
            layer -> {
              Map<String, TileGenerationSchema> schemas =
                  layer.isCombined()
                      ? layer.getCombine().stream()
                          .flatMap(
                              subLayer -> {
                                if (Objects.equals(subLayer, TilesetFeatures.COMBINE_ALL)) {
                                  return layers.entrySet().stream()
                                      .filter(entry -> !entry.getValue().isCombined())
                                      .map(Entry::getKey);
                                }
                                return Stream.of(subLayer);
                              })
                          .map(
                              subLayer ->
                                  new SimpleImmutableEntry<>(
                                      subLayer,
                                      tileGenerator.getGenerationSchema(subLayer, Map.of())))
                          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                      : Map.of(
                          layer.getId(),
                          tileGenerator.getGenerationSchema(layer.getId(), Map.of()));

              return new SimpleImmutableEntry<>(layer.getId(), schemas);
            })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  protected boolean onStartup() throws InterruptedException {

    cleanupCache32();

    return super.onStartup();
  }

  @Override
  public TileResult getTile(TileQuery tile) {
    Optional<TileResult> error = validate(tile);

    if (error.isPresent()) {
      return error.get();
    }

    TilesetFeatures layer = getData().getTilesets().get(tile.getTileset());
    TileResult result =
        layer.isCombined() ? combinerProviderChain.get(tile) : generatorProviderChain.get(tile);

    if (result.isNotFound() && tileEncoders.canEncode(tile.getMediaType())) {
      return TileResult.notFound(tileEncoders.empty(tile.getMediaType(), tile.getTileMatrixSet()));
    }

    return result;
  }

  // TODO: add to TileCacheDynamic, use canProvide + clip limits
  @Override
  public void deleteFromCache(
      String layer, TileMatrixSetBase tileMatrixSet, TileMatrixSetLimits limits) {
    for (TileStore cache :
        tileStores.values().stream()
            .flatMap(m -> m.values().stream())
            .collect(Collectors.toList())) {
      try {
        cache.delete(layer, tileMatrixSet, limits, false);
      } catch (IOException e) {

      }
    }
  }

  @Override
  public boolean supportsGeneration() {
    return true;
  }

  @Override
  public TileGenerator generator() {
    return tileGenerator;
  }

  @Override
  public String getType() {
    return TileProviderFeaturesData.PROVIDER_TYPE;
  }

  @Override
  public void seed(
      Map<String, TileGenerationParameters> layers,
      List<MediaType> mediaTypes,
      boolean reseed,
      TaskContext taskContext)
      throws IOException {

    Map<String, TileGenerationParameters> validLayers = validLayers(layers);
    Map<String, TileGenerationParameters> sourcedLayers = sourcedLayers(validLayers);
    Map<String, TileGenerationParameters> combinedLayers = combinedLayers(validLayers);

    if (!sourcedLayers.isEmpty()) {
      for (TileCache cache : generatorCaches) {
        cache.purge(sourcedLayers, mediaTypes, reseed, "tile generator", taskContext);
      }
    }
    if (!combinedLayers.isEmpty()) {
      for (TileCache cache : combinerCaches) {
        cache.purge(combinedLayers, mediaTypes, reseed, "tile combiner", taskContext);
      }
    }

    if (!sourcedLayers.isEmpty()) {
      for (TileCache cache : generatorCaches) {
        cache.seed(sourcedLayers, mediaTypes, reseed, "tile generator", taskContext);
      }
    }
    if (!combinedLayers.isEmpty()) {
      for (TileCache cache : combinerCaches) {
        cache.seed(combinedLayers, mediaTypes, reseed, "tile combiner", taskContext);
      }
    }

    // TODO: cleanup all orphaned tiles with merged limits
  }

  private Map<String, TileGenerationParameters> validLayers(
      Map<String, TileGenerationParameters> layers) {
    return layers.entrySet().stream()
        .filter(
            entry -> {
              if (!getData().getTilesets().containsKey(entry.getKey())) {
                LOGGER.warn("Layer with name '{}' not found", entry.getKey());
                return false;
              }
              return true;
            })
        .collect(MapStreams.toMap());
  }

  private Map<String, TileGenerationParameters> sourcedLayers(
      Map<String, TileGenerationParameters> layers) {
    return layers.entrySet().stream()
        .filter(
            entry ->
                getData().getTilesets().containsKey(entry.getKey())
                    && !getData().getTilesets().get(entry.getKey()).isCombined())
        .collect(MapStreams.toMap());
  }

  private Map<String, TileGenerationParameters> combinedLayers(
      Map<String, TileGenerationParameters> layers) {
    return layers.entrySet().stream()
        .filter(
            entry ->
                getData().getTilesets().containsKey(entry.getKey())
                    && getData().getTilesets().get(entry.getKey()).isCombined())
        .collect(MapStreams.toMap());
  }

  @Deprecated(since = "3.3")
  private void cleanupCache32() {
    try {
      List<Path> unknownDirs = getUnknownDirs(tilesStore);

      for (Path unknownDir : unknownDirs) {
        deleteDir(tilesStore, unknownDir);
      }

    } catch (IOException e) {
      // ignore
    }
  }

  private List<Path> getUnknownDirs(BlobStore tileStore) throws IOException {
    try (Stream<Path> paths = tileStore.walk(Path.of(""), 1, (p, a) -> !a.isValue()).skip(1)) {
      return paths
          .map(Path::getFileName)
          .filter(
              path ->
                  !Objects.equals(
                          path.toString(), String.format("cache_%s", Type.DYNAMIC.getSuffix()))
                      && !Objects.equals(
                          path.toString(), String.format("cache_%s", Type.IMMUTABLE.getSuffix())))
          .collect(Collectors.toList());
    }
  }

  private void deleteDir(BlobStore blobStore, Path dir) {
    try (Stream<Path> paths = blobStore.walk(dir, 6, (p, a) -> true)) {
      paths
          .sorted(Comparator.reverseOrder())
          .forEach(
              path -> {
                Path path1 = dir.resolve(path);
                try {
                  blobStore.delete(path1);
                } catch (IOException e) {
                  // ignore
                }
              });
    } catch (IOException e) {
      // ignore
    }
  }
}
