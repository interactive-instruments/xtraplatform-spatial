/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.graphql.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import dagger.assisted.AssistedFactory;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.features.domain.ConnectorFactory;
import de.ii.xtraplatform.features.domain.ExternalTypesResolver;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableProviderCommonData;
import de.ii.xtraplatform.features.domain.ProviderExtensionRegistry;
import de.ii.xtraplatform.features.graphql.domain.FeatureProviderGraphQlData;
import de.ii.xtraplatform.features.graphql.domain.ImmutableFeatureProviderGraphQlData;
import de.ii.xtraplatform.store.domain.BlobStore;
import de.ii.xtraplatform.store.domain.entities.AbstractEntityFactory;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityFactory;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.PersistentEntity;
import de.ii.xtraplatform.streams.domain.Reactive;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class FeatureProviderGraphQlFactory
    extends AbstractEntityFactory<FeatureProviderDataV2, FeatureProviderGraphQl>
    implements EntityFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderGraphQlFactory.class);

  private final BlobStore blobStore;
  private final ConnectorFactory connectorFactory;

  @Inject
  public FeatureProviderGraphQlFactory(
      BlobStore blobStore,
      // TODO: needed because dagger-auto does not parse FeatureProviderSql
      CrsTransformerFactory crsTransformerFactory,
      Cql cql,
      ConnectorFactory connectorFactory,
      Reactive reactive,
      EntityRegistry entityRegistry,
      ProviderExtensionRegistry extensionRegistry,
      ProviderGraphQlFactoryAssisted providerGraphQlFactoryAssisted) {
    super(providerGraphQlFactoryAssisted);
    this.blobStore = blobStore;
    this.connectorFactory = connectorFactory;
  }

  @Override
  public String type() {
    return FeatureProviderGraphQl.ENTITY_TYPE;
  }

  @Override
  public Optional<String> subType() {
    return Optional.of(FeatureProviderGraphQl.ENTITY_SUB_TYPE);
  }

  @Override
  public Class<? extends PersistentEntity> entityClass() {
    return FeatureProviderGraphQl.class;
  }

  @Override
  public EntityDataBuilder<FeatureProviderDataV2> dataBuilder() {
    return new ImmutableFeatureProviderGraphQlData.Builder();
  }

  @Override
  public EntityDataBuilder<? extends EntityData> superDataBuilder() {
    return new ImmutableProviderCommonData.Builder();
  }

  @Override
  public Class<? extends EntityData> dataClass() {
    return FeatureProviderGraphQlData.class;
  }

  @Override
  public EntityData hydrateData(EntityData entityData) {
    FeatureProviderGraphQlData data = (FeatureProviderGraphQlData) entityData;

    // TODO: also for sql
    ExternalTypesResolver resolver = new ExternalTypesResolver(blobStore.with("schemas"));

    if (resolver.needsResolving(data.getTypes())) {
      Map<String, FeatureSchema> types = resolver.resolve(data.getTypes());

      return new ImmutableFeatureProviderGraphQlData.Builder().from(data).types(types).build();
    }

    return data;
  }

  @AssistedFactory
  public interface ProviderGraphQlFactoryAssisted
      extends FactoryAssisted<FeatureProviderDataV2, FeatureProviderGraphQl> {
    @Override
    FeatureProviderGraphQl create(FeatureProviderDataV2 data);
  }
}
