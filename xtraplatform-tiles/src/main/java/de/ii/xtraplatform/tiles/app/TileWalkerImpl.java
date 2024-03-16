/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.Range;
import de.ii.xtraplatform.base.domain.resiliency.AbstractVolatileComposed;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.services.domain.TaskContext;
import de.ii.xtraplatform.tiles.domain.MinMax;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetBase;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetLimits;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetRepository;
import de.ii.xtraplatform.tiles.domain.TileWalker;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
public class TileWalkerImpl extends AbstractVolatileComposed implements TileWalker {

  private final TileMatrixSetRepository tileMatrixSetRepository;
  private final CrsTransformerFactory crsTransformerFactory;

  @Inject
  public TileWalkerImpl(
      TileMatrixSetRepository tileMatrixSetRepository,
      CrsTransformerFactory crsTransformerFactory,
      VolatileRegistry volatileRegistry) {
    super(volatileRegistry);
    this.tileMatrixSetRepository = tileMatrixSetRepository;
    this.crsTransformerFactory = crsTransformerFactory;

    onVolatileStart();

    addSubcomponent(tileMatrixSetRepository);
    addSubcomponent(crsTransformerFactory);

    onVolatileStarted();
  }

  @Override
  public long getNumberOfTiles(
      Set<String> tilesets,
      List<MediaType> outputFormats,
      Map<String, Map<String, Range<Integer>>> tmsRanges,
      Map<String, Optional<BoundingBox>> boundingBoxes,
      TaskContext taskContext) {
    final long[] numberOfTiles = {0};

    try {
      walkTilesetsAndLimits(
          tilesets,
          tmsRanges,
          boundingBoxes,
          (tileset, tms, limits) -> {
            numberOfTiles[0] +=
                taskContext.isPartial()
                    ? limits.getNumberOfTiles(taskContext::matchesPartialModulo)
                    : limits.getNumberOfTiles();
          });
    } catch (IOException e) {
      // ignore
    }

    return numberOfTiles[0] * outputFormats.size();
  }

  @Override
  public void walkTilesetsAndTiles(
      Set<String> tilesets,
      List<MediaType> outputFormats,
      Map<String, Map<String, Range<Integer>>> tmsRanges,
      Map<String, Optional<BoundingBox>> boundingBoxes,
      TaskContext taskContext,
      TileVisitor tileWalker)
      throws IOException {
    for (Map.Entry<String, Map<String, Range<Integer>>> entry : tmsRanges.entrySet()) {
      String tileset = entry.getKey();

      if (tilesets.contains(tileset)) {
        Map<String, Range<Integer>> ranges = entry.getValue();
        Optional<BoundingBox> boundingBox = boundingBoxes.get(tileset);

        if (boundingBox.isPresent()) {
          walkTiles(tileset, outputFormats, ranges, boundingBox.get(), taskContext, tileWalker);
        }
      }
    }
  }

  @Override
  public void walkTilesetsAndLimits(
      Set<String> tilesets,
      Map<String, Map<String, Range<Integer>>> tmsRanges,
      Map<String, Optional<BoundingBox>> boundingBoxes,
      LimitsVisitor limitsVisitor)
      throws IOException {
    for (Map.Entry<String, Map<String, Range<Integer>>> entry : tmsRanges.entrySet()) {
      String tileset = entry.getKey();

      if (tilesets.contains(tileset)) {
        Map<String, Range<Integer>> ranges = entry.getValue();
        Optional<BoundingBox> boundingBox = boundingBoxes.get(tileset);

        if (boundingBox.isPresent()) {
          walkLimits(tileset, ranges, boundingBox.get(), limitsVisitor);
        }
      }
    }
  }

  @Override
  public void walkTilesetsAndLimits(
      Set<String> tilesets,
      Map<String, Map<String, Range<Integer>>> tmsRanges,
      LimitsVisitor limitsVisitor)
      throws IOException {
    for (Map.Entry<String, Map<String, Range<Integer>>> entry : tmsRanges.entrySet()) {
      String tileset = entry.getKey();

      if (tilesets.contains(tileset)) {
        Map<String, Range<Integer>> ranges = entry.getValue();

        walkLimits(tileset, ranges, limitsVisitor);
      }
    }
  }

