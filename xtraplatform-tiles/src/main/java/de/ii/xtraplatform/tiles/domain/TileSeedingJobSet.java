/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.ii.xtraplatform.jobs.domain.Job;
import de.ii.xtraplatform.jobs.domain.JobProgress;
import de.ii.xtraplatform.jobs.domain.JobSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.Collectors;
import org.immutables.value.Value;

@Value.Immutable
public interface TileSeedingJobSet {

  String TYPE = "tile-seeding";
  String TYPE_SETUP = type("setup");
  String LABEL = "Tile cache seeding";

  static String type(String... parts) {
    return String.join(":", TYPE, String.join(":", parts));
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
                .tileSets(TilesetDetails.of(tileSets))
                .isReseed(reseed)
                .build())
        .with(Job.of(TYPE_SETUP, false), Job.of(TYPE_SETUP, true));
  }

  static JobSet with(JobSet jobSet, Map<String, TileGenerationParameters> tileSets) {
    TileSeedingJobSet details =
        new ImmutableTileSeedingJobSet.Builder()
            .from((TileSeedingJobSet) jobSet.getDetails())
            .putAllTileSets(TilesetDetails.of(tileSets))
            .build();
    return jobSet.with(String.format(" (Tilesets: %s)", details.getTileSets().keySet()), details);
  }

  String getTileProvider();

  Map<String, TilesetDetails> getTileSets();

  @JsonIgnore
  @Value.Lazy
  default Map<String, TileGenerationParameters> getTileSetParameters() {
    return getTileSets().entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getParameters()));
  }

  boolean isReseed();

  default void withLevel(String tileSet, String tileMatrixSet, int level, int count) {
    LinkedHashMap<String, AtomicIntegerArray> levels =
        getTileSets().get(tileSet).getProgress().getLevels();

    if (!levels.containsKey(tileMatrixSet)) {
      int[] levelProgress = new int[24];
      Arrays.fill(levelProgress, -1);
      levels.put(tileMatrixSet, new AtomicIntegerArray(levelProgress));
    }

    levels.get(tileMatrixSet).getAndUpdate(level, val -> val == -1 ? count : val + count);
  }

  default void withLevelSub(String tileSet, String tileMatrixSet, int level, int count) {
    LinkedHashMap<String, AtomicIntegerArray> levels =
        getTileSets().get(tileSet).getProgress().getLevels();

    if (levels.containsKey(tileMatrixSet)) {
      levels.get(tileMatrixSet).addAndGet(level, -1 * count);
    }
  }

  @Value.Immutable
  interface TilesetDetails {

    static Map<String, TilesetDetails> of(Map<String, TileGenerationParameters> tilesets) {
      return tilesets.entrySet().stream()
          .collect(Collectors.toMap(Map.Entry::getKey, e -> TilesetDetails.of(e.getValue())));
    }

    static TilesetDetails of(TileGenerationParameters parameters) {
      return new ImmutableTilesetDetails.Builder()
          .parameters(parameters)
          .progress(new ImmutableTilesetProgress.Builder().levels(new LinkedHashMap<>()).build())
          .build();
    }

    TileGenerationParameters getParameters();

    TilesetProgress getProgress();
  }

  @Value.Immutable
  interface TilesetProgress extends JobProgress {
    @JsonIgnore
    LinkedHashMap<String, AtomicIntegerArray> getLevels();

    @JsonProperty("levels")
    default Map<String, List<Integer>> getLevelsArray() {

      return getLevels().entrySet().stream()
          .map(
              e -> {
                List<Integer> levels = new ArrayList<>();
                for (int i = 0; i < e.getValue().length(); i++) {
                  levels.add(e.getValue().get(i));
                }
                return Map.entry(e.getKey(), levels);
              })
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
  }
}
