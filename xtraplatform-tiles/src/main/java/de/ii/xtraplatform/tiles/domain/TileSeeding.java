/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import de.ii.xtraplatform.services.domain.TaskContext;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;

public interface TileSeeding {

  String CAPABILITY = "seeding";

  SeedingOptions getOptions();

  void seed(
      Map<String, TileGenerationParameters> tilesets,
      List<MediaType> mediaTypes,
      boolean reseed,
      TaskContext taskContext)
      throws IOException;

  default void deleteFromCache(
      String tileset, TileMatrixSetBase tileMatrixSet, TileMatrixSetLimits limits) {}
}
