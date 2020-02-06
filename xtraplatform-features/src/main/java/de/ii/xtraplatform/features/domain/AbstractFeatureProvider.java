package de.ii.xtraplatform.features.domain;

import akka.Done;
import akka.NotUsed;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import de.ii.xtraplatform.entity.api.AbstractPersistentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public abstract class AbstractFeatureProvider<T,U,V extends FeatureProviderConnector.QueryOptions> extends AbstractPersistentEntity<FeatureProviderDataV1> implements FeatureProvider2, FeatureQueries {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFeatureProvider.class);

    private final ActorMaterializer materializer;

    protected AbstractFeatureProvider(ActorMaterializer materializer) {
        this.materializer = materializer;
    }

    @Override
    public FeatureProviderDataV1 getData() {
        return super.getData();
    }

    @Override
    protected boolean shouldRegister() {
        return getConnector().isConnected();
    }

    @Override
    protected void onStart() {
        if (!getConnector().isConnected()) {
            Optional<Throwable> connectionError = getConnector().getConnectionError();
            String message = connectionError.map(Throwable::getMessage).orElse("unknown reason");
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

    @Override
    public FeatureStream2 getFeatureStream2(FeatureQuery query) {
        return new FeatureStream2() {

            @Override
            public CompletionStage<Result> runWith(FeatureTransformer2 transformer) {
                Optional<FeatureStoreTypeInfo> typeInfo = Optional.empty();//Optional.ofNullable(typeInfos.get(query.getType()));

                if (!typeInfo.isPresent()) {
                    //TODO: put error message into Result, complete successfully
                    CompletableFuture<Result> promise = new CompletableFuture<>();
                    promise.completeExceptionally(new IllegalStateException("No features available for type"));
                    return promise;
                }

                U sqlQueries = getQueryTransformer().transformQuery(query);

                Source<T, NotUsed> rowStream = getConnector().getSourceStream(sqlQueries);

                Sink<T, CompletionStage<Result>> sink = getNormalizer().normalizeAndTransform(transformer, query);

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
            public <W extends Property<?>,X extends Feature<W>> CompletionStage<Result> runWith(FeatureTransformer3<W, X> transformer) {
                Optional<FeatureStoreTypeInfo> typeInfo = Optional.empty();//Optional.ofNullable(typeInfos.get(query.getType()));

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
