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
import de.ii.xtraplatform.base.domain.AppContext;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.features.domain.ImmutableProviderCommonData;
import de.ii.xtraplatform.store.domain.BlobStore;
import de.ii.xtraplatform.store.domain.entities.AbstractEntityFactory;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityFactory;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.PersistentEntity;
import de.ii.xtraplatform.tiles.domain.ImmutableTileProviderFeaturesData;
import de.ii.xtraplatform.tiles.domain.ImmutableTilesetFeaturesDefaults;
import de.ii.xtraplatform.tiles.domain.TileProviderData;
import de.ii.xtraplatform.tiles.domain.TileProviderFeaturesData;
import de.ii.xtraplatform.tiles.domain.TileWalker;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class TileProviderFeaturesFactory
    extends AbstractEntityFactory<TileProviderFeaturesData, TileProviderFeatures>
    implements EntityFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(TileProviderFeaturesFactory.class);

  @Inject
  public TileProviderFeaturesFactory(
      // TODO: needed because dagger-auto does not parse TileProviderFeatures
      CrsInfo crsInfo,
      EntityRegistry entityRegistry,
      AppContext appContext,
      Cql cql,
      BlobStore blobStore,
      TileWalker tileWalker,
      TileProviderFeaturesFactoryAssisted factoryAssisted) {
    super(factoryAssisted);
  }

  @Override
  public String type() {
    return TileProviderData.ENTITY_TYPE;
  }

  @Override
  public Optional<String> subType() {
    return Optional.of(TileProviderFeaturesData.ENTITY_SUBTYPE);
  }

  @Override
  public Class<? extends PersistentEntity> entityClass() {
    return TileProviderFeatures.class;
  }

  @Override
  public EntityDataBuilder<TileProviderData> dataBuilder() {
    return new ImmutableTileProviderFeaturesData.Builder()
        .tilesetDefaultsBuilder(new ImmutableTilesetFeaturesDefaults.Builder());
  }

  @Override
  public EntityDataBuilder<? extends EntityData> superDataBuilder() {
    return new ImmutableProviderCommonData.Builder();
  }

  @Override
  public Class<? extends EntityData> dataClass() {
    return TileProviderFeaturesData.class;
  }

  @Override
  public EntityData hydrateData(EntityData entityData) {
    TileProviderFeaturesData data = (TileProviderFeaturesData) entityData;

    // TODO: auto mode

    return data;
  }

  @AssistedFactory
  public interface TileProviderFeaturesFactoryAssisted
      extends FactoryAssisted<TileProviderFeaturesData, TileProviderFeatures> {
    @Override
    TileProviderFeatures create(TileProviderFeaturesData data);
  }
}
