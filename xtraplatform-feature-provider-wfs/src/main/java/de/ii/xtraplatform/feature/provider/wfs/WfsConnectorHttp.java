/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs;

import akka.Done;
import akka.NotUsed;
import akka.japi.Pair;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.StreamConverters;
import akka.util.ByteString;
import de.ii.xtraplatform.akka.http.AkkaHttp;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
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
import java.util.Map;
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

    static final String CONNECTOR_TYPE = "HTTP";

    private static final Logger LOGGER = LoggerFactory.getLogger(WfsConnectorHttp.class);

    @Requires
    private AkkaHttp akkaHttp;

    private FeatureQueryEncoderWfs queryEncoder;
    private WfsRequestEncoder requestEncoder;

    private final boolean useHttpPost;

    WfsConnectorHttp(@Property(name = ".data") FeatureProviderDataWfs data) {
        this.useHttpPost = data.getConnectionInfo()
                               .getMethod() == ConnectionInfo.METHOD.POST;
    }

    @Override
    public void setQueryEncoder(FeatureQueryEncoderWfs queryEncoder) {
        this.queryEncoder = queryEncoder;
        this.requestEncoder = queryEncoder.getWfsRequestEncoder();
    }

    @Override
    public CompletionStage<Done> runQuery(FeatureQuery query, Sink<ByteString, CompletionStage<Done>> transformer, Map<String, String> additionalQueryParameters) {
        final Source<ByteString, NotUsed> source;

        if (useHttpPost) {
            final Pair<String, String> requestUrlAndBody = queryEncoder.encodeFeatureQueryPost(query, additionalQueryParameters);

            source = akkaHttp.postXml(requestUrlAndBody.first(), requestUrlAndBody.second());
        } else {
            final String requestUrl = queryEncoder.encodeFeatureQuery(query, additionalQueryParameters);

            source = akkaHttp.get(requestUrl);
        }

        return source
                .runWith(transformer, akkaHttp.getMaterializer())
                .exceptionally(throwable -> {
                    LOGGER.error("Error during request: {}", throwable.getMessage());

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Exception:", throwable);
                    }

                    return Done.getInstance();
                });
    }

    @Override
    public InputStream runWfsOperation(final WfsOperation wfsOperation) {
        final String requestUrl = requestEncoder.getAsUrl(wfsOperation);

        return akkaHttp.get(requestUrl)
                       .runWith(StreamConverters.asInputStream(), akkaHttp.getMaterializer());
    }
}
