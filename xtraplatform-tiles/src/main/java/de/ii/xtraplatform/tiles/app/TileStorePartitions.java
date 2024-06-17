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
  final int maxTilesPerPartition;
  final int singlePartitionLevel;
  final int singleRowCol;

  public TileStorePartitions(int maxTilesPerPartition) {
    this.maxTilesPerPartition = maxTilesPerPartition;
    this.singleRowCol = (int) Math.sqrt(maxTilesPerPartition);
    this.singlePartitionLevel = (int) (Math.log(singleRowCol) / Math.log(2));
  }

  public String getPartitionName(int level, int row, int col) {
    if (level <= singlePartitionLevel) {
      return String.format("%d", level);
    }

    // 645 / 256 = 2
    int rowPartition = row / singleRowCol;
    // 322 / 256 = 1
    int colPartition = col / singleRowCol;

    return String.format(
        "%d_%d-%d_%d-%d",
        level,
        rowPartition * singleRowCol,
        ((rowPartition + 1) * singleRowCol) - 1,
        colPartition * singleRowCol,
        ((colPartition + 1) * singleRowCol) - 1);
  }

  public int[] getPartitionScope(String partitionName) {
    String[] parts = partitionName.split("_|-");

    if (parts.length == 1) {
      int level = Integer.parseInt(parts[0]);
      int max = (int) Math.pow(2, level);
      return new int[] {level, 0, max, 0, max};
    }

    return new int[] {
      Integer.parseInt(parts[0]),
      Integer.parseInt(parts[1]),
      Integer.parseInt(parts[2]),
      Integer.parseInt(parts[3]),
      Integer.parseInt(parts[4])
    };
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
        subMatrices.add(getSubMatrix(level, row, col, limits));
      }
    }

    return subMatrices;
  }

  public TileSubMatrix getSubMatrix(int level, int row, int col, TileMatrixSetLimits limits) {
    // 645 / 256 = 2
    int rowPartition = row / singleRowCol;
    // 322 / 256 = 1
    int colPartition = col / singleRowCol;

    int rowMin = rowPartition * singleRowCol;
    int rowMax = ((rowPartition + 1) * singleRowCol) - 1;
    int colMin = colPartition * singleRowCol;
    int colMax = ((colPartition + 1) * singleRowCol) - 1;

    return new ImmutableTileSubMatrix.Builder()
        .level(level)
        .rowMin(Math.max(rowMin, limits.getMinTileRow()))
        .rowMax(Math.min(rowMax, limits.getMaxTileRow()))
        .colMin(Math.max(colMin, limits.getMinTileCol()))
        .colMax(Math.min(colMax, limits.getMaxTileCol()))
        .build();
  }
}
