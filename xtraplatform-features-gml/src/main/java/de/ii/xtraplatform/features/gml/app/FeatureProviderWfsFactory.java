/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.gml.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import dagger.assisted.AssistedFactory;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.ConnectorFactory;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableProviderCommonData;
import de.ii.xtraplatform.features.domain.ProviderExtensionRegistry;
import de.ii.xtraplatform.features.gml.domain.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.features.gml.domain.FeatureProviderWfsData;
import de.ii.xtraplatform.features.gml.domain.ImmutableFeatureProviderWfsData;
import de.ii.xtraplatform.features.gml.infra.WfsConnectorHttp;
import de.ii.xtraplatform.features.gml.infra.WfsSchemaCrawler;
import de.ii.xtraplatform.store.domain.entities.AbstractEntityFactory;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityFactory;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.PersistentEntity;
import de.ii.xtraplatform.streams.domain.Reactive;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class FeatureProviderWfsFactory
    extends AbstractEntityFactory<FeatureProviderDataV2, FeatureProviderWfs>
    implements EntityFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderWfsFactory.class);

  private final ConnectorFactory connectorFactory;

  @Inject
  public FeatureProviderWfsFactory(
      // TODO: needed because dagger-auto does not parse FeatureProviderSql
      CrsTransformerFactory crsTransformerFactory,
      Cql cql,
      ConnectorFactory connectorFactory,
      Reactive reactive,
      EntityRegistry entityRegistry,
      ProviderExtensionRegistry extensionRegistry,
      ProviderWfsFactoryAssisted providerWfsFactoryAssisted) {
    super(providerWfsFactoryAssisted);
    this.connectorFactory = connectorFactory;
  }

  @Override
  public String type() {
    return FeatureProviderWfs.ENTITY_TYPE;
  }

  @Override
  public Optional<String> subType() {
    return Optional.of(FeatureProviderWfs.ENTITY_SUB_TYPE);
  }

  @Override
  public Class<? extends PersistentEntity> entityClass() {
    return FeatureProviderWfs.class;
  }

  @Override
  public EntityDataBuilder<FeatureProviderDataV2> dataBuilder() {
    return new ImmutableFeatureProviderWfsData.Builder();
  }

  @Override
  public EntityDataBuilder<? extends EntityData> superDataBuilder() {
    return new ImmutableProviderCommonData.Builder();
  }

  @Override
  public Class<? extends EntityData> dataClass() {
    return FeatureProviderWfsData.class;
  }

  @Override
  public EntityData hydrateData(EntityData entityData) {
    FeatureProviderWfsData data = (FeatureProviderWfsData) entityData;

    if (data.isAuto()) {
      LOGGER.info(
          "Feature provider with id '{}' is in auto mode, generating configuration ...",
          data.getId());
    }

    WfsConnectorHttp connector =
        (WfsConnectorHttp)
            connectorFactory.createConnector(
                data.getProviderSubType(), data.getId(), data.getConnectionInfo());

    try {
      if (!connector.isConnected()) {
        connectorFactory.disposeConnector(connector);

        RuntimeException connectionError =
            connector
                .getConnectionError()
                .map(
                    throwable ->
                        throwable instanceof RuntimeException
                            ? (RuntimeException) throwable
                            : new RuntimeException(throwable))
                .orElse(new IllegalStateException("unknown reason"));

        throw connectionError;
      }

      return cleanupAutoPersist(
          cleanupAdditionalInfo(
              completeConnectionInfoIfNecessary(
                  connector,
                  generateNativeCrsIfNecessary(generateTypesIfNecessary(connector, data)))));

    } catch (Throwable e) {
      LogContext.error(
          LOGGER, e, "Feature provider with id '{}' could not be started", data.getId());
    } finally {
      connectorFactory.disposeConnector(connector);
    }

    throw new IllegalStateException();
  }

  private FeatureProviderWfsData completeConnectionInfoIfNecessary(
      WfsConnectorHttp connector, FeatureProviderWfsData data) {
    ConnectionInfoWfsHttp connectionInfo = (ConnectionInfoWfsHttp) data.getConnectionInfo();

    if (data.isAuto() && connectionInfo.getNamespaces().isEmpty()) {

      WfsSchemaCrawler schemaCrawler = new WfsSchemaCrawler(connector, connectionInfo);

      ConnectionInfoWfsHttp connectionInfoWfsHttp = schemaCrawler.completeConnectionInfo();

      return new ImmutableFeatureProviderWfsData.Builder()
          .from(data)
          .connectionInfo(connectionInfoWfsHttp)
          .build();
    }

    return data;
  }

  private FeatureProviderWfsData generateTypesIfNecessary(
      WfsConnectorHttp connector, FeatureProviderWfsData data) {
    if (data.isAuto() && data.getTypes().isEmpty()) {

      ConnectionInfoWfsHttp connectionInfo = (ConnectionInfoWfsHttp) data.getConnectionInfo();

      WfsSchemaCrawler schemaCrawler = new WfsSchemaCrawler(connector, connectionInfo);

      List<FeatureSchema> types = schemaCrawler.parseSchema();

      ImmutableMap<String, FeatureSchema> typeMap =
          types.stream()
              .map(type -> new AbstractMap.SimpleImmutableEntry<>(type.getName(), type))
              .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

      return new ImmutableFeatureProviderWfsData.Builder().from(data).types(typeMap).build();
    }

    return data;
  }

  private FeatureProviderWfsData generateNativeCrsIfNecessary(FeatureProviderWfsData data) {
    if (data.isAuto() && !data.getNativeCrs().isPresent()) {
      EpsgCrs nativeCrs =
          data.getTypes().values().stream()
              .flatMap(type -> type.getProperties().stream())
              .filter(
                  property ->
                      property.isSpatial() && property.getAdditionalInfo().containsKey("crs"))
              .findFirst()
              .map(property -> EpsgCrs.fromString(property.getAdditionalInfo().get("crs")))
              .orElseGet(() -> OgcCrs.CRS84);

      return new ImmutableFeatureProviderWfsData.Builder().from(data).nativeCrs(nativeCrs).build();
    }

    return data;
  }

  private FeatureProviderWfsData cleanupAdditionalInfo(FeatureProviderWfsData data) {
    if (data.isAuto()) {
      return new ImmutableFeatureProviderWfsData.Builder()
          .from(data)
          .types(
              data.getTypes().entrySet().stream()
                  .map(
                      entry ->
                          new SimpleImmutableEntry<>(
                              entry.getKey(),
                              new ImmutableFeatureSchema.Builder()
                                  .from(entry.getValue())
                                  .additionalInfo(ImmutableMap.of())
                                  .propertyMap(
                                      entry.getValue().getPropertyMap().entrySet().stream()
                                          .map(
                                              entry2 ->
                                                  new SimpleImmutableEntry<>(
                                                      entry2.getKey(),
                                                      new ImmutableFeatureSchema.Builder()
                                                          .from(entry2.getValue())
                                                          .additionalInfo(ImmutableMap.of())
                                                          .build()))
                                          .collect(
                                              ImmutableMap.toImmutableMap(
                                                  Map.Entry::getKey, Map.Entry::getValue)))
                                  .build()))
                  .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)))
          .build();
    }

    return data;
  }

  private FeatureProviderWfsData cleanupAutoPersist(FeatureProviderWfsData data) {
    if (data.isAuto() && data.isAutoPersist()) {
      return new ImmutableFeatureProviderWfsData.Builder()
          .from(data)
          .auto(Optional.empty())
          .autoPersist(Optional.empty())
          .build();
    }

    return data;
  }

  @AssistedFactory
  public interface ProviderWfsFactoryAssisted
      extends FactoryAssisted<FeatureProviderDataV2, FeatureProviderWfs> {
    @Override
    FeatureProviderWfs create(FeatureProviderDataV2 data);
  }
}
