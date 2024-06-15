/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.app;

import de.ii.xtraplatform.tiles.domain.ImmutableTileSubMatrix;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetLimits;
import de.ii.xtraplatform.tiles.domain.TileSubMatrix;
import java.util.LinkedHashSet;
import java.util.Set;

public class TileStorePartitions {
  private final int maxTilesPerPartition;
  private final int singlePartitionLevel;
  private final int singleRowCol;

  public TileStorePartitions(int singlePartitionLevel) {
    this.maxTilesPerPartition = 256;
    this.singlePartitionLevel = singlePartitionLevel;
    this.singleRowCol = (int) Math.pow(2, singlePartitionLevel);
  }

  // 10/645/322
  public String getPartition(String tms, int level, int row, int col) {
    if (level < singlePartitionLevel) {
      return String.format("0-%d", singlePartitionLevel - 1);
    }
    if (level == singlePartitionLevel) {
      return String.format("%d", singlePartitionLevel);
    }

    int levelRowCol = (int) Math.pow(2, level);
    int levelTiles = (int) Math.pow(levelRowCol, 2);

    // 645 / 256 = 2
    int rowPartition = row / singleRowCol;
    // 322 / 256 = 1
    int colPartition = col / singleRowCol;

    return String.format(
        "%s/%d/%d/%d", tms, level, row / maxTilesPerPartition, col / maxTilesPerPartition);
  }

  private String getPartitionName(int level, int row, int col) {
    // 645 / 256 = 2
    int rowPartition = row / singleRowCol;
    // 322 / 256 = 1
    int colPartition = col / singleRowCol;

    return String.format(
        "%d_%d-%d_%d-%d",
        level,
        rowPartition * singleRowCol,
        (rowPartition + 1) * singleRowCol,
        colPartition * singleRowCol,
        (colPartition + 1) * singleRowCol);
  }

  public Set<TileSubMatrix> getSubMatrices(TileMatrixSetLimits limits) {
    int level = Integer.parseInt(limits.getTileMatrix());

    if (level <= singlePartitionLevel) {
      return Set.of(
          new ImmutableTileSubMatrix.Builder()
              .level(level)
              .rowMin(limits.getMinTileRow())
              .rowMax(limits.getMaxTileRow())
              .colMin(limits.getMinTileCol())
              .colMax(limits.getMaxTileCol())
              .build());
    }

    Set<TileSubMatrix> subMatrices = new LinkedHashSet<>();

    for (int row = limits.getMinTileRow(); row <= limits.getMaxTileRow(); row++) {
      for (int col = limits.getMinTileCol(); col <= limits.getMaxTileCol(); col++) {
        subMatrices.add(getSubMatrix(level, row, col));
      }
    }

    return subMatrices;
  }

  public TileSubMatrix getSubMatrix(int level, int row, int col) {
    if (level < singlePartitionLevel) {
      return new ImmutableTileSubMatrix.Builder()
          .level(singlePartitionLevel - 1)
          .rowMin(0)
          .rowMax(-1)
          .colMin(0)
          .colMax(-1)
          .build();
    }
    if (level == singlePartitionLevel) {
      return new ImmutableTileSubMatrix.Builder()
          .level(singlePartitionLevel)
          .rowMin(0)
          .rowMax(-1)
          .colMin(0)
          .colMax(-1)
          .build();
    }

    // 645 / 256 = 2
    int rowPartition = row / singleRowCol;
    // 322 / 256 = 1
    int colPartition = col / singleRowCol;

    return new ImmutableTileSubMatrix.Builder()
        .level(level)
        .rowMin(rowPartition * singleRowCol)
        .rowMax(((rowPartition + 1) * singleRowCol) - 1)
        .colMin(colPartition * singleRowCol)
        .colMax(((colPartition + 1) * singleRowCol) - 1)
        .build();
  }
}