  private void walkLimits(
      String tileset,
      Map<String, Range<Integer>> tmsRanges,
      BoundingBox boundingBox,
      LimitsVisitor limitsVisitor)
      throws IOException {
    for (Map.Entry<String, Range<Integer>> entry : tmsRanges.entrySet()) {
      TileMatrixSetBase tileMatrixSet = getTileMatrixSetById(entry.getKey());
      BoundingBox bbox = getBbox(boundingBox, tileMatrixSet);

      List<? extends TileMatrixSetLimits> allLimits =
          tileMatrixSet.getLimitsList(MinMax.of(entry.getValue()), bbox);

      for (TileMatrixSetLimits limits : allLimits) {
        limitsVisitor.visit(tileset, tileMatrixSet, limits);
      }
    }
  }

  private void walkLimits(
      String tileset, Map<String, Range<Integer>> tmsRanges, LimitsVisitor limitsVisitor)
      throws IOException {
    for (Map.Entry<String, Range<Integer>> entry : tmsRanges.entrySet()) {
      TileMatrixSetBase tileMatrixSet = getTileMatrixSetById(entry.getKey());

      List<? extends TileMatrixSetLimits> allLimits =
          tileMatrixSet.getLimitsList(MinMax.of(entry.getValue()), tileMatrixSet.getBoundingBox());

      for (TileMatrixSetLimits limits : allLimits) {
        limitsVisitor.visit(tileset, tileMatrixSet, limits);
      }
    }
  }

  private void walkTiles(
      String tileset,
      List<MediaType> outputFormats,
      Map<String, Range<Integer>> tmsRanges,
      BoundingBox boundingBox,
      TaskContext taskContext,
      TileVisitor tileWalker)
      throws IOException {
    for (MediaType outputFormat : outputFormats) {
      for (Map.Entry<String, Range<Integer>> entry : tmsRanges.entrySet()) {
        TileMatrixSetBase tileMatrixSet = getTileMatrixSetById(entry.getKey());
        BoundingBox bbox = getBbox(boundingBox, tileMatrixSet);

        List<? extends TileMatrixSetLimits> allLimits =
            tileMatrixSet.getLimitsList(MinMax.of(entry.getValue()), bbox);

        for (TileMatrixSetLimits limits : allLimits) {
          int level = Integer.parseInt(limits.getTileMatrix());

          for (int row = limits.getMinTileRow(); row <= limits.getMaxTileRow(); row++) {
            for (int col = limits.getMinTileCol(); col <= limits.getMaxTileCol(); col++) {
              if (taskContext.isPartial() && !taskContext.matchesPartialModulo(col)) {
                continue;
              }
              boolean shouldContinue =
                  tileWalker.visit(tileset, outputFormat, tileMatrixSet, level, row, col);
              if (!shouldContinue) {
                return;
              }
            }
          }
        }
      }
    }
  }

  private BoundingBox getBbox(BoundingBox boundingBox, TileMatrixSetBase tileMatrixSet) {
    return Objects.equals(boundingBox.getEpsgCrs(), tileMatrixSet.getCrs())
        ? boundingBox
        : crsTransformerFactory
            .getTransformer(boundingBox.getEpsgCrs(), tileMatrixSet.getCrs())
            .map(
                transformer -> {
                  try {
                    return transformer.transformBoundingBox(boundingBox);
                  } catch (CrsTransformationException e) {
                    return tileMatrixSet.getBoundingBox();
                  }
                })
            .orElse(tileMatrixSet.getBoundingBox());
  }

  private TileMatrixSetBase getTileMatrixSetById(String tileMatrixSetId) {
    return tileMatrixSetRepository.get(tileMatrixSetId).orElseThrow();
  }
}
