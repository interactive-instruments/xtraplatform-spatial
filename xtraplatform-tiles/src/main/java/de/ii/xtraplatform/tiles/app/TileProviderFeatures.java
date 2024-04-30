/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.resiliency.OptionalVolatileCapability;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry;
import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.blobs.domain.ResourceStore;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.entities.domain.Entity;
import de.ii.xtraplatform.entities.domain.Entity.SubType;
import de.ii.xtraplatform.entities.domain.EntityRegistry;
import de.ii.xtraplatform.features.domain.FeatureProvider.FeatureVolatileCapability;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ProviderData;
import de.ii.xtraplatform.services.domain.TaskContext;
import de.ii.xtraplatform.tiles.domain.Cache;
import de.ii.xtraplatform.tiles.domain.Cache.Storage;
import de.ii.xtraplatform.tiles.domain.Cache.Type;
import de.ii.xtraplatform.tiles.domain.ChainedTileProvider;
import de.ii.xtraplatform.tiles.domain.ImmutableSeedingOptions;
import de.ii.xtraplatform.tiles.domain.ImmutableTilesetMetadata;
import de.ii.xtraplatform.tiles.domain.SeedingOptions;
import de.ii.xtraplatform.tiles.domain.TileAccess;
import de.ii.xtraplatform.tiles.domain.TileCache;
import de.ii.xtraplatform.tiles.domain.TileGenerationParameters;
import de.ii.xtraplatform.tiles.domain.TileGenerationSchema;
import de.ii.xtraplatform.tiles.domain.TileGenerator;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetBase;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetLimits;
import de.ii.xtraplatform.tiles.domain.TileProvider;
import de.ii.xtraplatform.tiles.domain.TileProviderData;
import de.ii.xtraplatform.tiles.domain.TileProviderFeaturesData;
import de.ii.xtraplatform.tiles.domain.TileQuery;
import de.ii.xtraplatform.tiles.domain.TileResult;
import de.ii.xtraplatform.tiles.domain.TileSeeding;
import de.ii.xtraplatform.tiles.domain.TileStore;
import de.ii.xtraplatform.tiles.domain.TileWalker;
import de.ii.xtraplatform.tiles.domain.TilesetFeatures;
import de.ii.xtraplatform.tiles.domain.TilesetMetadata;
import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
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

@Entity(
    type = TileProviderData.ENTITY_TYPE,
    subTypes = {
      @SubType(key = ProviderData.PROVIDER_TYPE_KEY, value = TileProviderData.PROVIDER_TYPE),
      @SubType(
          key = ProviderData.PROVIDER_SUB_TYPE_KEY,
          value = TileProviderFeaturesData.PROVIDER_SUBTYPE)
    },
    data = TileProviderFeaturesData.class)
