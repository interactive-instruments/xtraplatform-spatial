/**
 * Copyright 2022 interactive instruments GmbH
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.app;

import com.fasterxml.jackson.core.JsonParseException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.AbstractFeatureProvider;
import de.ii.xtraplatform.features.domain.AggregateStatsReader;
import de.ii.xtraplatform.features.domain.ConnectionInfo;
import de.ii.xtraplatform.features.domain.ConnectorFactory;
import de.ii.xtraplatform.features.domain.FeatureCrs;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.FeatureExtents;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureQueries;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureQueryEncoder;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureStoreAttribute;
import de.ii.xtraplatform.features.domain.FeatureStorePathParser;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.features.domain.FeatureTokenDecoder;
import de.ii.xtraplatform.features.domain.FeatureTokenSource;
import de.ii.xtraplatform.features.domain.FeatureTransactions;
import de.ii.xtraplatform.features.domain.FeatureTransactions.MutationResult.Builder;
import de.ii.xtraplatform.features.domain.ImmutableMutationResult;
import de.ii.xtraplatform.features.domain.ProviderExtensionRegistry;
import de.ii.xtraplatform.features.domain.SchemaMapping;
import de.ii.xtraplatform.features.domain.TypeInfoValidator;
import de.ii.xtraplatform.features.sql.ImmutableSqlPathSyntax;
import de.ii.xtraplatform.features.sql.SqlPathSyntax;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.features.sql.domain.ConnectionInfoSql.Dialect;
import de.ii.xtraplatform.features.sql.domain.FeatureProviderSqlData;
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
import de.ii.xtraplatform.features.sql.domain.SqlQueries;
import de.ii.xtraplatform.features.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.features.sql.domain.SqlRow;
import de.ii.xtraplatform.features.sql.infra.db.SqlTypeInfoValidator;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.streams.domain.Reactive;
import de.ii.xtraplatform.streams.domain.Reactive.RunnableStream;
import de.ii.xtraplatform.streams.domain.Reactive.Sink;
import de.ii.xtraplatform.streams.domain.Reactive.Stream;
import de.ii.xtraplatform.streams.domain.Reactive.Transformer;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

public class FeatureProviderSql extends AbstractFeatureProvider<SqlRow, SqlQueries, SqlQueryOptions>
    implements FeatureProvider2, FeatureQueries, FeatureExtents, FeatureCrs, FeatureTransactions {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderSql.class);

  public static final String ENTITY_SUB_TYPE = "feature/sql";
  public static final String PROVIDER_TYPE = "SQL";

  private final CrsTransformerFactory crsTransformerFactory;
  private final Cql cql;
  private final EntityRegistry entityRegistry;

  private FeatureQueryEncoderSql queryTransformer;
  private AggregateStatsReader aggregateStatsReader;
  private FeatureMutationsSql featureMutationsSql;
  private FeatureSchemaSwapperSql schemaSwapperSql;
  private FeatureStorePathParser pathParser;
  private PathParserSql pathParser2;
  private SqlPathParser pathParser3;
  private TypeInfoValidator typeInfoValidator;
  private Map<String, List<SchemaSql>> tableSchemas;

  @AssistedInject
  public FeatureProviderSql(
      CrsTransformerFactory crsTransformerFactory,
      Cql cql,
      ConnectorFactory connectorFactory,
      Reactive reactive,
      EntityRegistry entityRegistry,
      ProviderExtensionRegistry extensionRegistry,
      @Assisted FeatureProviderDataV2 data) {
    super(connectorFactory, reactive, crsTransformerFactory, extensionRegistry, data);

    this.crsTransformerFactory = crsTransformerFactory;
    this.cql = cql;
    this.entityRegistry = entityRegistry;
  }

  public static FeatureStorePathParser createPathParser(SqlPathDefaults sqlPathDefaults, Cql cql) {
    SqlPathSyntax syntax = ImmutableSqlPathSyntax.builder().options(sqlPathDefaults).build();
    return new FeatureStorePathParserSql(syntax, cql);
  }

  private static FeatureSchemaSwapperSql createSchemaSwapper(
      SqlPathDefaults sqlPathDefaults, Cql cql) {
    SqlPathSyntax syntax = ImmutableSqlPathSyntax.builder().options(sqlPathDefaults).build();
    return new FeatureSchemaSwapperSql(syntax, cql);
  }

  private static PathParserSql createPathParser2(SqlPathDefaults sqlPathDefaults, Cql cql) {
    SqlPathSyntax syntax = ImmutableSqlPathSyntax.builder().options(sqlPathDefaults).build();
    return new PathParserSql(syntax, cql);
  }

  private static SqlPathParser createPathParser3(SqlPathDefaults sqlPathDefaults, Cql cql) {
    return new SqlPathParser(sqlPathDefaults, cql);
  }

  @Override
  protected boolean onStartup() throws InterruptedException {
    this.pathParser = createPathParser(getData().getSourcePathDefaults(), cql);
    List<String> validationSchemas =
        getData().getConnectionInfo().getDialect() == Dialect.PGIS
                && getData().getConnectionInfo().getSchemas().isEmpty()
            ? ImmutableList.of("public")
            : getData().getConnectionInfo().getSchemas();
    this.typeInfoValidator = new SqlTypeInfoValidator(validationSchemas, this::getSqlClient);

    boolean success = super.onStartup();

    if (!success) {
      return false;
    }

    // TODO: from config
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
            cql,
            accentiCollation);
    FeatureStoreQueryGeneratorSql queryGeneratorSql =
        new FeatureStoreQueryGeneratorSql(
            sqlDialect, getData().getNativeCrs().orElse(OgcCrs.CRS84), crsTransformerFactory);

    this.pathParser3 = createPathParser3(getData().getSourcePathDefaults(), cql);
    QuerySchemaDeriver querySchemaDeriver = new QuerySchemaDeriver(pathParser3);
    SqlQueryTemplatesDeriver queryTemplatesDeriver =
        new SqlQueryTemplatesDeriver(
            filterEncoder, sqlDialect, getData().getQueryGeneration().getComputeNumberMatched());

    this.tableSchemas =
        getData().getTypes().entrySet().stream()
            .map(
                entry ->
                    new SimpleImmutableEntry<>(
                        entry.getKey(), entry.getValue().accept(querySchemaDeriver)))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    Map<String, List<SqlQueryTemplates>> schemas =
        getData().getTypes().entrySet().stream()
            .map(
                entry ->
                    new SimpleImmutableEntry<>(
                        entry.getKey(),
                        ImmutableList.of(
                            entry
                                .getValue()
                                .accept(querySchemaDeriver)
                                .get(0)
                                .accept(queryTemplatesDeriver))))
            .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    this.queryTransformer = new FeatureQueryEncoderSql(schemas, getTypeInfos());

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
    this.schemaSwapperSql = createSchemaSwapper(getData().getSourcePathDefaults(), cql);
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
            fs.getName(), fs.accept(new MutationSchemaDeriver(pathParser2, pathParser3)));
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
      for (FeatureStoreTypeInfo typeInfo : getTypeInfos().values()) {
        int numberOfQueries =
            typeInfo.getInstanceContainers().get(0).getAllAttributesContainers().size();
        int capacity = maxConnections / numberOfQueries;
        // LOGGER.info("{}: {}", typeInfo.getName(), capacity);
        if (capacity >= 0 && capacity < runnerCapacity) {
          runnerCapacity = capacity;
        }
      }
    }
    // LOGGER.info("RUNNER: {}", runnerCapacity);

    return runnerCapacity;
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

  private int getMaxQueries() {
    int maxQueries = 0;

    for (FeatureStoreTypeInfo typeInfo : getTypeInfos().values()) {
      int numberOfQueries =
          typeInfo.getInstanceContainers().get(0).getAllAttributesContainers().size();

      if (numberOfQueries > maxQueries) {
        maxQueries = numberOfQueries;
      }
    }
    return maxQueries <= 0 ? 1 : maxQueries;
  }

  @Override
  protected Optional<String> getRunnerError(ConnectionInfo connectionInfo) {
    if (getStreamRunner().getCapacity() == 0) {
      ConnectionInfoSql connectionInfoSql = (ConnectionInfoSql) connectionInfo;

      int maxConnections = connectionInfoSql.getPool().getMaxConnections();

      int minRequired = 0;

      for (FeatureStoreTypeInfo typeInfo : getTypeInfos().values()) {
        int numberOfQueries =
            typeInfo.getInstanceContainers().get(0).getAllAttributesContainers().size();
        if (numberOfQueries > minRequired) {
          minRequired = numberOfQueries;
        }
      }

      return Optional.of(
          String.format(
              "maxConnections=%d is too low, a minimum of %d is required",
              maxConnections, minRequired));
    }

    return Optional.empty();
  }

  @Override
  protected FeatureStorePathParser getPathParser() {
    return pathParser;
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
  protected Optional<TypeInfoValidator> getTypeInfoValidator() {
    return Optional.ofNullable(typeInfoValidator);
  }

  @Override
  protected FeatureTokenDecoder<
          SqlRow, FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>>
      getDecoder(FeatureQuery query) {
    return new FeatureDecoderSql(
        ImmutableList.of(getTypeInfos().get(query.getType())),
        tableSchemas.get(query.getType()),
        getData().getTypes().get(query.getType()),
        query);
  }

  @Override
  protected Map<String, Codelist> getCodelists() {
    // TODO
    getData().getCodelists();

    return entityRegistry.getEntitiesForType(Codelist.class).stream()
        .map(codelist -> new SimpleImmutableEntry<>(codelist.getId(), codelist))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public FeatureProviderSqlData getData() {
    return (FeatureProviderSqlData) super.getData();
  }

  @Override
  protected FeatureQueryEncoder<SqlQueries, SqlQueryOptions> getQueryEncoder() {
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
    Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(typeName));

    if (!typeInfo.isPresent()) {
      return -1;
    }

    try {
      Stream<Long> countGraph = aggregateStatsReader.getCount(typeInfo.get());

      return countGraph
          .on(getStreamRunner())
          .run()
          .exceptionally(throwable -> -1L)
          .toCompletableFuture()
          .join();
    } catch (Throwable e) {
      // continue
    }

    return -1;
  }

  @Override
  public Optional<BoundingBox> getSpatialExtent(String typeName) {
    Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(typeName));

    if (!typeInfo.isPresent()) {
      return Optional.empty();
    }

    // TODO do not use the first spatial attribute; if there is a primary one, use that
    typeInfo
        .get()
        .getInstanceContainers()
        .get(0)
        .getSpatialAttribute()
        .map(FeatureStoreAttribute::getName)
        .ifPresent(
            spatialProperty ->
                LOGGER.debug("Computing spatial extent for '{}.{}'", typeName, spatialProperty));

    try {
      Stream<Optional<BoundingBox>> extentGraph =
          aggregateStatsReader.getSpatialExtent(typeInfo.get());

      return extentGraph
          .on(getStreamRunner())
          .run()
          .exceptionally(throwable -> Optional.empty())
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
                    .getTransformer(getNativeCrs(), crs, true)
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
  public Optional<Interval> getTemporalExtent(String typeName, String property) {
    Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(typeName));

    if (!typeInfo.isPresent()) {
      return Optional.empty();
    }

    LOGGER.debug("Computing temporal extent for '{}.{}'", typeName, property);

    try {
      Stream<Optional<Interval>> extentGraph =
          aggregateStatsReader.getTemporalExtent(typeInfo.get(), property);

      return computeTemporalExtent(extentGraph);
    } catch (Throwable e) {
      // continue
    }

    return Optional.empty();
  }

  @Override
  public Optional<Interval> getTemporalExtent(
      String typeName, String startProperty, String endProperty) {
    Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(typeName));

    if (!typeInfo.isPresent()) {
      return Optional.empty();
    }

    LOGGER.debug(
        "Computing temporal extent for '{}.{}' and '{}.{}'",
        typeName,
        startProperty,
        typeName,
        endProperty);

    try {
      Stream<Optional<Interval>> extentGraph =
          aggregateStatsReader.getTemporalExtent(typeInfo.get(), startProperty, endProperty);

      return computeTemporalExtent(extentGraph);
    } catch (Throwable e) {
      // continue
    }

    return Optional.empty();
  }

  private Optional<Interval> computeTemporalExtent(Stream<Optional<Interval>> extentComputation) {
    return getStreamRunner()
        .run(extentComputation)
        .exceptionally(
            throwable -> {
              LOGGER.warn(
                  "Cannot compute temporal extent: {}",
                  Objects.nonNull(throwable.getCause())
                      ? throwable.getCause().getMessage()
                      : throwable.getMessage());
              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Stacktrace:", throwable);
              }
              return Optional.empty();
            })
        .toCompletableFuture()
        .join();
  }

  @Override
  public MutationResult createFeatures(String featureType, FeatureTokenSource featureTokenSource) {

    // TODO: where does crs transformation happen?
    // decoder should write source crs to Feature, encoder should transform to target crs
    return writeFeatures(featureType, featureTokenSource, Optional.empty());
  }

  @Override
  public MutationResult updateFeature(
      String featureType, String featureId, FeatureTokenSource featureTokenSource) {
    return writeFeatures(featureType, featureTokenSource, Optional.of(featureId));
  }

  @Override
  public MutationResult deleteFeature(String featureType, String id) {
    Optional<FeatureSchema> schema = Optional.ofNullable(getData().getTypes().get(featureType));
    Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(featureType));

    if (!schema.isPresent() || !typeInfo.isPresent()) {
      throw new IllegalArgumentException(
          String.format("Feature type '%s' not found.", featureType));
    }

    FeatureSchema migrated = schema.get(); // FeatureSchemaNamePathSwapper.migrate(schema.get());

    List<SchemaSql> sqlSchema = migrated.accept(new MutationSchemaDeriver(pathParser2, null));

    if (sqlSchema.isEmpty()) {
      throw new IllegalStateException(
          "Mutation mapping could not be derived from provider schema.");
    }

    SchemaSql mutationSchemaSql = sqlSchema.get(0).accept(new MutationSchemaBuilderSql());

    Reactive.Source<SqlRow> deletionSource =
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

    // TODO: test
    RunnableStream<MutationResult> deletionStream =
        deletionSource
            .to(Sink.ignore())
            .withResult(ImmutableMutationResult.builder())
            .handleError(ImmutableMutationResult.Builder::error)
            .handleEnd(Builder::build)
            .on(getStreamRunner());

    return deletionStream.run().toCompletableFuture().join();
  }

  private MutationResult writeFeatures(
      String featureType, FeatureTokenSource featureTokenSource, Optional<String> featureId) {

    Optional<FeatureSchema> schema = Optional.ofNullable(getData().getTypes().get(featureType));
    Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(featureType));

    if (!schema.isPresent() || !typeInfo.isPresent()) {
      throw new IllegalArgumentException(
          String.format("Feature type '%s' not found.", featureType));
    }

    FeatureSchema migrated = schema.get(); // FeatureSchemaNamePathSwapper.migrate(schema.get());

    // SchemaMapping<FeatureSchema> mapping = new
    // ImmutableSchemaMappingSql.Builder().targetSchema(migrated)
    //                                                                              .build();

    // TODO: multiple mappings per path
    // Multimap<List<String>, FeatureSchema> mapping2 = migrated.accept(new
    // SchemaToMappingVisitor<>());

    List<SchemaSql> sqlSchema = migrated.accept(new MutationSchemaDeriver(pathParser2, null));

    if (sqlSchema.isEmpty()) {
      throw new IllegalStateException(
          "Mutation mapping could not be derived from provider schema.");
    }

    // Multimap<List<String>, SchemaSql> mapping3 = sqlSchema.accept(new
    // SchemaToMappingVisitor<>());

    SchemaSql mutationSchemaSql = sqlSchema.get(0).accept(new MutationSchemaBuilderSql());

    SchemaMappingSql mapping4 =
        new ImmutableSchemaMappingSql.Builder().targetSchema(mutationSchemaSql).build();

    Transformer<FeatureSql, String> featureWriter =
        featureId.isPresent()
            ? featureMutationsSql.getUpdaterFlow(mutationSchemaSql, null, featureId.get())
            : featureMutationsSql.getCreatorFlow(mutationSchemaSql, null);

    RunnableStream<MutationResult> mutationStream =
        featureTokenSource
            .via(new FeatureEncoderSql2(mapping4))
            // TODO: support generic encoders, not only to byte[]
            .via(Transformer.map(feature -> (FeatureSql) feature))
            .via(featureWriter)
            .to(Sink.ignore())
            .withResult((Builder) ImmutableMutationResult.builder())
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
  public boolean supportsCql2() {
    return true;
  }
}
