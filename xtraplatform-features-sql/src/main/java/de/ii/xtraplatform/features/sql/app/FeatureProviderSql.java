/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.app;

import static de.ii.xtraplatform.cql.domain.In.ID_PLACEHOLDER;

import com.fasterxml.jackson.core.JsonParseException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.entities.domain.Entity;
import de.ii.xtraplatform.entities.domain.Entity.SubType;
import de.ii.xtraplatform.features.domain.AbstractFeatureProvider;
import de.ii.xtraplatform.features.domain.AggregateStatsReader;
import de.ii.xtraplatform.features.domain.ConnectionInfo;
import de.ii.xtraplatform.features.domain.ConnectorFactory;
import de.ii.xtraplatform.features.domain.Decoder;
import de.ii.xtraplatform.features.domain.DecoderFactories;
import de.ii.xtraplatform.features.domain.FeatureCrs;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.FeatureExtents;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureQueries;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureQueryEncoder;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureStream;
import de.ii.xtraplatform.features.domain.FeatureStreamImpl;
import de.ii.xtraplatform.features.domain.FeatureTokenDecoder;
import de.ii.xtraplatform.features.domain.FeatureTokenSource;
import de.ii.xtraplatform.features.domain.FeatureTokenTransformer;
import de.ii.xtraplatform.features.domain.FeatureTransactions;
import de.ii.xtraplatform.features.domain.FeatureTransactions.MutationResult.Builder;
import de.ii.xtraplatform.features.domain.FeatureTransactions.MutationResult.Type;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import de.ii.xtraplatform.features.domain.ImmutableMultiFeatureQuery;
import de.ii.xtraplatform.features.domain.ImmutableMutationResult;
import de.ii.xtraplatform.features.domain.ImmutableSubQuery;
import de.ii.xtraplatform.features.domain.MultiFeatureQueries;
import de.ii.xtraplatform.features.domain.MultiFeatureQuery;
import de.ii.xtraplatform.features.domain.MultiFeatureQuery.SubQuery;
import de.ii.xtraplatform.features.domain.ProviderData;
import de.ii.xtraplatform.features.domain.ProviderExtensionRegistry;
import de.ii.xtraplatform.features.domain.Query;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaMapping;
import de.ii.xtraplatform.features.domain.SortKey;
import de.ii.xtraplatform.features.domain.SourceSchemaValidator;
import de.ii.xtraplatform.features.domain.transform.OnlyQueryables;
import de.ii.xtraplatform.features.domain.transform.OnlySortables;
import de.ii.xtraplatform.features.sql.ImmutableSqlPathSyntax;
import de.ii.xtraplatform.features.sql.SqlPathSyntax;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql.Dialect;
import de.ii.xtraplatform.features.sql.domain.FeatureProviderSqlData;
import de.ii.xtraplatform.features.sql.domain.FeatureTokenStatsCollector;
import de.ii.xtraplatform.features.sql.domain.ImmutableConnectionInfoSql;
import de.ii.xtraplatform.features.sql.domain.ImmutablePoolSettings;
import de.ii.xtraplatform.features.sql.domain.ImmutableSchemaMappingSql;
import de.ii.xtraplatform.features.sql.domain.SchemaMappingSql;
import de.ii.xtraplatform.features.sql.domain.SchemaSql;
import de.ii.xtraplatform.features.sql.domain.SqlClient;
import de.ii.xtraplatform.features.sql.domain.SqlConnector;
import de.ii.xtraplatform.features.sql.domain.SqlDialect;
import de.ii.xtraplatform.features.sql.domain.SqlDialectGpkg;
import de.ii.xtraplatform.features.sql.domain.SqlDialectPostGis;
import de.ii.xtraplatform.features.sql.domain.SqlPathDefaults;
import de.ii.xtraplatform.features.sql.domain.SqlPathParser;
import de.ii.xtraplatform.features.sql.domain.SqlQueryBatch;
import de.ii.xtraplatform.features.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.features.sql.domain.SqlRow;
import de.ii.xtraplatform.features.sql.infra.db.SourceSchemaValidatorSql;
import de.ii.xtraplatform.streams.domain.Reactive;
import de.ii.xtraplatform.streams.domain.Reactive.RunnableStream;
import de.ii.xtraplatform.streams.domain.Reactive.Sink;
import de.ii.xtraplatform.streams.domain.Reactive.Source;
import de.ii.xtraplatform.streams.domain.Reactive.Stream;
import de.ii.xtraplatform.streams.domain.Reactive.Transformer;
import de.ii.xtraplatform.values.domain.ValueStore;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.ws.rs.core.MediaType;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

