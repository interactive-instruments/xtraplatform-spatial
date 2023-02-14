/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.app;

import static de.ii.xtraplatform.base.domain.util.LambdaWithException.consumerMayThrow;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.store.domain.BlobStore;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetBase;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetLimits;
import de.ii.xtraplatform.tiles.domain.TileQuery;
import de.ii.xtraplatform.tiles.domain.TileResult;
import de.ii.xtraplatform.tiles.domain.TileStore;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TileStorePlain implements TileStore {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileStorePlain.class);

  private static Map<MediaType, String> EXTENSIONS =
      ImmutableMap.of(FeatureEncoderMVT.FORMAT, "mvt");

  private final BlobStore blobStore;

  TileStorePlain(BlobStore blobStore) {
    this.blobStore = blobStore;
  }

  @Override
  public boolean has(TileQuery tile) throws IOException {
    return blobStore.has(path(tile));
  }

  @Override
  public TileResult get(TileQuery tile) throws IOException {
    Optional<InputStream> content = blobStore.get(path(tile));

    if (content.isEmpty()) {
      return TileResult.notFound();
    }

    try (InputStream result = content.get()) {
      return TileResult.found(result.readAllBytes());
    }
  }

  @Override
  public Optional<Boolean> isEmpty(TileQuery tile) throws IOException {
    long size = blobStore.size(path(tile));

    return size < 0 ? Optional.empty() : Optional.of(size == 0);
  }

  @Override
  public boolean isEmpty() throws IOException {
    try (Stream<Path> paths = blobStore.walk(Path.of(""), 5, (p, a) -> a.isValue())) {
      return paths.findAny().isEmpty();
    } catch (IOException e) {
      // ignore
    }

    return false;
  }

  @Override
  public void walk(Walker walker) {
    try (Stream<Path> paths = blobStore.walk(Path.of(""), 5, (p, a) -> a.isValue())) {
      paths.forEach(
          path -> {
            if (path.getNameCount() == 5) {
              walker.walk(
                  path.getName(0).toString(),
                  path.getName(1).toString(),
                  Integer.parseInt(path.getName(2).toString()),
                  Integer.parseInt(path.getName(3).toString()),
                  Integer.parseInt(Files.getNameWithoutExtension(path.getName(4).toString())));
            }
          });
    } catch (IOException e) {
      LogContext.errorAsDebug(
          LOGGER, e, "Could not walk cache level tiles {}.", blobStore.getPrefix());
    }
  }

  @Override
  public boolean has(String layer, String tms, int level, int row, int col) throws IOException {
    return blobStore.has(path(layer, tms, level, row, col));
  }

  @Override
  public void put(TileQuery tile, InputStream content) throws IOException {
    blobStore.put(path(tile), content);
  }

  @Override
  public void delete(TileQuery tile) throws IOException {
    blobStore.delete(path(tile));
  }

  @Override
  public void delete(
      String layer, TileMatrixSetBase tileMatrixSet, TileMatrixSetLimits limits, boolean inverse)
      throws IOException {
    try (Stream<Path> matchingFiles =
        blobStore.walk(
            Path.of(""),
            5,
            (path, fileAttributes) ->
                fileAttributes.isValue()
                    && TileStore.isInsideBounds(
                        path, layer, tileMatrixSet.getId(), limits, inverse))) {

      try {
        matchingFiles.forEach(consumerMayThrow(blobStore::delete));
      } catch (RuntimeException e) {
        if (e.getCause() instanceof IOException) {
          throw (IOException) e.getCause();
        }
        throw e;
      }
    }
  }

  @Override
  public void delete(String layer, String tms, int level, int row, int col) throws IOException {
    blobStore.delete(path(layer, tms, level, row, col));
  }

  private static Path path(TileQuery tile) {
    return Path.of(
        tile.getLayer(),
        tile.getTileMatrixSet().getId(),
        String.valueOf(tile.getLevel()),
        String.valueOf(tile.getRow()),
        String.format("%d.%s", tile.getCol(), EXTENSIONS.get(tile.getMediaType())));
  }

  private static Path path(String layer, String tms, int level, int row, int col) {
    return Path.of(
        layer,
        tms,
        String.valueOf(level),
        String.valueOf(row),
        String.format("%d.%s", col, EXTENSIONS.get(FeatureEncoderMVT.FORMAT)));
  }
}
