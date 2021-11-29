/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.fasterxml.jackson.core.JsonParseException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.feature.provider.sql.ImmutableSqlPathSyntax;
import de.ii.xtraplatform.feature.provider.sql.SqlPathSyntax;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql.Dialect;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureProviderSqlData;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableSchemaMappingSql;
import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlClient;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlConnector;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialect;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialectGpkg;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialectPostGis;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlPathDefaults;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlPathParser;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueries;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRow;
import de.ii.xtraplatform.feature.provider.sql.infra.db.SqlTypeInfoValidator;
import de.ii.xtraplatform.features.domain.AbstractFeatureProvider;
import de.ii.xtraplatform.features.domain.ConnectionInfo;
import de.ii.xtraplatform.features.domain.ConnectorFactory;
import de.ii.xtraplatform.features.domain.ExtentReader;
import de.ii.xtraplatform.features.domain.FeatureCrs;
import de.ii.xtraplatform.features.domain.FeatureExtents;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureQueries;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureQueryTransformer;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureStoreAttribute;
import de.ii.xtraplatform.features.domain.FeatureStorePathParser;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.features.domain.FeatureTokenDecoder;
import de.ii.xtraplatform.features.domain.FeatureTokenSource;
import de.ii.xtraplatform.features.domain.FeatureTransactions;
import de.ii.xtraplatform.features.domain.FeatureTransactions.MutationResult.Builder;
import de.ii.xtraplatform.features.domain.ImmutableMutationResult;
import de.ii.xtraplatform.features.domain.SchemaMappingBase;
import de.ii.xtraplatform.features.domain.TypeInfoValidator;
import de.ii.xtraplatform.store.domain.entities.EntityComponent;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.handler.Entity;
import de.ii.xtraplatform.streams.domain.Reactive;
import de.ii.xtraplatform.streams.domain.Reactive.RunnableStream;
import de.ii.xtraplatform.streams.domain.Reactive.Sink;
import de.ii.xtraplatform.streams.domain.Reactive.Stream;
import de.ii.xtraplatform.streams.domain.RunnableGraphWrapper;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.felix.ipojo.annotations.Requires;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

