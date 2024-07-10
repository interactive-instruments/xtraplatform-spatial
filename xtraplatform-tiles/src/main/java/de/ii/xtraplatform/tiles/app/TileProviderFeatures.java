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
import com.google.common.io.Files;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import de.ii.xtraplatform.base.domain.resiliency.OptionalVolatileCapability;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry;
import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.blobs.domain.BlobStore;
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
import de.ii.xtraplatform.jobs.domain.JobQueue;
import de.ii.xtraplatform.tiles.domain.Cache;
import de.ii.xtraplatform.tiles.domain.Cache.Storage;
import de.ii.xtraplatform.tiles.domain.Cache.Type;
import de.ii.xtraplatform.tiles.domain.ChainedTileProvider;
import de.ii.xtraplatform.tiles.domain.ImmutableMinMax;
import de.ii.xtraplatform.tiles.domain.ImmutableSeedingOptions;
import de.ii.xtraplatform.tiles.domain.ImmutableTilesetMetadata;
import de.ii.xtraplatform.tiles.domain.MinMax;
import de.ii.xtraplatform.tiles.domain.SeedingOptions;
import de.ii.xtraplatform.tiles.domain.TileAccess;
import de.ii.xtraplatform.tiles.domain.TileCache;
import de.ii.xtraplatform.tiles.domain.TileGenerationParameters;
import de.ii.xtraplatform.tiles.domain.TileGenerationSchema;
import de.ii.xtraplatform.tiles.domain.TileGenerator;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetBase;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetLimits;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetRepository;
import de.ii.xtraplatform.tiles.domain.TileProvider;
import de.ii.xtraplatform.tiles.domain.TileProviderData;
import de.ii.xtraplatform.tiles.domain.TileProviderFeaturesData;
import de.ii.xtraplatform.tiles.domain.TileQuery;
import de.ii.xtraplatform.tiles.domain.TileResult;
import de.ii.xtraplatform.tiles.domain.TileSeeding;
import de.ii.xtraplatform.tiles.domain.TileSeedingJob;
import de.ii.xtraplatform.tiles.domain.TileSeedingJobSet;
import de.ii.xtraplatform.tiles.domain.TileStore;
import de.ii.xtraplatform.tiles.domain.TileSubMatrix;
import de.ii.xtraplatform.tiles.domain.TileWalker;
import de.ii.xtraplatform.tiles.domain.TilesFormat;
import de.ii.xtraplatform.tiles.domain.TilesetCommon;
import de.ii.xtraplatform.tiles.domain.TilesetFeatures;
import de.ii.xtraplatform.tiles.domain.TilesetMetadata;
import de.ii.xtraplatform.tiles.domain.TilesetRaster;
import de.ii.xtraplatform.values.domain.ValueStore;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.AmountFormats;

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
  private final BlobStore stylesStore;
  private final TileWalker tileWalker;
  private final boolean asyncStartup;
  private final Optional<TileMatrixSetRepository> tileMatrixSetRepository;
  private final String dataDir;
  private TileEncoders tileEncoders;
  private ChainedTileProvider generatorProviderChain;
  private ChainedTileProvider combinerProviderChain;
  private ChainedTileProvider rasterProviderChain;

  @AssistedInject
  public TileProviderFeatures(
      CrsInfo crsInfo,
      CrsTransformerFactory crsTransformerFactory,
      EntityRegistry entityRegistry,
      AppContext appContext,
      Cql cql,
      ResourceStore blobStore,
      ValueStore valueStore,
      TileWalker tileWalker,
      VolatileRegistry volatileRegistry,
      JobQueue jobQueue,
      TileMatrixSetRepository tileMatrixSetRepository,
      @Assisted TileProviderFeaturesData data) {
    super(volatileRegistry, data, "access", "generation", "seeding");

    this.asyncStartup = appContext.getConfiguration().getModules().isStartupAsync();
    this.tileMatrixSetRepository = Optional.of(tileMatrixSetRepository);
    this.dataDir = appContext.getDataDir().toString();
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
    this.stylesStore = valueStore.asBlobStore();
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
      TileStore tileStore =
          getTileStore(cache, cacheStore, getId(), getData().getTilesets(), getRasterTilesets());

      if (cache.getType() == Type.DYNAMIC) {
        current =
            new TileCacheDynamic(
                tileWalker,
                tileStore,
                current,
                getCacheRanges(cache),
                getCacheRanges(cache, 1),
                cache.getSeeded());
        generatorCaches.add((TileCache) current);
      } else if (cache.getType() == Type.IMMUTABLE) {
        current =
            new TileCacheImmutable(
                tileWalker, tileStore, current, getCacheRanges(cache), getCacheRanges(cache, 1));
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
      TileStore tileStore =
          getTileStore(cache, cacheStore, getId(), getData().getTilesets(), getRasterTilesets());

      if (cache.getType() == Type.DYNAMIC) {
        current =
            new TileCacheDynamic(
                tileWalker,
                tileStore,
                current,
                getCacheRanges(cache),
                getCacheRanges(cache, 1),
                cache.getSeeded());
        combinerCaches.add((TileCache) current);
      } else if (cache.getType() == Type.IMMUTABLE) {
        current =
            new TileCacheImmutable(
                tileWalker, tileStore, current, getCacheRanges(cache), getCacheRanges(cache, 1));
        combinerCaches.add((TileCache) current);
      }
    }

    this.combinerProviderChain = current;

    current = ChainedTileProvider.noOp();

    for (int i = 0; i < getData().getCaches().size(); i++) {
      Cache cache = getData().getCaches().get(i);

      if (!cache.getSeeded()) {
        continue;
      }

      ResourceStore cacheStore =
          tilesStore.writableWith(String.format("cache_%s", cache.getType().getSuffix()));
      TileStore tileStore =
          getTileStore(cache, cacheStore, getId(), getData().getTilesets(), getRasterTilesets());

      if (cache.getType() == Type.DYNAMIC) {
        current =
            new TileCacheDynamic(
                tileWalker,
                tileStore,
                current,
                getCacheRanges(cache, 1),
                getCacheRanges(cache, 1),
                cache.getSeeded());
      } else if (cache.getType() == Type.IMMUTABLE) {
        current =
            new TileCacheImmutable(
                tileWalker, tileStore, current, getCacheRanges(cache, 1), getCacheRanges(cache, 1));
      }
    }

    this.rasterProviderChain = current;

    loadMetadata();
  }

  private Set<String> getTileMatrixSets(String tileset) {
    if (!getData().getTilesets().containsKey(tileset) && !getRasterTilesets().contains(tileset)) {
      return Set.of();
    }

    String vectorTileset =
        getData().getTilesets().containsKey(tileset)
            ? tileset
            : getRasterToVectorTilesets().get(tileset);

    return getData()
        .getTilesets()
        .get(vectorTileset)
        .mergeDefaults(getData().getTilesetDefaults())
        .getTmsRanges()
        .keySet();
  }

  private TileStore getTileStore(
      Cache cache,
      ResourceStore cacheStore,
      String id,
      Map<String, TilesetFeatures> tilesets,
      List<String> rasterTilesets) {
    Map<String, Set<String>> tileMatrixSets =
        Stream.concat(tilesets.keySet().stream(), rasterTilesets.stream())
            .map(tileset -> Map.entry(tileset, getTileMatrixSets(tileset)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return tileStores
        .get(cache.getType())
        .computeIfAbsent(
            cache.getStorage(),
            storage -> {
              Optional<TileStorePartitions> partitions =
                  cache.getStorage() == Storage.PER_JOB
                      ? Optional.of(
                          new TileStorePartitions(
                              seeding().get().getOptions().getEffectiveJobSize()))
                      : Optional.empty();

              if (cache.getType() == Type.IMMUTABLE) {
                return new TileStoreMulti(
                    cacheStore,
                    cache.getStorage(),
                    id,
                    getTileSchemas(tileGenerator, tilesets, rasterTilesets),
                    tileMatrixSets,
                    tileMatrixSetRepository,
                    partitions);
              }

              return storage == Storage.MBTILES
                      || storage == Storage.PER_TILESET
                      || storage == Storage.PER_JOB
                  ? TileStoreMbTiles.readWrite(
                      cacheStore,
                      id,
                      getTileSchemas(tileGenerator, tilesets, rasterTilesets),
                      tileMatrixSets,
                      tileMatrixSetRepository,
                      partitions)
                  : new TileStorePlain(cacheStore);
            });
  }

  private List<String> getRasterTilesets() {
    return getData().getRasterTilesets().entrySet().stream()
        .flatMap(
            entry ->
                entry.getValue().getStyles().stream()
                    .map(
                        style ->
                            getRasterTilesetId(
                                entry.getValue().getPrefix().orElse(entry.getKey()), style)))
        .collect(Collectors.toList());
  }

  private Map<String, String> getRasterToVectorTilesets() {
    return getData().getRasterTilesets().entrySet().stream()
        .flatMap(
            entry ->
                entry.getValue().getStyles().stream()
                    .map(
                        style ->
                            Map.entry(
                                getRasterTilesetId(
                                    entry.getValue().getPrefix().orElse(entry.getKey()), style),
                                entry.getKey())))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Map<String, Map<String, Range<Integer>>> getCacheRanges(Cache cache) {
    return getCacheRanges(cache, (tileset, ranges) -> ranges);
  }

  private Map<String, Map<String, Range<Integer>>> getCacheRanges(Cache cache, int delta) {
    return getCacheRanges(cache, (tileset, ranges) -> addToCacheRanges(ranges, delta));
  }

  private Map<String, Map<String, Range<Integer>>> getCacheRanges(
      Cache cache,
      BiFunction<String, Map<String, Range<Integer>>, Map<String, Range<Integer>>> rangeMapper) {
    return Stream.concat(
            getData().getTilesets().keySet().stream()
                .map(
                    tileset ->
                        new SimpleImmutableEntry<>(
                            tileset,
                            rangeMapper.apply(
                                tileset,
                                capCacheRanges(
                                    mergeCacheRanges(
                                        cache.getTmsRanges(),
                                        cache.getTilesetTmsRanges().get(tileset)),
                                    getTilesetRanges(tileset))))),
            getRasterTilesets().stream()
                .map(
                    tileset ->
                        new SimpleImmutableEntry<>(
                            tileset,
                            rangeMapper.apply(
                                tileset,
                                capCacheRanges(
                                    mergeCacheRanges(
                                        cache.getTmsRanges(),
                                        cache.getTilesetTmsRanges().get(tileset)),
                                    getTilesetRanges(getVectorTilesetId(tileset).orElseThrow()))))))
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

  private Map<String, Range<Integer>> addToCacheRanges(
      Map<String, Range<Integer>> ranges, int delta) {
    return ranges.entrySet().stream()
        .map(
            entry ->
                new SimpleImmutableEntry<>(
                    entry.getKey(),
                    Range.closed(
                        entry.getValue().lowerEndpoint() + delta,
                        entry.getValue().upperEndpoint() + delta)))
        .collect(MapStreams.toMap());
  }

  private Map<String, Range<Integer>> capCacheRanges(
      Map<String, Range<Integer>> ranges, Map<String, Range<Integer>> capRanges) {
    return ranges.entrySet().stream()
        .filter(
            entry ->
                capRanges.containsKey(entry.getKey())
                    && capRanges.get(entry.getKey()).isConnected(entry.getValue()))
        .map(
            entry ->
                new SimpleImmutableEntry<>(
                    entry.getKey(),
                    Range.closed(
                        Math.max(
                            entry.getValue().lowerEndpoint(),
                            capRanges.get(entry.getKey()).lowerEndpoint()),
                        Math.min(
                            entry.getValue().upperEndpoint(),
                            capRanges.get(entry.getKey()).upperEndpoint()))))
        .collect(MapStreams.toMap());
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
      TileGeneratorFeatures tileGenerator,
      Map<String, TilesetFeatures> tilesets,
      List<String> rasterTilesets) {
    return Stream.concat(
            tilesets.values().stream()
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
                              : Map.of(
                                  tileset.getId(),
                                  tileGenerator.getGenerationSchema(tileset.getId()));

                      return new SimpleImmutableEntry<>(tileset.getId(), schemas);
                    }),
            rasterTilesets.stream()
                .map(
                    tileset ->
                        new SimpleImmutableEntry<>(
                            tileset, Map.<String, TileGenerationSchema>of())))
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
  public Optional<TilesetMetadata> getMetadata(String tilesetId) {
    return Optional.ofNullable(metadata.get(tilesetId));
  }

  @Override
  public Optional<TilesetMetadata> getMetadata(String vectorTilesetId, String mapStyleId) {
    return Optional.ofNullable(metadata.get(getMapStyleTileset(vectorTilesetId, mapStyleId)));
  }

  @Override
  public List<String> getMapStyles(String vectorTilesetId) {
    return metadata.entrySet().stream()
        .filter(entry -> entry.getValue().isRaster())
        .map(entry -> getStyleId(vectorTilesetId, entry.getKey()))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public String getMapStyleTileset(String vectorTilesetId, String mapStyleId) {
    return String.format("%s_%s", vectorTilesetId, mapStyleId);
  }

  @Override
  public TileResult getTile(TileQuery tile) {
    Optional<TileResult> error = validate(tile);

    if (error.isPresent()) {
      return error.get();
    }

    TilesetFeatures tileset = getData().getTilesets().get(tile.getTileset());

    TileResult result =
        Objects.isNull(tileset)
            ? rasterProviderChain.get(tile)
            : tileset.isCombined()
                ? combinerProviderChain.get(tile)
                : generatorProviderChain.get(tile);

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
  public Map<String, Map<String, Set<TileMatrixSetLimits>>> getCoverage(
      Map<String, TileGenerationParameters> tilesets) throws IOException {
    Map<String, TileGenerationParameters> validTilesets = validTilesets(tilesets);
    Map<String, TileGenerationParameters> sourcedTilesets = sourcedTilesets(validTilesets);
    Map<String, TileGenerationParameters> combinedTilesets = combinedTilesets(validTilesets);

    Map<String, Map<String, Set<TileMatrixSetLimits>>> coverage = new LinkedHashMap<>();

    if (!sourcedTilesets.isEmpty()) {
      for (TileCache cache : generatorCaches) {
        if (cache.isSeeded()) {
          mergeCoverageInto(cache.getCoverage(sourcedTilesets), coverage);
        }
      }
    }
    if (!combinedTilesets.isEmpty()) {
      for (TileCache cache : combinerCaches) {
        if (cache.isSeeded()) {
          mergeCoverageInto(cache.getCoverage(combinedTilesets), coverage);
        }
      }
    }

    return coverage;
  }

  @Override
  public Map<String, Map<String, Set<TileMatrixSetLimits>>> getRasterCoverage(
      Map<String, TileGenerationParameters> tilesets) throws IOException {
    Map<String, TileGenerationParameters> validTilesets = rasterTilesets(tilesets);

    Map<String, Map<String, Set<TileMatrixSetLimits>>> coverage = new LinkedHashMap<>();

    for (TileCache cache : generatorCaches) {
      if (cache.isSeeded()) {
        mergeCoverageInto(cache.getRasterCoverage(validTilesets), coverage);
      }
    }
    for (TileCache cache : combinerCaches) {
      if (cache.isSeeded()) {
        mergeCoverageInto(cache.getRasterCoverage(validTilesets), coverage);
      }
    }

    return coverage;
  }

  @Override
  public Map<String, String> getRasterStorageInfo(
      String rasterTileset, String tileMatrixSet, TileSubMatrix subMatrix) {
    Optional<String> vectorTilesetId = getVectorTilesetId(rasterTileset);

    if (vectorTilesetId.isEmpty()) {
      return Map.of();
    }

    return getRasterStorageInfo(
        rasterTileset, tileMatrixSet, subMatrix, vectorTilesetId.get(), subMatrix);
  }

  @Override
  public Map<String, String> getRasterStorageInfo(
      String rasterTileset,
      String tileMatrixSet,
      TileSubMatrix subMatrix,
      String vectorTileset,
      TileSubMatrix vectorSubMatrix) {
    Map<String, String> result = new LinkedHashMap<>();

    for (TileCache cache : generatorCaches) {
      if (cache.isSeeded()) {
        Optional<String> vectorStorage =
            cache
                .getStorageInfo(vectorTileset, tileMatrixSet, vectorSubMatrix.toLimits())
                .map(
                    path -> {
                      if (cache.getStorageType() == Storage.PER_JOB) {
                        String partition =
                            Files.getNameWithoutExtension(Path.of(path).getFileName().toString());
                        return path.replace(partition, "{partition}");
                      }
                      return path;
                    });
        Optional<String> rasterStorage =
            cache.getStorageInfo(rasterTileset, tileMatrixSet, subMatrix.toLimits());

        result.put("type", cache.getStorageType().name());
        result.put("jobSize", String.valueOf(getOptions().getEffectiveJobSize()));
        vectorStorage.ifPresent(s -> result.put("vector", s.replace(dataDir + "/", "")));
        rasterStorage.ifPresent(s -> result.put("raster", s.replace(dataDir + "/", "")));
        break;
      }
    }
    if (result.isEmpty()) {
      for (TileCache cache : combinerCaches) {
        if (cache.isSeeded()) {
          Optional<String> vectorStorage =
              cache
                  .getStorageInfo(vectorTileset, tileMatrixSet, vectorSubMatrix.toLimits())
                  .map(
                      path -> {
                        if (cache.getStorageType() == Storage.PER_JOB) {
                          String partition =
                              Files.getNameWithoutExtension(Path.of(path).getFileName().toString());
                          return path.replace(partition, "{partition}");
                        }
                        return path;
                      });
          Optional<String> rasterStorage =
              cache.getStorageInfo(rasterTileset, tileMatrixSet, subMatrix.toLimits());

          result.put("type", cache.getStorageType().name());
          result.put("jobSize", String.valueOf(getOptions().getEffectiveJobSize()));
          vectorStorage.ifPresent(s -> result.put("vector", s.replace(dataDir + "/", "")));
          rasterStorage.ifPresent(s -> result.put("raster", s.replace(dataDir + "/", "")));
          break;
        }
      }
    }

    // TODO: get style from api/values
    getStyleId(vectorTileset, rasterTileset)
        .ifPresent(
            style -> {
              try {
                stylesStore
                    .asLocalPath(Path.of("maplibre-styles", clean(getId()), style + ".mbs"), false)
                    .ifPresent(
                        path -> result.put("style", path.toString().replace(dataDir + "/", "")));

                if (!result.containsKey("style")) {
                  stylesStore
                      .asLocalPath(
                          Path.of("maplibre-styles", clean(getId()), style + ".json"), false)
                      .ifPresent(
                          path -> result.put("style", path.toString().replace(dataDir + "/", "")));
                }
              } catch (IOException e) {
                // ignore
              }
            });

    return result;
  }

  private static void mergeCoverageInto(
      Map<String, Map<String, Set<TileMatrixSetLimits>>> source,
      Map<String, Map<String, Set<TileMatrixSetLimits>>> target) {
    source.forEach(
        (tileset, tms) -> {
          if (!target.containsKey(tileset)) {
            target.put(tileset, new LinkedHashMap<>());
          }
          tms.forEach(
              (tmsId, limits) -> {
                if (!target.get(tileset).containsKey(tmsId)) {
                  target.get(tileset).put(tmsId, new LinkedHashSet<>());
                }
                target.get(tileset).get(tmsId).addAll(limits);
              });
        });
  }

  @Override
  public void setupSeeding(TileSeedingJobSet jobSet) throws IOException {
    for (Tuple<TileCache, String> cache : getCaches(jobSet)) {
      cache.first().setupSeeding(jobSet, cache.second());
    }
  }

  @Override
  public void cleanupSeeding(TileSeedingJobSet jobSet) throws IOException {
    LOGGER.debug("{}: cleaning up tile caches", TileSeedingJobSet.LABEL);

    for (Tuple<TileCache, String> cache : getCaches(jobSet)) {
      cache.first().cleanupSeeding(jobSet, cache.second());
    }

    // TODO: cleanup all orphaned tiles that are not within current cache limits
  }

  private List<Tuple<TileCache, String>> getCaches(TileSeedingJobSet jobSet) {
    List<Tuple<TileCache, String>> result = new ArrayList<>();
    Set<TileCache> done = new LinkedHashSet<>();

    jobSet
        .getTileSets()
        .keySet()
        .forEach(
            tileSet -> {
              if (!metadata.containsKey(tileSet)) {
                LOGGER.warn("Tileset with name '{}' not found", tileSet);
                return;
              }

              boolean isCombined =
                  getData().getTilesets().containsKey(tileSet)
                      && getData().getTilesets().get(tileSet).isCombined();
              List<TileCache> caches = isCombined ? combinerCaches : generatorCaches;
              String label = isCombined ? "tile combiner" : "tile generator";

              for (TileCache cache : caches) {
                if (cache.canProcess(jobSet) && !done.contains(cache)) {
                  done.add(cache);
                  result.add(Tuple.of(cache, label));
                }
              }
            });

    return result;
  }

  @Override
  public void runSeeding(TileSeedingJob job) throws IOException {
    if (!metadata.containsKey(job.getTileSet())) {
      LOGGER.warn("Tileset with name '{}' not found", job.getTileSet());
      return;
    }

    boolean isCombined = getData().getTilesets().get(job.getTileSet()).isCombined();
    List<TileCache> caches = isCombined ? combinerCaches : generatorCaches;
    String label = isCombined ? "tile combiner" : "tile generator";
    String action = isCombined ? "combining" : "generating";
    Instant start = Instant.now();

    if (LOGGER.isDebugEnabled() || LOGGER.isDebugEnabled(MARKER.JOBS)) {
      LOGGER.debug(
          MARKER.JOBS,
          "{}: {} {} tiles (Tileset: {}, TileMatrixSet: {}, Scope: {}, Encoding: {})",
          TileSeedingJobSet.LABEL,
          action,
          job.getNumberOfTiles(),
          job.getTileSet(),
          job.getTileMatrixSet(),
          job.getSubMatrices().get(0).asString(),
          job.getEncoding());
    }

    if (job.isReseed()) {
      for (TileCache cache : caches) {
        if (cache.canProcess(job)) {
          cache.purge(job, label);
        }
      }
    }

    for (TileCache cache : caches) {
      if (cache.canProcess(job)) {
        cache.seed(job, label);
      }
    }

    if (LOGGER.isDebugEnabled() || LOGGER.isDebugEnabled(MARKER.JOBS)) {
      long duration = Instant.now().toEpochMilli() - start.toEpochMilli();
      LOGGER.debug(
          MARKER.JOBS,
          "{}: finished {} {} tiles in {} (Tileset: {}, TileMatrixSet: {}, Scope: {}, Encoding: {})",
          TileSeedingJobSet.LABEL,
          action,
          job.getNumberOfTiles(),
          pretty(duration),
          job.getTileSet(),
          job.getTileMatrixSet(),
          job.getSubMatrices().get(0).asString(),
          job.getEncoding());
    }
  }

  private static String pretty(long milliseconds) {
    Duration d =
        milliseconds > 999
            ? Duration.ofSeconds(milliseconds / 1000)
            : Duration.ofMillis(milliseconds);
    return AmountFormats.wordBased(d, Locale.ENGLISH);
  }

  private Map<String, TileGenerationParameters> validTilesets(
      Map<String, TileGenerationParameters> tilesets) {
    return tilesets.entrySet().stream()
        .filter(
            entry -> {
              if (getRasterTilesets().contains(entry.getKey())) {
                return false;
              }
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

  private Map<String, TileGenerationParameters> rasterTilesets(
      Map<String, TileGenerationParameters> tilesets) {
    return tilesets.entrySet().stream()
        .filter(entry -> getRasterTilesets().contains(entry.getKey()))
        .collect(MapStreams.toMap());
  }

  private void loadMetadata() {
    getData().getTilesets().forEach((key, tileset) -> metadata.put(key, loadMetadata(tileset)));

    Optional<Cache> rasterCache =
        getData().getCaches().stream().filter(Cache::getSeeded).findFirst();

    if (rasterCache.isPresent()) {
      getData()
          .getRasterTilesets()
          .forEach(
              (key, tileset) ->
                  tileset
                      .getStyles()
                      .forEach(
                          style ->
                              loadMetadata(
                                      tileset.getPrefix().orElse(key),
                                      style,
                                      tileset,
                                      rasterCache.get())
                                  .ifPresent(
                                      tilesetMetadata ->
                                          metadata.put(
                                              getRasterTilesetId(
                                                  tileset.getPrefix().orElse(key), style),
                                              tilesetMetadata))));
    }
  }

  private static String getRasterTilesetId(String vectorTilesetId, String style) {
    return String.format("%s_%s", vectorTilesetId, getStyleId(style));
  }

  private Optional<String> getVectorTilesetId(String rasterTilesetId) {
    for (String vectorTilesetId : getData().getTilesets().keySet()) {
      if (rasterTilesetId.startsWith(String.format("%s_", vectorTilesetId))) {
        return Optional.of(vectorTilesetId);
      }
    }
    return Optional.empty();
  }

  private static String getStyleId(String style) {
    return Files.getNameWithoutExtension(Path.of(style).getFileName().toString());
  }

  private static Optional<String> getStyleId(String vectorTilesetId, String rasterTilesetId) {
    if (rasterTilesetId.startsWith(String.format("%s_", vectorTilesetId))) {
      return Optional.of(rasterTilesetId.substring(vectorTilesetId.length() + 1));
    }
    return Optional.empty();
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
        .addEncodings(TilesFormat.MVT)
        .levels(getTilesetLevels(tileset))
        .center(tileset.getCenter().or(() -> getData().getTilesetDefaults().getCenter()))
        .bounds(bounds)
        .vectorSchemas(vectorSchemas)
        .build();
  }

  private Map<String, MinMax> getTilesetLevels(TilesetCommon tileset) {
    return tileset.getLevels().isEmpty()
        ? getData().getTilesetDefaults().getLevels()
        : tileset.getLevels();
  }

  private Map<String, Range<Integer>> getTilesetRanges(String tileset) {
    return getTilesetLevels(getData().getTilesets().get(tileset)).entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().asRange()));
  }

  private Optional<TilesetMetadata> loadMetadata(
      String vectorTilesetId, String style, TilesetRaster tileset, Cache cache) {
    Optional<BoundingBox> bounds = tileGenerator.getBounds(vectorTilesetId);

    Map<String, Map<String, Range<Integer>>> cacheRanges = getCacheRanges(cache, 1);

    Map<String, Integer> defaults =
        getMetadata(vectorTilesetId)
            .map(TilesetMetadata::getLevels)
            .map(
                levels ->
                    levels.entrySet().stream()
                        .filter(entry -> entry.getValue().getDefault().isPresent())
                        .collect(
                            Collectors.toMap(
                                Entry::getKey, entry -> entry.getValue().getDefault().get())))
            .orElse(ImmutableMap.of());
    Map<String, MinMax> levels =
        cacheRanges.get(getRasterTilesetId(vectorTilesetId, style)).entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    entry ->
                        new ImmutableMinMax.Builder()
                            .min(entry.getValue().lowerEndpoint())
                            .max(entry.getValue().upperEndpoint())
                            .getDefault(Optional.ofNullable(defaults.get(entry.getKey())))
                            .build()));

    if (levels.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(
        ImmutableTilesetMetadata.builder()
            .addEncodings(TilesFormat.PNG)
            .levels(levels)
            .center(tileset.getCenter().or(() -> getData().getTilesetDefaults().getCenter()))
            .bounds(bounds)
            .styleId(getStyleId(style))
            .build());
  }
}
