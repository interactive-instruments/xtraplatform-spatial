/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableMap;
import com.google.common.graph.Graph;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableGraph;
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
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableProviderCommonData;
import de.ii.xtraplatform.features.domain.MappingOperationResolver;
import de.ii.xtraplatform.features.domain.ProviderData;
import de.ii.xtraplatform.features.domain.ProviderExtensionRegistry;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaFragmentResolver;
import de.ii.xtraplatform.features.domain.SchemaReferenceResolver;
import de.ii.xtraplatform.features.domain.SchemaVisitorTopDown;
import de.ii.xtraplatform.features.domain.transform.FeatureRefEmbedder;
import de.ii.xtraplatform.features.domain.transform.FeatureRefResolver;
import de.ii.xtraplatform.features.domain.transform.ImplicitMappingResolver;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.features.sql.domain.ConstantsResolver;
import de.ii.xtraplatform.features.sql.domain.FeatureProviderSql;
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
import de.ii.xtraplatform.strings.domain.StringTemplateFilters;
import de.ii.xtraplatform.values.domain.ValueStore;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
  private final SqlDbmsAdapters dbmsAdapters;
  private final FeatureProviderSqlAuto featureProviderSqlAuto;
  private final boolean skipHydration;
  private final Set<String> connectors;

  @Inject
  public FeatureProviderSqlFactory(
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
      ProviderSqlFactoryAssisted providerSqlFactoryAssisted) {
    super(providerSqlFactoryAssisted);
    this.schemaResolvers = schemaResolvers;
    this.dbmsAdapters = dbmsAdapters;
    this.featureProviderSqlAuto =
        new FeatureProviderSqlAuto(
            new SqlClientBasicFactoryDefault(connectorFactory, dbmsAdapters));
    this.skipHydration = false;
    // TODO
    this.connectors = Set.of("JSON");
  }

  // for ldproxy-cfg
  public FeatureProviderSqlFactory(
      SqlDbmsAdapters dbmsAdapters, SqlClientBasicFactory sqlClientBasicFactory) {
    super(null);
    this.schemaResolvers = null;
    this.dbmsAdapters = dbmsAdapters;
    this.featureProviderSqlAuto = new FeatureProviderSqlAuto(sqlClientBasicFactory);
    this.skipHydration = true;
    this.connectors = Set.of();
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

        List<String> schemas = dbmsAdapters.get(connectionInfo.getDialect()).getDefaultSchemas();
        Map<String, List<String>> tables = featureProviderSqlAuto.analyze(data);

        if (!schemas.isEmpty()) {
          Map<String, List<String>> schemaTables = new LinkedHashMap<>();

          for (String schema : schemas) {
            if (tables.containsKey(schema)) {
              schemaTables.put(schema, tables.get(schema));
            }
          }

          tables = schemaTables;
        }

        data = featureProviderSqlAuto.generate(data, tables, ignore -> {});
      }

      return normalizeConstants(
          normalizeImplicitMappings(
              normalizeFeatureRefs(
                  resolveMappingOperationsIfNecessary(
                      embedFeatureRefs(applyLabelTemplate(resolveSchemasIfNecessary(data)))))));
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
      return new Builder().from(data).types(types).fragments(Map.of()).build();
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

  private FeatureProviderSqlData applyLabelTemplate(FeatureProviderSqlData data) {
    if (data.getLabelTemplate().isPresent() && !"{{value}}".equals(data.getLabelTemplate().get())) {

      Map<String, FeatureSchema> types =
          data.getTypes().entrySet().stream()
              .map(
                  entry ->
                      new SimpleImmutableEntry<>(
                          entry.getKey(),
                          entry
                              .getValue()
                              .accept(
                                  (SchemaVisitorTopDown<FeatureSchema, FeatureSchema>)
                                      (schema, parents, visitedProperties) -> {
                                        ImmutableFeatureSchema.Builder builder =
                                            new ImmutableFeatureSchema.Builder().from(schema);
                                        visitedProperties.forEach(
                                            prop -> builder.putPropertyMap(prop.getName(), prop));
                                        Map<String, String> lookup = new HashMap<>();
                                        lookup.put(
                                            "value", schema.getLabel().orElse(schema.getName()));
                                        schema
                                            .getUnit()
                                            .ifPresent(unit -> lookup.put("unit", unit));

                                        builder.label(
                                            StringTemplateFilters.applyTemplate(
                                                data.getLabelTemplate().get(), lookup::get));

                                        return builder.build();
                                      })))
              .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

      return new ImmutableFeatureProviderSqlData.Builder()
          .from(data)
          .types(types)
          .labelTemplate(Optional.empty())
          .build();
    }

    return data;
  }

  @SuppressWarnings("UnstableApiUsage")
  private FeatureProviderSqlData embedFeatureRefs(FeatureProviderSqlData data) {
    // determine graph of feature refs
    Graph<String> graph = getEmbeds(data.getTypes());
    if (Graphs.hasCycle(graph)) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(
            "Feature provider with id '{}' has a cycle in the feature references that are embedded. No feature references will be embedded.",
            data.getId());
      }
      return data;
    }
    Graph<String> graph2 = Graphs.transitiveClosure(graph);
    Map<String, Integer> map = new HashMap<>();
    int prio = 0;
    int numTypes = data.getTypes().keySet().size();
    while (map.keySet().size() < numTypes && prio < numTypes) {
      int currentPrio = prio;
      Map<String, Integer> map2 = Map.copyOf(map);
      data.getTypes()
          .forEach(
              (key, value) -> {
                if (!map.containsKey(key)) {
                  if (!graph2.nodes().contains(key)
                      || graph2.successors(key).stream()
                          .filter(n -> !n.equals(key))
                          .allMatch(map2::containsKey)) {
                    map.put(key, currentPrio);
                  }
                }
              });
      prio++;
    }

    if (map.keySet().size() < numTypes) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error(
            "Internal error while analyzing feature provider with id '{}' for feature references that should be embedded. No feature references will be embedded. Missed types: '{}'. Graph: {}",
            data.getId(),
            data.getTypes().keySet().stream()
                .filter(k -> !map.containsKey(k))
                .collect(Collectors.joining(", ")),
            graph);
      }
      return data;
    }

    FeatureProviderSqlData dataNew = data;
    for (int i = 1; i < prio; i++) {
      for (Map.Entry<String, Integer> entry : map.entrySet()) {
        if (entry.getValue() == i) {
          FeatureSchema schema =
              dataNew
                  .getTypes()
                  .get(entry.getKey())
                  .accept(new FeatureRefEmbedder(dataNew.getTypes()));
          dataNew = new Builder().from(dataNew).putTypes(entry.getKey(), schema).build();
        }
      }
    }

    return dataNew;
  }

  @SuppressWarnings("UnstableApiUsage")
  private static Graph<String> getEmbeds(Map<String, FeatureSchema> types) {
    ImmutableGraph.Builder<String> builder =
        GraphBuilder.directed().allowsSelfLoops(false).immutable();
    types.forEach(
        (key, value) ->
            value.getAllNestedProperties().stream()
                .filter(SchemaBase::isEmbed)
                .forEach(
                    p -> {
                      if (!p.getConcat().isEmpty()) {
                        p.getConcat().stream()
                            .map(FeatureSchema::getRefType)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .filter(ref -> !ref.equals(key))
                            .forEach(ref -> builder.putEdge(key, ref));
                      } else if (!p.getCoalesce().isEmpty()) {
                        p.getCoalesce().stream()
                            .map(FeatureSchema::getRefType)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .filter(ref -> !ref.equals(key))
                            .forEach(ref -> builder.putEdge(key, ref));
                      } else {
                        p.getRefType()
                            .filter(ref -> !ref.equals(key))
                            .ifPresent(ref -> builder.putEdge(key, ref));
                      }
                    }));
    return builder.build();
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
}
