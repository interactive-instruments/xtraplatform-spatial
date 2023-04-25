/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.app;

import static de.ii.xtraplatform.base.domain.util.LambdaWithException.consumerMayThrow;

import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.store.domain.BlobStore;
import de.ii.xtraplatform.tiles.domain.ImmutableMbtilesMetadata;
import de.ii.xtraplatform.tiles.domain.ImmutableVectorLayer;
import de.ii.xtraplatform.tiles.domain.MbtilesMetadata;
import de.ii.xtraplatform.tiles.domain.MbtilesTileset;
import de.ii.xtraplatform.tiles.domain.TileGenerationSchema;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetBase;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetLimits;
import de.ii.xtraplatform.tiles.domain.TileQuery;
import de.ii.xtraplatform.tiles.domain.TileResult;
import de.ii.xtraplatform.tiles.domain.TileStore;
import de.ii.xtraplatform.tiles.domain.TileStoreReadOnly;
import de.ii.xtraplatform.tiles.domain.VectorLayer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TileStoreMbTiles implements TileStore {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileStoreMbTiles.class);
  private static final String MBTILES_SUFFIX = ".mbtiles";

  static TileStoreReadOnly readOnly(Map<String, Path> tileSetSources) {
    Map<String, MbtilesTileset> tileSets =
        tileSetSources.entrySet().stream()
            .map(
                entry ->
                    new SimpleImmutableEntry<>(
                        entry.getKey(), new MbtilesTileset(entry.getValue())))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return new TileStoreMbTiles("", null, tileSets, Map.of());
  }

  static TileStore readWrite(
      BlobStore rootStore,
      String providerId,
      Map<String, Map<String, TileGenerationSchema>> tileSchemas) {

    Map<String, MbtilesTileset> tileSets = new ConcurrentHashMap<>();

    try (Stream<Path> files =
        rootStore.walk(
            Path.of(""),
            2,
            (p, a) -> a.isValue() && p.getFileName().toString().endsWith(MBTILES_SUFFIX))) {
      files
          .filter(path -> path.getNameCount() == 2)
          .forEach(
              consumerMayThrow(
                  path -> {
                    String layer = path.getName(0).toString();
                    String tms = path.getName(1).toString().replace(MBTILES_SUFFIX, "");
                    tileSets.put(
                        key(layer, tms),
                        createTileSet(
                            rootStore,
                            providerId,
                            layer,
                            tms,
                            getVectorLayers(tileSchemas, layer)));
                  }));
    } catch (IOException e) {
      LogContext.errorAsWarn(LOGGER, e, "Error when loading tile caches");
    } catch (RuntimeException e) {
      if (e.getCause() instanceof IOException) {
        LogContext.errorAsWarn(LOGGER, e.getCause(), "Error when loading tile caches");
      }
      throw e;
    }
    return new TileStoreMbTiles(providerId, rootStore, tileSets, tileSchemas);
  }

  private final String providerId;
  private final BlobStore rootStore;
  private final Map<String, Map<String, TileGenerationSchema>> tileSchemas;
  private final Map<String, MbtilesTileset> tileSets;

  private TileStoreMbTiles(
      String providerId,
      BlobStore rootStore,
      Map<String, MbtilesTileset> tileSets,
      Map<String, Map<String, TileGenerationSchema>> tileSchemas) {
    this.providerId = providerId;
    this.rootStore = rootStore;
    this.tileSchemas = tileSchemas;
    this.tileSets = tileSets;
  }

  @Override
  public boolean has(TileQuery tile) {
    try {
      return tileSets.containsKey(key(tile)) && tileSets.get(key(tile)).tileExists(tile);
    } catch (SQLException | IOException e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(
            "Failed to check existence of tile {}/{}/{}/{} for layer '{}'. Reason: {}",
            tile.getTileMatrixSet().getId(),
            tile.getLevel(),
            tile.getRow(),
            tile.getCol(),
            tile.getTileset(),
            e.getMessage());
        if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE)) {
          LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace: ", e);
        }
      }
    }
    return false;
  }

  @Override
  public TileResult get(TileQuery tile) throws IOException {
    if (!tileSets.containsKey(key(tile))) {
      return TileResult.notFound();
    }

    try {
      Optional<InputStream> content = tileSets.get(key(tile)).getTile(tile);

      if (content.isEmpty()) {
        return TileResult.notFound();
      }

      return TileResult.found(content.get().readAllBytes());
    } catch (SQLException e) {
      return TileResult.error(e.getMessage());
    }
  }

  @Override
  public Optional<Boolean> isEmpty(TileQuery tile) throws IOException {
    try {
      if (tileSets.containsKey(key(tile))) {
        return tileSets.get(key(tile)).tileIsEmpty(tile);
      }
    } catch (SQLException e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(
            "Failed to retrieve tile {}/{}/{}/{} for layer '{}'. Reason: {}",
            tile.getTileMatrixSet().getId(),
            tile.getLevel(),
            tile.getRow(),
            tile.getCol(),
            tile.getTileset(),
            e.getMessage());
        if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE)) {
          LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace: ", e);
        }
      }
    }
    return Optional.empty();
  }

  @Override
  public boolean isEmpty() throws IOException {
    return tileSets.isEmpty()
        || tileSets.values().stream()
            .allMatch(
                mbtilesTileset -> {
                  try {
                    return !mbtilesTileset.hasAnyTiles();
                  } catch (SQLException | IOException e) {
                    return false;
                  }
                });
  }

  @Override
  public void put(TileQuery tile, InputStream content) throws IOException {
    try {
      synchronized (tileSets) {
        if (!tileSets.containsKey(key(tile))) {
          tileSets.put(
              key(tile),
              createTileSet(
                  rootStore,
                  providerId,
                  tile.getTileset(),
                  tile.getTileMatrixSet().getId(),
                  getVectorLayers(tileSchemas, tile.getTileset())));
        }
      }
      tileSets.get(key(tile)).writeTile(tile, content.readAllBytes());

    } catch (SQLException e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(
            "Failed to write tile {}/{}/{}/{} for layer '{}'. Reason: {}",
            tile.getTileMatrixSet().getId(),
            tile.getLevel(),
            tile.getRow(),
            tile.getCol(),
            tile.getTileset(),
            e.getMessage());
        if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE)) {
          LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace: ", e);
        }
      }
    }
  }

  @Override
  public void delete(TileQuery tile) throws IOException {
    try {
      if (tileSets.containsKey(key(tile))) {
        tileSets.get(key(tile)).deleteTile(tile);
      }
    } catch (SQLException e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(
            "Failed to delete tile {}/{}/{}/{} for layer '{}'. Reason: {}",
            tile.getTileMatrixSet().getId(),
            tile.getLevel(),
            tile.getRow(),
            tile.getCol(),
            tile.getTileset(),
            e.getMessage());
        if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE)) {
          LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace: ", e);
        }
      }
    }
  }

  @Override
  public void delete(
      String layer, TileMatrixSetBase tileMatrixSet, TileMatrixSetLimits limits, boolean inverse)
      throws IOException {
    try {
      if (tileSets.containsKey(key(layer, tileMatrixSet))) {
        tileSets.get(key(layer, tileMatrixSet)).deleteTiles(tileMatrixSet, limits);
      }
    } catch (SQLException e) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(
            "Failed to delete tiles {}/{}/{}-{}/{}-{} for layer '{}'. Reason: {}",
            tileMatrixSet,
            limits.getTileMatrix(),
            limits.getMinTileRow(),
            limits.getMaxTileRow(),
            limits.getMinTileCol(),
            limits.getMaxTileCol(),
            layer,
            e.getMessage());
        if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE)) {
          LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace: ", e);
        }
      }
    }
  }

  @Override
  public void walk(Walker walker) {
    tileSets.forEach(
        (key, mbtiles) -> {
          String[] fromKey = fromKey(key);
          try {
            mbtiles.walk(
                (level, row, col) -> {
                  walker.walk(fromKey[0], fromKey[1], level, row, col);
                });
          } catch (SQLException | IOException e) {
            // ignore
          }
        });
  }

  @Override
  public boolean has(String layer, String tms, int level, int row, int col) throws IOException {
    try {
      return tileSets.containsKey(key(layer, tms))
          && tileSets.get(key(layer, tms)).tileExists(level, row, col);
    } catch (SQLException | IOException e) {
      // ignore
    }
    return false;
  }

  @Override
  public void delete(String layer, String tms, int level, int row, int col) throws IOException {
    try {
      if (tileSets.containsKey(key(layer, tms)))
        tileSets.get(key(layer, tms)).deleteTile(level, row, col, false);
    } catch (SQLException | IOException e) {
      // ignore
    }
  }

  private static List<VectorLayer> getVectorLayers(
      Map<String, Map<String, TileGenerationSchema>> tileSchemas, String layer) {
    return tileSchemas.get(layer).entrySet().stream()
        .map(entry -> getVectorLayer(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }

  // TODO: fields, minzoom, maxzoom
  private static VectorLayer getVectorLayer(
      String subLayer, TileGenerationSchema generationSchema) {
    return ImmutableVectorLayer.builder()
        .id(subLayer)
        .fields(generationSchema.getProperties())
        .geometryType(
            VectorLayer.getGeometryTypeAsString(
                generationSchema.getGeometryType().orElse(SimpleFeatureGeometry.ANY)))
        .build();
  }

  // TODO: minzoom, maxzoom, bounds, center
  private static MbtilesTileset createTileSet(
      BlobStore rootStore,
      String name,
      String layer,
      String tileMatrixSet,
      List<VectorLayer> vectorLayers)
      throws IOException {
    Path relPath = Path.of(layer).resolve(tileMatrixSet + MBTILES_SUFFIX);
    Optional<Path> filePath = rootStore.asLocalPath(relPath, true);

    if (filePath.isEmpty()) {
      throw new IllegalStateException(
          "Could not create MBTiles file. Make sure you have a writable localizable source defined in cfg.yml.");
    }

    if (rootStore.has(relPath)) {
      return new MbtilesTileset(filePath.get());
    }

    MbtilesMetadata md =
        ImmutableMbtilesMetadata.builder()
            .name(name)
            .format(MbtilesMetadata.MbtilesFormat.pbf)
            .vectorLayers(vectorLayers)
            .build();
    try {
      return new MbtilesTileset(filePath.get(), md);
    } catch (FileAlreadyExistsException e) {
      throw new IllegalStateException(
          "A MBTiles file already exists. It must have been created by a parallel thread, which should not occur. MBTiles file creation must be synchronized.");
    }
  }

  private static String key(TileQuery tile) {
    return key(tile.getTileset(), tile.getTileMatrixSet());
  }

  private static String key(String layer, TileMatrixSetBase tileMatrixSet) {
    return key(layer, tileMatrixSet.getId());
  }

  private static String key(String layer, String tileMatrixSet) {
    return String.join("/", layer, tileMatrixSet);
  }

  private static String[] fromKey(String key) {
    return key.split("/");
  }
}
