/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import akka.Done;
import akka.NotUsed;
import akka.japi.function.Function2;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.fasterxml.jackson.core.JsonParseException;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.feature.provider.api.ConnectorFactory;
import de.ii.xtraplatform.feature.provider.sql.ImmutableSqlPathSyntax;
import de.ii.xtraplatform.feature.provider.sql.SqlPathSyntax;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableSchemaMappingSql;
import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlConnector;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialect;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialectPostGis;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlPathParser;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueries;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRow;
import de.ii.xtraplatform.feature.provider.sql.infra.db.SqlTypeInfoValidator;
import de.ii.xtraplatform.features.domain.AbstractFeatureProvider;
import de.ii.xtraplatform.features.domain.ExtentReader;
import de.ii.xtraplatform.features.domain.FeatureCrs;
import de.ii.xtraplatform.features.domain.FeatureDecoder;
import de.ii.xtraplatform.features.domain.FeatureExtents;
import de.ii.xtraplatform.features.domain.FeatureNormalizer;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureQueries;
import de.ii.xtraplatform.features.domain.FeatureQueryTransformer;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureSchemaToTypeVisitor;
import de.ii.xtraplatform.features.domain.FeatureStorePathParser;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.features.domain.FeatureTransactions;
import de.ii.xtraplatform.features.domain.FeatureTransformer;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.features.domain.ImmutableMutationResult;
import de.ii.xtraplatform.features.domain.SchemaMapping;
import de.ii.xtraplatform.features.domain.TypeInfoValidator;
import de.ii.xtraplatform.store.domain.entities.EntityComponent;
import de.ii.xtraplatform.store.domain.entities.handler.Entity;
import de.ii.xtraplatform.streams.domain.ActorSystemProvider;
import de.ii.xtraplatform.streams.domain.LogContextStream;
import de.ii.xtraplatform.streams.domain.RunnableGraphWithMdc;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletionException;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

