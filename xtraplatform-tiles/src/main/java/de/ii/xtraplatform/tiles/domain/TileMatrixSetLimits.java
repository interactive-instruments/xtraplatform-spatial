/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import org.immutables.value.Value;

@Value.Immutable
public interface TileMatrixSetLimits {

  String getTileMatrix();

  Integer getMinTileRow();

  Integer getMaxTileRow();

  Integer getMinTileCol();

  Integer getMaxTileCol();

  default boolean contains(int row, int col) {
    return getMaxTileCol() >= col
        && getMinTileCol() <= col
        && getMaxTileRow() >= row
        && getMinTileRow() <= row;
  }

  @Value.Derived
  @Value.Auxiliary
  default long getNumberOfTiles() {
    return ((long) getMaxTileRow() - getMinTileRow() + 1) * (getMaxTileCol() - getMinTileCol() + 1);
  }

  default long getNumberOfTiles(IntPredicate whereColMatches) {
    long numCols =
        IntStream.rangeClosed(getMinTileCol(), getMaxTileCol()).filter(whereColMatches).count();

    return (getMaxTileRow() - getMinTileRow() + 1) * numCols;
  }
}