@EntityComponent
@Entity(type = FeatureProvider2.ENTITY_TYPE, subType = FeatureProviderSql.ENTITY_SUB_TYPE, dataClass = FeatureProviderDataV2.class, dataSubClass = FeatureProviderSqlData.class)
public class FeatureProviderSql extends
    AbstractFeatureProvider<SqlRow, SqlQueries, SqlQueryOptions> implements FeatureProvider2,
    FeatureQueries, FeatureExtents, FeatureCrs, FeatureTransactions {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderSql.class);

  static final String ENTITY_SUB_TYPE = "feature/sql";
  public static final String PROVIDER_TYPE = "SQL";

  private final CrsTransformerFactory crsTransformerFactory;
  private final Cql cql;
  private final EntityRegistry entityRegistry;

  private FeatureQueryTransformerSql queryTransformer;
  private ExtentReader extentReader;
  private FeatureMutationsSql featureMutationsSql;
  private FeatureSchemaSwapperSql schemaSwapperSql;
  private FeatureStorePathParser pathParser;
  private PathParserSql pathParser2;
  private SqlPathParser pathParser3;
  private TypeInfoValidator typeInfoValidator;
  private Map<String, Optional<BoundingBox>> spatialExtentCache;
  private Map<String, Optional<Interval>> temporalExtentCache;
  private Map<String, List<SchemaSql>> tableSchemas;

  public FeatureProviderSql(@Requires CrsTransformerFactory crsTransformerFactory,
      @Requires Cql cql,
      @Requires ConnectorFactory connectorFactory,
      @Requires Reactive reactive,
      @Requires EntityRegistry entityRegistry) {
    //TODO: starts akka for every instance, move to singleton
    super(connectorFactory, reactive, crsTransformerFactory);

    this.crsTransformerFactory = crsTransformerFactory;
    this.cql = cql;
    this.entityRegistry = entityRegistry;

  }

  public static FeatureStorePathParser createPathParser(SqlPathDefaults sqlPathDefaults,
      Cql cql) {
    SqlPathSyntax syntax = ImmutableSqlPathSyntax.builder()
        .options(sqlPathDefaults)
        .build();
    return new FeatureStorePathParserSql(syntax, cql);
  }

  private static FeatureSchemaSwapperSql createSchemaSwapper(SqlPathDefaults sqlPathDefaults,
      Cql cql) {
    SqlPathSyntax syntax = ImmutableSqlPathSyntax.builder()
        .options(sqlPathDefaults)
        .build();
    return new FeatureSchemaSwapperSql(syntax, cql);
  }

  private static PathParserSql createPathParser2(SqlPathDefaults sqlPathDefaults, Cql cql) {
    SqlPathSyntax syntax = ImmutableSqlPathSyntax.builder()
        .options(sqlPathDefaults)
        .build();
    return new PathParserSql(syntax, cql);
  }

  private static SqlPathParser createPathParser3(SqlPathDefaults sqlPathDefaults, Cql cql) {
    return new SqlPathParser(sqlPathDefaults, cql);
  }

  @Override
  protected boolean onStartup() throws InterruptedException {
    this.pathParser = createPathParser(getData().getSourcePathDefaults(), cql);
    List<String> validationSchemas = getData().getConnectionInfo().getDialect() == Dialect.PGIS && getData().getConnectionInfo().getSchemas().isEmpty()
        ? ImmutableList.of("public")
        : getData().getConnectionInfo().getSchemas();
    this.typeInfoValidator = new SqlTypeInfoValidator(validationSchemas, this::getSqlClient);

    boolean success = super.onStartup();

    if (!success) {
      return false;
    }

    //TODO: from config
    SqlDialect sqlDialect = getData().getConnectionInfo().getDialect() == Dialect.PGIS ? new SqlDialectPostGis() : new SqlDialectGpkg();
    FilterEncoderSql filterEncoder = new FilterEncoderSql(getData().getNativeCrs()
        .orElse(OgcCrs.CRS84), sqlDialect, crsTransformerFactory, cql);
    FeatureStoreQueryGeneratorSql queryGeneratorSql = new FeatureStoreQueryGeneratorSql(sqlDialect,
        getData().getNativeCrs()
            .orElse(OgcCrs.CRS84), crsTransformerFactory);

    this.pathParser3 = createPathParser3(getData().getSourcePathDefaults(), cql);
    QuerySchemaDeriver querySchemaDeriver = new QuerySchemaDeriver(pathParser3);
    SqlQueryTemplatesDeriver queryTemplatesDeriver = new SqlQueryTemplatesDeriver(filterEncoder, sqlDialect,
        getData().getQueryGeneration().getComputeNumberMatched());

    this.tableSchemas = getData().getTypes().entrySet().stream()
        .map(entry -> new SimpleImmutableEntry<>(entry.getKey(),
            entry.getValue().accept(querySchemaDeriver)))
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    Map<String, List<SqlQueryTemplates>> schemas = getData().getTypes().entrySet().stream()
        .map(entry -> new SimpleImmutableEntry<>(entry.getKey(),
            ImmutableList.of(entry.getValue().accept(querySchemaDeriver).get(0).accept(queryTemplatesDeriver))))
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));

    this.queryTransformer = new FeatureQueryTransformerSql(schemas, getTypeInfos(), queryGeneratorSql,
        getData().getQueryGeneration().getComputeNumberMatched());

    this.extentReader = new ExtentReaderSql(this::getSqlClient, queryGeneratorSql, sqlDialect,
        getData().getNativeCrs()
            .orElse(OgcCrs.CRS84));
    this.featureMutationsSql = new FeatureMutationsSql(this::getSqlClient,
        new SqlInsertGenerator2(getData().getNativeCrs()
            .orElse(OgcCrs.CRS84), crsTransformerFactory,
            getData().getSourcePathDefaults()));
    this.schemaSwapperSql = createSchemaSwapper(getData().getSourcePathDefaults(), cql);
    this.pathParser2 = createPathParser2(getData().getSourcePathDefaults(), cql);
    this.spatialExtentCache = new HashMap<>();
    this.temporalExtentCache = new HashMap<>();

    return true;
  }

  @Override
  protected void onStarted() {
    super.onStarted();
    if (Runtime.getRuntime().availableProcessors() > getStreamRunner().getCapacity()) {
      LOGGER.info("Recommended max connections for optimal performance under load: {}",
          getMaxQueries() * Runtime.getRuntime().availableProcessors());
    }
    Map<String, List<SchemaSql>> sourceSchema = new LinkedHashMap<>();
    try {
      for (FeatureSchema fs : getData().getTypes().values()) {
        sourceSchema
            .put(fs.getName(), fs.accept(new MutationSchemaDeriver(pathParser2, pathParser3)));
      }
    } catch (Throwable e) {
      boolean br = true;
    }
    boolean br = true;
  }

  //TODO: implement auto mode for maxConnections=-1, how to get numberOfQueries in Connector?
  @Override
  protected int getRunnerCapacity(ConnectionInfo connectionInfo) {
    ConnectionInfoSql connectionInfoSql = (ConnectionInfoSql) connectionInfo;

    int maxConnections = connectionInfoSql.getPool().getMaxConnections();

    int runnerCapacity = Runtime.getRuntime()
        .availableProcessors();
    if (maxConnections > 0) {
      for (FeatureStoreTypeInfo typeInfo : getTypeInfos().values()) {
        int numberOfQueries = typeInfo.getInstanceContainers()
            .get(0)
            .getAllAttributesContainers()
            .size();
        int capacity = maxConnections / numberOfQueries;
        //LOGGER.info("{}: {}", typeInfo.getName(), capacity);
        if (capacity >= 0 && capacity < runnerCapacity) {
          runnerCapacity = capacity;
        }
      }
    }
    //LOGGER.info("RUNNER: {}", runnerCapacity);

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
      maxConnections = maxQueries * Runtime.getRuntime()
          .availableProcessors();
    }
    int capacity = maxConnections / maxQueries;
    //TODO
    int queueSize = Math.max(1024, maxConnections * capacity * 2) / maxQueries;
    //LOGGER.info("RUNNERQ: {} {} {} {}", maxQueries ,maxConnections, capacity, queueSize);
    return queueSize;
  }

  private int getMaxQueries() {
    int maxQueries = 0;

    for (FeatureStoreTypeInfo typeInfo : getTypeInfos().values()) {
      int numberOfQueries = typeInfo.getInstanceContainers()
          .get(0)
          .getAllAttributesContainers()
          .size();

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
        int numberOfQueries = typeInfo.getInstanceContainers().get(0).getAllAttributesContainers()
            .size();
        if (numberOfQueries > minRequired) {
          minRequired = numberOfQueries;
        }
      }

      return Optional.of(String
          .format("maxConnections=%d is too low, a minimum of %d is required", maxConnections,
              minRequired));
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

    //TODO: get other infos from connector

    ImmutableMap.Builder<String, String> info = new ImmutableMap.Builder<String, String>()
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
  protected FeatureTokenDecoder<SqlRow> getDecoder(FeatureQuery query) {
    return new FeatureDecoderSql(ImmutableList.of(getTypeInfos().get(query.getType())), tableSchemas.get(query.getType()), getData().getTypes().get(query.getType()), query);
  }

  @Override
  protected Map<String, Codelist> getCodelists() {
    //TODO
    getData().getCodelists();

    return entityRegistry.getEntitiesForType(Codelist.class).stream().map(codelist -> new SimpleImmutableEntry<>(codelist.getId(), codelist)).collect(
        ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public FeatureProviderSqlData getData() {
    return (FeatureProviderSqlData) super.getData();
  }

  @Override
  protected FeatureQueryTransformer<SqlQueries, SqlQueryOptions> getQueryTransformer() {
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
    return super.supportsCrs() && getData().getNativeCrs()
        .isPresent();
  }

  @Override
  public EpsgCrs getNativeCrs() {
    return getData().getNativeCrs()
        .get();
  }

  @Override
  public boolean isCrsSupported(EpsgCrs crs) {
    return Objects.equals(getNativeCrs(), crs) || crsTransformerFactory.isCrsSupported(crs);
  }

  @Override
  public boolean is3dSupported() {
    return crsTransformerFactory.isCrs3d(getNativeCrs());
  }

  @Override
  public Optional<BoundingBox> getSpatialExtent(String typeName) {
    return spatialExtentCache.computeIfAbsent(typeName, ignore -> {
      Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(typeName));

      if (!typeInfo.isPresent()) {
        return Optional.empty();
      }

      // TODO do not use the first spatial attribute; if there is a primary one, use that
      typeInfo.get().getInstanceContainers().get(0).getSpatialAttribute().map(
          FeatureStoreAttribute::getName).ifPresent(spatialProperty -> LOGGER.debug("Computing spatial extent for '{}.{}'", typeName, spatialProperty));

      try {
        Stream<Optional<BoundingBox>> extentGraph = extentReader
            .getExtent(typeInfo.get());

        return extentGraph.on(getStreamRunner()).run()
            .exceptionally(throwable -> Optional.empty())
            .toCompletableFuture()
            .join();
      } catch (Throwable e) {
        //continue
      }

      return Optional.empty();
    });
  }

  @Override
  public Optional<BoundingBox> getSpatialExtent(String typeName, EpsgCrs crs) {
    return spatialExtentCache.computeIfAbsent(typeName + crs.toSimpleString(),
        ignore -> getSpatialExtent(typeName)
            .flatMap(boundingBox -> crsTransformerFactory.getTransformer(getNativeCrs(), crs)
                .flatMap(crsTransformer -> {
                  try {
                    return Optional.of(crsTransformer.transformBoundingBox(boundingBox));
                  } catch (Exception e) {
                    return Optional.empty();
                  }
                })));
  }

  @Override
  public Optional<Interval> getTemporalExtent(String typeName, String property) {
    return temporalExtentCache.computeIfAbsent(typeName + property, ignore -> {
      Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(typeName));

      if (!typeInfo.isPresent()) {
        return Optional.empty();
      }

      LOGGER.debug("Computing temporal extent for '{}.{}'", typeName, property);

      try {
        RunnableGraphWrapper<Optional<Interval>> extentGraph = ((ExtentReaderSql) extentReader)
            .getTemporalExtent(typeInfo.get(), property);

        return computeTemporalExtent(extentGraph);
      } catch (Throwable e) {
        //continue
      }

      return Optional.empty();
    });
  }

  @Override
  public Optional<Interval> getTemporalExtent(String typeName, String startProperty,
      String endProperty) {
    return temporalExtentCache.computeIfAbsent(typeName + startProperty + endProperty, ignore -> {
      Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(typeName));

      if (!typeInfo.isPresent()) {
        return Optional.empty();
      }

      LOGGER.debug("Computing temporal extent for '{}.{}' and '{}.{}'", typeName, startProperty, typeName, endProperty);

      try {
        RunnableGraphWrapper<Optional<Interval>> extentGraph = ((ExtentReaderSql) extentReader)
            .getTemporalExtent(typeInfo.get(), startProperty, endProperty);

        return computeTemporalExtent(extentGraph);
      } catch (Throwable e) {
        //continue
      }

      return Optional.empty();
    });
  }

  private Optional<Interval> computeTemporalExtent(
      RunnableGraphWrapper<Optional<Interval>> extentComputation) {
    return getStreamRunner().run(extentComputation)
        .exceptionally(throwable -> {
          LOGGER.warn("Cannot compute temporal extent: {}",
              Objects.nonNull(throwable.getCause()) ? throwable.getCause().getMessage()
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
  public MutationResult createFeatures(String featureType,
      FeatureTokenSource featureTokenSource) {

    //TODO: where does crs transformation happen?
    // decoder should write source crs to Feature, encoder should transform to target crs
    return writeFeatures(featureType, featureTokenSource, Optional.empty());
  }

  @Override
  public MutationResult updateFeature(String featureType, String featureId, FeatureTokenSource featureTokenSource) {
    return writeFeatures(featureType, featureTokenSource, Optional.of(featureId));
  }

  @Override
  public MutationResult deleteFeature(String featureType, String id) {
    Optional<FeatureSchema> schema = Optional.ofNullable(getData().getTypes()
        .get(featureType));
    Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(featureType));

    if (!schema.isPresent() || !typeInfo.isPresent()) {
      throw new IllegalArgumentException(
          String.format("Feature type '%s' not found.", featureType));
    }

    FeatureSchema migrated = schema.get();//FeatureSchemaNamePathSwapper.migrate(schema.get());

    List<SchemaSql> sqlSchema = migrated.accept(new MutationSchemaDeriver(pathParser2, null));

    if (sqlSchema.isEmpty()) {
      throw new IllegalStateException(
          "Mutation mapping could not be derived from provider schema.");
    }

    SchemaSql mutationSchemaSql = sqlSchema.get(0).accept(new MutationSchemaBuilderSql());

    Source<SqlRow, NotUsed> deletionSource = featureMutationsSql
        .getDeletionSource(mutationSchemaSql, id)
        /*.watchTermination(
            (Function2<NotUsed, CompletionStage<Done>, CompletionStage<MutationResult>>) (notUsed, completionStage) -> completionStage
                .handle((done, throwable) -> {
                  return ImmutableMutationResult.builder()
                      .error(Optional.ofNullable(throwable))
                      .build();
                }))*/;
    //RunnableGraphWrapper<MutationResult> graph = LogContextStream
    //    .graphWithMdc(deletionSource, Sink.ignore(), Keep.left());

    /*ReactiveStream<SqlRow, SqlRow, MutationResult.Builder, MutationResult> reactiveStream = ImmutableReactiveStream.<SqlRow, SqlRow, MutationResult.Builder, MutationResult>builder()
        .source(ReactiveStream.Source.of(deletionSource))
        .emptyResult(ImmutableMutationResult.builder())
        .build();*/

    //TODO: test
    RunnableStream<MutationResult> deletionStream = Reactive.Source.akka(deletionSource)
        .to(Sink.ignore())
        .withResult(ImmutableMutationResult.builder())
        .handleError(ImmutableMutationResult.Builder::error)
        .handleEnd(Builder::build)
        .on(getStreamRunner());

    return deletionStream.run()
        .toCompletableFuture()
        .join();
  }

  private MutationResult writeFeatures(String featureType,
      FeatureTokenSource featureTokenSource,
      Optional<String> featureId) {

    Optional<FeatureSchema> schema = Optional.ofNullable(getData().getTypes()
        .get(featureType));
    Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(featureType));

    if (!schema.isPresent() || !typeInfo.isPresent()) {
      throw new IllegalArgumentException(
          String.format("Feature type '%s' not found.", featureType));
    }

    FeatureSchema migrated = schema.get();//FeatureSchemaNamePathSwapper.migrate(schema.get());

    //SchemaMapping<FeatureSchema> mapping = new ImmutableSchemaMappingSql.Builder().targetSchema(migrated)
    //                                                                              .build();

    //TODO: multiple mappings per path
    //Multimap<List<String>, FeatureSchema> mapping2 = migrated.accept(new SchemaToMappingVisitor<>());

    List<SchemaSql> sqlSchema = migrated.accept(new MutationSchemaDeriver(pathParser2, null));

    if (sqlSchema.isEmpty()) {
      throw new IllegalStateException(
          "Mutation mapping could not be derived from provider schema.");
    }

    //Multimap<List<String>, SchemaSql> mapping3 = sqlSchema.accept(new SchemaToMappingVisitor<>());

    SchemaSql mutationSchemaSql = sqlSchema.get(0).accept(new MutationSchemaBuilderSql());

    SchemaMappingBase<SchemaSql> mapping4 = new ImmutableSchemaMappingSql.Builder()
        .targetSchema(mutationSchemaSql)
        .build();

    //TODO: test
    RunnableStream<MutationResult> mutationStream = featureTokenSource
        //TODO .via(newFeatureObjectBuilder())
        //TODO .via(new FeatureEncoderSql())
        .to(Sink.ignore())
        .withResult((Builder)ImmutableMutationResult.builder())
        .handleError((result, throwable) -> {
          Throwable error = throwable instanceof PSQLException || throwable instanceof JsonParseException
              ? new IllegalArgumentException(throwable.getMessage())
              : throwable;
          return result.error(error);
        })
        //TODO .handleItem(MutationResult.Builder::addIds)
        .handleEnd(Builder::build)
        .on(getStreamRunner());

    return mutationStream.run()
        .toCompletableFuture()
        .join();
  }

  @Override
  public boolean supportsSorting() {
    return true;
  }

  @Override
  public boolean supportsHighLoad() {
    return true;
  }
}
