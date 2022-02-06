/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import static de.ii.xtraplatform.features.domain.transform.FeaturePropertyTransformerDateFormat.DATETIME_FORMAT;
import static de.ii.xtraplatform.features.domain.transform.FeaturePropertyTransformerDateFormat.DATE_FORMAT;
import static de.ii.xtraplatform.features.domain.transform.PropertyTransformations.WILDCARD;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.FeatureStream.Result.Builder;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.runtime.domain.LogContext;
import de.ii.xtraplatform.store.domain.entities.AbstractPersistentEntity;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import de.ii.xtraplatform.streams.domain.Reactive;
import de.ii.xtraplatform.streams.domain.Reactive.BasicStream;
import de.ii.xtraplatform.streams.domain.Reactive.RunnableStream;
import de.ii.xtraplatform.streams.domain.Reactive.SinkReduced;
import de.ii.xtraplatform.streams.domain.Reactive.SinkReducedTransformed;
import de.ii.xtraplatform.streams.domain.Reactive.SinkTransformed;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFeatureProvider<T,U,V extends FeatureProviderConnector.QueryOptions> extends AbstractPersistentEntity<FeatureProviderDataV2> implements FeatureProvider2, FeatureQueries {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFeatureProvider.class);

    private final ConnectorFactory connectorFactory;
    private final Reactive reactive;
    private final CrsTransformerFactory crsTransformerFactory;
    private Reactive.Runner streamRunner;
    private Map<String, FeatureStoreTypeInfo> typeInfos;
    private FeatureProviderConnector<T, U, V> connector;

    protected AbstractFeatureProvider(ConnectorFactory connectorFactory, Reactive reactive,
        CrsTransformerFactory crsTransformerFactory) {
        this.connectorFactory = connectorFactory;
        this.reactive = reactive;
        this.crsTransformerFactory = crsTransformerFactory;
    }

    private void onConnectorDispose() {
        if (getState() != STATE.RELOADING) {
            LOGGER.debug("CONNECTOR GONE {}", getId());
            doReload();
        }
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
        this.streamRunner = reactive.runner(getData().getId(), getRunnerCapacity(((WithConnectionInfo<?>)getData()).getConnectionInfo()), getRunnerQueueSize(((WithConnectionInfo<?>)getData()).getConnectionInfo()));
        this.connector = (FeatureProviderConnector<T, U, V>) connectorFactory.createConnector(getData());
        connectorFactory.onDispose(connector, this::onConnectorDispose);

        if (!getConnector().isConnected()) {
            connectorFactory.disposeConnector(connector);

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
        return Reactive.Runner.DYNAMIC_CAPACITY;
    }

    protected int getRunnerQueueSize(ConnectionInfo connectionInfo) {
        return Reactive.Runner.DYNAMIC_CAPACITY;
    }

    protected Optional<String> getRunnerError(ConnectionInfo connectionInfo) {
        return Optional.empty();
    }

    protected abstract FeatureStorePathParser getPathParser();

    protected abstract FeatureQueryTransformer<U, V> getQueryTransformer();

    protected FeatureProviderConnector<T, U, V> getConnector() {
        return Objects.requireNonNull(connector);
    }

    protected Map<String, FeatureStoreTypeInfo> getTypeInfos() {
        return typeInfos;
    }

    protected Reactive.Runner getStreamRunner() {
        return streamRunner;
    }

    protected Optional<Map<String,String>> getStartupInfo() {
        return Optional.empty();
    }

    protected Optional<TypeInfoValidator> getTypeInfoValidator() {
        return Optional.empty();
    }

    protected abstract FeatureTokenDecoder<T, FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>> getDecoder(FeatureQuery query);

    protected abstract Map<String, Codelist> getCodelists();

    public static Map<String, FeatureStoreTypeInfo> createTypeInfos(
            FeatureStorePathParser pathParser, Map<String, FeatureSchema> featureTypes) {
        return featureTypes.entrySet()
                           .stream()
                           .map(entry -> {
                               List<FeatureStoreInstanceContainer> instanceContainers = pathParser.parse(entry.getValue());
                               FeatureStoreTypeInfo typeInfo = ImmutableFeatureStoreTypeInfo.builder()
                                                                                            .name(entry.getKey())
                                                                                            .instanceContainers(instanceContainers)
                                                                                            .build();

                               return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), typeInfo);
                           })
                           .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public FeatureStream getFeatureStream(FeatureQuery query) {
        return new FeatureStream() {

            @Override
            public <X> CompletionStage<ResultReduced<X>> runWith(SinkReduced<Object, X> sink,
                Optional<PropertyTransformations> propertyTransformations) {

                FeatureTokenSource byteSource = getFeatureTokenSource(propertyTransformations);

                BasicStream<Object, X> to = byteSource.to(sink);

                RunnableStream<ResultReduced<X>> runnableStream = to.withResult(
                        ImmutableResultReduced.<X>builder().isEmpty(true))
                    .handleError((result, throwable) -> {
                        /*TODO Throwable error = throwable instanceof PSQLException || throwable instanceof JsonParseException
                            ? new IllegalArgumentException(throwable.getMessage())
                            : throwable;*/
                        return result.error(throwable);
                    })
                    .handleItem((builder, x) -> builder.reduced((X) x).isEmpty(false))
                    .handleEnd(ResultReduced.Builder::build)
                    .on(streamRunner);

                return runnableStream.run();
            }

            @Override
            public CompletionStage<Result> runWith(Reactive.Sink<Object> sink,
                Optional<PropertyTransformations> propertyTransformations) {

                FeatureTokenSource byteSource = getFeatureTokenSource(propertyTransformations);

                RunnableStream<Result> runnableStream = byteSource.to(sink)
                    .withResult((Builder)ImmutableResult.builder().isEmpty(true))
                    .handleError((result, throwable) -> {
                        /*TODO Throwable error = throwable instanceof PSQLException || throwable instanceof JsonParseException
                            ? new IllegalArgumentException(throwable.getMessage())
                            : throwable;*/
                        return result.error(throwable);
                    })
                    .handleItem((builder, x) -> builder.isEmpty(false))
                    .handleEnd(Builder::build)
                    .on(streamRunner);

                return runnableStream.run();
            }

            @Override
            public CompletionStage<Result> runWith(SinkTransformed<Object, byte[]> sink,
                Optional<PropertyTransformations> propertyTransformations) {
                FeatureTokenSource tokenSource = getFeatureTokenSource(propertyTransformations);

                RunnableStream<Result> runnableStream = tokenSource.to(sink)
                    .withResult((Builder)ImmutableResult.builder().isEmpty(true))
                    .handleError((result, throwable) -> {
                        /*TODO Throwable error = throwable instanceof PSQLException || throwable instanceof JsonParseException
                            ? new IllegalArgumentException(throwable.getMessage())
                            : throwable;*/
                        return result.error(throwable);
                    })
                    .handleItem((builder, x) -> builder.isEmpty(false))
                    .handleEnd(Builder::build)
                    .on(streamRunner);

                return runnableStream.run();
            }

            @Override
            public CompletionStage<ResultReduced<byte[]>> runWith(
                SinkReducedTransformed<Object, byte[], byte[]> sink,
                Optional<PropertyTransformations> propertyTransformations) {
                    FeatureTokenSource tokenSource = getFeatureTokenSource(propertyTransformations);

                    RunnableStream<ResultReduced<byte[]>> runnableStream = tokenSource.to(sink)
                        .withResult(ImmutableResultReduced.<byte[]>builder()
                            .reduced(new byte[0])
                            .isEmpty(true))
                        .handleError((result, throwable) -> {
                        /*TODO Throwable error = throwable instanceof PSQLException || throwable instanceof JsonParseException
                            ? new IllegalArgumentException(throwable.getMessage())
                            : throwable;*/
                            return result.error(throwable);
                        })
                        .handleItem((builder, bytes) -> builder.reduced(bytes).isEmpty(bytes.length > 0))
                        .handleEnd(ResultReduced.Builder::build)
                        .on(streamRunner);

                    return runnableStream.run();
            }

            private FeatureTokenSource getFeatureTokenSource(
                Optional<PropertyTransformations> propertyTransformations) {

                Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(query.getType()));

                if (!typeInfo.isPresent()) {
                    throw new IllegalStateException("No features available for type");
                }

                //TODO: encapsulate in FeatureQueryRunnerSql
                U transformedQuery = getQueryTransformer().transformQuery(query, ImmutableMap.of());
                V options = getQueryTransformer().getOptions(query);
                Reactive.Source<T> source = getConnector().getSourceStream(transformedQuery, options);

                FeatureTokenDecoder<T, FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>> decoder = getDecoder(query);

                FeatureTokenSource featureTokenSource = source.via(decoder);

                FeatureSchema featureSchema = getData().getTypes().get(query.getType());
                Map<String, List<PropertyTransformation>> providerTransformationMap = featureSchema.accept(
                    (schema, visitedProperties) -> java.util.stream.Stream
                        .concat(
                            schema.getTransformations().isEmpty()
                                ? schema.isTemporal()
                                    ? java.util.stream.Stream
                                    .of(new SimpleImmutableEntry<String, List<PropertyTransformation>>(
                                        String.join(".", schema.getFullPath()),
                                        ImmutableList.of(
                                            new ImmutablePropertyTransformation.Builder().dateFormat(
                                                schema.getType() == Type.DATETIME ? DATETIME_FORMAT
                                                    : DATE_FORMAT).build())))
                                    : java.util.stream.Stream.empty()
                                : java.util.stream.Stream
                                    .of(new SimpleImmutableEntry<String, List<PropertyTransformation>>(
                                        schema.getFullPath().isEmpty() ? WILDCARD : String.join(".", schema.getFullPath()),
                                        schema.getTransformations())),
                            visitedProperties.stream().flatMap(m -> m.entrySet().stream())
                        )
                        .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue)));
                PropertyTransformations providerTransformations = () -> providerTransformationMap;

                PropertyTransformations mergedTransformations = propertyTransformations
                    .map(p -> p.mergeInto(providerTransformations)).orElse(providerTransformations);

                FeatureTokenTransformerSchemaMappings schemaMapper = new FeatureTokenTransformerSchemaMappings(mergedTransformations);

                Optional<CrsTransformer> crsTransformer = query.getCrs().flatMap(
                    targetCrs -> crsTransformerFactory
                        .getTransformer(getData().getNativeCrs().orElse(OgcCrs.CRS84), targetCrs));
                FeatureTokenTransformerValueMappings valueMapper = new FeatureTokenTransformerValueMappings(
                    mergedTransformations, getCodelists(), getData().getNativeTimeZone(),
                    crsTransformer);

                FeatureTokenTransformerRemoveEmptyOptionals cleaner = new FeatureTokenTransformerRemoveEmptyOptionals();

                FeatureTokenTransformerSorting sorter = new FeatureTokenTransformerSorting();

                FeatureTokenTransformerLogger logger = new FeatureTokenTransformerLogger();

                return featureTokenSource
                    //.via(sorter)
                    .via(schemaMapper)
                    .via(valueMapper)
                    .via(cleaner);
                    //.via(logger);
            }
        };
    }

    //TODO
    @Override
    public long getFeatureCount(FeatureQuery featureQuery) {
        return 0;
    }

}