public class TileProviderFeatures extends AbstractTileProvider<TileProviderFeaturesData>
    implements TileProvider, TileAccess, TileSeeding {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileProviderFeatures.class);
  static final String TILES_DIR_NAME = "tiles";

  private final TileGeneratorFeatures tileGenerator;
  private final Map<Type, Map<Storage, TileStore>> tileStores;
  private final List<TileCache> generatorCaches;
  private final List<TileCache> combinerCaches;
  private final Map<String, TilesetMetadata> metadata;
  private final ResourceStore tilesStore;
  private final TileWalker tileWalker;
  private final boolean asyncStartup;
  private TileEncoders tileEncoders;
  private ChainedTileProvider generatorProviderChain;
  private ChainedTileProvider combinerProviderChain;

  @AssistedInject
  public TileProviderFeatures(
      CrsInfo crsInfo,
      CrsTransformerFactory crsTransformerFactory,
      EntityRegistry entityRegistry,
      AppContext appContext,
      Cql cql,
      ResourceStore blobStore,
      TileWalker tileWalker,
      VolatileRegistry volatileRegistry,
      @Assisted TileProviderFeaturesData data) {
    super(volatileRegistry, data, "access", "generation", "seeding");

    this.asyncStartup = appContext.getConfiguration().getModules().isStartupAsync();
    this.tileGenerator =
        new TileGeneratorFeatures(
            data,
            crsInfo,
            crsTransformerFactory,
            entityRegistry,
            cql,
            volatileRegistry,
            asyncStartup);
    this.tileStores =
        new ConcurrentHashMap<>(
            Map.of(
                Type.DYNAMIC,
                new ConcurrentHashMap<>(),
                Type.IMMUTABLE,
                new ConcurrentHashMap<>()));
    this.generatorCaches = new ArrayList<>();
    this.combinerCaches = new ArrayList<>();
    this.metadata = new LinkedHashMap<>();
    this.tilesStore = blobStore.with(TILES_DIR_NAME, clean(data.getId()));
    this.tileWalker = tileWalker;
  }

  @Override
  protected boolean onStartup() throws InterruptedException {
    onVolatileStart();

    addSubcomponent(tilesStore, true, "access", "generation", "seeding");
    addSubcomponent(tileGenerator, true, "generation", "seeding");
    addSubcomponent(tileWalker, "seeding");

    if (!asyncStartup) {
      init();
    }

    return super.onStartup();
  }

  @Override
  protected Tuple<State, String> volatileInit() {
    if (asyncStartup) {
      init();
    }
    return super.volatileInit();
  }

  private void init() {
    ChainedTileProvider current = tileGenerator;

    for (int i = 0; i < getData().getCaches().size(); i++) {
      Cache cache = getData().getCaches().get(i);
      ResourceStore cacheStore =
          tilesStore.writableWith(String.format("cache_%s", cache.getType().getSuffix()));
      TileStore tileStore = getTileStore(cache, cacheStore, getId(), getData().getTilesets());

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

    this.tileEncoders = new TileEncoders(getData(), generatorProviderChain);
    current = tileEncoders;

    for (int i = 0; i < getData().getCaches().size(); i++) {
      Cache cache = getData().getCaches().get(i);
      ResourceStore cacheStore =
          tilesStore.writableWith(String.format("cache_%s", cache.getType().getSuffix()));
      TileStore tileStore = getTileStore(cache, cacheStore, getId(), getData().getTilesets());

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

    loadMetadata();
  }

  private TileStore getTileStore(
      Cache cache, ResourceStore cacheStore, String id, Map<String, TilesetFeatures> tilesets) {
    return tileStores
        .get(cache.getType())
        .computeIfAbsent(
            cache.getStorage(),
            storage -> {
              if (cache.getType() == Type.IMMUTABLE) {
                return new TileStoreMulti(
                    cacheStore, cache.getStorage(), id, getTileSchemas(tileGenerator, tilesets));
              }

              return storage == Storage.MBTILES
                  ? TileStoreMbTiles.readWrite(
                      cacheStore, id, getTileSchemas(tileGenerator, tilesets))
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
                        cache.getTmsRanges(), cache.getTilesetTmsRanges().get(entry.getKey()))))
        .collect(MapStreams.toMap());
  }

  private Map<String, Range<Integer>> mergeCacheRanges(
      Map<String, Range<Integer>> defaults, Map<String, Range<Integer>> tileset) {
    if (Objects.isNull(tileset)) {
      return defaults;
    }
    Map<String, Range<Integer>> merged = new LinkedHashMap<>();

    merged.putAll(defaults);
    merged.putAll(tileset);

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
      TileGeneratorFeatures tileGenerator, Map<String, TilesetFeatures> tilesets) {
    return tilesets.values().stream()
        .map(
            tileset -> {
              Map<String, TileGenerationSchema> schemas =
                  tileset.isCombined()
                      ? tileset.getCombine().stream()
                          .flatMap(
                              layer -> {
                                if (Objects.equals(layer, TilesetFeatures.COMBINE_ALL)) {
                                  return tilesets.entrySet().stream()
                                      .filter(entry -> !entry.getValue().isCombined())
                                      .map(Entry::getKey);
                                }
                                return Stream.of(layer);
                              })
                          .map(
                              layer ->
                                  new SimpleImmutableEntry<>(
                                      layer, tileGenerator.getGenerationSchema(layer)))
                          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                      : Map.of(tileset.getId(), tileGenerator.getGenerationSchema(tileset.getId()));

              return new SimpleImmutableEntry<>(tileset.getId(), schemas);
            })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private List<String> getLayers(TilesetFeatures tileset) {
    if (!tileset.isCombined()) {
      return ImmutableList.of(tileset.getId());
    }

    return tileset.getCombine().stream()
        .flatMap(
            layer -> {
              if (Objects.equals(layer, TilesetFeatures.COMBINE_ALL)) {
                return getData().getTilesets().entrySet().stream()
                    .filter(entry -> !entry.getValue().isCombined())
                    .map(Entry::getKey);
              }
              return Stream.of(layer);
            })
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public Optional<TilesetMetadata> getMetadata(String tileset) {
    return Optional.ofNullable(metadata.get(tileset));
  }

  @Override
  public TileResult getTile(TileQuery tile) {
    Optional<TileResult> error = validate(tile);

    if (error.isPresent()) {
      return error.get();
    }

    TilesetFeatures tileset = getData().getTilesets().get(tile.getTileset());
    TileResult result =
        tileset.isCombined() ? combinerProviderChain.get(tile) : generatorProviderChain.get(tile);

    if (result.isNotFound() && tileEncoders.canEncode(tile.getMediaType())) {
      return TileResult.notFound(tileEncoders.empty(tile.getMediaType(), tile.getTileMatrixSet()));
    }

    return result;
  }

  @Override
  public void deleteFromCache(
      String tileset, TileMatrixSetBase tileMatrixSet, TileMatrixSetLimits limits) {
    for (TileStore cache :
        tileStores.values().stream()
            .flatMap(m -> m.values().stream())
            .collect(Collectors.toList())) {
      try {
        cache.delete(tileset, tileMatrixSet, limits, false);
      } catch (IOException e) {

      }
    }

    for (TileStore cache :
        tileStores.values().stream()
            .flatMap(m -> m.values().stream())
            .collect(Collectors.toList())) {
      cache.tidyup();
    }
  }

  @Override
  public OptionalVolatileCapability<TileGenerator> generator() {
    return new FeatureVolatileCapability<>(tileGenerator, TileGenerator.CAPABILITY, this);
  }

  @Override
  public SeedingOptions getOptions() {
    return getData().getSeeding().orElseGet(() -> new ImmutableSeedingOptions.Builder().build());
  }

  @Override
  public void seed(
      Map<String, TileGenerationParameters> tilesets,
      List<MediaType> mediaTypes,
      boolean reseed,
      TaskContext taskContext)
      throws IOException {

    Map<String, TileGenerationParameters> validTilesets = validTilesets(tilesets);
    Map<String, TileGenerationParameters> sourcedTilesets = sourcedTilesets(validTilesets);
    Map<String, TileGenerationParameters> combinedTilesets = combinedTilesets(validTilesets);

    if (!sourcedTilesets.isEmpty()) {
      for (TileCache cache : generatorCaches) {
        cache.purge(sourcedTilesets, mediaTypes, reseed, "tile generator", taskContext);
      }
    }
    if (!combinedTilesets.isEmpty()) {
      for (TileCache cache : combinerCaches) {
        cache.purge(combinedTilesets, mediaTypes, reseed, "tile combiner", taskContext);
      }
    }

    if (!sourcedTilesets.isEmpty()) {
      for (TileCache cache : generatorCaches) {
        cache.seed(sourcedTilesets, mediaTypes, reseed, "tile generator", taskContext);
      }
    }
    if (!combinedTilesets.isEmpty()) {
      for (TileCache cache : combinerCaches) {
        cache.seed(combinedTilesets, mediaTypes, reseed, "tile combiner", taskContext);
      }
    }

    // TODO: cleanup all orphaned tiles that are not within current cache limits
  }

  private Map<String, TileGenerationParameters> validTilesets(
      Map<String, TileGenerationParameters> tilesets) {
    return tilesets.entrySet().stream()
        .filter(
            entry -> {
              if (!getData().getTilesets().containsKey(entry.getKey())) {
                LOGGER.warn("Tileset with name '{}' not found", entry.getKey());
                return false;
              }
              return true;
            })
        .collect(MapStreams.toMap());
  }

  private Map<String, TileGenerationParameters> sourcedTilesets(
      Map<String, TileGenerationParameters> tilesets) {
    return tilesets.entrySet().stream()
        .filter(
            entry ->
                getData().getTilesets().containsKey(entry.getKey())
                    && !getData().getTilesets().get(entry.getKey()).isCombined())
        .collect(MapStreams.toMap());
  }

  private Map<String, TileGenerationParameters> combinedTilesets(
      Map<String, TileGenerationParameters> tilesets) {
    return tilesets.entrySet().stream()
        .filter(
            entry ->
                getData().getTilesets().containsKey(entry.getKey())
                    && getData().getTilesets().get(entry.getKey()).isCombined())
        .collect(MapStreams.toMap());
  }

  private void loadMetadata() {
    getData()
        .getTilesets()
        .forEach(
            (key, tileset) -> {
              metadata.put(key, loadMetadata(tileset));
            });
  }

  private TilesetMetadata loadMetadata(TilesetFeatures tileset) {
    List<FeatureSchema> vectorSchemas =
        getLayers(tileset).stream()
            .map(id -> tileGenerator.getVectorSchema(id, FeatureEncoderMVT.FORMAT))
            .collect(Collectors.toList());
    Optional<BoundingBox> bounds =
        getLayers(tileset).stream()
            .map(tileGenerator::getBounds)
            .reduce(
                Optional.empty(),
                (a, b) -> {
                  if (b.isEmpty()) {
                    return a;
                  }
                  if (a.isPresent()) {
                    return Optional.of(BoundingBox.merge(b.get(), a.get()));
                  }
                  return b;
                });

    return ImmutableTilesetMetadata.builder()
        .addEncodings("MVT")
        .levels(
            tileset.getLevels().isEmpty()
                ? getData().getTilesetDefaults().getLevels()
                : tileset.getLevels())
        .center(tileset.getCenter().or(() -> getData().getTilesetDefaults().getCenter()))
        .bounds(bounds)
        .vectorSchemas(vectorSchemas)
        .build();
  }
}
