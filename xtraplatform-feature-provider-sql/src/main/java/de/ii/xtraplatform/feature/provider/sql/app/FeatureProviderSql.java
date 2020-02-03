package de.ii.xtraplatform.feature.provider.sql.app;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import de.ii.xtraplatform.akka.ActorSystemProvider;
import de.ii.xtraplatform.geometries.domain.BoundingBox;
import de.ii.xtraplatform.geometries.domain.CrsTransformerFactory;
import de.ii.xtraplatform.geometries.domain.EpsgCrs;
import de.ii.xtraplatform.entity.api.EntityComponent;
import de.ii.xtraplatform.entity.api.handler.Entity;
import de.ii.xtraplatform.feature.provider.api.AbstractFeatureProvider;
import de.ii.xtraplatform.feature.provider.api.Feature;
import de.ii.xtraplatform.feature.provider.api.FeatureCrs;
import de.ii.xtraplatform.feature.provider.api.FeatureExtents;
import de.ii.xtraplatform.feature.provider.api.FeatureProvider2;
import de.ii.xtraplatform.feature.provider.api.FeatureProviderDataV1;
import de.ii.xtraplatform.feature.provider.api.FeatureQueries;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
import de.ii.xtraplatform.feature.provider.api.FeatureStream2;
import de.ii.xtraplatform.feature.provider.api.FeatureTransformer2;
import de.ii.xtraplatform.feature.provider.api.FeatureTransformer3;
import de.ii.xtraplatform.feature.provider.api.FeatureType;
import de.ii.xtraplatform.feature.provider.sql.ImmutableSqlPathSyntax;
import de.ii.xtraplatform.feature.provider.sql.SqlFeatureTypeParser;
import de.ii.xtraplatform.feature.provider.sql.SqlMappingParser;
import de.ii.xtraplatform.feature.provider.sql.SqlPathSyntax;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableFeatureStoreTypeInfo;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlConnector;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialect;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialectPostGis;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueries;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRow;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

