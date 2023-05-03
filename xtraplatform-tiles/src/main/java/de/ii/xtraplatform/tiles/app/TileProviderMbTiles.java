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
import de.ii.xtraplatform.base.domain.util.Tuple;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.store.domain.BlobStore;
import de.ii.xtraplatform.tiles.domain.ChainedTileProvider;
import de.ii.xtraplatform.tiles.domain.ImmutableMinMax;
import de.ii.xtraplatform.tiles.domain.ImmutableTilesetMetadata;
import de.ii.xtraplatform.tiles.domain.MbtilesMetadata;
import de.ii.xtraplatform.tiles.domain.MbtilesMetadata.MbtilesFormat;
import de.ii.xtraplatform.tiles.domain.MbtilesTileset;
import de.ii.xtraplatform.tiles.domain.MinMax;
import de.ii.xtraplatform.tiles.domain.TileMatrixSet;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetRepository;
import de.ii.xtraplatform.tiles.domain.TileProvider;
import de.ii.xtraplatform.tiles.domain.TileProviderMbtilesData;
import de.ii.xtraplatform.tiles.domain.TileQuery;
import de.ii.xtraplatform.tiles.domain.TileResult;
import de.ii.xtraplatform.tiles.domain.TileStoreReadOnly;
import de.ii.xtraplatform.tiles.domain.TilesetMetadata;
import de.ii.xtraplatform.tiles.domain.VectorLayer;
import de.ii.xtraplatform.tiles.domain.WithCenter;
import de.ii.xtraplatform.tiles.domain.WithCenter.LonLat;
import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TileProviderMbTiles extends AbstractTileProvider<TileProviderMbtilesData>
    implements TileProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileProviderMbTiles.class);
  private final BlobStore tilesStore;
  private final TileMatrixSetRepository tileMatrixSetRepository;
  private final Map<String, TilesetMetadata> metadata;
  private final Map<String, Map<String, Range<Integer>>> tmsRanges;
  private ChainedTileProvider providerChain;

  @AssistedInject
  public TileProviderMbTiles(
      BlobStore blobStore,
      TileMatrixSetRepository tileMatrixSetRepository,
      @Assisted TileProviderMbtilesData data) {
    super(data);

    this.tilesStore = blobStore.with(TileProviderFeatures.TILES_DIR_NAME);
    this.tileMatrixSetRepository = tileMatrixSetRepository;
    this.metadata = new LinkedHashMap<>();
    this.tmsRanges = new LinkedHashMap<>();
  }

  @Override
  protected boolean onStartup() throws InterruptedException {
    // we know there is exactly one tileset and one tile matrix set
    Map<String, Path> tilesetSources =
        getData().getTilesets().entrySet().stream()
            .map(
                entry -> {
                  Path source = Path.of(entry.getValue().getSource());

                  if (!source.isAbsolute()) {
                    if (source.startsWith("api-resources/tiles")) {
                      source = Path.of("api-resources/tiles").relativize(source);
                    }
                    Optional<Path> localPath = Optional.empty();
                    try {
                      localPath = tilesStore.asLocalPath(source, false);
                    } catch (IOException e) {
                      // continue
                    }
                    if (localPath.isEmpty()) {
                      throw new IllegalStateException(
                          "Could not locate MBTiles file. Make sure you have a localizable source defined in cfg.yml.");
                    }
                    source = localPath.get();
                  }

                  Set<String> tmsSet = entry.getValue().getLevels().keySet();
                  if (tmsSet.isEmpty()) {
                    tmsSet = getData().getTilesetDefaults().getLevels().keySet();
                  }
                  String tms = tmsSet.isEmpty() ? "WebMercatorQuad" : tmsSet.iterator().next();

                  return new SimpleImmutableEntry<>(toTilesetKey(entry.getKey(), tms), source);
                })
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    TileStoreReadOnly tileStore = TileStoreMbTiles.readOnly(tilesetSources);

    this.providerChain =
        new ChainedTileProvider() {
          @Override
          public Map<String, Map<String, Range<Integer>>> getTmsRanges() {
            return tmsRanges;
          }

          @Override
          public TileResult getTile(TileQuery tile) throws IOException {
            return tileStore.get(tile);
          }
        };

    loadMetadata(tilesetSources);

    return true;
  }

  @Override
  public Optional<TilesetMetadata> metadata(String tileset) {
    return Optional.ofNullable(metadata.get(tileset));
  }

  @Override
  public TileResult getTile(TileQuery tile) {
    Optional<TileResult> error = validate(tile);

    if (error.isPresent()) {
      return error.get();
    }

    return providerChain.get(tile);
  }

  @Override
  public boolean supportsGeneration() {
    return false;
  }

  @Override
  public String getType() {
    return TileProviderMbtilesData.PROVIDER_TYPE;
  }

  private void loadMetadata(Map<String, Path> tilesetSources) {
    tilesetSources.forEach(
        (key, path) -> {
          Tuple<String, String> tilesetKey = toTuple(key);

          metadata.put(tilesetKey.first(), loadMetadata(tilesetKey.second(), path));
          tmsRanges.put(tilesetKey.first(), metadata.get(tilesetKey.first()).getTmsRanges());
        });
  }

  private TilesetMetadata loadMetadata(String tms, Path path) {
    try {
      MbtilesMetadata metadata = new MbtilesTileset(path).getMetadata();
      TileMatrixSet tileMatrixSet =
          tileMatrixSetRepository
              .get(tms)
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          String.format("Unknown tile matrix set: '%s'.", tms)));
      int minzoom = metadata.getMinzoom().orElse(tileMatrixSet.getMinLevel());
      int maxzoom = metadata.getMaxzoom().orElse(tileMatrixSet.getMaxLevel());
      Optional<Integer> defzoom =
          metadata.getCenter().size() == 3
              ? Optional.of(Math.round(metadata.getCenter().get(2).floatValue()))
              : Optional.empty();
      Optional<LonLat> center =
          metadata.getCenter().size() >= 2
              ? Optional.of(
                  WithCenter.LonLat.of(
                      metadata.getCenter().get(0).doubleValue(),
                      metadata.getCenter().get(1).doubleValue()))
              : Optional.empty();
      Map<String, MinMax> zoomLevels =
          ImmutableMap.of(
              tms,
              new ImmutableMinMax.Builder().min(minzoom).max(maxzoom).getDefault(defzoom).build());
      List<Double> bbox = metadata.getBounds();
      Optional<BoundingBox> bounds =
          bbox.size() == 4
              ? Optional.of(
                  BoundingBox.of(bbox.get(0), bbox.get(1), bbox.get(2), bbox.get(3), OgcCrs.CRS84))
              : Optional.empty();
      String format = getFormat(metadata.getFormat());
      List<FeatureSchema> vectorSchemas =
          metadata.getVectorLayers().stream()
              .map(VectorLayer::toFeatureSchema)
              .collect(Collectors.toList());

      return ImmutableTilesetMetadata.builder()
          .addEncodings(format)
          .levels(zoomLevels)
          .center(center)
          .bounds(bounds)
          .vectorSchemas(vectorSchemas)
          .build();
    } catch (Exception e) {
      throw new RuntimeException("Could not derive metadata from Mbtiles tile provider.", e);
    }
  }

  private String getFormat(MbtilesFormat format) {
    if (format == MbtilesMetadata.MbtilesFormat.pbf) return "MVT";
    else if (format == MbtilesMetadata.MbtilesFormat.jpg) return "JPEG";
    else if (format == MbtilesMetadata.MbtilesFormat.png) return "PNG";
    else if (format == MbtilesMetadata.MbtilesFormat.webp) return "WEBP";
    else if (format == MbtilesMetadata.MbtilesFormat.tiff) return "TIFF";

    throw new UnsupportedOperationException(
        String.format("Mbtiles format '%s' is currently not supported.", format));
  }

  private String toTilesetKey(String tileset, String tms) {
    return String.join("/", tileset, tms);
  }

  private Tuple<String, String> toTuple(String tilesetKey) {
    String[] split = tilesetKey.split("/");
    return Tuple.of(split[0], split[1]);
  }
}
