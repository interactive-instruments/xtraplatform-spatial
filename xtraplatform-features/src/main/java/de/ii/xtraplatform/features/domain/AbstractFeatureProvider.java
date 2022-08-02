/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.features.app.FeatureChangeHandlerImpl;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.FeatureQueriesExtension.LIFECYCLE_HOOK;
import de.ii.xtraplatform.features.domain.FeatureSchema.Scope;
import de.ii.xtraplatform.features.domain.FeatureStream.ResultBase;
import de.ii.xtraplatform.features.domain.transform.WithScope;
import de.ii.xtraplatform.store.domain.entities.AbstractPersistentEntity;
import de.ii.xtraplatform.store.domain.entities.ValidationResult;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import de.ii.xtraplatform.streams.domain.Reactive;
import de.ii.xtraplatform.streams.domain.Reactive.Stream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFeatureProvider<
        T, U, V extends FeatureProviderConnector.QueryOptions, W extends SchemaBase<W>>
    extends AbstractPersistentEntity<FeatureProviderDataV2>
    implements FeatureProvider2, FeatureQueries {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFeatureProvider.class);
  protected static final WithScope WITH_SCOPE_QUERIES = new WithScope(Scope.QUERIES);
  protected static final WithScope WITH_SCOPE_MUTATIONS = new WithScope(Scope.MUTATIONS);

  private final ConnectorFactory connectorFactory;
  private final Reactive reactive;
  private final CrsTransformerFactory crsTransformerFactory;
  private final ProviderExtensionRegistry extensionRegistry;
  private final FeatureChangeHandler changeHandler;
  private Reactive.Runner streamRunner;
  private FeatureProviderConnector<T, U, V> connector;

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
    this.streamRunner =
        reactive.runner(
            getData().getId(),
            getRunnerCapacity(((WithConnectionInfo<?>) getData()).getConnectionInfo()),
            getRunnerQueueSize(((WithConnectionInfo<?>) getData()).getConnectionInfo()));
    this.connector =
        (FeatureProviderConnector<T, U, V>)
            connectorFactory.createConnector(
                getData().getFeatureProviderType(), getData().getId(), getConnectionInfo());

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
    connectorFactory.onDispose(connector, this::onConnectorDispose);

    Optional<String> runnerError =
        getRunnerError(((WithConnectionInfo<?>) getData()).getConnectionInfo());

    if (runnerError.isPresent()) {
      LOGGER.error(
          "Feature provider with id '{}' could not be started: {}", getId(), runnerError.get());
      return false;
    }

    if (getTypeInfoValidator().isPresent() && getData().getTypeValidation() != MODE.NONE) {
      final boolean[] isSuccess = {true};
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
                      sourceSchema.getKey(),
                      sourceSchema.getValue(),
                      getData().getTypeValidation());

          isSuccess[0] = isSuccess[0] && result.isSuccess();
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
        isSuccess[0] = false;
      }

      if (!isSuccess[0]) {
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
  }

  @Override
  protected void onStopped() {
    connectorFactory.disposeConnector(connector);
    LOGGER.info("Feature provider with id '{}' stopped.", getId());
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
      getDecoder(FeatureQuery query);

  protected FeatureTokenDecoder<
          T, FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>>
      getDecoderPassThrough(FeatureQuery query) {
    return getDecoder(query);
  }

  protected List<FeatureTokenTransformer> getDecoderTransformers() {
    return ImmutableList.of();
  }

  protected abstract Map<String, Codelist> getCodelists();

  @Override
  public FeatureStream getFeatureStream(FeatureQuery query) {
    return new FeatureStreamImpl(
        query, getData(), crsTransformerFactory, getCodelists(), this::runQuery, true);
  }

  @Override
  public FeatureChangeHandler getFeatureChangeHandler() {
    return changeHandler;
  }

  private FeatureTokenSource getFeatureTokenSource(
      FeatureQuery query, Map<String, String> virtualTables, boolean passThrough) {
    if (!getSourceSchemas().containsKey(query.getType())) {
      throw new IllegalStateException("No features available for type");
    }

    U transformedQuery = getQueryEncoder().encode(query, virtualTables);
    V options = getQueryEncoder().getOptions(query);
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
      FeatureQuery query,
      boolean passThrough) {
    Map<String, String> virtualTables = beforeQuery(query);

    FeatureTokenSource tokenSource = getFeatureTokenSource(query, virtualTables, passThrough);

    Reactive.RunnableStream<W> runnableStream =
        stream.apply(tokenSource, virtualTables).on(streamRunner);

    return runnableStream.run().whenComplete((result, throwable) -> afterQuery(query));
  }

  private Map<String, String> beforeQuery(FeatureQuery query) {
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

  private void afterQuery(FeatureQuery query) {
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
