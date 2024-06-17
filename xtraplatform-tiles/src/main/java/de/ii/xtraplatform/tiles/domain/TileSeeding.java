/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public interface TileSeeding {

  String CAPABILITY = "seeding";

  SeedingOptions getOptions();

  Map<String, Map<String, Set<TileMatrixSetLimits>>> getCoverage(
      Map<String, TileGenerationParameters> tilesets) throws IOException;

  Map<String, Map<String, Set<TileMatrixSetLimits>>> getRasterCoverage(
      Map<String, TileGenerationParameters> tilesets) throws IOException;

  void setupSeeding(TileSeedingJobSet jobSet) throws IOException;

  void cleanupSeeding(TileSeedingJobSet jobSet) throws IOException;

  void runSeeding(TileSeedingJob job) throws IOException;

  default void deleteFromCache(
      String tileset, TileMatrixSetBase tileMatrixSet, TileMatrixSetLimits limits) {}
}
