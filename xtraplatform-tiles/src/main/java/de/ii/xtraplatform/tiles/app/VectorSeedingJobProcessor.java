/*
 * Copyright 2024 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.xtraplatform.entities.domain.EntityRegistry;
import de.ii.xtraplatform.jobs.domain.Job;
import de.ii.xtraplatform.jobs.domain.JobProcessor;
import de.ii.xtraplatform.jobs.domain.JobSet;
import de.ii.xtraplatform.tiles.domain.TileProvider;
import de.ii.xtraplatform.tiles.domain.TileSeedingJob;
import de.ii.xtraplatform.tiles.domain.TileSeedingJobSet;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class VectorSeedingJobProcessor implements JobProcessor<TileSeedingJob, TileSeedingJobSet> {

  private static final Logger LOGGER = LoggerFactory.getLogger(VectorSeedingJobProcessor.class);

  private final EntityRegistry entityRegistry;

  @Inject
  VectorSeedingJobProcessor(EntityRegistry entityRegistry) {
    this.entityRegistry = entityRegistry;
  }

  @Override
  public String getJobType() {
    return TileSeedingJob.TYPE_MVT;
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
    TileSeedingJob seedingJob = getDetails(job);

    getTileProvider(seedingJob.getTileProvider())
        .ifPresent(
            tileProvider -> {
              if (!tileProvider.seeding().isSupported()) {
                LOGGER.error("Tile provider does not support seeding: {}", tileProvider.getId());
                return; // early return
              }
              if (!tileProvider.seeding().isAvailable()) {
                return; // early return
              }

              try {
                tileProvider.seeding().get().runSeeding(seedingJob);
              } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
              }
            });
  }

  @Override
  public Class<TileSeedingJob> getDetailsType() {
    return TileSeedingJob.class;
  }

  @Override
  public Class<TileSeedingJobSet> getSetDetailsType() {
    return TileSeedingJobSet.class;
  }

  private Optional<TileProvider> getTileProvider(String id) {
    return entityRegistry.getEntity(TileProvider.class, id);
  }
}