@EntityComponent
@Entity(type = FeatureProvider2.ENTITY_TYPE, subType = FeatureProviderSql.ENTITY_SUB_TYPE, dataClass = FeatureProviderDataV2.class, dataSubClass = FeatureProviderDataV2.class)
public class FeatureProviderSql extends AbstractFeatureProvider<SqlRow, SqlQueries, SqlQueryOptions> implements FeatureProvider2, FeatureQueries, FeatureExtents, FeatureCrs, FeatureTransactions {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderSql.class);

    static final String ENTITY_SUB_TYPE = "feature/sql";
    public static final String PROVIDER_TYPE = "SQL";

    private final CrsTransformerFactory crsTransformerFactory;
    private final SqlConnector connector;
    private final FeatureStoreQueryGeneratorSql queryGeneratorSql;
    private final FeatureQueryTransformerSql queryTransformer;
    private final FeatureNormalizerSql featureNormalizer;
    private final ExtentReader extentReader;
    private final FeatureMutationsSql featureMutationsSql;
    private final FeatureSchemaSwapperSql schemaSwapperSql;
    private final PathParserSql pathParser;
    private final SqlPathParser pathParser3;
    private final TypeInfoValidator typeInfoValidator;

    public FeatureProviderSql(@Context BundleContext context,
                              @Requires ActorSystemProvider actorSystemProvider,
                              @Requires CrsTransformerFactory crsTransformerFactory,
                              @Requires Cql cql,
                              @Requires ConnectorFactory connectorFactory,
                              @Property(name = Entity.DATA_KEY) FeatureProviderDataV2 data) {
        //TODO: starts akka for every instance, move to singleton
        super(context, actorSystemProvider, data, createPathParser((ConnectionInfoSql) data.getConnectionInfo(), cql));

        this.crsTransformerFactory = crsTransformerFactory;
        this.connector = (SqlConnector) connectorFactory.createConnector(data);
        //TODO: from config
        SqlDialect sqlDialect = new SqlDialectPostGis();
        this.queryGeneratorSql = new FeatureStoreQueryGeneratorSql(sqlDialect, data.getNativeCrs()
                                                                                   .orElse(OgcCrs.CRS84), crsTransformerFactory);
        this.queryTransformer = new FeatureQueryTransformerSql(getTypeInfos(), queryGeneratorSql, ((ConnectionInfoSql) data.getConnectionInfo()).getComputeNumberMatched());

        Map<String, FeatureType> types = data.getTypes()
                                             .entrySet()
                                             .stream()
                                             .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue()
                                                                                                                       .accept(new FeatureSchemaToTypeVisitor(entry.getKey()))))
                                             .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        this.featureNormalizer = new FeatureNormalizerSql(getTypeInfos(), types);
        this.extentReader = new ExtentReaderSql(connector, queryGeneratorSql, sqlDialect, data.getNativeCrs()
                                                                                              .orElse(OgcCrs.CRS84));
        this.featureMutationsSql = new FeatureMutationsSql(connector.getSqlClient(), new SqlInsertGenerator2(data.getNativeCrs()
                                                                                                                 .orElse(OgcCrs.CRS84), crsTransformerFactory, ((ConnectionInfoSql)data.getConnectionInfo()).getSourcePathDefaults()));
        this.schemaSwapperSql = createSchemaSwapper((ConnectionInfoSql) data.getConnectionInfo(), cql);
        this.pathParser = createPathParser2((ConnectionInfoSql) data.getConnectionInfo(), cql);
        this.pathParser3 = createPathParser3((ConnectionInfoSql) data.getConnectionInfo(), cql);
        this.typeInfoValidator = new SqlTypeInfoValidator(((ConnectionInfoSql) data.getConnectionInfo()).getSchemas(), connector.getSqlClient());
    }

    public static FeatureStorePathParser createPathParser(ConnectionInfoSql connectionInfoSql, Cql cql) {
        SqlPathSyntax syntax = ImmutableSqlPathSyntax.builder()
                                                     .options(connectionInfoSql.getSourcePathDefaults())
                                                     .build();
        return new FeatureStorePathParserSql(syntax, cql);
    }

    private static FeatureSchemaSwapperSql createSchemaSwapper(ConnectionInfoSql connectionInfoSql, Cql cql) {
        SqlPathSyntax syntax = ImmutableSqlPathSyntax.builder()
                                                     .options(connectionInfoSql.getSourcePathDefaults())
                                                     .build();
        return new FeatureSchemaSwapperSql(syntax, cql);
    }

    private static PathParserSql createPathParser2(ConnectionInfoSql connectionInfoSql, Cql cql) {
        SqlPathSyntax syntax = ImmutableSqlPathSyntax.builder()
                                                     .options(connectionInfoSql.getSourcePathDefaults())
                                                     .build();
        return new PathParserSql(syntax, cql);
    }

    private static SqlPathParser createPathParser3(ConnectionInfoSql connectionInfoSql, Cql cql) {
        return new SqlPathParser(connectionInfoSql.getSourcePathDefaults(), cql);
    }


    @Override
    protected void onStarted() {
        super.onStarted();
        if (Runtime.getRuntime().availableProcessors() > getStreamRunner().getCapacity()) {
            LOGGER.info("Recommended max connections for optimal performance under load: {}", getMaxQueries() * Runtime.getRuntime().availableProcessors());
        }
        Map<String, List<SchemaSql>> sourceSchema = new LinkedHashMap<>();
        try{
        for (FeatureSchema fs: getData().getTypes().values()) {
            sourceSchema.put(fs.getName(), fs.accept(new MutationSchemaDeriver(pathParser, pathParser3)));
        }
        } catch (Throwable e) {
            boolean br = true;
        }
        boolean br = true;
    }

    //TODO: implement auto mode for maxConnections=-1, how to get numberOfQueries in Connector?
    @Override
    protected int getRunnerCapacity(FeatureProviderDataV2 data) {
        ConnectionInfoSql connectionInfo = (ConnectionInfoSql) data.getConnectionInfo();

        int maxConnections = connectionInfo.getMaxConnections();

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
    protected int getRunnerQueueSize(FeatureProviderDataV2 data) {
        ConnectionInfoSql connectionInfo = (ConnectionInfoSql) data.getConnectionInfo();

        int maxQueries = getMaxQueries();

        int maxConnections;
        if (connectionInfo.getMaxConnections() > 0) {
            maxConnections = connectionInfo.getMaxConnections();
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
    protected Optional<String> getRunnerError(FeatureProviderDataV2 data) {
        if (getStreamRunner().getCapacity() == 0) {
            ConnectionInfoSql connectionInfo = (ConnectionInfoSql) data.getConnectionInfo();

            int maxConnections = connectionInfo.getMaxConnections();

            int minRequired = 0;

            for (FeatureStoreTypeInfo typeInfo : getTypeInfos().values()) {
                int numberOfQueries = typeInfo.getInstanceContainers().get(0).getAllAttributesContainers().size();
                if (numberOfQueries > minRequired) {
                    minRequired = numberOfQueries;
                }
            }

            return Optional.of(String.format("maxConnections=%d is too low, a minimum of %d is required", maxConnections, minRequired));
        }

        return Optional.empty();
    }

    @Override
    protected Optional<Map<String, String>> getStartupInfo() {
        String parallelism = String.valueOf(getStreamRunner().getCapacity());

        //TODO: get other infos from connector

        return Optional.of(ImmutableMap.of(
                "min connections", String.valueOf(connector.getMinConnections()),
                "max connections", String.valueOf(connector.getMaxConnections()),
                "stream capacity", parallelism)
        );
    }

    @Override
    protected Optional<TypeInfoValidator> getTypeInfoValidator() {
        return Optional.ofNullable(typeInfoValidator);
    }

    @Override
    public FeatureProviderDataV2 getData() {
        return super.getData();
    }

    @Override
    protected FeatureQueryTransformer<SqlQueries, SqlQueryOptions> getQueryTransformer() {
        return queryTransformer;
    }

    @Override
    protected FeatureProviderConnector<SqlRow, SqlQueries, SqlQueryOptions> getConnector() {
        return connector;
    }

    @Override
    protected FeatureNormalizer<SqlRow> getNormalizer() {
        return featureNormalizer;
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
        Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(typeName));

        if (!typeInfo.isPresent()) {
            return Optional.empty();
        }

        try {
            RunnableGraphWithMdc<CompletionStage<Optional<BoundingBox>>> extentGraph = extentReader.getExtent(typeInfo.get());
            return getStreamRunner().run(extentGraph)
                                    .exceptionally(throwable -> Optional.empty())
                                    .toCompletableFuture()
                                    .join();
        } catch (Throwable e) {
            //continue
        }

        return Optional.empty();
    }

    @Override
    public Optional<BoundingBox> getSpatialExtent(String typeName, EpsgCrs crs) {
        return getSpatialExtent(typeName).flatMap(boundingBox -> crsTransformerFactory.getTransformer(getNativeCrs(), crs)
                                                                                      .flatMap(crsTransformer -> {
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

        try {
            RunnableGraphWithMdc<CompletionStage<Optional<Interval>>> extentGraph = ((ExtentReaderSql) extentReader).getTemporalExtent(typeInfo.get(), property);

            return computeTemporalExtent(extentGraph);
        } catch (Throwable e) {
            //continue
        }

        return Optional.empty();
    }

    @Override
    public Optional<Interval> getTemporalExtent(String typeName, String startProperty, String endProperty) {
        Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(typeName));

        if (!typeInfo.isPresent()) {
            return Optional.empty();
        }

        try {
            RunnableGraphWithMdc<CompletionStage<Optional<Interval>>> extentGraph = ((ExtentReaderSql) extentReader).getTemporalExtent(typeInfo.get(), startProperty, endProperty);

            return computeTemporalExtent(extentGraph);
        } catch (Throwable e) {
            //continue
        }

        return Optional.empty();
    }

    private Optional<Interval> computeTemporalExtent(RunnableGraphWithMdc<CompletionStage<Optional<Interval>>> extentComputation) {
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
    public List<String> addFeaturesFromStream(String featureType, CrsTransformer crsTransformer,
                                              Function<FeatureTransformer, RunnableGraph<CompletionStage<Done>>> stream) {
        return null;
    }


    @Override
    public MutationResult createFeatures(String featureType,
                                         FeatureDecoder.WithSource featureSource) {

        //TODO: where does crs transformation happen?
        // decoder should write source crs to Feature, encoder should transform to target crs
        return writeFeatures(featureType, featureSource, Optional.empty());
    }

    @Override
    public MutationResult updateFeature(String featureType, FeatureDecoder.WithSource featureSource, String id) {

        //TODO:
        return writeFeatures(featureType, featureSource, Optional.of(id));
    }

    @Override
    public void updateFeatureFromStream(String featureType, String id, CrsTransformer crsTransformer,
                                        Function<FeatureTransformer, RunnableGraph<CompletionStage<Done>>> stream) {

    }

    @Override
    public MutationResult deleteFeature(String featureType, String id) {
        Optional<FeatureSchema> schema = Optional.ofNullable(getData().getTypes()
                                                                      .get(featureType));
        Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(featureType));

        if (!schema.isPresent() || !typeInfo.isPresent()) {
            throw new IllegalArgumentException(String.format("Feature type '%s' not found.", featureType));
        }

        FeatureSchema migrated = schema.get();//FeatureSchemaNamePathSwapper.migrate(schema.get());

        List<SchemaSql> sqlSchema = migrated.accept(new MutationSchemaDeriver(pathParser, null));

        if (sqlSchema.isEmpty()) {
            throw new IllegalStateException("Mutation mapping could not be derived from provider schema.");
        }

        SchemaSql mutationSchemaSql = sqlSchema.get(0).accept(new MutationSchemaBuilderSql());

        Source<SqlRow, CompletionStage<MutationResult>> deletionSource = featureMutationsSql.getDeletionSource(mutationSchemaSql, id)
                                                                                             .watchTermination((Function2<NotUsed, CompletionStage<Done>, CompletionStage<MutationResult>>) (notUsed, completionStage) -> completionStage.handle((done, throwable) -> {
                                                                                                 return ImmutableMutationResult.builder()
                                                                                                                               .error(Optional.ofNullable(throwable))
                                                                                                                               .build();
                                                                                             }));
        RunnableGraphWithMdc<CompletionStage<MutationResult>> graph = LogContextStream.graphWithMdc(deletionSource, Sink.ignore(), Keep.left());

        return getStreamRunner().run(graph)
                                .toCompletableFuture()
                                .join();
    }

    private MutationResult writeFeatures(String featureType,
                                         FeatureDecoder.WithSource featureSource,
                                         Optional<String> id) {

        Optional<FeatureSchema> schema = Optional.ofNullable(getData().getTypes()
                                                                      .get(featureType));
        Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(featureType));

        if (!schema.isPresent() || !typeInfo.isPresent()) {
            throw new IllegalArgumentException(String.format("Feature type '%s' not found.", featureType));
        }

        FeatureSchema migrated = schema.get();//FeatureSchemaNamePathSwapper.migrate(schema.get());

        //SchemaMapping<FeatureSchema> mapping = new ImmutableSchemaMappingSql.Builder().targetSchema(migrated)
        //                                                                              .build();

        //TODO: multiple mappings per path
        //Multimap<List<String>, FeatureSchema> mapping2 = migrated.accept(new SchemaToMappingVisitor<>());

        List<SchemaSql> sqlSchema = migrated.accept(new MutationSchemaDeriver(pathParser, null));

        if (sqlSchema.isEmpty()) {
            throw new IllegalStateException("Mutation mapping could not be derived from provider schema.");
        }

        //Multimap<List<String>, SchemaSql> mapping3 = sqlSchema.accept(new SchemaToMappingVisitor<>());

        SchemaSql mutationSchemaSql = sqlSchema.get(0).accept(new MutationSchemaBuilderSql());

        SchemaMapping<SchemaSql> mapping4 = new ImmutableSchemaMappingSql.Builder().targetSchema(mutationSchemaSql)
                                                                                   .build();


        Source<FeatureSql, ?> features = featureSource.decode(mapping4, ModifiableFeatureSql::create, ModifiablePropertySql::create);

        Flow<FeatureSql, String, NotUsed> creator = id.isPresent()
                ? featureMutationsSql.getUpdaterFlow(mutationSchemaSql, getStreamRunner().getDispatcher(), id.get())
                : featureMutationsSql.getCreatorFlow(mutationSchemaSql, getStreamRunner().getDispatcher());

        Sink<String, CompletionStage<MutationResult>> of = Flow.of(String.class)
                                                               .watchTermination((Function2<NotUsed, CompletionStage<Done>, CompletionStage<ImmutableMutationResult.Builder>>) (notUsed, completionStage) -> completionStage.handle((done, throwable) -> {
                                                                   return ImmutableMutationResult.builder()
                                                                                                 .error(Optional.ofNullable(throwable));
                                                               }))
                                                               .toMat(Sink.seq(), FeatureProviderSql::writeIdsToResult);

        //combined
        Source<String, CompletionStage<ImmutableMutationResult.Builder>> idSource = features.viaMat(creator, Keep.right())
                                                                                            //TODO: only catches errors from downstream
                                                                                            .watchTermination((Function2<NotUsed, CompletionStage<Done>, CompletionStage<ImmutableMutationResult.Builder>>) (notUsed, completionStage) -> completionStage.handle((done, throwable) -> {
                                                                                                return ImmutableMutationResult.builder()
                                                                                                                              .error(Optional.ofNullable(throwable));
                                                                                            }));

        // result is
        RunnableGraphWithMdc<CompletionStage<MutationResult>> graph = LogContextStream.graphWithMdc(idSource, Sink.seq(), FeatureProviderSql::writeIdsToResult);

        return getStreamRunner().run(graph)
            .exceptionally(throwable -> {
                Throwable error = throwable.getCause() instanceof PSQLException
                    || throwable.getCause() instanceof JsonParseException
                    ? new IllegalArgumentException(throwable.getCause().getMessage())
                    : throwable.getCause();

                return ImmutableMutationResult.builder()
                    .error(Optional.ofNullable(error))
                    .build();
            })
                                .toCompletableFuture()
                                .join();
    }

    private static CompletionStage<MutationResult> writeIdsToResult(CompletionStage<ImmutableMutationResult.Builder> resultStage, CompletionStage<List<String>> idsStage) {
        return resultStage.thenCombine(idsStage, (result, ids) -> result.ids(ids)
                                                                        .build());
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
