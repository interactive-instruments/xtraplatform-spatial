package de.ii.xtraplatform.features.domain;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import de.ii.xtraplatform.akka.ActorSystemProvider;
import de.ii.xtraplatform.entity.api.AbstractPersistentEntity;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public abstract class AbstractFeatureProvider<T,U,V extends FeatureProviderConnector.QueryOptions> extends AbstractPersistentEntity<FeatureProviderDataV1> implements FeatureProvider2, FeatureQueries {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFeatureProvider.class);

    private static final Config config = ConfigFactory.parseMap(new ImmutableMap.Builder<String, Object>()
            .build());

    private final ActorMaterializer materializer;
    private Map<String, FeatureStoreTypeInfo> typeInfos;

    protected AbstractFeatureProvider(BundleContext context,
                                      ActorSystemProvider actorSystemProvider) {
        ActorSystem system = actorSystemProvider.getActorSystem(context, config);
        this.materializer = ActorMaterializer.create(system);
    }

    @Override
    protected boolean shouldRegister() {
        return getConnector().isConnected();
    }

    @Override
    protected void onStart() {
        if (!getConnector().isConnected()) {
            Optional<Throwable> connectionError = getConnector().getConnectionError();
            String message = connectionError.map(Throwable::getMessage)
                                            .orElse("unknown reason");
            LOGGER.error("Feature provider with id '{}' could not be started: {}", getId(), message);
            if (connectionError.isPresent() && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Stacktrace:", connectionError.get());
            }
        }
    }

    protected abstract FeatureStorePathParser getPathParser();

    protected abstract FeatureQueryTransformer<U> getQueryTransformer();

    protected abstract FeatureProviderConnector<T, U, V> getConnector();

    protected abstract FeatureNormalizer<T> getNormalizer();

    protected Map<String, FeatureStoreTypeInfo> getTypeInfos() {
        if (Objects.isNull(typeInfos)) {
            this.typeInfos = createTypeInfos(getData().getTypes());
        }

        return typeInfos;
    }

    protected Materializer getMaterializer() {
        return materializer;
    }

    private Map<String, FeatureStoreTypeInfo> createTypeInfos(Map<String, FeatureType> featureTypes) {
        return featureTypes.entrySet()
                           .stream()
                           .map(entry -> {
                               List<FeatureStoreInstanceContainer> instanceContainers = getPathParser().parse(entry.getValue());
                               FeatureStoreTypeInfo typeInfo = ImmutableFeatureStoreTypeInfo.builder()
                                                                                            .name(entry.getKey())
                                                                                            .instanceContainers(instanceContainers)
                                                                                            .build();

                               return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), typeInfo);
                           })
                           .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
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

                U transformedQuery = getQueryTransformer().transformQuery(query);

                Source<T, NotUsed> sourceStream = getConnector().getSourceStream(transformedQuery);

                Sink<T, CompletionStage<Result>> sink = getNormalizer().normalizeAndTransform(transformer, query);

                return sourceStream.runWith(sink, materializer);
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
            public <W extends Property<?>,X extends Feature<W>> CompletionStage<Result> runWith(FeatureTransformer3<W, X> transformer) {
                Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(typeInfos.get(query.getType()));

                if (!typeInfo.isPresent()) {
                    //TODO: put error message into Result, complete successfully
                    CompletableFuture<Result> promise = new CompletableFuture<>();
                    promise.completeExceptionally(new IllegalStateException("No features available for type"));
                    return promise;
                }

                U sqlQueries = getQueryTransformer().transformQuery(query);

                Source<T, NotUsed> rowStream = getConnector().getSourceStream(sqlQueries);

                Source<X, CompletionStage<Result>> featureStream = getNormalizer().normalize(rowStream, query, transformer::createFeature, transformer::createProperty);

                Sink<X, CompletionStage<Done>> sink = Sink.foreach(feature -> {

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

}
