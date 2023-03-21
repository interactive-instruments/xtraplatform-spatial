/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dagger.assisted.AssistedFactory;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.ConnectorFactory;
import de.ii.xtraplatform.features.domain.DecoderFactories;
import de.ii.xtraplatform.features.domain.ExternalTypesResolver;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableProviderCommonData;
import de.ii.xtraplatform.features.domain.ProviderExtensionRegistry;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql.Dialect;
import de.ii.xtraplatform.features.sql.domain.FeatureProviderSqlData;
import de.ii.xtraplatform.features.sql.domain.ImmutableConnectionInfoSql;
import de.ii.xtraplatform.features.sql.domain.ImmutableFeatureProviderSqlData;
import de.ii.xtraplatform.features.sql.domain.ImmutableFeatureProviderSqlData.Builder;
import de.ii.xtraplatform.features.sql.domain.ImmutablePoolSettings;
import de.ii.xtraplatform.features.sql.domain.SqlConnector;
import de.ii.xtraplatform.features.sql.domain.SqlDialectGpkg;
import de.ii.xtraplatform.features.sql.domain.SqlDialectPostGis;
import de.ii.xtraplatform.features.sql.infra.db.SchemaGeneratorSql;
import de.ii.xtraplatform.store.domain.BlobStore;
import de.ii.xtraplatform.store.domain.entities.AbstractEntityFactory;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityFactory;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.PersistentEntity;
import de.ii.xtraplatform.streams.domain.Reactive;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class FeatureProviderSqlFactory
    extends AbstractEntityFactory<FeatureProviderDataV2, FeatureProviderSql>
    implements EntityFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderSqlFactory.class);

  private final BlobStore blobStore;
  private final ConnectorFactory connectorFactory;

  @Inject
  public FeatureProviderSqlFactory(
      BlobStore blobStore,
      // TODO: needed because dagger-auto does not parse FeatureProviderSql
      CrsTransformerFactory crsTransformerFactory,
      CrsInfo crsInfo,
      Cql cql,
      ConnectorFactory connectorFactory,
      Reactive reactive,
      EntityRegistry entityRegistry,
      ProviderExtensionRegistry extensionRegistry,
      DecoderFactories decoderFactories,
      ProviderSqlFactoryAssisted providerSqlFactoryAssisted) {
    super(providerSqlFactoryAssisted);
    this.blobStore = blobStore;
    this.connectorFactory = connectorFactory;
  }

  @Override
  public String type() {
    return FeatureProviderSql.ENTITY_TYPE;
  }

  @Override
  public Optional<String> subType() {
    return Optional.of(FeatureProviderSql.ENTITY_SUB_TYPE);
  }

  @Override
  public Class<? extends PersistentEntity> entityClass() {
    return FeatureProviderSql.class;
  }

  @Override
  public EntityDataBuilder<FeatureProviderDataV2> dataBuilder() {
    return new ImmutableFeatureProviderSqlData.Builder();
  }

  @Override
  public EntityDataBuilder<? extends EntityData> superDataBuilder() {
    return new ImmutableProviderCommonData.Builder();
  }

  @Override
  public Class<? extends EntityData> dataClass() {
    return FeatureProviderSqlData.class;
  }

  @Override
  public EntityData hydrateData(EntityData entityData) {
    FeatureProviderSqlData data = (FeatureProviderSqlData) entityData;

    if (data.isAuto()) {
      LOGGER.info(
          "Feature provider with id '{}' is in auto mode, generating configuration ...",
          data.getId());
    }

    try {
      return resolveSchemasIfNecessary(
          normalizeConstants(
              cleanupAutoPersist(
                  cleanupAdditionalInfo(
                      generateNativeCrsIfNecessary(generateTypesIfNecessary(data))))));
    } catch (Throwable e) {
      LogContext.error(
          LOGGER, e, "Feature provider with id '{}' could not be started", data.getId());
    }

    throw new IllegalStateException();
  }

  private FeatureProviderSqlData resolveSchemasIfNecessary(FeatureProviderSqlData data) {
    ExternalTypesResolver resolver = new ExternalTypesResolver(blobStore.with("schemas"));

    if (resolver.needsResolving(data.getTypes())) {
      Map<String, FeatureSchema> types = resolver.resolve(data.getTypes());

      return new Builder().from(data).types(types).build();
    }
    return data;
  }

  private FeatureProviderSqlData generateTypesIfNecessary(FeatureProviderSqlData data) {
    if (data.isAuto() && data.getTypes().isEmpty()) {
      SqlConnector connector =
          (SqlConnector)
              connectorFactory.createConnector(
                  data.getProviderSubType(),
                  data.getId(),
                  getConnectionInfoWith4Connections(data.getConnectionInfo()));

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

      SchemaGeneratorSql schemaGeneratorSql = null;

      try {
        ConnectionInfoSql connectionInfo = data.getConnectionInfo();

        List<String> schemas =
            connectionInfo.getSchemas().isEmpty()
                ? connectionInfo.getDialect() == Dialect.GPKG
                    ? ImmutableList.of()
                    : ImmutableList.of("public")
                : connectionInfo.getSchemas();

        schemaGeneratorSql =
            new SchemaGeneratorSql(
                connector.getSqlClient(),
                schemas,
                data.getAutoTypes(),
                connectionInfo.getDialect() == Dialect.GPKG
                    ? new SqlDialectGpkg()
                    : new SqlDialectPostGis());

        List<FeatureSchema> types = schemaGeneratorSql.generate();

        Map<String, Integer> idCounter = new LinkedHashMap<>();
        types.forEach(
            featureSchema ->
                featureSchema.getProperties().stream()
                    .filter(FeatureSchema::isId)
                    .map(FeatureSchema::getSourcePath)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst()
                    .ifPresent(
                        path -> {
                          if (!idCounter.containsKey(path)) {
                            idCounter.put(path, 0);
                          }
                          idCounter.put(path, idCounter.get(path) + 1);
                        }));

        String mostOftenUsedId =
            idCounter.entrySet().stream()
                .max(Comparator.comparingInt(Entry::getValue))
                .map(Entry::getKey)
                .orElse("id");

        ImmutableMap<String, FeatureSchema> typeMap =
            types.stream()
                .map(
                    type -> {
                      Optional<String> differingSortKey =
                          type.getIdProperty()
                              .filter(
                                  idProperty ->
                                      !Objects.equals(idProperty.getName(), mostOftenUsedId))
                              .map(FeatureSchema::getName);

                      if (differingSortKey.isPresent() && type.getSourcePath().isPresent()) {
                        return new ImmutableFeatureSchema.Builder()
                            .from(type)
                            .sourcePath(
                                String.format(
                                    "%s{sortKey=%s}",
                                    type.getSourcePath().get(), differingSortKey.get()))
                            .build();
                      }

                      return type;
                    })
                .map(type -> new AbstractMap.SimpleImmutableEntry<>(type.getName(), type))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        Builder builder = new Builder().from(data).types(typeMap);

        if (!Objects.equals(mostOftenUsedId, "id")) {
          // LOGGER.debug("CHANGING defaultSortKey to {}", mostOftenUsedId);
          builder.sourcePathDefaultsBuilder().primaryKey(mostOftenUsedId).sortKey(mostOftenUsedId);
        }

        return builder.build();
      } finally {
        if (Objects.nonNull(schemaGeneratorSql)) {
          try {
            schemaGeneratorSql.close();
          } catch (Throwable e) {
            // ignore
          }
          try {
            connectorFactory.disposeConnector(connector);
          } catch (Throwable e) {
            // ignore
          }
        }
      }
    }

    return data;
  }

  private FeatureProviderSqlData generateNativeCrsIfNecessary(FeatureProviderSqlData data) {
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

      return new Builder().from(data).nativeCrs(nativeCrs).build();
    }

    return data;
  }

  private FeatureProviderSqlData cleanupAdditionalInfo(FeatureProviderSqlData data) {
    if (data.isAuto()) {
      return new Builder()
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

  private FeatureProviderSqlData cleanupAutoPersist(FeatureProviderSqlData data) {
    if (data.isAuto() && data.isAutoPersist()) {
      return new Builder()
          .from(data)
          .auto(Optional.empty())
          .autoPersist(Optional.empty())
          .autoTypes(new ArrayList<>())
          .build();
    }

    return data;
  }

  private FeatureProviderSqlData normalizeConstants(FeatureProviderSqlData data) {
    boolean hasConstants =
        data.getTypes().values().stream()
            .flatMap(t -> t.getAllNestedProperties().stream())
            .anyMatch(p -> p.isConstant() && p.getSourcePaths().isEmpty());

    if (hasConstants) {
      Map<String, FeatureSchema> types =
          data.getTypes().entrySet().stream()
              .map(
                  entry ->
                      new SimpleImmutableEntry<>(
                          entry.getKey(), entry.getValue().accept(new NormalizeConstants())))
              .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

      return new Builder().from(data).types(types).build();
    }

    return data;
  }

  protected ConnectionInfoSql getConnectionInfoWith4Connections(ConnectionInfoSql connectionInfo) {

    if (connectionInfo.getPool().getMaxConnections() <= 0) {
      return new ImmutableConnectionInfoSql.Builder()
          .from(connectionInfo)
          .pool(
              new ImmutablePoolSettings.Builder()
                  .from(connectionInfo.getPool())
                  .maxConnections(4)
                  .build())
          .build();
    }

    return connectionInfo;
  }

  @AssistedFactory
  public interface ProviderSqlFactoryAssisted
      extends FactoryAssisted<FeatureProviderDataV2, FeatureProviderSql> {
    @Override
    FeatureProviderSql create(FeatureProviderDataV2 data);
  }

  public class NormalizeConstants implements SchemaVisitorTopDown<FeatureSchema, FeatureSchema> {
    final int[] constantCounter = {0};

    @Override
    public FeatureSchema visit(
        FeatureSchema schema, List<FeatureSchema> parents, List<FeatureSchema> visitedProperties) {
      Map<String, FeatureSchema> visitedPropertiesMap =
          visitedProperties.stream()
              .filter(Objects::nonNull)
              .map(
                  featureSchema ->
                      new SimpleImmutableEntry<>(
                          featureSchema.getName(),
                          normalizeConstants(schema.getName(), featureSchema)))
              .collect(
                  ImmutableMap.toImmutableMap(
                      Entry::getKey, Entry::getValue, (first, second) -> second));

      return new ImmutableFeatureSchema.Builder()
          .from(schema)
          .propertyMap(visitedPropertiesMap)
          .build();
    }

    private FeatureSchema normalizeConstants(String parent, FeatureSchema schema) {
      if (schema.getConstantValue().isPresent() && schema.getSourcePaths().isEmpty()) {
        String constantValue =
            schema.getType() == Type.STRING
                ? String.format("'%s'", schema.getConstantValue().get())
                : schema.getConstantValue().get();
        String constantSourcePath =
            String.format(
                "constant_%s_%d{constant=%s}", parent, constantCounter[0]++, constantValue);

        return new ImmutableFeatureSchema.Builder()
            .from(schema)
            .addSourcePaths(constantSourcePath)
            .build();
      }
      return schema;
    }
  }
}
