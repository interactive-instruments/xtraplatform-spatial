/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.runtime.domain.LogContext;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import de.ii.xtraplatform.streams.domain.ActorSystemProvider;
import de.ii.xtraplatform.store.domain.entities.AbstractPersistentEntity;
import de.ii.xtraplatform.streams.domain.StreamRunner;
import java.io.IOException;
import java.util.Objects;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public abstract class AbstractFeatureProvider<T,U,V extends FeatureProviderConnector.QueryOptions> extends AbstractPersistentEntity<FeatureProviderDataV2> implements FeatureProvider2, FeatureQueries {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFeatureProvider.class);

    private final BundleContext context;
    private final ActorSystemProvider actorSystemProvider;
    private final ConnectorFactory connectorFactory;
    private StreamRunner streamRunner;
    private Map<String, FeatureStoreTypeInfo> typeInfos;
    private FeatureProviderConnector<T, U, V> connector;

    protected AbstractFeatureProvider(BundleContext context,
        ActorSystemProvider actorSystemProvider, ConnectorFactory connectorFactory) {
        this.context = context;
        this.actorSystemProvider = actorSystemProvider;
        this.connectorFactory = connectorFactory;
    }

    @Override
    protected boolean onStartup() throws InterruptedException {
        // TODO: delay disposing old connector and streamRunner until all queries are finished
        if (Objects.nonNull(connector)) {
            connectorFactory.disposeConnector(connector);
        }
        if (Objects.nonNull(streamRunner)) {
            try {
                streamRunner.close();
            } catch (IOException e) {
                // ignore
            }
        }
        this.typeInfos = createTypeInfos(getPathParser(), getData().getTypes());
        this.streamRunner = new StreamRunner(context, actorSystemProvider, getData().getId(), getRunnerCapacity(((WithConnectionInfo<?>)getData()).getConnectionInfo()), getRunnerQueueSize(((WithConnectionInfo<?>)getData()).getConnectionInfo()));
        this.connector = (FeatureProviderConnector<T, U, V>) connectorFactory.createConnector(getData());

        if (!getConnector().isConnected()) {
            Optional<Throwable> connectionError = getConnector().getConnectionError();
            String message = connectionError.map(Throwable::getMessage)
                                            .orElse("unknown reason");
            LOGGER.error("Feature provider with id '{}' could not be started: {}", getId(), message);
            if (connectionError.isPresent() && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Stacktrace:", connectionError.get());
            }
            return false;
        }

        Optional<String> runnerError = getRunnerError(((WithConnectionInfo<?>)getData()).getConnectionInfo());

        if (runnerError.isPresent()) {
            LOGGER.error("Feature provider with id '{}' could not be started: {}", getId(), runnerError.get());
            return false;
        }

        if (getTypeInfoValidator().isPresent() && getData().getTypeValidation() != MODE.NONE) {
            final boolean[] isSuccess = {true};
            try {
                for (FeatureStoreTypeInfo typeInfo : getTypeInfos().values()) {
                    LOGGER.info("Validating type '{}' ({})", typeInfo.getName(),
                        getData().getTypeValidation().name().toLowerCase());

                    ValidationResult result = getTypeInfoValidator().get()
                        .validate(typeInfo, getData().getTypeValidation());

                    isSuccess[0] = isSuccess[0] && result.isSuccess();
                    result.getErrors().forEach(LOGGER::error);
                    result.getStrictErrors()
                        .forEach(result.getMode() == MODE.STRICT ? LOGGER::error : LOGGER::warn);
                    result.getWarnings().forEach(LOGGER::warn);

                    checkForStartupCancel();
                }
            } catch (Throwable e) {
                if (e instanceof InterruptedException) {
                    throw e;
                }
                LogContext.error(LOGGER, e, "Cannot validate types");
                isSuccess[0] = false;
            }

            if (!isSuccess[0]) {
                LOGGER.error("Feature provider with id '{}' could not be started: {} {}", getId(), getData().getTypeValidation().name().toLowerCase(), "validation failed");
                return false;
            }
        }

        return true;
    }

    @Override
    protected void onStarted() {
        String startupInfo = getStartupInfo().map(
            map -> String.format(" (%s)", map.toString().replace("{", "").replace("}", "")))
            .orElse("");

        LOGGER.info("Feature provider with id '{}' started successfully.{}", getId(),
            startupInfo);
    }

    @Override
    protected void onReloaded() {
        String startupInfo = getStartupInfo().map(
            map -> String.format(" (%s)", map.toString().replace("{", "").replace("}", "")))
            .orElse("");

        LOGGER.info("Feature provider with id '{}' reloaded successfully.{}", getId(),
            startupInfo);
    }

    @Override
    protected void onStopped() {
        connectorFactory.disposeConnector(connector);
        LOGGER.info("Feature provider with id '{}' stopped.", getId());
    }

    @Override
    protected void onStartupFailure(Throwable throwable) {
        LogContext.error(LOGGER, throwable, "Feature provider with id '{}' could not be started", getId());
    }

    protected int getRunnerCapacity(ConnectionInfo connectionInfo) {
        return StreamRunner.DYNAMIC_CAPACITY;
    }

    protected int getRunnerQueueSize(ConnectionInfo connectionInfo) {
        return StreamRunner.DYNAMIC_CAPACITY;
    }

    protected Optional<String> getRunnerError(ConnectionInfo connectionInfo) {
        return Optional.empty();
    }

    protected abstract FeatureStorePathParser getPathParser();

    protected abstract FeatureQueryTransformer<U, V> getQueryTransformer();

    protected FeatureProviderConnector<T, U, V> getConnector() {
        return Objects.requireNonNull(connector);
    }

    protected abstract FeatureNormalizer<T> getNormalizer();

    protected Map<String, FeatureStoreTypeInfo> getTypeInfos() {
        return typeInfos;
    }

    protected StreamRunner getStreamRunner() {
        return streamRunner;
    }

    protected Optional<Map<String,String>> getStartupInfo() {
        return Optional.empty();
    }

    protected Optional<TypeInfoValidator> getTypeInfoValidator() {
        return Optional.empty();
    }

    public static Map<String, FeatureStoreTypeInfo> createTypeInfos(
            FeatureStorePathParser pathParser, Map<String, FeatureSchema> featureTypes) {
        return featureTypes.entrySet()
                           .stream()
                           .map(entry -> {
                               FeatureType featureType = entry.getValue()
                                                         .accept(new FeatureSchemaToTypeVisitor(entry.getKey()));
                               List<FeatureStoreInstanceContainer> instanceContainers = pathParser.parse(featureType);
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
                Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(query.getType()));

                if (!typeInfo.isPresent()) {
                    //TODO: put error message into Result, complete successfully
                    CompletableFuture<Result> promise = new CompletableFuture<>();
                    promise.completeExceptionally(new IllegalStateException("No features available for type"));
                    return promise;
                }

                U transformedQuery = getQueryTransformer().transformQuery(query, ImmutableMap.of());

                V options = getQueryTransformer().getOptions(query);

                Source<T, NotUsed> sourceStream = getConnector().getSourceStream(transformedQuery, options);

                Sink<T, CompletionStage<Result>> sink = getNormalizer().normalizeAndTransform(transformer, query);

                return getStreamRunner().run(sourceStream, sink);
            }

            @Override
            public CompletionStage<Result> runWith(Sink<Feature, CompletionStage<Done>> transformer) {
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
            public <Y extends PropertyBase<Y,Z>, W extends FeatureBase<Y,Z>, Z extends SchemaBase<Z>> CompletionStage<Result> runWith(
                    FeatureProcessor<Y,W,Z> transformer) {
                Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(query.getType()));

                if (!typeInfo.isPresent()) {
                    //TODO: put error message into Result, complete successfully
                    CompletableFuture<Result> promise = new CompletableFuture<>();
                    promise.completeExceptionally(new IllegalStateException("No features available for type"));
                    return promise;
                }

                U sqlQueries = getQueryTransformer().transformQuery(query, ImmutableMap.of());

                V options = getQueryTransformer().getOptions(query);

                Source<T, NotUsed> rowStream = getConnector().getSourceStream(sqlQueries, options);

                Source<W, CompletionStage<Result>> featureStream = getNormalizer().normalize(rowStream, query, transformer::createFeature, transformer::createProperty);

                Sink<W, CompletionStage<Done>> sink = Sink.foreach(feature -> {

            /*if (!numberReturned.isDone() && feature.getProperties().containsKey(ImmutableList.of("numberReturned"))) {
                numberReturned.complete(Long.getLong(feature.getProperties().get(ImmutableList.of("numberReturned"))));
            }*/

                    transformer.process(feature);
                });

                return getStreamRunner().run(featureStream, sink, Keep.left());
            }
        };
    }

    //TODO
    @Override
    public long getFeatureCount(FeatureQuery featureQuery) {
        return 0;
    }

}