@Entity(
    type = ProviderData.ENTITY_TYPE,
    subTypes = {
      @SubType(key = ProviderData.PROVIDER_TYPE_KEY, value = FeatureProvider2.PROVIDER_TYPE),
      @SubType(
          key = ProviderData.PROVIDER_SUB_TYPE_KEY,
          value = FeatureProviderSql.PROVIDER_SUB_TYPE,
          keyAlias = {FeatureProviderDataV2.PROVIDER_SUB_TYPE_KEY_OLD})
    },
    data = FeatureProviderSqlData.class)
public class FeatureProviderSql
    extends AbstractFeatureProvider<SqlRow, SqlQueryBatch, SqlQueryOptions, SchemaSql>
    implements FeatureProvider2,
        FeatureQueries,
        FeatureExtents,
        FeatureCrs,
        FeatureTransactions,
        MultiFeatureQueries {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderSql.class);

  public static final String ENTITY_SUB_TYPE = "feature/sql";
  public static final String PROVIDER_SUB_TYPE = "SQL";

  private final CrsTransformerFactory crsTransformerFactory;
  private final CrsInfo crsInfo;
  private final Cql cql;
  private final Map<String, Supplier<Decoder>> subdecoders;

  private FeatureQueryEncoderSql queryTransformer;
  private AggregateStatsReader<SchemaSql> aggregateStatsReader;
  private FeatureMutationsSql featureMutationsSql;
  private PathParserSql pathParser2;
  private SqlPathParser pathParser3;
  private SourceSchemaValidator<SchemaSql> sourceSchemaValidator;
  private Map<String, List<SchemaSql>> tableSchemas;
  private Map<String, List<SchemaSql>> tableSchemasMutations;

  @AssistedInject
  public FeatureProviderSql(
      CrsTransformerFactory crsTransformerFactory,
      CrsInfo crsInfo,
      Cql cql,
      ConnectorFactory connectorFactory,
      Reactive reactive,
      ValueStore valueStore,
      ProviderExtensionRegistry extensionRegistry,
      DecoderFactories decoderFactories,
      @Assisted FeatureProviderDataV2 data) {
    super(
        connectorFactory,
        reactive,
        crsTransformerFactory,
        extensionRegistry,
        valueStore.forType(Codelist.class),
        data);

    this.crsTransformerFactory = crsTransformerFactory;
    this.crsInfo = crsInfo;
    this.cql = cql;

    this.subdecoders =
        Map.of(
            "JSON",
            () -> decoderFactories.createDecoder(MediaType.APPLICATION_JSON_TYPE).orElseThrow());
  }

  private static PathParserSql createPathParser2(SqlPathDefaults sqlPathDefaults, Cql cql) {
    SqlPathSyntax syntax = ImmutableSqlPathSyntax.builder().options(sqlPathDefaults).build();
    return new PathParserSql(syntax, cql);
  }

  private static SqlPathParser createPathParser3(
      SqlPathDefaults sqlPathDefaults, Cql cql, Set<String> connectors) {
    return new SqlPathParser(sqlPathDefaults, cql, connectors);
  }

  @Override
  protected boolean onStartup() throws InterruptedException {
    List<String> validationSchemas =
        getData().getConnectionInfo().getDialect() == Dialect.PGIS
                && getData().getConnectionInfo().getSchemas().isEmpty()
            ? ImmutableList.of("public")
            : getData().getConnectionInfo().getSchemas();
    this.sourceSchemaValidator =
        new SourceSchemaValidatorSql(validationSchemas, this::getSqlClient);

    this.pathParser3 =
        createPathParser3(getData().getSourcePathDefaults(), cql, subdecoders.keySet());
    QuerySchemaDeriver querySchemaDeriver = new QuerySchemaDeriver(pathParser3);
    this.tableSchemas =
        getData().getTypes().entrySet().stream()
            .map(
                entry ->
                    new SimpleImmutableEntry<>(
                        entry.getKey(),
                        entry.getValue().accept(WITH_SCOPE_QUERIES).accept(querySchemaDeriver)))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    boolean success = super.onStartup();

    if (!success) {
      return false;
    }

    SqlDialect sqlDialect =
        getData().getConnectionInfo().getDialect() == Dialect.PGIS
            ? new SqlDialectPostGis()
            : new SqlDialectGpkg();
    String accentiCollation =
        Objects.nonNull(getData().getQueryGeneration())
            ? getData().getQueryGeneration().getAccentiCollation().orElse(null)
            : null;
    FilterEncoderSql filterEncoder =
        new FilterEncoderSql(
            getData().getNativeCrs().orElse(OgcCrs.CRS84),
            sqlDialect,
            crsTransformerFactory,
            crsInfo,
            cql,
            accentiCollation);
    AggregateStatsQueryGenerator queryGeneratorSql =
        new AggregateStatsQueryGenerator(sqlDialect, filterEncoder);

    SqlQueryTemplatesDeriver queryTemplatesDeriver =
        new SqlQueryTemplatesDeriver(
            filterEncoder,
            sqlDialect,
            getData().getQueryGeneration().getComputeNumberMatched(),
            false);

    this.tableSchemasMutations =
        getData().getTypes().entrySet().stream()
            .map(
                entry ->
                    new SimpleImmutableEntry<>(
                        entry.getKey(),
                        entry.getValue().accept(WITH_SCOPE_MUTATIONS).accept(querySchemaDeriver)))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    Map<String, List<SqlQueryTemplates>> allQueryTemplates =
        tableSchemas.entrySet().stream()
            .map(
                entry ->
                    new SimpleImmutableEntry<>(
                        entry.getKey(),
                        entry.getValue().stream()
                            .map(
                                schemaSql ->
                                    schemaSql.accept(
                                        new SqlQueryTemplatesDeriver(
                                            filterEncoder,
                                            sqlDialect,
                                            getData()
                                                .getQueryGeneration()
                                                .getComputeNumberMatched(),
                                            true)))
                            .collect(Collectors.toList())))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    Map<String, List<SqlQueryTemplates>> allQueryTemplatesMutations =
        getData().getTypes().entrySet().stream()
            .map(
                entry ->
                    new SimpleImmutableEntry<>(
                        entry.getKey(),
                        ImmutableList.of(
                            entry
                                .getValue()
                                .accept(WITH_SCOPE_MUTATIONS)
                                .accept(querySchemaDeriver)
                                .get(0)
                                .accept(queryTemplatesDeriver))))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    this.queryTransformer =
        new FeatureQueryEncoderSql(
            allQueryTemplates,
            allQueryTemplatesMutations,
            getData().getQueryGeneration(),
            sqlDialect);

    this.aggregateStatsReader =
        new AggregateStatsReaderSql(
            this::getSqlClient,
            queryGeneratorSql,
            sqlDialect,
            getData().getNativeCrs().orElse(OgcCrs.CRS84));
    this.featureMutationsSql =
        new FeatureMutationsSql(
            this::getSqlClient,
            new SqlInsertGenerator2(
                getData().getNativeCrs().orElse(OgcCrs.CRS84),
                crsTransformerFactory,
                getData().getSourcePathDefaults()));
    this.pathParser2 = createPathParser2(getData().getSourcePathDefaults(), cql);

    return true;
  }

  @Override
  protected void onStarted() {
    super.onStarted();
    if (Runtime.getRuntime().availableProcessors() > getStreamRunner().getCapacity()) {
      LOGGER.info(
          "Recommended max connections for optimal performance under load: {}",
          getMaxQueries() * Runtime.getRuntime().availableProcessors());
    }
    Map<String, List<SchemaSql>> sourceSchema = new LinkedHashMap<>();
    try {
      for (FeatureSchema fs : getData().getTypes().values()) {
        sourceSchema.put(
            fs.getName(),
            fs.accept(WITH_SCOPE_MUTATIONS)
                .accept(new MutationSchemaDeriver(pathParser2, pathParser3)));
      }
    } catch (Throwable e) {
      boolean br = true;
    }
    boolean br = true;
  }

  // TODO: implement auto mode for maxConnections=-1, how to get numberOfQueries in Connector?
  @Override
  protected int getRunnerCapacity(ConnectionInfo connectionInfo) {
    ConnectionInfoSql connectionInfoSql = (ConnectionInfoSql) connectionInfo;

    int maxConnections = connectionInfoSql.getPool().getMaxConnections();

    int runnerCapacity = Runtime.getRuntime().availableProcessors();
    if (maxConnections > 0) {
      int capacity = maxConnections / getMaxQueries();
      // LOGGER.info("{}: {}", typeInfo.getName(), capacity);
      if (capacity >= 0 && capacity < runnerCapacity) {
        runnerCapacity = capacity;
      }
    }
    // LOGGER.info("RUNNER: {}", runnerCapacity);

    return runnerCapacity;
  }

  private List<SchemaSql> getAllSourceSchemas() {
    return getSourceSchemas().values().stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private int getMaxQueries() {
    return getSourceSchemas().values().stream()
        .flatMap(Collection::stream)
        .mapToInt(s -> s.getAllObjects().size())
        .max()
        .orElse(1);
  }

  // TODO: move to hydration
  @Override
  protected ConnectionInfo getConnectionInfo() {
    ConnectionInfoSql connectionInfo = (ConnectionInfoSql) super.getConnectionInfo();

    if (connectionInfo.getPool().getMaxConnections() <= 0) {
      int maxConnections = getMaxQueries() * Runtime.getRuntime().availableProcessors();

      return new ImmutableConnectionInfoSql.Builder()
          .from(connectionInfo)
          .pool(
              new ImmutablePoolSettings.Builder()
                  .from(connectionInfo.getPool())
                  .maxConnections(maxConnections)
                  .build())
          .build();
    }

    return connectionInfo;
  }

  @Override
  protected int getRunnerQueueSize(ConnectionInfo connectionInfo) {
    ConnectionInfoSql connectionInfoSql = (ConnectionInfoSql) connectionInfo;

    int maxQueries = getMaxQueries();

    int maxConnections;
    if (connectionInfoSql.getPool().getMaxConnections() > 0) {
      maxConnections = connectionInfoSql.getPool().getMaxConnections();
    } else {
      maxConnections = maxQueries * Runtime.getRuntime().availableProcessors();
    }
    int capacity = maxConnections / maxQueries;
    // TODO
    int queueSize = Math.max(1024, maxConnections * capacity * 2) / maxQueries;
    // LOGGER.info("RUNNERQ: {} {} {} {}", maxQueries ,maxConnections, capacity, queueSize);
    return queueSize;
  }

  @Override
  protected Optional<String> getRunnerError(ConnectionInfo connectionInfo) {
    if (getStreamRunner().getCapacity() == 0) {
      ConnectionInfoSql connectionInfoSql = (ConnectionInfoSql) connectionInfo;

      int maxConnections = connectionInfoSql.getPool().getMaxConnections();
      int minRequired = getMaxQueries();

      return Optional.of(
          String.format(
              "maxConnections=%d is too low, a minimum of %d is required",
              maxConnections, minRequired));
    }

    return Optional.empty();
  }

  public Map<String, List<SchemaSql>> getSourceSchemas() {
    return tableSchemas;
  }

  @Override
  protected Optional<Map<String, String>> getStartupInfo() {
    String parallelism = String.valueOf(getStreamRunner().getCapacity());

    // TODO: get other infos from connector

    ImmutableMap.Builder<String, String> info =
        new ImmutableMap.Builder<String, String>()
            .put("min connections", String.valueOf(getConnector().getMinConnections()))
            .put("max connections", String.valueOf(getConnector().getMaxConnections()))
            .put("stream capacity", parallelism);

    if (getData().getConnectionInfo().isShared()) {
      info.put("shared", "true");
    }

    return Optional.of(info.build());
  }

  @Override
  protected Optional<SourceSchemaValidator<SchemaSql>> getTypeInfoValidator() {
    return Optional.ofNullable(sourceSchemaValidator);
  }

  @Override
  protected String applySourcePathDefaults(String path, boolean isValue) {
    Optional<String> schemaPrefix = getData().getSourcePathDefaults().getSchema();
    if (schemaPrefix.isPresent() && !isValue) {
      return pathParser3.tablePathWithDefaults(path);
    }
    return path;
  }

  @Override
  protected FeatureTokenDecoder<
          SqlRow, FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>>
      getDecoder(Query query, Map<String, SchemaMapping> mappings) {
    if (query instanceof FeatureQuery) {
      FeatureQuery featureQuery = (FeatureQuery) query;

      List<SchemaSql> schemas =
          featureQuery.getSchemaScope() == SchemaBase.Scope.RETURNABLE
              ? tableSchemas.get(featureQuery.getType())
              : tableSchemasMutations.get(featureQuery.getType());

      return new FeatureDecoderSql(mappings, schemas, query, subdecoders);
    }

    if (query instanceof MultiFeatureQuery) {
      MultiFeatureQuery multiFeatureQuery = (MultiFeatureQuery) query;

      List<SchemaSql> schemas =
          multiFeatureQuery.getQueries().stream()
              .flatMap(typeQuery -> tableSchemas.get(typeQuery.getType()).stream())
              .collect(Collectors.toList());

      return new FeatureDecoderSql(mappings, schemas, query, subdecoders);
    }

    throw new IllegalArgumentException();
  }

  @Override
  protected List<FeatureTokenTransformer> getDecoderTransformers() {
    return ImmutableList.of(); // new FeatureTokenTransformerSorting());
  }

  @Override
  public FeatureProviderSqlData getData() {
    return (FeatureProviderSqlData) super.getData();
  }

  @Override
  protected FeatureQueryEncoder<SqlQueryBatch, SqlQueryOptions> getQueryEncoder() {
    return queryTransformer;
  }

  @Override
  protected SqlConnector getConnector() {
    return (SqlConnector) super.getConnector();
  }

  private SqlClient getSqlClient() {
    return getConnector().getSqlClient();
  }

  @Override
  public boolean supportsTransactions() {
    return super.supportsTransactions()
        && getData().getConnectionInfo().getDialect() == Dialect.PGIS;
  }

  @Override
  public boolean supportsCrs() {
    return super.supportsCrs() && getData().getNativeCrs().isPresent();
  }

  @Override
  public EpsgCrs getNativeCrs() {
    return getData().getNativeCrs().get();
  }

  @Override
  public boolean isCrsSupported(EpsgCrs crs) {
    return Objects.equals(getNativeCrs(), crs) || crsTransformerFactory.isSupported(crs);
  }

  @Override
  public boolean is3dSupported() {
    return ((CrsInfo) crsTransformerFactory).is3d(getNativeCrs());
  }

  @Override
  public long getFeatureCount(String typeName) {
    if (!getSourceSchemas().containsKey(typeName)) {
      return -1;
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Computing feature count for '{}'", typeName);
    }

    try {
      Stream<Long> countGraph = aggregateStatsReader.getCount(getSourceSchemas().get(typeName));

      return countGraph
          .on(getStreamRunner())
          .run()
          .exceptionally(
              throwable -> {
                LogContext.errorAsWarn(LOGGER, throwable, "Cannot compute feature count");
                return -1L;
              })
          .toCompletableFuture()
          .join();
    } catch (Throwable e) {
      // continue
    }

    return -1;
  }

  @Override
  public Optional<BoundingBox> getSpatialExtent(String typeName) {
    if (!getSourceSchemas().containsKey(typeName)) {
      return Optional.empty();
    }

    if (LOGGER.isDebugEnabled()) {
      Optional.ofNullable(getData().getTypes().get(typeName))
          .flatMap(SchemaBase::getPrimaryGeometry)
          .map(SchemaBase::getName)
          .ifPresent(
              spatialProperty ->
                  LOGGER.debug("Computing spatial extent for '{}.{}'", typeName, spatialProperty));
    }

    try {
      Stream<Optional<BoundingBox>> extentGraph =
          aggregateStatsReader.getSpatialExtent(getSourceSchemas().get(typeName), is3dSupported());

      return extentGraph
          .on(getStreamRunner())
          .run()
          .exceptionally(
              throwable -> {
                LogContext.errorAsWarn(LOGGER, throwable, "Cannot compute spatial extent");
                return Optional.empty();
              })
          .toCompletableFuture()
          .join();
    } catch (Throwable e) {
      // continue
    }

    return Optional.empty();
  }

  @Override
  public Optional<BoundingBox> getSpatialExtent(String typeName, EpsgCrs crs) {
    return getSpatialExtent(typeName)
        .flatMap(
            boundingBox ->
                crsTransformerFactory
                    .getTransformer(getNativeCrs(), crs, false)
                    .flatMap(
                        crsTransformer -> {
                          try {
                            return Optional.of(crsTransformer.transformBoundingBox(boundingBox));
                          } catch (Exception e) {
                            return Optional.empty();
                          }
                        }));
  }

  @Override
  public Optional<Interval> getTemporalExtent(String typeName) {
    if (!getSourceSchemas().containsKey(typeName)) {
      return Optional.empty();
    }

    if (LOGGER.isDebugEnabled()) {
      Optional.ofNullable(getData().getTypes().get(typeName))
          .flatMap(SchemaBase::getPrimaryInstant)
          .map(SchemaBase::getName)
          .ifPresent(
              temporalProperty ->
                  LOGGER.debug(
                      "Computing temporal extent for '{}.{}'", typeName, temporalProperty));

      Optional.ofNullable(getData().getTypes().get(typeName))
          .flatMap(SchemaBase::getPrimaryInterval)
          .ifPresent(
              temporalProperties ->
                  LOGGER.debug(
                      "Computing temporal extent for '{}.{}' and '{}.{}'",
                      typeName,
                      temporalProperties.first().getName(),
                      typeName,
                      temporalProperties.second().getName()));
    }

    try {
      Stream<Optional<Interval>> extentGraph =
          aggregateStatsReader.getTemporalExtent(getSourceSchemas().get(typeName));

      return extentGraph
          .on(getStreamRunner())
          .run()
          .exceptionally(
              throwable -> {
                LogContext.errorAsWarn(LOGGER, throwable, "Cannot compute temporal extent");
                return Optional.empty();
              })
          .toCompletableFuture()
          .join();
    } catch (Throwable e) {
      // continue
    }

    return Optional.empty();
  }

  @Override
  public MutationResult createFeatures(
      String featureType, FeatureTokenSource featureTokenSource, EpsgCrs crs) {

    return writeFeatures(featureType, featureTokenSource, Optional.empty(), crs, false);
  }

  @Override
  public MutationResult updateFeature(
      String featureType,
      String featureId,
      FeatureTokenSource featureTokenSource,
      EpsgCrs crs,
      boolean partial) {
    return writeFeatures(featureType, featureTokenSource, Optional.of(featureId), crs, partial);
  }

  @Override
  public MutationResult deleteFeature(String featureType, String id) {
    Optional<FeatureSchema> schema = Optional.ofNullable(getData().getTypes().get(featureType));

    if (schema.isEmpty()) {
      throw new IllegalArgumentException(
          String.format("Feature type '%s' not found.", featureType));
    }

    FeatureSchema migrated = schema.get(); // FeatureSchemaNamePathSwapper.migrate(schema.get());

    List<SchemaSql> sqlSchema =
        migrated
            .accept(WITH_SCOPE_MUTATIONS)
            .accept(new MutationSchemaDeriver(pathParser2, pathParser3));

    if (sqlSchema.isEmpty()) {
      throw new IllegalStateException(
          "Mutation mapping could not be derived from provider schema.");
    }

    SchemaSql mutationSchemaSql = sqlSchema.get(0).accept(new MutationSchemaBuilderSql());

    Reactive.Source<String> deletionSource =
        featureMutationsSql.getDeletionSource(mutationSchemaSql, id)
        /*.watchTermination(
        (Function2<NotUsed, CompletionStage<Done>, CompletionStage<MutationResult>>) (notUsed, completionStage) -> completionStage
            .handle((done, throwable) -> {
              return ImmutableMutationResult.builder()
                  .error(Optional.ofNullable(throwable))
                  .build();
            }))*/ ;
    // RunnableGraphWrapper<MutationResult> graph = LogContextStream
    //    .graphWithMdc(deletionSource, Sink.ignore(), Keep.left());

    /*ReactiveStream<SqlRow, SqlRow, MutationResult.Builder, MutationResult> reactiveStream = ImmutableReactiveStream.<SqlRow, SqlRow, MutationResult.Builder, MutationResult>builder()
    .source(ReactiveStream.Source.of(deletionSource))
    .emptyResult(ImmutableMutationResult.builder())
    .build();*/

    RunnableStream<MutationResult> deletionStream =
        deletionSource
            .to(Sink.ignore())
            .withResult(ImmutableMutationResult.builder().type(Type.DELETE).hasFeatures(false))
            .handleError(ImmutableMutationResult.Builder::error)
            .handleItem(ImmutableMutationResult.Builder::addIds)
            .handleEnd(Builder::build)
            .on(getStreamRunner());

    return deletionStream.run().toCompletableFuture().join();
  }

  private MutationResult writeFeatures(
      String featureType,
      FeatureTokenSource featureTokenSource,
      Optional<String> featureId,
      EpsgCrs crs,
      boolean partial) {

    Optional<FeatureSchema> schema = Optional.ofNullable(getData().getTypes().get(featureType));

    if (schema.isEmpty()) {
      throw new IllegalArgumentException(
          String.format("Feature type '%s' not found.", featureType));
    }

    List<SchemaSql> sqlSchema =
        schema
            .get()
            .accept(WITH_SCOPE_MUTATIONS)
            .accept(new MutationSchemaDeriver(pathParser2, pathParser3));

    if (sqlSchema.isEmpty()) {
      throw new IllegalStateException(
          "Mutation mapping could not be derived from provider schema.");
    }

    SchemaSql mutationSchemaSql = sqlSchema.get(0).accept(new MutationSchemaBuilderSql());

    SchemaMappingSql mapping4 =
        new ImmutableSchemaMappingSql.Builder()
            .targetSchema(mutationSchemaSql)
            .sourcePathTransformer(this::applySourcePathDefaults)
            .build();

    Transformer<FeatureSql, String> featureWriter =
        featureId.isPresent()
            ? featureMutationsSql.getUpdaterFlow(mutationSchemaSql, null, featureId.get(), crs)
            : featureMutationsSql.getCreatorFlow(mutationSchemaSql, null, crs);

    ImmutableMutationResult.Builder builder =
        ImmutableMutationResult.builder()
            .type(featureId.isPresent() ? partial ? Type.UPDATE : Type.REPLACE : Type.CREATE)
            .hasFeatures(false);
    FeatureTokenStatsCollector statsCollector = new FeatureTokenStatsCollector(builder, crs);

    Source<FeatureSql> featureSqlSource =
        featureTokenSource
            .via(statsCollector)
            .via(
                new FeatureEncoderSql2(
                    mapping4,
                    partial ? Optional.of(FeatureTransactions.PATCH_NULL_VALUE) : Optional.empty()))
            // TODO: support generic encoders, not only to byte[]
            .via(Transformer.map(feature -> (FeatureSql) feature));

    if (partial) {
      featureSqlSource =
          featureSqlSource.via(
              Transformer.reduce(
                  ModifiableFeatureSql.create(),
                  (a, b) -> a.getProperties().isEmpty() ? b : a.patchWith(b)));
    }

    RunnableStream<MutationResult> mutationStream =
        featureSqlSource
            .via(featureWriter)
            .to(Sink.ignore())
            .withResult((Builder) builder)
            .handleError(
                (result, throwable) -> {
                  Throwable error =
                      throwable instanceof PSQLException || throwable instanceof JsonParseException
                          ? new IllegalArgumentException(throwable.getMessage())
                          : throwable;
                  return result.error(error);
                })
            .handleItem((Builder::addIds))
            .handleEnd(Builder::build)
            .on(getStreamRunner());

    return mutationStream.run().toCompletableFuture().join();
  }

  protected Query preprocessQuery(Query query) {
    if (query instanceof FeatureQuery
        && (((FeatureQuery) query).getFields().size() > 1
            || !"*".equals(((FeatureQuery) query).getFields().get(0)))) {
      FeatureSchema schema = getData().getTypes().get(((FeatureQuery) query).getType());

      List<String> fields =
          ((FeatureQuery) query)
              .getFields().stream()
                  .flatMap(
                      f -> {
                        if (schema.getAllNestedProperties().stream()
                            .map(SchemaBase::getFullPathAsString)
                            .noneMatch(p -> p.equals(f))) {
                          int i = f.lastIndexOf('.') + 1;
                          String pattern = f.substring(0, i) + "[0-9]+_" + f.substring(i);
                          return schema.getAllNestedProperties().stream()
                              .map(SchemaBase::getFullPathAsString)
                              .filter(p -> p.matches(pattern));
                        }
                        return java.util.stream.Stream.of(f);
                      })
                  .collect(Collectors.toList());

      if (!fields.isEmpty()) {
        return ImmutableFeatureQuery.builder().from(query).fields(fields).build();
      }
    }
    if (query instanceof MultiFeatureQuery) {
      // TODO: this is a workaround to support subqueries with different numbers of sort keys
      // to remove this, we have to
      // - disable optimized paging as soon as a sort key is specified for at least one subquery
      // - fix a bug in SqlRowVals or transformations, it seems that the same number of columns is
      // expected for all queries
      List<SubQuery> queries = ((MultiFeatureQuery) query).getQueries();
      OptionalInt maxSortKeys =
          queries.stream().mapToInt(subQuery -> subQuery.getSortKeys().size()).max();

      if (maxSortKeys.orElse(0) > 0) {
        return ImmutableMultiFeatureQuery.builder()
            .from(query)
            .queries(
                queries.stream()
                    .map(
                        subQuery ->
                            ImmutableSubQuery.builder()
                                .from(subQuery)
                                .sortKeys(
                                    subQuery.getSortKeys().size() < maxSortKeys.getAsInt()
                                        ? IntStream.range(0, maxSortKeys.getAsInt())
                                            .mapToObj(i -> SortKey.of(ID_PLACEHOLDER))
                                            .collect(Collectors.toList())
                                        : subQuery.getSortKeys())
                                .build())
                    .collect(Collectors.toList()))
            .build();
      }
    }

    return query;
  }

  @Override
  public FeatureStream getFeatureStream(MultiFeatureQuery query) {
    validateQuery(query);

    Query query2 = preprocessQuery(query);

    return new FeatureStreamImpl(
        query2,
        getData(),
        crsTransformerFactory,
        getCodelists(),
        this::runQuery,
        !query.hitsOnly());
  }

  @Override
  public boolean supportsSorting() {
    return true;
  }

  @Override
  public boolean supportsHighLoad() {
    return true;
  }

  @Override
  public boolean supportsAccenti() {
    if (Objects.nonNull(getData().getQueryGeneration()))
      return getData().getQueryGeneration().getAccentiCollation().isPresent();
    return false;
  }

  @Override
  public boolean supportsIsNull() {
    return true;
  }

  @Override
  public FeatureSchema getQueryablesSchema(
      FeatureSchema schema, List<String> included, List<String> excluded, String pathSeparator) {
    Predicate<String> excludeConnectors = path -> path.matches(".+?\\[[^=\\]]+].+");
    OnlyQueryables queryablesSelector =
        new OnlyQueryables(included, excluded, pathSeparator, excludeConnectors);

    return schema.accept(queryablesSelector);
  }

  @Override
  public FeatureSchema getSortablesSchema(
      FeatureSchema schema, List<String> included, List<String> excluded, String pathSeparator) {
    Predicate<String> excludeConnectors = path -> path.matches(".+?\\[[^=\\]]+].+");
    OnlySortables sortablesSelector =
        new OnlySortables(included, excluded, pathSeparator, excludeConnectors);

    return schema.accept(sortablesSelector);
  }

  @Override
  public boolean supportsCql2() {
    return true;
  }
}
