/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.app;

import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry;
import de.ii.xtraplatform.entities.domain.AbstractPersistentEntity;
import de.ii.xtraplatform.services.domain.AbstractService;
import de.ii.xtraplatform.tiles.domain.TileProvider;
import de.ii.xtraplatform.tiles.domain.TileProviderData;
import de.ii.xtraplatform.tiles.domain.TilesetMetadata;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTileProvider<T extends TileProviderData>
    extends AbstractPersistentEntity<T> implements TileProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractService.class);

  public AbstractTileProvider(VolatileRegistry volatileRegistry, T data, String... capabilities) {
    super(data, volatileRegistry, capabilities);
  }

  @Override
  protected boolean onStartup() throws InterruptedException {
    onVolatileStart();

    return super.onStartup();
  }

  @Override
  protected void onStarted() {
    onVolatileStarted();

    LOGGER.info("Tile provider with id '{}' started successfully.", getId());
  }

  @Override
  protected void onReloaded() {
    LOGGER.info("Tile provider with id '{}' reloaded successfully.", getId());
  }

  @Override
  protected void onStopped() {
    LOGGER.info("Tile provider with id '{}' stopped.", getId());
  }

  @Override
  protected void onStartupFailure(Throwable throwable) {
    LogContext.error(LOGGER, throwable, "Tile provider with id '{}' could not be started", getId());
  }

  @Override
  public Optional<TilesetMetadata> metadata(String tileset) {
    return Optional.empty();
  }
}
