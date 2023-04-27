/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import dagger.assisted.AssistedFactory;
import de.ii.xtraplatform.features.domain.ImmutableProviderCommonData;
import de.ii.xtraplatform.store.domain.entities.AbstractEntityFactory;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityFactory;
import de.ii.xtraplatform.store.domain.entities.PersistentEntity;
import de.ii.xtraplatform.tiles.domain.ImmutableTileProviderHttpData;
import de.ii.xtraplatform.tiles.domain.TileProviderData;
import de.ii.xtraplatform.tiles.domain.TileProviderHttpData;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class TileProviderHttpFactory
    extends AbstractEntityFactory<TileProviderHttpData, TileProviderHttp> implements EntityFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileProviderHttpFactory.class);

  @Inject
  public TileProviderHttpFactory(TileProviderHttpFactoryAssisted factoryAssisted) {
    super(factoryAssisted);
  }

  @Override
  public String type() {
    return TileProviderData.ENTITY_TYPE;
  }

  @Override
  public Optional<String> subType() {
    return Optional.of(TileProviderHttpData.ENTITY_SUBTYPE);
  }

  @Override
  public Class<? extends PersistentEntity> entityClass() {
    return TileProviderHttp.class;
  }

  @Override
  public EntityDataBuilder<TileProviderData> dataBuilder() {
    return new ImmutableTileProviderHttpData.Builder();
  }

  @Override
  public EntityDataBuilder<? extends EntityData> superDataBuilder() {
    return new ImmutableProviderCommonData.Builder();
  }

  @Override
  public Class<? extends EntityData> dataClass() {
    return TileProviderHttpData.class;
  }

  @Override
  public EntityData hydrateData(EntityData entityData) {
    TileProviderHttpData data = (TileProviderHttpData) entityData;

    return data;
  }

  @AssistedFactory
  public interface TileProviderHttpFactoryAssisted
      extends FactoryAssisted<TileProviderHttpData, TileProviderHttp> {
    @Override
    TileProviderHttp create(TileProviderHttpData data);
  }
}
