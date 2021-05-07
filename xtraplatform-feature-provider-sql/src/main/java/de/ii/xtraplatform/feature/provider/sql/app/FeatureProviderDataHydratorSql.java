/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql.Dialect;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureProviderSqlData;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableFeatureProviderSqlData;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlConnector;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialectGpkg;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialectPostGis;
import de.ii.xtraplatform.feature.provider.sql.infra.db.SchemaGeneratorSql;
import de.ii.xtraplatform.features.domain.ConnectorFactory;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.store.domain.entities.EntityHydrator;
import de.ii.xtraplatform.store.domain.entities.handler.Entity;
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
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Provides(
    properties = {
        @StaticServiceProperty(
            name = Entity.TYPE_KEY,
            type = "java.lang.String",
            value = FeatureProvider2.ENTITY_TYPE),
        @StaticServiceProperty(
            name = Entity.SUB_TYPE_KEY,
            type = "java.lang.String",
            value = FeatureProviderSql.ENTITY_SUB_TYPE)
    })
@Instantiate
public class FeatureProviderDataHydratorSql implements EntityHydrator<FeatureProviderSqlData> {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(FeatureProviderDataHydratorSql.class);

  private final ClassLoader classLoader;
  private final ConnectorFactory connectorFactory;

  public FeatureProviderDataHydratorSql(
      @Context BundleContext context, @Requires ConnectorFactory connectorFactory) {
    this.classLoader = context.getBundle().adapt(BundleWiring.class).getClassLoader();
    Thread.currentThread().setContextClassLoader(classLoader);
    this.connectorFactory = connectorFactory;
  }

  @Override
  public FeatureProviderSqlData hydrateData(FeatureProviderSqlData data) {

    if (data.isAuto()) {
      LOGGER.info(
          "Feature provider with id '{}' is in auto mode, generating configuration ...",
          data.getId());
    }

    try {
      return cleanupAutoPersist(cleanupAdditionalInfo(
          generateNativeCrsIfNecessary(generateTypesIfNecessary(data))));

    } catch (Throwable e) {
      LOGGER.error(
          "Feature provider with id '{}' could not be hydrated: {}", data.getId(), e.getMessage());
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Stacktrace", e);
      }
    }

    throw new IllegalStateException();
  }

  private FeatureProviderSqlData generateTypesIfNecessary(FeatureProviderSqlData data) {
    if (data.isAuto() && data.getTypes().isEmpty()) {
      SqlConnector connector = (SqlConnector) connectorFactory.createConnector(data);

      try {
        ConnectionInfoSql connectionInfo = data.getConnectionInfo();

        List<String> schemas =
            connectionInfo.getSchemas().isEmpty()
                ? connectionInfo.getDialect() == Dialect.GPKG
                ? ImmutableList.of()
                : ImmutableList.of("public")
                : connectionInfo.getSchemas();

        SchemaGeneratorSql schemaGeneratorSql =
            new SchemaGeneratorSql(connector.getSqlClient(), schemas, data.getAutoTypes(),
                connectionInfo.getDialect() == Dialect.GPKG ? new SqlDialectGpkg()
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

        String mostOftenUsedId = idCounter.entrySet().stream()
            .max(Comparator.comparingInt(Entry::getValue))
            .map(Entry::getKey)
            .orElse("id");

        ImmutableMap<String, FeatureSchema> typeMap =
            types.stream()
                .map(type -> {
                  Optional<String> differingSortKey = type.getIdProperty()
                      .filter(idProperty -> !Objects.equals(idProperty.getName(), mostOftenUsedId))
                      .map(FeatureSchema::getName);

                  if (differingSortKey.isPresent() && type.getSourcePath().isPresent()) {
                    return new ImmutableFeatureSchema.Builder().from(type).sourcePath(
                        String.format("%s{sortKey=%s}", type.getSourcePath().get(),
                            differingSortKey.get()))
                        .build();
                  }

                  return type;
                })
                .map(type -> new AbstractMap.SimpleImmutableEntry<>(type.getName(), type))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        ImmutableFeatureProviderSqlData.Builder builder =
            new ImmutableFeatureProviderSqlData.Builder().from(data).types(typeMap);

        if (!Objects.equals(mostOftenUsedId, "id")) {
          // LOGGER.debug("CHANGING defaultSortKey to {}", mostOftenUsedId);
          builder.sourcePathDefaultsBuilder()
              .primaryKey(mostOftenUsedId)
              .sortKey(mostOftenUsedId);
        }

        return builder.build();
      } finally {
        connectorFactory.disposeConnector(connector);
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

      return new ImmutableFeatureProviderSqlData.Builder().from(data).nativeCrs(nativeCrs).build();
    }

    return data;
  }

  private FeatureProviderSqlData cleanupAdditionalInfo(FeatureProviderSqlData data) {
    if (data.isAuto()) {
      return new ImmutableFeatureProviderSqlData.Builder()
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
      return new ImmutableFeatureProviderSqlData.Builder()
          .from(data)
          .auto(Optional.empty())
          .autoPersist(Optional.empty())
          .autoTypes(new ArrayList<>())
          .build();
    }

    return data;
  }
}
