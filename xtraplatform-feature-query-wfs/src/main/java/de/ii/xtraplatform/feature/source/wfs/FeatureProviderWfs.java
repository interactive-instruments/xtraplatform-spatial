/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.source.wfs;

import akka.Done;
import akka.stream.javadsl.Sink;
import akka.util.ByteString;
import com.google.common.util.concurrent.ListenableFuture;
import de.ii.xtraplatform.akka.http.AkkaHttp;
import de.ii.xtraplatform.feature.query.api.FeatureQuery;
import de.ii.xtraplatform.feature.query.api.FeatureStream;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfiguration;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import de.ii.xtraplatform.feature.transformer.api.GmlConsumer;
import de.ii.xtraplatform.feature.transformer.api.GmlStreamParser;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSAdapter;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSRequest;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
public class FeatureProviderWfs implements GmlProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderWfs.class);

    private static final String SOURCE_FORMAT = "application/xml+gml";

    //@Requires
    //private AkkaHttp akkaHttp;

    private final WFSAdapter wfsAdapter;
    private final Map<String, FeatureTypeConfiguration> featureTypes;
    private final FeatureQueryEncoderWfs queryEncoder;

    public FeatureProviderWfs(final WFSAdapter wfsAdapter, final Map<String, FeatureTypeConfiguration> featureTypes) {
        this.wfsAdapter = wfsAdapter;
        this.featureTypes = featureTypes;
        queryEncoder = new FeatureQueryEncoderWfs(featureTypes, wfsAdapter.getNsStore());
    }

    public FeatureProviderWfs(final AkkaHttp akkaHttp, final WFSAdapter wfsAdapter, final Map<String, FeatureTypeConfiguration> featureTypes) {
        //this.akkaHttp = akkaHttp;
        this.wfsAdapter = wfsAdapter;
        this.featureTypes = featureTypes;
        queryEncoder = new FeatureQueryEncoderWfs(featureTypes, wfsAdapter.getNsStore());
    }

    @Override
    public FeatureStream<GmlConsumer> getFeatureStream(FeatureQuery query, AkkaHttp akkaHttp) {

        //TODO: GET/POST
        return featureConsumer -> {
            //StreamingGmlTransformerFlow.transformer(featureType, featureTypeMapping, null/*FeatureConsumer*/);
            Sink<ByteString, CompletionStage<Done>> parser = GmlStreamParser.consume(queryEncoder.getFeatureTypeName(query)
                                                                                                 .get(), featureConsumer);
            return akkaHttp.get(encodeFeatureQuery(query).get())
                    .runWith(parser, akkaHttp.getMaterializer())
                    .exceptionally(throwable -> {
                        LOGGER.error("Feature stream error", throwable);
                        return Done.getInstance();
                    });
        };


        // TODO: measure performance with files to compare processing time only
//        Source<ByteString, Date> fromFile = FileIO.fromFile(new File("/home/zahnen/development/ldproxy/artillery/flurstueck-" + count.get() + "-" + page.get() + ".xml"))
//                .mapMaterializedValue(nu -> new Date());

        //return queryEncoder.encode(query)
        //                   .map(getFeature -> new WFSRequest(wfsAdapter, getFeature).getResponse());
    }

    @Override
    public FeatureStream<FeatureTransformer> getFeatureTransformStream(FeatureQuery query, AkkaHttp akkaHttp) {
        //TODO: GET/POST
        return featureTransformer -> {
            Optional<FeatureTypeMapping> featureTypeMapping = getFeatureTypeMapping(query.getType());

            if (!featureTypeMapping.isPresent()) {
                CompletableFuture<Done> promise = new CompletableFuture<>();
                promise.completeExceptionally(new IllegalStateException("No features available for type"));
                return promise;
            }

            //StreamingGmlTransformerFlow.transformer(featureType, featureTypeMapping, null/*FeatureConsumer*/);
            Sink<ByteString, CompletionStage<Done>> parser = GmlStreamParser.transform(queryEncoder.getFeatureTypeName(query)
                                                                                                 .get(), featureTypeMapping.get(), featureTransformer);
            return akkaHttp.get(encodeFeatureQuery(query).get())
                           .runWith(parser, akkaHttp.getMaterializer())
                           .exceptionally(throwable -> {
                               LOGGER.error("Feature stream error", throwable);
                               return Done.getInstance();
                           });
        };
    }

    @Override
    public Optional<ListenableFuture<HttpEntity>> getFeatureCount(FeatureQuery query) {
        return queryEncoder.encode(query, true)
                           .map(getFeature -> new WFSRequest(wfsAdapter, getFeature).getResponse());
    }

    @Override
    public Optional<String> encodeFeatureQuery(FeatureQuery query) {
        return queryEncoder.encode(query)
                           .map(getFeature -> new WFSRequest(wfsAdapter, getFeature).getAsUrl());
    }

    @Override
    public String getSourceFormat() {
        return SOURCE_FORMAT;
    }

    private Optional<FeatureTypeMapping> getFeatureTypeMapping(final String typeName) {
            return featureTypes.values()
                               .stream()
                               .filter(ft -> ft.getName().equals(typeName))
                               .findFirst()
                               .map(FeatureTypeConfiguration::getMappings);
    }
}
