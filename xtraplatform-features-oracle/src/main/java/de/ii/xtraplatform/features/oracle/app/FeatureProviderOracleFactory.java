/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.oracle.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import dagger.Lazy;
import dagger.assisted.AssistedFactory;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.base.domain.resiliency.VolatileRegistry;
import de.ii.xtraplatform.cache.domain.Cache;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.entities.domain.AbstractEntityFactory;
import de.ii.xtraplatform.entities.domain.AutoEntityFactory;
import de.ii.xtraplatform.entities.domain.EntityData;
import de.ii.xtraplatform.entities.domain.EntityDataBuilder;
import de.ii.xtraplatform.entities.domain.EntityFactory;
import de.ii.xtraplatform.entities.domain.PersistentEntity;
import de.ii.xtraplatform.entities.domain.ValidationResult.MODE;
import de.ii.xtraplatform.features.domain.ConnectorFactory;
import de.ii.xtraplatform.features.domain.DecoderFactories;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableProviderCommonData;
import de.ii.xtraplatform.features.domain.MappingOperationResolver;
import de.ii.xtraplatform.features.domain.ProviderData;
import de.ii.xtraplatform.features.domain.ProviderExtensionRegistry;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaFragmentResolver;
import de.ii.xtraplatform.features.domain.SchemaReferenceResolver;
import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import de.ii.xtraplatform.features.domain.transform.FeatureRefResolver;
import de.ii.xtraplatform.features.domain.transform.ImplicitMappingResolver;
import de.ii.xtraplatform.features.sql.domain.ConstantsResolver;
import de.ii.xtraplatform.features.sql.domain.FeatureProviderSqlData;
import de.ii.xtraplatform.features.sql.domain.ImmutableConnectionInfoSql;
import de.ii.xtraplatform.features.sql.domain.ImmutableFeatureProviderSqlData;
import de.ii.xtraplatform.features.sql.domain.ImmutableFeatureProviderSqlData.Builder;
import de.ii.xtraplatform.features.sql.domain.ImmutablePoolSettings;
import de.ii.xtraplatform.features.sql.domain.ImmutableQueryGeneratorSettings;
import de.ii.xtraplatform.features.sql.domain.ImmutableSqlPathDefaults;
import de.ii.xtraplatform.features.sql.domain.SqlClientBasicFactory;
import de.ii.xtraplatform.features.sql.domain.SqlDbmsAdapters;
import de.ii.xtraplatform.streams.domain.Reactive;
import de.ii.xtraplatform.values.domain.ValueStore;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class FeatureProviderOracleFactory
    extends AbstractEntityFactory<FeatureProviderDataV2, FeatureProviderOracle>
    implements EntityFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderOracleFactory.class);

  private final Lazy<Set<SchemaFragmentResolver>> schemaResolvers;
  private final boolean skipHydration;
  private final Set<String> connectors;

  @Inject
  public FeatureProviderOracleFactory(
      Lazy<Set<SchemaFragmentResolver>> schemaResolvers,
      // TODO: needed because dagger-auto does not parse FeatureProviderSql
      CrsTransformerFactory crsTransformerFactory,
      CrsInfo crsInfo,
      Cql cql,
      ConnectorFactory connectorFactory,
      SqlDbmsAdapters dbmsAdapters,
      Reactive reactive,
      ValueStore valueStore,
      ProviderExtensionRegistry extensionRegistry,
      DecoderFactories decoderFactories,
      VolatileRegistry volatileRegistry,
      Cache cache,
      ProviderOracleFactoryAssisted providerOracleFactoryAssisted) {
    super(providerOracleFactoryAssisted);
    this.schemaResolvers = schemaResolvers;
    this.skipHydration = false;
    this.connectors = Set.of();
  }

  // for ldproxy-cfg
  public FeatureProviderOracleFactory(SqlClientBasicFactory sqlClientBasicFactory) {
    super(null);
    this.schemaResolvers = null;
    this.skipHydration = true;
    this.connectors = Set.of();
  }

  @Override
  public String type() {
    return ProviderData.ENTITY_TYPE;
  }

  @Override
  public Optional<String> subType() {
    return Optional.of(FeatureProviderOracle.ENTITY_SUB_TYPE);
  }

  @Override
  public Class<? extends PersistentEntity> entityClass() {
    return FeatureProviderOracle.class;
  }

  @Override
  public EntityDataBuilder<FeatureProviderDataV2> dataBuilder() {
    return new ImmutableFeatureProviderSqlData.Builder()
        .typeValidation(MODE.NONE)
        .sourcePathDefaults(new ImmutableSqlPathDefaults.Builder().build())
        .queryGeneration(new ImmutableQueryGeneratorSettings.Builder().build())
        .connectionInfo(
            new ImmutableConnectionInfoSql.Builder()
                .database("")
                .dialect(SqlDbmsAdapterOras.ID)
                .pool(
                    new ImmutablePoolSettings.Builder()
                        .maxConnections(-1)
                        .minConnections(1)
                        .idleTimeout("10m")
                        .shared(false)
                        .build())
                .build());
  }

  @Override
  public EntityDataBuilder<? extends EntityData> superDataBuilder() {
    return new ImmutableProviderCommonData.Builder();
  }

  @Override
  public EntityDataBuilder<FeatureProviderDataV2> emptyDataBuilder() {
    return new ImmutableFeatureProviderSqlData.Builder();
  }

  @Override
  public EntityDataBuilder<? extends EntityData> emptySuperDataBuilder() {
    return new ImmutableProviderCommonData.Builder();
  }

  @Override
  public Class<? extends EntityData> dataClass() {
    return FeatureProviderSqlData.class;
  }

  @Override
  public Optional<AutoEntityFactory> auto() {
    return Optional.empty();
  }

  @Override
  public EntityData hydrateData(EntityData entityData) {
    FeatureProviderSqlData data = (FeatureProviderSqlData) entityData;

    if (skipHydration) {
      return entityData;
    }

    try {
      return normalizeConstants(
          normalizeImplicitMappings(
              normalizeFeatureRefs(
                  resolveMappingOperationsIfNecessary(resolveSchemasIfNecessary(data)))));
    } catch (Throwable e) {
      LogContext.error(
          LOGGER, e, "Feature provider with id '{}' could not be started", data.getId());
    }

    throw new IllegalStateException();
  }

  @Override
  public Map<String, String> getListEntryKeys() {
    return Map.of("extensions", "type");
  }

  private FeatureProviderSqlData resolveSchemasIfNecessary(FeatureProviderSqlData data) {
    SchemaReferenceResolver resolver = new SchemaReferenceResolver(data, schemaResolvers);
    Map<String, FeatureSchema> types = data.getTypes();

    int rounds = 0;
    while (resolver.needsResolving(types)) {
      types = resolver.resolve(types);
      if (++rounds > 16) {
        LOGGER.warn("Exceeded the maximum length of 16 for provider schema reference chains.");
        break;
      }
    }

    if (rounds > 0) {
      return new Builder().from(data).types(types).build();
    }

    return data;
  }

  private FeatureProviderSqlData resolveMappingOperationsIfNecessary(FeatureProviderSqlData data) {
    MappingOperationResolver resolver = new MappingOperationResolver();

    if (resolver.needsResolving(data.getTypes())) {
      Map<String, FeatureSchema> types = resolver.resolve(data.getTypes());

      ImmutableFeatureProviderSqlData build = new Builder().from(data).types(types).build();
      return build;
    }
    return data;
  }

  private FeatureProviderSqlData normalizeConstants(FeatureProviderSqlData data) {
    return applySchemaTransformation(
        data, p -> p.isConstant() && p.getSourcePaths().isEmpty(), new ConstantsResolver());
  }

  private FeatureProviderSqlData normalizeImplicitMappings(FeatureProviderSqlData data) {
    ImplicitMappingResolver implicitMappingResolver = new ImplicitMappingResolver();
    return applySchemaTransformation(
        data, implicitMappingResolver::needsResolving, implicitMappingResolver);
  }

  private FeatureProviderSqlData normalizeFeatureRefs(FeatureProviderSqlData data) {
    return applySchemaTransformation(
        data,
        p -> p.getType() == Type.FEATURE_REF || p.getType() == Type.FEATURE_REF_ARRAY,
        new FeatureRefResolver(connectors));
  }

  private FeatureProviderSqlData applySchemaTransformation(
      FeatureProviderSqlData data,
      Predicate<FeatureSchema> propertyMatcher,
      SchemaVisitorTopDown<FeatureSchema, FeatureSchema> transformer) {
    boolean anyPropertyMatches =
        data.getTypes().values().stream()
            .flatMap(t -> t.getAllNestedProperties().stream())
            .anyMatch(propertyMatcher);

    if (anyPropertyMatches) {
      Map<String, FeatureSchema> types =
          data.getTypes().entrySet().stream()
              .map(entry -> Map.entry(entry.getKey(), entry.getValue().accept(transformer)))
              .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

      return new Builder().from(data).types(types).build();
    }

    return data;
  }

  @AssistedFactory
  public interface ProviderOracleFactoryAssisted
      extends FactoryAssisted<FeatureProviderDataV2, FeatureProviderOracle> {
    @Override
    FeatureProviderOracle create(FeatureProviderDataV2 data);
  }
}
