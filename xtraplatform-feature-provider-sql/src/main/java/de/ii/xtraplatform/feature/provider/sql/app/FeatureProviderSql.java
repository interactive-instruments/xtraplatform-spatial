/**
 * Copyright 2020 interactive instruments GmbH
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
import de.ii.xtraplatform.akka.ActorSystemProvider;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.entity.api.EntityComponent;
import de.ii.xtraplatform.entity.api.handler.Entity;
import de.ii.xtraplatform.feature.provider.api.ConnectorFactory;
import de.ii.xtraplatform.feature.provider.sql.ImmutableSqlPathSyntax;
import de.ii.xtraplatform.feature.provider.sql.SqlPathSyntax;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableSchemaMappingSql;
import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlConnector;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialect;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialectPostGis;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueries;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRow;
import de.ii.xtraplatform.features.domain.AbstractFeatureProvider;
import de.ii.xtraplatform.features.domain.ExtentReader;
import de.ii.xtraplatform.features.domain.FeatureCrs;
import de.ii.xtraplatform.features.domain.FeatureDecoder;
import de.ii.xtraplatform.features.domain.FeatureExtents;
import de.ii.xtraplatform.features.domain.FeatureNormalizer;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV1;
import de.ii.xtraplatform.features.domain.FeatureQueries;
import de.ii.xtraplatform.features.domain.FeatureQueryTransformer;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureStorePathParser;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.features.domain.FeatureTransactions;
import de.ii.xtraplatform.features.domain.FeatureTransformer;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.features.domain.ImmutableMutationResult;
import de.ii.xtraplatform.features.domain.SchemaMapping;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

@EntityComponent
@Entity(type = FeatureProvider2.ENTITY_TYPE, subType = FeatureProviderSql.ENTITY_SUB_TYPE, dataClass = FeatureProviderDataV1.class)
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

    public FeatureProviderSql(@Context BundleContext context,
                              @Requires ActorSystemProvider actorSystemProvider,
                              @Requires CrsTransformerFactory crsTransformerFactory,
                              @Requires Cql cql,
                              @Requires ConnectorFactory connectorFactory,
                              @Property(name = Entity.DATA_KEY) FeatureProviderDataV1 data) {
        //TODO: starts akka for every instance, move to singleton
        super(context, actorSystemProvider, data, createPathParser((ConnectionInfoSql) data.getConnectionInfo(), cql));

        this.crsTransformerFactory = crsTransformerFactory;
        this.connector = (SqlConnector) connectorFactory.createConnector(data);
        //TODO: from config
        SqlDialect sqlDialect = new SqlDialectPostGis();
        this.queryGeneratorSql = new FeatureStoreQueryGeneratorSql(sqlDialect, data.getNativeCrs()
                                                                                   .orElse(OgcCrs.CRS84), crsTransformerFactory);
        this.queryTransformer = new FeatureQueryTransformerSql(getTypeInfos(), queryGeneratorSql, ((ConnectionInfoSql) data.getConnectionInfo()).getComputeNumberMatched());
        this.featureNormalizer = new FeatureNormalizerSql(getTypeInfos(), data.getTypes());
        this.extentReader = new ExtentReaderSql(connector, queryGeneratorSql, sqlDialect, data.getNativeCrs()
                                                                                              .orElse(OgcCrs.CRS84));
        this.featureMutationsSql = new FeatureMutationsSql(connector.getSqlClient(), new SqlInsertGenerator2(data.getNativeCrs()
                                                                                                                 .orElse(OgcCrs.CRS84), crsTransformerFactory));
        this.schemaSwapperSql = createSchemaSwapper((ConnectionInfoSql) data.getConnectionInfo(), cql);
        this.pathParser = createPathParser2((ConnectionInfoSql) data.getConnectionInfo(), cql);
    }

    private static FeatureStorePathParser createPathParser(ConnectionInfoSql connectionInfoSql, Cql cql) {
        SqlPathSyntax syntax = ImmutableSqlPathSyntax.builder()
                                                     .options(connectionInfoSql.getPathSyntax())
                                                     .build();
        return new FeatureStorePathParserSql(syntax, cql);
    }

    private static FeatureSchemaSwapperSql createSchemaSwapper(ConnectionInfoSql connectionInfoSql, Cql cql) {
        SqlPathSyntax syntax = ImmutableSqlPathSyntax.builder()
                                                     .options(connectionInfoSql.getPathSyntax())
                                                     .build();
        return new FeatureSchemaSwapperSql(syntax, cql);
    }

    private static PathParserSql createPathParser2(ConnectionInfoSql connectionInfoSql, Cql cql) {
        SqlPathSyntax syntax = ImmutableSqlPathSyntax.builder()
                                                     .options(connectionInfoSql.getPathSyntax())
                                                     .build();
        return new PathParserSql(syntax, cql);
    }

    @Override
    public FeatureProviderDataV1 getData() {
        return super.getData();
    }

    @Override
    protected FeatureQueryTransformer<SqlQueries> getQueryTransformer() {
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
        return FeatureProvider2.super.supportsCrs() && getData().getNativeCrs()
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
            return extentReader.getExtent(typeInfo.get())
                               .run(getMaterializer())
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
                                                                                          } catch (CrsTransformationException e) {
                                                                                              return Optional.empty();
                                                                                          }
                                                                                      }));
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
        Optional<FeatureType> schema = Optional.ofNullable(getData().getTypes()
                                                                    .get(featureType));
        Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(featureType));

        if (!schema.isPresent() || !typeInfo.isPresent()) {
            throw new IllegalArgumentException(String.format("Feature type '%s' not found.", featureType));
        }

        FeatureSchema migrated = FeatureSchemaNamePathSwapper.migrate(schema.get());

        SchemaSql sqlSchema = migrated.accept(new SchemaBuilderSql(pathParser));

        SchemaSql mutationSchemaSql = sqlSchema.accept(new MutationSchemaBuilderSql());

        RunnableGraph<CompletionStage<MutationResult>> result = featureMutationsSql.getDeletionSource(mutationSchemaSql, id)
                                                                                           .watchTermination((Function2<NotUsed, CompletionStage<Done>, CompletionStage<MutationResult>>) (notUsed, completionStage) -> completionStage.handle((done, throwable) -> {
                                                                                               return ImmutableMutationResult.builder()
                                                                                                                             .error(Optional.ofNullable(throwable))
                                                                                                                             .build();
                                                                                           }))
                                                                                           .toMat(Sink.ignore(), Keep.left());

        return result.run(getMaterializer())
                     .toCompletableFuture()
                     .join();
    }

    private MutationResult writeFeatures(String featureType,
                                         FeatureDecoder.WithSource featureSource,
                                         Optional<String> id) {

        Optional<FeatureType> schema = Optional.ofNullable(getData().getTypes()
                                                                    .get(featureType));
        Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(featureType));

        if (!schema.isPresent() || !typeInfo.isPresent()) {
            throw new IllegalArgumentException(String.format("Feature type '%s' not found.", featureType));
        }

        FeatureSchema migrated = FeatureSchemaNamePathSwapper.migrate(schema.get());

        //SchemaMapping<FeatureSchema> mapping = new ImmutableSchemaMappingSql.Builder().targetSchema(migrated)
        //                                                                              .build();

        //TODO: multiple mappings per path
        //Multimap<List<String>, FeatureSchema> mapping2 = migrated.accept(new SchemaToMappingVisitor<>());

        SchemaSql sqlSchema = migrated.accept(new SchemaBuilderSql(pathParser));

        //Multimap<List<String>, SchemaSql> mapping3 = sqlSchema.accept(new SchemaToMappingVisitor<>());

        SchemaSql mutationSchemaSql = sqlSchema.accept(new MutationSchemaBuilderSql());

        SchemaMapping<SchemaSql> mapping4 = new ImmutableSchemaMappingSql.Builder().targetSchema(mutationSchemaSql)
                                                                                   .build();


        Source<FeatureSql, ?> features = featureSource.decode(mapping4, ModifiableFeatureSql::create, ModifiablePropertySql::create);

        Flow<FeatureSql, String, NotUsed> creator = id.isPresent()
                ? featureMutationsSql.getUpdaterFlow(mutationSchemaSql, getMaterializer().system()
                                                                                         .dispatcher(), id.get())
                : featureMutationsSql.getCreatorFlow(mutationSchemaSql, getMaterializer().system()
                                                                                         .dispatcher());

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
        RunnableGraph<CompletionStage<MutationResult>> result = idSource.toMat(Sink.seq(), FeatureProviderSql::writeIdsToResult);

        return result.run(getMaterializer())
                     .toCompletableFuture()
                     .join();
    }

    private static CompletionStage<MutationResult> writeIdsToResult(CompletionStage<ImmutableMutationResult.Builder> resultStage, CompletionStage<List<String>> idsStage) {
        return resultStage.thenCombine(idsStage, (result, ids) -> result.ids(ids)
                                                                        .build());
    }


}
