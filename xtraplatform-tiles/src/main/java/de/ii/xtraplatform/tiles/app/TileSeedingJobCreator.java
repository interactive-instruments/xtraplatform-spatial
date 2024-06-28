/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.base.domain.LogContext.MARKER;
import de.ii.xtraplatform.entities.domain.EntityRegistry;
import de.ii.xtraplatform.jobs.domain.Job;
import de.ii.xtraplatform.jobs.domain.JobProcessor;
import de.ii.xtraplatform.jobs.domain.JobSet;
import de.ii.xtraplatform.tiles.domain.Cache.Storage;
import de.ii.xtraplatform.tiles.domain.TileGenerationParameters;
import de.ii.xtraplatform.tiles.domain.TileMatrixSetLimits;
import de.ii.xtraplatform.tiles.domain.TileProvider;
import de.ii.xtraplatform.tiles.domain.TileProviderFeaturesData;
import de.ii.xtraplatform.tiles.domain.TileSeedingJob;
import de.ii.xtraplatform.tiles.domain.TileSeedingJobSet;
import de.ii.xtraplatform.tiles.domain.TileSubMatrix;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.AmountFormats;

@Singleton
@AutoBind
public class TileSeedingJobCreator implements JobProcessor<Boolean, TileSeedingJobSet> {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileSeedingJobCreator.class);

  private final EntityRegistry entityRegistry;

  @Inject
  TileSeedingJobCreator(EntityRegistry entityRegistry) {
    this.entityRegistry = entityRegistry;
  }

  @Override
  public String getJobType() {
    return TileSeedingJobSet.TYPE_SETUP;
  }

  @Override
  public int getConcurrency(JobSet jobSet) {
    TileSeedingJobSet seedingJobSet = getSetDetails(jobSet);

    Optional<TileProvider> tileProvider = getTileProvider(seedingJobSet.getTileProvider());

    if (tileProvider.isPresent() && tileProvider.get().seeding().isSupported()) {
      return tileProvider.get().seeding().get().getOptions().getEffectiveMaxThreads();
    }

    return 1;
  }

  @Override
  public void process(Job job, JobSet jobSet, Consumer<Job> pushJob) {

    TileSeedingJobSet seedingJobSet = getSetDetails(jobSet);
    boolean isCleanup = getDetails(job);

    getTileProvider(seedingJobSet.getTileProvider())
        .ifPresent(
            tileProvider -> {
              if (!tileProvider.seeding().isSupported()) {
                LOGGER.error("Tile provider does not support seeding: {}", tileProvider.getId());
                return; // early return
              }

              try {

                if (isCleanup) {
                  tileProvider.seeding().get().cleanupSeeding(seedingJobSet);

                  long duration = Instant.now().toEpochMilli() - jobSet.getStartedAt().get();

                  if (LOGGER.isInfoEnabled() || LOGGER.isInfoEnabled(MARKER.JOBS)) {
                    LOGGER.info(
                        MARKER.JOBS,
                        "{} finished in {}{}",
                        jobSet.getLabel(),
                        pretty(duration),
                        jobSet.getDescription().orElse(""));
                  }

                  return; // early return
                }

                if (LOGGER.isInfoEnabled() || LOGGER.isInfoEnabled(MARKER.JOBS)) {
                  LOGGER.info(
                      MARKER.JOBS,
                      "{} started (Tilesets: {})",
                      jobSet.getLabel(),
                      seedingJobSet.getTileSets().keySet());
                }

                jobSet.getStartedAt().set(Instant.now().toEpochMilli());
                Map<String, Map<String, Set<TileMatrixSetLimits>>> coverage =
                    tileProvider.seeding().get().getCoverage(seedingJobSet.getTileSets());
                Map<String, Map<String, Set<TileMatrixSetLimits>>> rasterCoverage =
                    tileProvider.seeding().get().getRasterCoverage(seedingJobSet.getTileSets());
                TileStorePartitions tileStorePartitions =
                    new TileStorePartitions(
                        tileProvider.seeding().get().getOptions().getEffectiveJobSize());
                boolean perJob =
                    ((TileProviderFeaturesData) tileProvider.getData())
                        .getCaches().stream()
                            .anyMatch(
                                cache ->
                                    cache.getSeeded() && cache.getStorage() == Storage.PER_JOB);

                Map<String, List<String>> rasterForVector =
                    seedingJobSet.getTileSets().entrySet().stream()
                        .map(
                            entry ->
                                Map.entry(
                                    entry.getKey(),
                                    tileProvider
                                        .access()
                                        .get()
                                        .getMapStyles(entry.getKey())
                                        .stream()
                                        .map(
                                            style ->
                                                tileProvider
                                                    .access()
                                                    .get()
                                                    .getMapStyleTileset(entry.getKey(), style))
                                        .collect(Collectors.toList())))
                        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

                Map<String, TileGenerationParameters> rasterForVectorTilesets =
                    seedingJobSet.getTileSets().entrySet().stream()
                        .flatMap(
                            entry ->
                                rasterForVector.get(entry.getKey()).stream()
                                    .map(
                                        rasterTileset ->
                                            Map.entry(rasterTileset, entry.getValue())))
                        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
                Map<String, Map<String, Set<TileMatrixSetLimits>>> rasterForVectorCoverage =
                    tileProvider.seeding().get().getRasterCoverage(rasterForVectorTilesets);

                tileProvider.seeding().get().setupSeeding(seedingJobSet);

                seedingJobSet
                    .getTileSets()
                    .keySet()
                    .forEach(
                        (tileSet) -> {
                          Map<String, Set<TileMatrixSetLimits>> tileMatrixSets =
                              coverage.containsKey(tileSet)
                                  ? coverage.get(tileSet)
                                  : rasterCoverage.containsKey(tileSet)
                                      ? rasterCoverage.get(tileSet)
                                      : Map.of();
                          boolean isRaster = rasterCoverage.containsKey(tileSet);

                          if (isRaster && perJob) {
                            // ignore top level raster tilesets, they are handled as follow-ups by
                            // the vector tilesets
                            return;
                          }

                          tileMatrixSets.forEach(
                              (tileMatrixSet, limits) -> {
                                Set<TileSubMatrix> subMatrices = new LinkedHashSet<>();

                                limits.forEach(
                                    (limit) -> {
                                      subMatrices.addAll(tileStorePartitions.getSubMatrices(limit));
                                    });

                                for (TileSubMatrix subMatrix : subMatrices) {
                                  Job job2 =
                                      isRaster
                                          ? TileSeedingJob.raster(
                                              tileProvider.getId(),
                                              tileSet,
                                              tileMatrixSet,
                                              seedingJobSet.isReseed(),
                                              Set.of(subMatrix),
                                              jobSet.getId(),
                                              tileSet)
                                          : TileSeedingJob.of(
                                              tileProvider.getId(),
                                              tileSet,
                                              tileMatrixSet,
                                              seedingJobSet.isReseed(),
                                              Set.of(subMatrix),
                                              jobSet.getId());

                                  if (perJob) {
                                    List<Job> followUps =
                                        rasterForVector.get(tileSet).stream()
                                            .flatMap(
                                                rasterTileset -> {
                                                  if (!rasterForVectorCoverage.containsKey(
                                                          rasterTileset)
                                                      || !rasterForVectorCoverage
                                                          .get(rasterTileset)
                                                          .containsKey(tileMatrixSet)) {
                                                    return Stream.empty();
                                                  }

                                                  Set<TileMatrixSetLimits> rasterLimits =
                                                      rasterForVectorCoverage
                                                          .get(rasterTileset)
                                                          .get(tileMatrixSet)
                                                          .stream()
                                                          .filter(
                                                              limit ->
                                                                  Integer.parseInt(
                                                                          limit.getTileMatrix())
                                                                      == subMatrix.getLevel() + 1)
                                                          .collect(Collectors.toSet());
                                                  Set<TileSubMatrix> rasterSubMatrices =
                                                      new LinkedHashSet<>();

                                                  rasterLimits.forEach(
                                                      (limit) -> {
                                                        rasterSubMatrices.addAll(
                                                            tileStorePartitions.getSubMatrices(
                                                                limit));
                                                      });

                                                  String storageHint =
                                                      rasterSubMatrices.stream()
                                                          .map(
                                                              tileStorePartitions::getPartitionName)
                                                          .collect(Collectors.joining(","));

                                                  return Stream.of(
                                                      TileSeedingJob.raster(
                                                          tileProvider.getId(),
                                                          rasterTileset,
                                                          tileMatrixSet,
                                                          seedingJobSet.isReseed(),
                                                          rasterSubMatrices,
                                                          jobSet.getId(),
                                                          storageHint));
                                                })
                                            .collect(Collectors.toList());

                                    if (!followUps.isEmpty()) {
                                      job2 = job2.with(followUps);
                                      jobSet.getTotal().addAndGet(followUps.size());
                                    }
                                  }

                                  pushJob.accept(job2);
                                  jobSet.getTotal().incrementAndGet();
                                }
                              });
                        });

                if (jobSet.isDone()) {
                  jobSet.getCleanup().ifPresent(pushJob);
                  return; // early return
                }

                if (LOGGER.isDebugEnabled() || LOGGER.isDebugEnabled(MARKER.JOBS)) {
                  LOGGER.debug(
                      MARKER.JOBS,
                      "{}: processing {} jobs with {} local processors",
                      jobSet.getLabel(),
                      jobSet.getTotal().get(),
                      getConcurrency(jobSet));
                }
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
  }

  @Override
  public Class<Boolean> getDetailsType() {
    return Boolean.class;
  }

  @Override
  public Class<TileSeedingJobSet> getSetDetailsType() {
    return TileSeedingJobSet.class;
  }

  private Optional<TileProvider> getTileProvider(String id) {
    return entityRegistry.getEntity(TileProvider.class, id);
  }

  private static String pretty(long milliseconds) {
    Duration d = Duration.ofSeconds(milliseconds / 1000);
    return AmountFormats.wordBased(d, Locale.ENGLISH);
  }
}
