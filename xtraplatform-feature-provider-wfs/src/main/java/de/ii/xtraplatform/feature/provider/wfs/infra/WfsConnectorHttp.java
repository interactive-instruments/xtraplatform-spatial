/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.infra;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.streams.domain.Http;
import de.ii.xtraplatform.streams.domain.HttpClient;
import de.ii.xtraplatform.dropwizard.domain.Dropwizard;
import de.ii.xtraplatform.feature.provider.wfs.FeatureProviderDataWfsFromMetadata;
import de.ii.xtraplatform.feature.provider.wfs.WFSCapabilitiesParser;
import de.ii.xtraplatform.feature.provider.wfs.app.FeatureProviderWfs;
import de.ii.xtraplatform.feature.provider.wfs.domain.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.feature.provider.wfs.domain.WfsConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.Metadata;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.wfs.GetCapabilities;
import de.ii.xtraplatform.ogc.api.wfs.WfsOperation;
import de.ii.xtraplatform.ogc.api.wfs.WfsRequestEncoder;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.codehaus.staxmate.SMInputFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * @author zahnen
 */
@Component
@Provides(properties = {
        @StaticServiceProperty(name = "providerType", type = "java.lang.String", value = FeatureProviderWfs.PROVIDER_TYPE),
        @StaticServiceProperty(name = "connectorType", type = "java.lang.String", value = WfsConnectorHttp.CONNECTOR_TYPE)
})
public class WfsConnectorHttp implements WfsConnector {

    public static final String CONNECTOR_TYPE = "HTTP";

    private static final Logger LOGGER = LoggerFactory.getLogger(WfsConnectorHttp.class);
    private static SMInputFactory staxFactory = new SMInputFactory(new InputFactoryImpl());

    private final MetricRegistry metricRegistry;
    private final HttpClient httpClient;
    private final WfsRequestEncoder wfsRequestEncoder;
    private final boolean useHttpPost;
    private final boolean includePrefixes;
    private final Optional<Metadata> metadata;
    private Optional<Throwable> connectionError;

    WfsConnectorHttp(@Property(name = ".data") FeatureProviderDataV2 data, @Requires Dropwizard dropwizard,
                     @Requires Http http) {
        ConnectionInfoWfsHttp connectionInfo = (ConnectionInfoWfsHttp) data.getConnectionInfo();

        this.useHttpPost = connectionInfo.getMethod() == ConnectionInfoWfsHttp.METHOD.POST;
        this.metricRegistry = dropwizard.getEnvironment()
                                        .metrics();

        Map<String, Map<WFS.METHOD, URI>> urls = ImmutableMap.of("default", ImmutableMap.of(WFS.METHOD.GET, FeatureProviderDataWfsFromMetadata.parseAndCleanWfsUrl(connectionInfo.getUri()), WFS.METHOD.POST, FeatureProviderDataWfsFromMetadata.parseAndCleanWfsUrl(connectionInfo.getUri())));

        this.wfsRequestEncoder = new WfsRequestEncoder(connectionInfo.getVersion(), connectionInfo.getGmlVersion(), connectionInfo.getNamespaces(), urls);

        URI host = connectionInfo.getUri();

        this.includePrefixes = connectionInfo.getIncludePrefixes();

        //TODO: get maxParallelRequests and idleTimeout from connectionInfo
        this.httpClient = http.getHostClient(host, 16, 30);

        this.metadata = crawlMetadata();

    }

    WfsConnectorHttp() {
        metricRegistry = null;
        httpClient = null;
        wfsRequestEncoder = null;
        useHttpPost = false;
        includePrefixes = false;
        metadata = Optional.empty();
    }


    private Optional<Metadata> crawlMetadata() {
        try {
            InputStream inputStream = runWfsOperation(new GetCapabilities());
            WfsCapabilitiesAnalyzer metadataConsumer = new WfsCapabilitiesAnalyzer(includePrefixes);
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
    public boolean isConnected() {
        return metadata.isPresent();
    }

    @Override
    public Optional<Throwable> getConnectionError() {
        return connectionError;
    }


    @Override
    public CompletionStage<Done> runQuery(FeatureQuery query, Sink<ByteString, CompletionStage<Done>> consumer,
                                          Map<String, String> additionalQueryParameters) {
        return null;
    }

    @Override
    public Source<ByteString, NotUsed> getSourceStream(String query) {
        return httpClient.get(query);
    }

    @Override
    public Source<ByteString, NotUsed> getSourceStream(String query, QueryOptions options) {
        return httpClient.get(query);
    }

    @Override
    public InputStream runWfsOperation(final WfsOperation operation) {
        return httpClient.getAsInputStream(wfsRequestEncoder.getAsUrl(operation));
    }
}
