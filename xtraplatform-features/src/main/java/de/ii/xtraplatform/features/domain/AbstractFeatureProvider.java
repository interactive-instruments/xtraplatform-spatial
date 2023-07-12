/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.features.app.FeatureChangeHandlerImpl;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.FeatureQueriesExtension.LIFECYCLE_HOOK;
import de.ii.xtraplatform.features.domain.FeatureStream.ResultBase;
import de.ii.xtraplatform.features.domain.transform.WithScope;
import de.ii.xtraplatform.store.domain.entities.AbstractPersistentEntity;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import de.ii.xtraplatform.streams.domain.Reactive;
import de.ii.xtraplatform.streams.domain.Reactive.Runner;
import de.ii.xtraplatform.streams.domain.Reactive.Stream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFeatureProvider<
        T, U, V extends FeatureProviderConnector.QueryOptions, W extends SchemaBase<W>>
    extends AbstractPersistentEntity<FeatureProviderDataV2>
    implements FeatureProvider2, FeatureQueries {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFeatureProvider.class);
  protected static final WithScope WITH_SCOPE_QUERIES =
      new WithScope(FeatureSchemaBase.Scope.QUERIES);
  protected static final WithScope WITH_SCOPE_MUTATIONS =
      new WithScope(FeatureSchemaBase.Scope.MUTATIONS);

  private final ConnectorFactory connectorFactory;
  private final Reactive reactive;
  private final CrsTransformerFactory crsTransformerFactory;
  private final ProviderExtensionRegistry extensionRegistry;
  private final FeatureChangeHandler changeHandler;
  private final ScheduledExecutorService delayedDisposer;
  private Reactive.Runner streamRunner;
  private FeatureProviderConnector<T, U, V> connector;
  private boolean datasetChanged;
  private String previousDataset;

  protected AbstractFeatureProvider(
      ConnectorFactory connectorFactory,
      Reactive reactive,
      CrsTransformerFactory crsTransformerFactory,
      ProviderExtensionRegistry extensionRegistry,
      FeatureProviderDataV2 data) {
    super(data);
    this.connectorFactory = connectorFactory;
    this.reactive = reactive;
    this.crsTransformerFactory = crsTransformerFactory;
    this.extensionRegistry = extensionRegistry;
    this.changeHandler = new FeatureChangeHandlerImpl();
    this.delayedDisposer =
        MoreExecutors.getExitingScheduledExecutorService(
            (ScheduledThreadPoolExecutor)
                Executors.newScheduledThreadPool(
                    1, new ThreadFactoryBuilder().setNameFormat("entity.lifecycle-%d").build()));
  }

  @Override
  protected boolean onStartup() throws InterruptedException {
    this.datasetChanged =
        Objects.nonNull(connector) && !connector.isSameDataset(getConnectionInfo());
    this.previousDataset =
        Optional.ofNullable(connector)
            .map(FeatureProviderConnector::getDatasetIdentifier)
            .orElse("");
    boolean previousAlive = softClosePrevious(streamRunner, connector);
    boolean isShared = getConnectionInfo().isShared();
    String connectorId = getConnectorId(previousAlive, isShared);

    this.streamRunner =
        reactive.runner(
            getData().getId(),
            getRunnerCapacity(getConnectionInfo()),
            getRunnerQueueSize(getConnectionInfo()));
    this.connector =
        (FeatureProviderConnector<T, U, V>)
            connectorFactory.createConnector(
                getData().getProviderSubType(), connectorId, getConnectionInfo());

    if (!getConnector().isConnected()) {
      connectorFactory.disposeConnector(connector);

      Optional<Throwable> connectionError = getConnector().getConnectionError();
      String message = connectionError.map(Throwable::getMessage).orElse("unknown reason");
      LOGGER.error("Feature provider with id '{}' could not be started: {}", getId(), message);
      if (connectionError.isPresent() && LOGGER.isDebugEnabled()) {
        LOGGER.debug("Stacktrace:", connectionError.get());
      }
      return false;
    }

    if (isShared) {
      connectorFactory.onDispose(connector, LogContext.withMdc(this::onSharedConnectorDispose));
    }

    Optional<String> runnerError = getRunnerError(getConnectionInfo());

    if (runnerError.isPresent()) {
      LOGGER.error(
          "Feature provider with id '{}' could not be started: {}", getId(), runnerError.get());
      return false;
    }

    if (getTypeInfoValidator().isPresent() && getData().getTypeValidation() != MODE.NONE) {
      final boolean isSuccess = validate();

      if (!isSuccess) {
        LOGGER.error(
            "Feature provider with id '{}' could not be started: {} {}",
            getId(),
            getData().getTypeValidation().name().toLowerCase(),
            "validation failed");
        return false;
      }
    }

    return true;
  }

  @Override
  protected void onStarted() {
    String startupInfo =
        getStartupInfo()
            .map(map -> String.format(" (%s)", map.toString().replace("{", "").replace("}", "")))
            .orElse("");

    LOGGER.info("Feature provider with id '{}' started successfully.{}", getId(), startupInfo);

    extensionRegistry
        .getAll()
        .forEach(
            extension -> {
              if (extension.isSupported(getConnector())) {
                extension.on(LIFECYCLE_HOOK.STARTED, this, getConnector());
              }
            });
  }

  @Override
  protected void onReloaded() {
    String startupInfo =
        getStartupInfo()
            .map(map -> String.format(" (%s)", map.toString().replace("{", "").replace("}", "")))
            .orElse("");

    LOGGER.info("Feature provider with id '{}' reloaded successfully.{}", getId(), startupInfo);

    if (datasetChanged) {
      LOGGER.info(
          "Dataset has changed ({} -> {}).",
          previousDataset,
          getConnectionInfo().getDatasetIdentifier());
      changeHandler.handle(
          ImmutableDatasetChange.builder().featureTypes(getData().getTypes().keySet()).build());
    }
    this.datasetChanged = false;
  }

  @Override
  protected void onStopped() {
    connectorFactory.disposeConnector(connector);
    LOGGER.info("Feature provider with id '{}' stopped.", getId());
  }

  private boolean softClosePrevious(
      Runner previousRunner, FeatureProviderConnector<T, U, V> previousConnector) {
    if (Objects.nonNull(previousConnector) || Objects.nonNull(previousRunner)) {
      if (previousRunner.getActiveStreams() > 0) {
        LOGGER.debug("Active streams found, keeping previous connection pool alive.");
        delayedDisposer.schedule(
            LogContext.withMdc(() -> softClosePrevious(previousRunner, previousConnector)),
            10,
            TimeUnit.SECONDS);
        return true;
      }

      if (Objects.nonNull(previousConnector)) {
        LOGGER.debug("Disposing previous connection pool.");
        connectorFactory.disposeConnector(previousConnector);
      }
      if (Objects.nonNull(previousRunner)) {
        try {
          previousRunner.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }
    return false;
  }

  private void onSharedConnectorDispose() {
    if (((WithConnectionInfo<?>) getData()).getConnectionInfo().isShared()
        && getState() != STATE.RELOADING) {
      LOGGER.debug("Shared connector has changed, reloading.");
      doReload();
    }
  }

  private String getConnectorId(boolean previousAlive, boolean isShared) {
    Optional<Integer> previousIteration =
        previousAlive
            ? connector.getProviderId().contains(".")
                ? Optional.of(
                        connector
                            .getProviderId()
                            .substring(connector.getProviderId().lastIndexOf('.') + 1))
                    .flatMap(
                        i -> {
                          try {
                            return Optional.of(Integer.parseInt(i));
                          } catch (Throwable e) {
                          }
                          return Optional.of(1);
                        })
                : Optional.of(1)
            : Optional.empty();

    return String.format(
        "%s%s%s",
        getData().getId(),
        isShared ? ".shared" : "",
        previousIteration.map(i -> "." + (i + 1)).orElse(""));
  }

  private boolean validate() throws InterruptedException {
    boolean isSuccess = true;

    try {
      for (Map.Entry<String, List<W>> sourceSchema : getSourceSchemas().entrySet()) {
        LOGGER.info(
            "Validating type '{}' ({})",
            sourceSchema.getKey(),
            getData().getTypeValidation().name().toLowerCase());

        ValidationResult result =
            getTypeInfoValidator()
                .get()
                .validate(
                    sourceSchema.getKey(), sourceSchema.getValue(), getData().getTypeValidation());

        isSuccess = isSuccess && result.isSuccess();

        result.getErrors().forEach(LOGGER::error);
        result
            .getStrictErrors()
            .forEach(result.getMode() == MODE.STRICT ? LOGGER::error : LOGGER::warn);
        result.getWarnings().forEach(LOGGER::warn);

        checkForStartupCancel();
      }
    } catch (Throwable e) {
      if (e instanceof InterruptedException) {
        throw e;
      }
      LogContext.error(LOGGER, e, "Cannot validate types");
      isSuccess = false;
    }

    return isSuccess;
  }

  @Override
  protected void onStartupFailure(Throwable throwable) {
    LogContext.error(
        LOGGER, throwable, "Feature provider with id '{}' could not be started", getId());
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

  protected ConnectionInfo getConnectionInfo() {
    return ((WithConnectionInfo<?>) getData()).getConnectionInfo();
  }

  protected abstract Map<String, List<W>> getSourceSchemas();

  protected abstract FeatureQueryEncoder<U, V> getQueryEncoder();

  protected FeatureProviderConnector<T, U, V> getConnector() {
    return Objects.requireNonNull(connector);
  }

  protected Reactive.Runner getStreamRunner() {
    return streamRunner;
  }

  protected Optional<Map<String, String>> getStartupInfo() {
    return Optional.empty();
  }

  protected Optional<SourceSchemaValidator<W>> getTypeInfoValidator() {
    return Optional.empty();
  }

  protected abstract FeatureTokenDecoder<
          T, FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>>
      getDecoder(Query query);

  protected FeatureTokenDecoder<
          T, FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>>
      getDecoderPassThrough(Query query) {
    return getDecoder(query);
  }

  protected List<FeatureTokenTransformer> getDecoderTransformers() {
    return ImmutableList.of();
  }

  protected abstract Map<String, Codelist> getCodelists();

  @Override
  public final FeatureProviderCapabilities getCapabilities() {
    return getQueryEncoder().getCapabilities();
  }

  @Override
  public FeatureStream getFeatureStream(FeatureQuery query) {
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

  // TODO: more tests
  protected final void validateQuery(Query query) {
    if (query instanceof FeatureQuery) {
      if (!getSourceSchemas().containsKey(((FeatureQuery) query).getType())) {
        throw new IllegalArgumentException("No features available for type");
      }
    }
    if (query instanceof MultiFeatureQuery) {
      for (TypeQuery typeQuery : ((MultiFeatureQuery) query).getQueries()) {
        if (!getSourceSchemas().containsKey(typeQuery.getType())) {
          throw new IllegalArgumentException("No features available for type");
        }
      }
    }
  }

  protected Query preprocessQuery(Query query) {
    return query;
  }

  @Override
  public FeatureChangeHandler getChangeHandler() {
    return changeHandler;
  }

  // TODO: encodingOptions vs executionOptions
  private FeatureTokenSource getFeatureTokenSource(
      Query query, Map<String, String> virtualTables, boolean passThrough) {
    TypeQuery typeQuery =
        query instanceof MultiFeatureQuery
            ? ((MultiFeatureQuery) query).getQueries().get(0)
            : (FeatureQuery) query;

    getQueryEncoder().validate(typeQuery, query);

    U transformedQuery = getQueryEncoder().encode(query, virtualTables);
    // TODO: remove options, already embedded in SqlQuerySet
    V options = getQueryEncoder().getOptions(typeQuery, query);
    Reactive.Source<T> source = getConnector().getSourceStream(transformedQuery, options);

    FeatureTokenDecoder<
            T,
            FeatureSchema,
            SchemaMapping,
            FeatureEventHandler.ModifiableContext<FeatureSchema, SchemaMapping>>
        decoder = passThrough ? getDecoderPassThrough(query) : getDecoder(query);

    FeatureTokenSource featureSource = source.via(decoder);

    for (FeatureTokenTransformer transformer : getDecoderTransformers()) {
      featureSource = featureSource.via(transformer);
    }

    return featureSource;
  }

  protected <W extends ResultBase> CompletionStage<W> runQuery(
      BiFunction<FeatureTokenSource, Map<String, String>, Stream<W>> stream,
      Query query,
      boolean passThrough) {
    // TODO: rename to context?
    Map<String, String> virtualTables = beforeQuery(query);

    FeatureTokenSource tokenSource = getFeatureTokenSource(query, virtualTables, passThrough);

    Reactive.RunnableStream<W> runnableStream =
        stream.apply(tokenSource, virtualTables).on(streamRunner);

    return runnableStream.run().whenComplete((result, throwable) -> afterQuery(query));
  }

  private Map<String, String> beforeQuery(Query query) {
    Map<String, String> virtualTables = new HashMap<>();

    extensionRegistry
        .getAll()
        .forEach(
            extension -> {
              if (extension.isSupported(getConnector())) {
                extension.on(
                    FeatureQueriesExtension.QUERY_HOOK.BEFORE,
                    getData(),
                    getConnector(),
                    query,
                    virtualTables::put);
              }
            });

    return virtualTables;
  }

  private void afterQuery(Query query) {
    extensionRegistry
        .getAll()
        .forEach(
            extension -> {
              if (extension.isSupported(getConnector())) {
                extension.on(
                    FeatureQueriesExtension.QUERY_HOOK.AFTER,
                    getData(),
                    getConnector(),
                    query,
                    (a, t) -> {});
              }
            });
  }
}