//@Component
//@Provides(properties = {@StaticServiceProperty(name = "providerType", type = "java.lang.String", value = FeatureProviderSql.PROVIDER_TYPE)})
@EntityComponent
@Entity(entityType = FeatureProvider2.class, dataType = FeatureProviderDataV1.class, type = "providers")
public class FeatureProviderSql extends AbstractFeatureProvider implements FeatureProvider2, FeatureQueries, FeatureExtents, FeatureCrs {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderSql.class);

    private static final Config config = ConfigFactory.parseMap(new ImmutableMap.Builder<String, Object>()
            .build());

    private final ActorSystem system;
    private final ActorMaterializer materializer;
    private final CrsTransformerFactory crsTransformerFactory;
    private final SqlConnector connector;
    private final Map<String, FeatureStoreTypeInfo> typeInfos;
    private final FeatureStoreQueryGeneratorSql queryGeneratorSql;
    private final FeatureQueryTransformerSql queryTransformer;
    private final FeatureNormalizerSql featureNormalizer;
    private final ExtentReaderSql extentReader;

    public FeatureProviderSql(@Context BundleContext context,
                              @Requires ActorSystemProvider actorSystemProvider,
                              @Requires CrsTransformerFactory crsTransformerFactory,
                              @Property(name = "data") FeatureProviderDataV1 data,
                              @Property(name = ".connector") SqlConnector sqlConnector) {
        //TODO: starts akka for every instance, move to singleton
        this.system = actorSystemProvider.getActorSystem(context, config);
        this.materializer = ActorMaterializer.create(system);
        this.crsTransformerFactory = crsTransformerFactory;
        this.connector = sqlConnector;
        this.typeInfos = getTypeInfos2(data.getTypes(), ((ConnectionInfoSql)data.getConnectionInfo()).getPathSyntax());
        //TODO: from config
        SqlDialect sqlDialect = new SqlDialectPostGis();
        this.queryGeneratorSql = new FeatureStoreQueryGeneratorSql(new FilterEncoderSqlNewImpl(), sqlDialect);
        this.queryTransformer = new FeatureQueryTransformerSql(typeInfos, queryGeneratorSql, false/*TODO data.computeNumberMatched()*/);
        this.featureNormalizer = new FeatureNormalizerSql(typeInfos, data.getTypes());
        this.extentReader = new ExtentReaderSql(connector, queryGeneratorSql, sqlDialect, data.getNativeCrs());
    }

    @Override
    protected boolean shouldRegister() {
        return connector.isConnected();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!connector.isConnected()) {
            Optional<Throwable> connectionError = connector.getConnectionError();
            String message = connectionError.map(Throwable::getMessage).orElse("unknown reason");
            LOGGER.error("Feature provider with id '{}' could not be started: {}", getId(), message);
            if (connectionError.isPresent() && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Stacktrace:", connectionError.get());
            }
        }
    }

    @Override
    public FeatureProviderDataV1 getData() {
        return super.getData();
    }

    @Override
    public FeatureStream2 getFeatureStream2(FeatureQuery query) {
        return new FeatureStream2() {

            @Override
            public CompletionStage<Result> runWith(FeatureTransformer2 transformer) {
                Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(typeInfos.get(query.getType()));

                if (!typeInfo.isPresent()) {
                    //TODO: put error message into Result, complete successfully
                    CompletableFuture<Result> promise = new CompletableFuture<>();
                    promise.completeExceptionally(new IllegalStateException("No features available for type"));
                    return promise;
                }

                SqlQueries sqlQueries = queryTransformer.transformQuery(query);

                Source<SqlRow, NotUsed> rowStream = connector.getSourceStream(sqlQueries);

                Sink<SqlRow, CompletionStage<Result>> sink = featureNormalizer.normalizeAndTransform(transformer, query);

                return rowStream.runWith(sink, materializer);
            }

            @Override
            public CompletionStage<Result> runWith(Sink<Feature<?>, CompletionStage<Done>> transformer) {
                /*Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(typeInfos.get(query.getType()));

                if (!typeInfo.isPresent()) {
                    //TODO: put error message into Result, complete successfully
                    CompletableFuture<Result> promise = new CompletableFuture<>();
                    promise.completeExceptionally(new IllegalStateException("No features available for type"));
                    return promise;
                }

                SqlQueries sqlQueries = queryTransformer.transformQuery(query);

                Source<SqlRow, NotUsed> rowStream = connector.getSourceStream(sqlQueries);

                Source<Feature<?>, CompletionStage<Result>> featureStream = featureNormalizer.normalize(rowStream, query);

                return featureStream.toMat(transformer, Keep.left())
                                    .run(materializer);*/

                return null;
            }

            @Override
            public <T extends de.ii.xtraplatform.feature.provider.api.Property<?>,U extends Feature<T>> CompletionStage<Result> runWith(FeatureTransformer3<T, U> transformer) {
                Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(typeInfos.get(query.getType()));

                if (!typeInfo.isPresent()) {
                    //TODO: put error message into Result, complete successfully
                    CompletableFuture<Result> promise = new CompletableFuture<>();
                    promise.completeExceptionally(new IllegalStateException("No features available for type"));
                    return promise;
                }

                SqlQueries sqlQueries = queryTransformer.transformQuery(query);

                Source<SqlRow, NotUsed> rowStream = connector.getSourceStream(sqlQueries);

                Source<U, CompletionStage<Result>> featureStream = featureNormalizer.normalize(rowStream, query, transformer::createFeature, transformer::createProperty);

                Sink<U, CompletionStage<Done>> sink = Sink.foreach(feature -> {

            /*if (!numberReturned.isDone() && feature.getProperties().containsKey(ImmutableList.of("numberReturned"))) {
                numberReturned.complete(Long.getLong(feature.getProperties().get(ImmutableList.of("numberReturned"))));
            }*/

                    transformer.processFeature(feature);
                });

                return featureStream.toMat(sink, Keep.left())
                                    .run(materializer);
            }
        };
    }

    //TODO
    @Override
    public long getFeatureCount(FeatureQuery featureQuery) {
        return 0;
    }

    @Override
    public boolean isCrsSupported(EpsgCrs crs) {
        return getData().getNativeCrs()
                        .equals(crs) || crsTransformerFactory.isCrsSupported(crs);
    }

    @Override
    public boolean is3dSupported() {
        return crsTransformerFactory.isCrs3d(getData().getNativeCrs());
    }

    //TODO: from data.getTypes()
    //TODO: move to derived in data?
    private Map<String, FeatureStoreTypeInfo> getTypeInfos(Map<String, FeatureTypeMapping> mappings) {
        //TODO: options from data
        SqlPathSyntax syntax = ImmutableSqlPathSyntax.builder()
                                                     .build();
        SqlMappingParser mappingParser = new SqlMappingParser(syntax);
        FeatureStorePathParser pathParser = new FeatureStorePathParser(syntax);

        return mappings.entrySet()
                       .stream()
                       .map(entry -> {
                           List<String> paths = mappingParser.parse(entry.getValue()
                                                                         .getMappings());
                           List<FeatureStoreInstanceContainer> instanceContainers = pathParser.parse(paths);
                           FeatureStoreTypeInfo typeInfo = ImmutableFeatureStoreTypeInfo.builder()
                                                                                        .name(entry.getKey())
                                                                                        .instanceContainers(instanceContainers)
                                                                                        .build();

                           return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), typeInfo);
                       })
                       .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, FeatureStoreTypeInfo> getTypeInfos2(Map<String, FeatureType> featureTypes, SqlPathSyntax.Options syntaxOptions) {
        SqlPathSyntax syntax = ImmutableSqlPathSyntax.builder()
                                                     .options(syntaxOptions)
                                                     .build();
        SqlFeatureTypeParser mappingParser = new SqlFeatureTypeParser(syntax);
        FeatureStorePathParser pathParser = new FeatureStorePathParser(syntax);

        return featureTypes.entrySet()
                           .stream()
                           .map(entry -> {
                               List<String> paths = mappingParser.parse(entry.getValue());
                               List<FeatureStoreInstanceContainer> instanceContainers = pathParser.parse(paths);
                               FeatureStoreTypeInfo typeInfo = ImmutableFeatureStoreTypeInfo.builder()
                                                                                            .name(entry.getKey())
                                                                                            .instanceContainers(instanceContainers)
                                                                                            .build();

                               return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), typeInfo);
                           })
                           .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    //TODO: crs as second param, transform here? yup
    @Override
    public BoundingBox getSpatialExtent(String typeName) {
        Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(typeInfos.get(typeName));
        //TODO: immutable
        //TODO: really provide fallback, or better move to caller?
        //TODO: has to be in native crs, otherwise it will not work
        BoundingBox boundingBoxFallback = new BoundingBox(-180.0D, -90.0D, 180.0D, 90.0D, EpsgCrs.of(4326));

        if (!typeInfo.isPresent()) {
            return boundingBoxFallback;
        }

        return extentReader.getExtent(typeInfo.get())
                           .run(materializer)
                           .exceptionally(throwable -> Optional.of(boundingBoxFallback))
                           .toCompletableFuture()
                           .join()
                           .orElse(boundingBoxFallback);
    }
}
