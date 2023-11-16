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
import dagger.Lazy;
import dagger.assisted.AssistedFactory;
import de.ii.xtraplatform.base.domain.LogContext;
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
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableProviderCommonData;
import de.ii.xtraplatform.features.domain.MappingOperationResolver;
import de.ii.xtraplatform.features.domain.ProviderData;
import de.ii.xtraplatform.features.domain.ProviderExtensionRegistry;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaFragmentResolver;
import de.ii.xtraplatform.features.domain.SchemaReferenceResolver;
import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import de.ii.xtraplatform.features.domain.transform.FeatureRefResolver;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql.Dialect;
import de.ii.xtraplatform.features.sql.domain.FeatureProviderSqlData;
import de.ii.xtraplatform.features.sql.domain.ImmutableConnectionInfoSql;
import de.ii.xtraplatform.features.sql.domain.ImmutableFeatureProviderSqlData;
import de.ii.xtraplatform.features.sql.domain.ImmutableFeatureProviderSqlData.Builder;
import de.ii.xtraplatform.features.sql.domain.ImmutablePoolSettings;
import de.ii.xtraplatform.features.sql.domain.ImmutableQueryGeneratorSettings;
import de.ii.xtraplatform.features.sql.domain.ImmutableSqlPathDefaults;
import de.ii.xtraplatform.features.sql.domain.SqlClientBasicFactory;
import de.ii.xtraplatform.streams.domain.Reactive;
import de.ii.xtraplatform.values.domain.ValueStore;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
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

  private final Lazy<Set<SchemaFragmentResolver>> schemaResolvers;
  private final FeatureProviderSqlAuto featureProviderSqlAuto;
  private final boolean skipHydration;

  @Inject
  public FeatureProviderSqlFactory(
      Lazy<Set<SchemaFragmentResolver>> schemaResolvers,
      // TODO: needed because dagger-auto does not parse FeatureProviderSql
      CrsTransformerFactory crsTransformerFactory,
      CrsInfo crsInfo,
      Cql cql,
      ConnectorFactory connectorFactory,
      Reactive reactive,
      ValueStore valueStore,
      ProviderExtensionRegistry extensionRegistry,
      DecoderFactories decoderFactories,
      ProviderSqlFactoryAssisted providerSqlFactoryAssisted) {
    super(providerSqlFactoryAssisted);
    this.schemaResolvers = schemaResolvers;
    this.featureProviderSqlAuto =
        new FeatureProviderSqlAuto(new SqlClientBasicFactoryDefault(connectorFactory));
    this.skipHydration = false;
  }

  // for ldproxy-cfg
  public FeatureProviderSqlFactory(SqlClientBasicFactory sqlClientBasicFactory) {
    super(null);
    this.schemaResolvers = null;
    this.featureProviderSqlAuto = new FeatureProviderSqlAuto(sqlClientBasicFactory);
    this.skipHydration = true;
  }

  @Override
  public String type() {
    return ProviderData.ENTITY_TYPE;
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
    return new ImmutableFeatureProviderSqlData.Builder()
        .typeValidation(MODE.NONE)
        .sourcePathDefaults(new ImmutableSqlPathDefaults.Builder().build())
        .queryGeneration(new ImmutableQueryGeneratorSettings.Builder().build())
        .connectionInfo(
            new ImmutableConnectionInfoSql.Builder()
                .database("")
                .pool(
                    new ImmutablePoolSettings.Builder()
                        .maxConnections(-1)
                        .minConnections(1)
                        .initFailFast(true)
                        .initFailTimeout("1")
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
    return Optional.of(featureProviderSqlAuto);
  }

  @Override
  public EntityData hydrateData(EntityData entityData) {
    FeatureProviderSqlData data = (FeatureProviderSqlData) entityData;

    if (skipHydration) {
      return entityData;
    }

    try {
      if (data.isAuto()) {
        LOGGER.info(
            "Feature provider with id '{}' is in auto mode, generating configuration ...",
            data.getId());

        ConnectionInfoSql connectionInfo = data.getConnectionInfo();

        List<String> schemas =
            connectionInfo.getSchemas().isEmpty()
                ? connectionInfo.getDialect() == Dialect.GPKG
                    ? ImmutableList.of()
                    : ImmutableList.of("public")
                : connectionInfo.getSchemas();
        List<String> autoTypes = data.getAutoTypes();

        // TODO: derive from schemas and autoTypes
        Map<String, List<String>> includeTypes = Map.of();

        data = featureProviderSqlAuto.generate(data, includeTypes, ignore -> {});
      }

      return normalizeConstants(
          normalizeFeatureRefs(
              resolveMappingOperationsIfNecessary(resolveSchemasIfNecessary(data))));
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
        data, p -> p.isConstant() && p.getSourcePaths().isEmpty(), new NormalizeConstants());
  }

  private FeatureProviderSqlData normalizeFeatureRefs(FeatureProviderSqlData data) {
    return applySchemaTransformation(
        data,
        p -> p.getType() == Type.FEATURE_REF || p.getType() == Type.FEATURE_REF_ARRAY,
        new FeatureRefResolver());
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
      List<FeatureSchema> visitedProperties2 =
          visitedProperties.stream()
              .filter(Objects::nonNull)
              .map(featureSchema -> normalizeConstants(schema.getName(), featureSchema))
              .collect(Collectors.toList());

      return new ImmutableFeatureSchema.Builder()
          .from(schema)
          .propertyMap(asMap(visitedProperties2, FeatureSchema::getFullPathAsString))
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
                "%sconstant_%s_%d{constant=%s}",
                schema.getSourcePath().orElse(""), parent, constantCounter[0]++, constantValue);

        return new ImmutableFeatureSchema.Builder()
            .from(schema)
            .sourcePath(Optional.empty())
            .addSourcePaths(constantSourcePath)
            .build();
      }
      return schema;
    }
  }
}
