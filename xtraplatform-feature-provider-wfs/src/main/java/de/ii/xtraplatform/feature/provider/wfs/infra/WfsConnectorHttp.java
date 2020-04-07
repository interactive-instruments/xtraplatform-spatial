/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.infra;

import akka.Done;
import akka.NotUsed;
import akka.japi.Pair;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.codahale.metrics.MetricRegistry;
import de.ii.xtraplatform.akka.http.Http;
import de.ii.xtraplatform.akka.http.HttpClient;
import de.ii.xtraplatform.dropwizard.api.Dropwizard;
import de.ii.xtraplatform.feature.provider.wfs.FeatureProviderWfs;
import de.ii.xtraplatform.feature.provider.wfs.FeatureQueryEncoderWfs;
import de.ii.xtraplatform.feature.provider.wfs.domain.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.feature.provider.wfs.domain.WfsConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV1;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.ogc.api.wfs.WfsOperation;
import de.ii.xtraplatform.ogc.api.wfs.WfsRequestEncoder;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
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

    //@Requires
    //private AkkaHttp akkaHttp;

    private FeatureQueryEncoderWfs queryEncoder;
    private WfsRequestEncoder requestEncoder;
    private final MetricRegistry metricRegistry;
    private final HttpClient httpClient;
    private final boolean useHttpPost;

    WfsConnectorHttp(@Property(name = ".data") FeatureProviderDataV1 data, @Requires Dropwizard dropwizard,
                     @Requires Http http) {
        this.useHttpPost = ((ConnectionInfoWfsHttp) data.getConnectionInfo()).getMethod() == ConnectionInfoWfsHttp.METHOD.POST;
        this.metricRegistry = dropwizard.getEnvironment()
                                        .metrics();

        URI host = ((ConnectionInfoWfsHttp) data.getConnectionInfo()).getUri();

        //TODO: get maxParallelRequests and idleTimeout from connectionInfo
        this.httpClient = http.getHostClient(host, 16, 30);

        /*Optional.ofNullable((ConnectionInfoWfsHttp) data.getConnectionInfo()).ifPresent(connectionInfoWfsHttp -> {
            akkaHttp.registerHost(connectionInfoWfsHttp.getUri());
        });*/
    }

    @Override
    public boolean isConnected() {
        //TODO: test request
        return true;
    }

    //TODO
    @Override
    public Optional<Throwable> getConnectionError() {
        return Optional.empty();
    }

    @Override
    public void setQueryEncoder(FeatureQueryEncoderWfs queryEncoder) {
        this.queryEncoder = queryEncoder;
        this.requestEncoder = queryEncoder.getWfsRequestEncoder();
    }

    @Override
    public CompletionStage<Done> runQuery(FeatureQuery query, Sink<ByteString, CompletionStage<Done>> consumer,
                                          Map<String, String> additionalQueryParameters) {

        if (useHttpPost) {
            final Pair<String, String> requestUrlAndBody = queryEncoder.encodeFeatureQueryPost(query, additionalQueryParameters);

            return httpClient.postXml(requestUrlAndBody.first(), requestUrlAndBody.second(), consumer);
        } else {
            final String requestUrl = queryEncoder.encodeFeatureQuery(query, additionalQueryParameters);

            return httpClient.get(requestUrl, consumer);
        }

        /*Timer.Context timer = metricRegistry.timer(name(WfsConnectorHttp.class, "stream"))
                                            .time();

        return source.runWith(transformer, akkaHttp.getMaterializer())
                     .exceptionally(throwable -> {
                         LOGGER.error("Error during request: {}", throwable.getMessage());

                         if (LOGGER.isDebugEnabled()) {
                             LOGGER.debug("Exception:", throwable);
                         }

                         return Done.getInstance();
                     })
                     .whenComplete((done, throwable) -> timer.stop());

         */
    }

    @Override
    public Source<ByteString, NotUsed> getSourceStream(String query) {
        return httpClient.get(query);
    }

    @Override
    public Source<ByteString, NotUsed> getSourceStream(String query, QueryOptions options) {
        return null;
    }

    @Override
    public InputStream runWfsOperation(final WfsOperation wfsOperation) {
        final String requestUrl = requestEncoder.getAsUrl(wfsOperation);

        return httpClient.getAsInputStream(requestUrl);
    }
}
