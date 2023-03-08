/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.graphql.infra;

import com.google.common.base.Strings;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.features.domain.ConnectionInfo;
import de.ii.xtraplatform.features.graphql.app.FeatureProviderGraphQl;
import de.ii.xtraplatform.features.graphql.domain.ConnectionInfoGraphQlHttp;
import de.ii.xtraplatform.features.graphql.domain.FeatureProviderGraphQlData;
import de.ii.xtraplatform.features.graphql.domain.GraphQlConnector;
import de.ii.xtraplatform.streams.domain.Reactive;
import de.ii.xtraplatform.web.domain.Http;
import de.ii.xtraplatform.web.domain.HttpClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
public class GraphQlConnectorHttp implements GraphQlConnector {

  public static final String CONNECTOR_TYPE = "HTTP";

  private static final Logger LOGGER = LoggerFactory.getLogger(GraphQlConnectorHttp.class);

  private final HttpClient httpClient;
  private Optional<Throwable> connectionError;
  private final String providerId;
  private final ConnectionInfoGraphQlHttp connectionInfo;

  @AssistedInject
  GraphQlConnectorHttp(
      Http http, @Assisted String providerId, @Assisted ConnectionInfoGraphQlHttp connectionInfo) {
    /*
     workaround for https://github.com/interactive-instruments/ldproxy/issues/225
     TODO: remove when fixed
    */
    Optional.ofNullable(
            Strings.emptyToNull(
                connectionInfo
                    .getUri()
                    .toString()
                    .replace(FeatureProviderGraphQlData.PLACEHOLDER_URI, "")))
        .orElseThrow(
            () -> new IllegalArgumentException("No 'uri' given, required for WFS connection"));

    URI host = connectionInfo.getUri();

    // TODO: get maxParallelRequests and idleTimeout from connectionInfo
    this.httpClient = http.getHostClient(host, 16, 30);

    this.providerId = providerId;
    this.connectionInfo = connectionInfo;
  }

  GraphQlConnectorHttp() {
    httpClient = null;
    providerId = null;
    connectionInfo = null;
  }

  @Override
  public String getType() {
    return String.format("%s/%s", FeatureProviderGraphQl.PROVIDER_TYPE, CONNECTOR_TYPE);
  }

  @Override
  public void start() {}

  @Override
  public void stop() {}

  @Override
  public String getProviderId() {
    return providerId;
  }

  // TODO
  @Override
  public boolean isConnected() {
    return true;
  }

  @Override
  public Optional<Throwable> getConnectionError() {
    return connectionError;
  }

  @Override
  public Reactive.Source<byte[]> getSourceStream(String query) {
    InputStream inputStream =
        httpClient.postAsInputStream(
            connectionInfo.getUri().toString(),
            query.getBytes(StandardCharsets.UTF_8),
            MediaType.APPLICATION_JSON_TYPE,
            Map.of("Accept", MediaType.APPLICATION_JSON));

    try {
      byte[] bytes = inputStream.readAllBytes();
      LOGGER.debug("Response \n{}", new String(bytes, StandardCharsets.UTF_8));

      return httpClient.getAsSource(new ByteArrayInputStream(bytes));
    } catch (IOException e) {
      LogContext.errorAsDebug(LOGGER, e, "GraphQL error");
    }

    return Reactive.Source.empty();
  }

  @Override
  public Reactive.Source<byte[]> getSourceStream(String query, QueryOptions options) {

    return getSourceStream(query);
  }

  @Override
  public boolean isSameDataset(ConnectionInfo connectionInfo) {
    return Objects.equals(
        connectionInfo.getDatasetIdentifier(), this.connectionInfo.getDatasetIdentifier());
  }

  @Override
  public String getDatasetIdentifier() {
    return this.connectionInfo.getDatasetIdentifier();
  }
}
