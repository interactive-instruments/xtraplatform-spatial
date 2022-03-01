/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.gml.infra;

import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import de.ii.xtraplatform.features.gml.FeatureProviderDataWfsFromMetadata;
import de.ii.xtraplatform.features.gml.WFSCapabilitiesParser;
import de.ii.xtraplatform.features.gml.app.FeatureProviderWfs;
import de.ii.xtraplatform.features.gml.domain.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.features.gml.domain.FeatureProviderWfsData;
import de.ii.xtraplatform.features.gml.domain.WfsConnector;
import de.ii.xtraplatform.features.domain.Metadata;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.features.gml.app.request.GetCapabilities;
import de.ii.xtraplatform.features.gml.app.request.WfsOperation;
import de.ii.xtraplatform.features.gml.app.request.WfsRequestEncoder;
import de.ii.xtraplatform.web.domain.Http;
import de.ii.xtraplatform.web.domain.HttpClient;
import de.ii.xtraplatform.streams.domain.Reactive;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import org.codehaus.staxmate.SMInputFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zahnen
 */
public class WfsConnectorHttp implements WfsConnector {

    public static final String CONNECTOR_TYPE = "HTTP";

    private static final Logger LOGGER = LoggerFactory.getLogger(WfsConnectorHttp.class);
    private static final SMInputFactory staxFactory = new SMInputFactory(new InputFactoryImpl());

    private final HttpClient httpClient;
    private final WfsRequestEncoder wfsRequestEncoder;
    private final boolean useHttpPost;
    private final Optional<Metadata> metadata;
    private Optional<Throwable> connectionError;
    private final String providerId;

    @AssistedInject
    WfsConnectorHttp(Http http, @Assisted FeatureProviderWfsData data) {
        ConnectionInfoWfsHttp connectionInfo = data.getConnectionInfo();

        this.useHttpPost = connectionInfo.getMethod() == ConnectionInfoWfsHttp.METHOD.POST;

        Map<String, Map<WFS.METHOD, URI>> urls = ImmutableMap.of("default", ImmutableMap.of(WFS.METHOD.GET, FeatureProviderDataWfsFromMetadata.parseAndCleanWfsUrl(connectionInfo.getUri()), WFS.METHOD.POST, FeatureProviderDataWfsFromMetadata.parseAndCleanWfsUrl(connectionInfo.getUri())));

        this.wfsRequestEncoder = new WfsRequestEncoder(connectionInfo.getVersion(), connectionInfo.getGmlVersion(), connectionInfo.getNamespaces(), urls);

        /*
         workaround for https://github.com/interactive-instruments/ldproxy/issues/225
         TODO: remove when fixed
        */
        Optional.ofNullable(Strings.emptyToNull(connectionInfo.getUri().toString().replace(FeatureProviderWfsData.PLACEHOLDER_URI, "")))
            .orElseThrow(() -> new IllegalArgumentException("No 'uri' given, required for WFS connection"));

        URI host = connectionInfo.getUri();

        //TODO: get maxParallelRequests and idleTimeout from connectionInfo
        this.httpClient = http.getHostClient(host, 16, 30);

        this.metadata = crawlMetadata();
        this.providerId = data.getId();
    }

    WfsConnectorHttp() {
        httpClient = null;
        wfsRequestEncoder = null;
        useHttpPost = false;
        metadata = Optional.empty();
        providerId = null;
    }

    @Override
    public String getType() {
        return String.format("%s/%s", FeatureProviderWfs.PROVIDER_TYPE, CONNECTOR_TYPE);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    private Optional<Metadata> crawlMetadata() {
        try {
            InputStream inputStream = runWfsOperation(new GetCapabilities());
            WfsCapabilitiesAnalyzer metadataConsumer = new WfsCapabilitiesAnalyzer();
            WFSCapabilitiesParser gmlSchemaParser = new WFSCapabilitiesParser(metadataConsumer, staxFactory);
            gmlSchemaParser.parse(inputStream);

            this.connectionError = Optional.empty();

            return Optional.of(metadataConsumer.getMetadata());
        } catch (Throwable e) {
            this.connectionError = Optional.of(e);
        }

        return Optional.empty();
    }

    @Override
    public Optional<Metadata> getMetadata() {
        return metadata;
    }

    @Override
    public String getProviderId() {
        return providerId;
    }

    @Override
    public boolean isConnected() {
        return metadata.isPresent();
    }

    @Override
    public Optional<Throwable> getConnectionError() {
        return connectionError;
    }


  @Override
    public Reactive.Source<byte[]> getSourceStream(String query) {
        return httpClient.get(query);
    }

    @Override
    public Reactive.Source<byte[]> getSourceStream(String query, QueryOptions options) {
        return httpClient.get(query);
    }

    @Override
    public InputStream runWfsOperation(final WfsOperation operation) {
        return httpClient.getAsInputStream(wfsRequestEncoder.getAsUrl(operation));
    }
}
