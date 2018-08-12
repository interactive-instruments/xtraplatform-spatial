/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs;

import akka.Done;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.util.ByteString;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import de.ii.xtraplatform.akka.http.AkkaHttp;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.feature.query.api.FeatureQuery;
import de.ii.xtraplatform.feature.query.api.FeatureStream;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import de.ii.xtraplatform.feature.transformer.api.GmlConsumer;
import de.ii.xtraplatform.feature.transformer.api.GmlStreamParser;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSAdapter;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSRequest;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static de.ii.xtraplatform.feature.provider.wfs.FeatureProviderWfs.PROVIDER_TYPE;

/**
 * @author zahnen
 */
@Component
@Provides(properties = {@StaticServiceProperty(name = "providerType", type = "java.lang.String", value = PROVIDER_TYPE)})
//@Instantiate
public class FeatureProviderWfs implements GmlProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderWfs.class);

    private static final String SOURCE_FORMAT = "application/xml+gml";

    static final String PROVIDER_TYPE = "WFS";

    @Requires
    private AkkaHttp akkaHttp;

    private final WFSAdapter wfsAdapter;
    private final Map<String, QName> featureTypes;
    private final Map<String, FeatureTypeMapping> featureTypeMappings;
    private final FeatureQueryEncoderWfs queryEncoder;
    private CrsTransformer defaultTransformer;
    private CrsTransformer defaultReverseTransformer;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    FeatureProviderWfs(@Requires CrsTransformation crsTransformation, @Property(name = ".data") FeatureProviderDataWfs data) {
        this.wfsAdapter = new WFSAdapter();
        wfsAdapter.setVersion(data.getConnectionInfo().getVersion());
        wfsAdapter.setGmlVersion(data.getConnectionInfo().getGmlVersion());
        wfsAdapter.setDefaultCrs(data.getConnectionInfo().getNativeCrs());
        wfsAdapter.setUrls(ImmutableMap.of("default", ImmutableMap.of(WFS.METHOD.GET, data.getConnectionInfo().getUri())));
        wfsAdapter.setNsStore(new XMLNamespaceNormalizer(data.getConnectionInfo().getNamespaces()));

        this.featureTypes = data.getMappings().entrySet().stream()
                .map(entry -> {
                    //TODO
                    String featureTypePath = entry.getValue()
                                       .getMappings()
                                       .keySet()
                                       .iterator()
                                       .next();
                    String localName = featureTypePath.substring(featureTypePath.lastIndexOf(":")+1);
                    String namespace = featureTypePath.substring(0,featureTypePath.lastIndexOf(":"));


                    return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), new QName(namespace, localName));
                })
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        this.featureTypeMappings = data.getMappings();

        this.queryEncoder = new FeatureQueryEncoderWfs(featureTypes, featureTypeMappings, wfsAdapter.getNsStore());

        //TODO: move somewhere else, feature provider only knows source crs, but not target
        EpsgCrs sourceCrs = data.getConnectionInfo().getNativeCrs();
        EpsgCrs wgs84 = new EpsgCrs(4326, true);

        executorService.schedule(() -> {
            try {
                this.defaultTransformer = crsTransformation.getTransformer(sourceCrs, wgs84);
                this.defaultReverseTransformer = crsTransformation.getTransformer(wgs84, sourceCrs);
                LOGGER.debug("TRANSFORMER {} {} -> {} {}", sourceCrs.getCode(), sourceCrs.isForceLongitudeFirst() ? "lonlat" : "latlon", wgs84.getCode(), wgs84.isForceLongitudeFirst() ? "lonlat" : "latlon");
            } catch (Throwable e) {
                LOGGER.error("CRS transformer could not created"/*, e*/);
            }
        }, 3, TimeUnit.SECONDS);
    }

    /*public FeatureProviderWfs(final WFSAdapter wfsAdapter, final Map<String, FeatureTypeConfigurationOld> featureTypes) {
        this.wfsAdapter = wfsAdapter;
        this.featureTypes = featureTypes;
        queryEncoder = new FeatureQueryEncoderWfs(featureTypes, wfsAdapter.getNsStore());
    }

    public FeatureProviderWfs(final AkkaHttp akkaHttp, final WFSAdapter wfsAdapter, final Map<String, FeatureTypeConfigurationOld> featureTypes) {
        this.akkaHttp = akkaHttp;
        this.wfsAdapter = wfsAdapter;
        this.featureTypes = featureTypes;
        queryEncoder = new FeatureQueryEncoderWfs(featureTypes, wfsAdapter.getNsStore());
    }*/

    @Override
    public FeatureStream<GmlConsumer> getFeatureStream(FeatureQuery query) {

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
    public FeatureStream<FeatureTransformer> getFeatureTransformStream(FeatureQuery query) {
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
    public List<String> addFeaturesFromStream(String featureType, Function<FeatureTransformer, RunnableGraph<CompletionStage<Done>>> stream) {
        return ImmutableList.of();
    }

    @Override
    public void updateFeatureFromStream(String featureType, String id, Function<FeatureTransformer, RunnableGraph<CompletionStage<Done>>> stream) {

    }

    @Override
    public void deleteFeature(String featureType, String id) {

    }

    @Override
    public CrsTransformer getCrsTransformer() {
        return defaultTransformer;
    }

    @Override
    public CrsTransformer getReverseCrsTransformer() {
        return defaultReverseTransformer;
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
        return Optional.ofNullable(featureTypeMappings.get(typeName));
            /*return featureTypeMappings.values()
                                      .stream()
                                      .filter(ft -> ft.getName().equals(typeName))
                                      .findFirst()
                                      .map(FeatureTypeConfigurationOld::getMappings);*/
    }
}
