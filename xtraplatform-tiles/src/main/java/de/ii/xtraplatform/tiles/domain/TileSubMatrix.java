/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.immutables.value.Value;

@Value.Immutable
public interface TileSubMatrix {

  static TileSubMatrix of(int level, int rowMin, int rowMax, int colMin, int colMax) {
    return new ImmutableTileSubMatrix.Builder()
        .level(level)
        .rowMin(rowMin)
        .rowMax(rowMax)
        .colMin(colMin)
        .colMax(colMax)
        .build();
  }

  int getLevel();

  int getRowMin();

  int getRowMax();

  int getColMin();

  int getColMax();

  @Value.Derived
  @Value.Auxiliary
  @JsonIgnore
  default long getNumberOfTiles() {
    return ((long) getRowMax() - getRowMin() + 1) * (getColMax() - getColMin() + 1);
  }

  default TileMatrixSetLimits toLimits() {
    return new ImmutableTileMatrixSetLimits.Builder()
        .tileMatrix(String.valueOf(getLevel()))
        .minTileRow(getRowMin())
        .maxTileRow(getRowMax())
        .minTileCol(getColMin())
        .maxTileCol(getColMax())
        .build();
  }

  default String asString() {
    return String.format(
        "%d/%d-%d/%d-%d", getLevel(), getRowMin(), getRowMax(), getColMin(), getColMax());
  }

  default boolean contains(TileSubMatrix other) {
    return getLevel() == other.getLevel()
        && getRowMin() <= other.getRowMin()
        && getRowMax() >= other.getRowMax()
        && getColMin() <= other.getColMin()
        && getColMax() >= other.getColMax();
  }

  default TileSubMatrix toLowerLevelSubMatrix() {
    return getLowerLevelSubMatrix(this, 1);
  }

  static TileSubMatrix getLowerLevelSubMatrix(TileSubMatrix subMatrix, int levelDelta) {
    return new ImmutableTileSubMatrix.Builder()
        .level(subMatrix.getLevel() - levelDelta)
        .rowMin(subMatrix.getRowMin() / (2 * levelDelta))
        .rowMax((subMatrix.getRowMax() - 1) / (2 * levelDelta))
        .colMin(subMatrix.getColMin() / (2 * levelDelta))
        .colMax((subMatrix.getColMax() - 1) / (2 * levelDelta))
        .build();
  }
}
