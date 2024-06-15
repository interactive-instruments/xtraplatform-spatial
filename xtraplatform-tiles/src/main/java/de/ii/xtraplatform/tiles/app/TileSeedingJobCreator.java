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
import de.ii.xtraplatform.tiles.domain.TileMatrixSetLimits;
import de.ii.xtraplatform.tiles.domain.TileProvider;
import de.ii.xtraplatform.tiles.domain.TileSeedingJob;
import de.ii.xtraplatform.tiles.domain.TileSeedingJobSet;
import de.ii.xtraplatform.tiles.domain.TileSubMatrix;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
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
  private final TileStorePartitions tileStorePartitions;

  @Inject
  TileSeedingJobCreator(EntityRegistry entityRegistry) {
    this.entityRegistry = entityRegistry;
    this.tileStorePartitions = new TileStorePartitions(3);
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

                tileProvider.seeding().get().setupSeeding(seedingJobSet);

                coverage.forEach(
                    (tileSet, tileMatrixSets) -> {
                      tileMatrixSets.forEach(
                          (tileMatrixSet, limits) -> {
                            Set<TileSubMatrix> subMatrices = new LinkedHashSet<>();

                            limits.forEach(
                                (limit) -> {
                                  subMatrices.addAll(tileStorePartitions.getSubMatrices(limit));
                                });

                            for (TileSubMatrix subMatrix : subMatrices) {
                              Job job2 =
                                  TileSeedingJob.of(
                                      tileProvider.getId(),
                                      tileSet,
                                      tileMatrixSet,
                                      seedingJobSet.isReseed(),
                                      Set.of(subMatrix),
                                      jobSet.getId());

                              pushJob.accept(job2);
                              jobSet.getTotal().incrementAndGet();
                            }
                          });
                    });

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
