/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import de.ii.xtraplatform.jobs.domain.Job;
import de.ii.xtraplatform.jobs.domain.JobSet;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
public interface TileSeedingJobSet {

  String TYPE = "tile-seeding";
  String TYPE_SETUP = type("setup");
  String LABEL = "Tile cache seeding";

  static String type(String... parts) {
    return String.join(":", TYPE, String.join(":", parts));
  }

  static Job init(String setId) {
    return Job.of(TYPE_SETUP, false, setId);
  }

  static JobSet of(
      String tileProvider, Map<String, TileGenerationParameters> tileSets, boolean reseed) {
    return JobSet.of(
            TYPE,
            tileProvider,
            LABEL,
            String.format(" (Tilesets: %s)", tileSets.keySet()),
            new ImmutableTileSeedingJobSet.Builder()
                .tileProvider(tileProvider)
                .tileSets(tileSets)
                .isReseed(reseed)
                .build())
        .with(Job.of(TYPE_SETUP, false), Job.of(TYPE_SETUP, true));
  }

  String getTileProvider();

  Map<String, TileGenerationParameters> getTileSets();

  boolean isReseed();
}
