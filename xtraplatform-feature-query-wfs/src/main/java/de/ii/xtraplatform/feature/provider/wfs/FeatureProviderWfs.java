/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs;

import akka.Done;
import akka.NotUsed;
import akka.japi.Pair;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.StreamConverters;
import akka.util.ByteString;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.akka.http.AkkaHttp;
import de.ii.xtraplatform.crs.api.CrsTransformation;
import de.ii.xtraplatform.crs.api.CrsTransformer;
import de.ii.xtraplatform.feature.query.api.FeatureProvider;
import de.ii.xtraplatform.feature.query.api.FeatureProviderMetadataConsumer;
import de.ii.xtraplatform.feature.query.api.FeatureQuery;
import de.ii.xtraplatform.feature.query.api.FeatureStream;
import de.ii.xtraplatform.feature.transformer.api.FeatureProviderSchemaConsumer;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import de.ii.xtraplatform.feature.transformer.api.GmlConsumer;
import de.ii.xtraplatform.feature.transformer.api.GmlStreamParser;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import de.ii.xtraplatform.feature.transformer.api.TransformingFeatureProvider;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.exceptions.ParseError;
import de.ii.xtraplatform.ogc.api.exceptions.SchemaParseException;
import de.ii.xtraplatform.ogc.api.exceptions.WFSException;
import de.ii.xtraplatform.ogc.api.wfs.client.DescribeFeatureType;
import de.ii.xtraplatform.ogc.api.wfs.client.GetCapabilities;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSAdapter;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSRequest;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.codehaus.staxmate.SMInputFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.net.URI;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static de.ii.xtraplatform.feature.provider.wfs.FeatureProviderWfs.PROVIDER_TYPE;

/**
 * @author zahnen
 */
@Component
@Provides(properties = {@StaticServiceProperty(name = "providerType", type = "java.lang.String", value = PROVIDER_TYPE)})
public class FeatureProviderWfs implements GmlProvider, FeatureProvider.MetadataAware, TransformingFeatureProvider.SchemaAware, TransformingFeatureProvider.DataGenerator<FeatureProviderDataWfs> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderWfs.class);

    private static final String SOURCE_FORMAT = "application/xml+gml";

    static final String PROVIDER_TYPE = "WFS";

    @Requires
    private AkkaHttp akkaHttp;

    private final WFSAdapter wfsAdapter;
    private final Map<String, QName> featureTypes;
    private final Map<String, FeatureTypeMapping> featureTypeMappings;
    private final FeatureQueryEncoderWfs queryEncoder;
    private final boolean useHttpPost;

    FeatureProviderWfs(@Requires CrsTransformation crsTransformation, @Property(name = ".data") FeatureProviderDataWfs data) {
        this.wfsAdapter = new WFSAdapter();
        wfsAdapter.setVersion(data.getConnectionInfo()
                                  .getVersion());
        wfsAdapter.setGmlVersion(data.getConnectionInfo()
                                     .getGmlVersion());
        wfsAdapter.setDefaultCrs(data.getNativeCrs());
        wfsAdapter.setUrls(ImmutableMap.of("default", ImmutableMap.of(WFS.METHOD.GET, data.getConnectionInfo()
                                                                                          .getUri(), WFS.METHOD.POST, data.getConnectionInfo()
                                                                                                                         .getUri())));
        wfsAdapter.setNsStore(new XMLNamespaceNormalizer(data.getConnectionInfo()
                                                             .getNamespaces()));

        this.featureTypes = !data.getFeatureTypes().isEmpty() ? data.getFeatureTypes() : data.getMappings()
                                .entrySet()
                                .stream()
                                .map(entry -> {
                                    //TODO
                                    String featureTypePath = entry.getValue()
                                                                  .getMappings()
                                                                  .keySet()
                                                                  .iterator()
                                                                  .next();
                                    String localName = featureTypePath.substring(featureTypePath.lastIndexOf(":") + 1);
                                    String namespace = featureTypePath.substring(0, featureTypePath.lastIndexOf(":"));


                                    return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), new QName(namespace, localName));
                                })
                                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        this.featureTypeMappings = data.getMappings();

        this.queryEncoder = new FeatureQueryEncoderWfs(featureTypes, featureTypeMappings, wfsAdapter.getNsStore());

        this.useHttpPost = data.getConnectionInfo().getMethod() == ConnectionInfo.METHOD.POST;
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
        return featureConsumer -> {
            //StreamingGmlTransformerFlow.transformer(featureType, featureTypeMapping, null/*FeatureConsumer*/);
            Sink<ByteString, CompletionStage<Done>> parser = GmlStreamParser.consume(queryEncoder.getFeatureTypeName(query)
                                                                                                 .get(), featureConsumer);
            return runQuery(query, parser);
        };


        // TODO: measure performance with files to compare processing time only
//        Source<ByteString, Date> fromFile = FileIO.fromFile(new File("/home/zahnen/development/ldproxy/artillery/flurstueck-" + count.get() + "-" + page.get() + ".xml"))
//                .mapMaterializedValue(nu -> new Date());

        //return queryEncoder.encode(query)
        //                   .map(getFeature -> new WFSRequest(wfsAdapter, getFeature).getResponse());
    }

    @Override
    public FeatureStream<FeatureTransformer> getFeatureTransformStream(FeatureQuery query) {
        return featureTransformer -> {
            Optional<FeatureTypeMapping> featureTypeMapping = getFeatureTypeMapping(query.getType());

            if (!featureTypeMapping.isPresent()) {
                CompletableFuture<Done> promise = new CompletableFuture<>();
                promise.completeExceptionally(new IllegalStateException("No features available for type"));
                return promise;
            }

            //StreamingGmlTransformerFlow.transformer(featureType, featureTypeMapping, null/*FeatureConsumer*/);
            Sink<ByteString, CompletionStage<Done>> parser = GmlStreamParser.transform(queryEncoder.getFeatureTypeName(query)
                                                                                                   .get(), featureTypeMapping.get(), featureTransformer, query.getFields());
            return runQuery(query, parser);
        };
    }

    private CompletionStage<Done> runQuery(FeatureQuery query, Sink<ByteString, CompletionStage<Done>> parser) {
        Source<ByteString, NotUsed> source;
        if (useHttpPost) {
            Pair<String, String> request = encodeFeatureQueryPost(query).get();
            source = akkaHttp.postXml(request.first(), request.second());
        } else {
            source = akkaHttp.get(encodeFeatureQuery(query).get());
        }

        return source
                .runWith(parser, akkaHttp.getMaterializer())
                .exceptionally(throwable -> {
                    LOGGER.error("Feature stream error", throwable);
                    return Done.getInstance();
                });
    }

    @Override
    public List<String> addFeaturesFromStream(String featureType, CrsTransformer crsTransformer, Function<FeatureTransformer, RunnableGraph<CompletionStage<Done>>> stream) {
        return ImmutableList.of();
    }

    @Override
    public void updateFeatureFromStream(String featureType, String id, CrsTransformer crsTransformer, Function<FeatureTransformer, RunnableGraph<CompletionStage<Done>>> stream) {

    }

    @Override
    public void deleteFeature(String featureType, String id) {

    }

    @Override
    public Optional<String> encodeFeatureQuery(FeatureQuery query) {
        return queryEncoder.encode(query)
                           .map(getFeature -> new WFSRequest(wfsAdapter, getFeature).getAsUrl());
    }

    public Optional<Pair<String, String>> encodeFeatureQueryPost(FeatureQuery query) {
        return queryEncoder.encode(query)
                           .map(getFeature -> new WFSRequest(wfsAdapter, getFeature).getAsUrlAndBody());
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

    @Override
    public void getMetadata(FeatureProviderMetadataConsumer metadataConsumer) {
        analyzeCapabilities(metadataConsumer);
    }



    private void analyzeCapabilities(FeatureProviderMetadataConsumer metadataConsumer) {
        try {
            analyzeCapabilities(metadataConsumer, null);
        } catch (WFSException ex) {
            for (WFS.VERSION version : WFS.VERSION.values()) {
                try {
                    analyzeCapabilities(metadataConsumer, version);
                    return;
                } catch (WFSException ex2) {
                    // ignore
                }
            }

            ParseError pe = new ParseError("Parsing of GetCapabilities response failed.");
            pe.addDetail(ex.getMsg());
            for (String det : ex.getDetails()) {
                pe.addDetail(det);
            }
            throw pe;
        }
    }

    static SMInputFactory staxFactory = new SMInputFactory(new InputFactoryImpl());

    private void analyzeCapabilities(FeatureProviderMetadataConsumer metadataConsumer, WFS.VERSION version) throws ParseError {

        GetCapabilities getCapabilities;

        if (version == null) {
            LOGGER.debug("Analyzing Capabilities");
            getCapabilities = new GetCapabilities();
        } else {
            LOGGER.debug("Analyzing Capabilities (version: {})", version.toString());
            getCapabilities = new GetCapabilities(version);
        }
        InputStream source = akkaHttp.get(new WFSRequest(wfsAdapter, getCapabilities).getAsUrl())
                                     .runWith(StreamConverters.asInputStream(), akkaHttp.getMaterializer());



        WFSCapabilitiesParser wfsParser = new WFSCapabilitiesParser(metadataConsumer, staxFactory);

        wfsParser.parse(source);
    }

    @Override
    public FeatureProviderMetadataConsumer getDataGenerator(FeatureProviderDataWfs data) {
        return new FeatureProviderDataWfsFromMetadata((ModifiableFeatureProviderDataWfs) data);
    }

    @Override
    public void getSchema(FeatureProviderSchemaConsumer schemaConsumer, Map<String,QName> featureTypes) {
        analyzeFeatureTypes(schemaConsumer, featureTypes);
    }

    public void analyzeFeatureTypes(FeatureProviderSchemaConsumer schemaConsumer, Map<String,QName> featureTypes) {
        //TODO FeatureTypeMappingStatus mappingStatus = serviceProperties.getMappingStatus();

        //mappingStatus.setEnabled(!disableMapping);
        //mappingStatus.setLoading(!disableMapping);

        // TODO: if loading, run analysis in background queue
        // separate (or this?) function, start from store
        //if (mappingStatus.isEnabled() && mappingStatus.isLoading()) {
            //mappingStatus.setEnabled(true);
            //mappingStatus.setLoading(true);



            Map<String, List<String>> featureTypesByNamespace = retrieveSupportedFeatureTypesPerNamespace(featureTypes);

            if (!featureTypes.isEmpty()) {

                try {
                    analyzeFeatureTypesWithDescribeFeatureType(schemaConsumer, featureTypesByNamespace);

                    //mappingStatus.setLoading(false);
                    //mappingStatus.setSupported(true);
                } catch (Exception ex) {
                    // TODO: message should be a service level warning
                    /*mappingStatus.setLoading(false);
                    mappingStatus.setSupported(false);
                    mappingStatus.setErrorMessage(ex.getMessage());
                    if (ex.getClass() == SchemaParseException.class) {
                        mappingStatus.setErrorMessageDetails(((SchemaParseException)ex).getDetails());
                    }*/

                    /*TODO try {
                        analyzeFeatureTypesWithGetFeature(featureTypesByNamespace);

                        mappingStatus.setLoading(false);
                        mappingStatus.setSupported(true);
                    } catch (Exception ex2) {
                        // TODO: we should never get here if we have a test for a working GetFeature
                        mappingStatus.setLoading(false);
                        mappingStatus.setSupported(false);
                        mappingStatus.setErrorMessage(ex2.getMessage());
                        if (ex2.getClass() == SchemaParseException.class) {
                            mappingStatus.setErrorMessageDetails(((SchemaParseException)ex2).getDetails());
                        }
                    }*/
                }
            }

            // only log warnings about timeouts in the analysis phase
            //wfsAdapter.setIgnoreTimeouts(true);
        //}
    }

    private void analyzeFeatureTypesWithDescribeFeatureType(FeatureProviderSchemaConsumer schemaConsumer, Map<String, List<String>> featureTypesByNamespace) throws SchemaParseException {
        URI baseUri = wfsAdapter.findUrl(WFS.OPERATION.DESCRIBE_FEATURE_TYPE, WFS.METHOD.GET);

        InputStream source = akkaHttp.get(new WFSRequest(wfsAdapter, new DescribeFeatureType()).getAsUrl())
                                     .runWith(StreamConverters.asInputStream(), akkaHttp.getMaterializer());

        // create mappings
        GMLSchemaParser gmlSchemaParser;
        // TODO: temporary basic auth hack
        //if (wfs.usesBasicAuth()) {
        //    gmlParser = new GMLSchemaParser(analyzers, baseUri, new OGCEntityResolver(sslHttpClient, wfs.getUser(), wfs.getPassword()));
        //} else {
        gmlSchemaParser = new GMLSchemaParser(ImmutableList.of(schemaConsumer), baseUri);
        //}

        gmlSchemaParser.parse(source, featureTypesByNamespace);
    }

    private Map<String, List<String>> retrieveSupportedFeatureTypesPerNamespace(Map<String,QName> featureTypes) {
        Map<String, List<String>> featureTypesPerNamespace = new HashMap<>();

        for (QName featureType : featureTypes.values()) {
            if (!featureTypesPerNamespace.containsKey(featureType.getNamespaceURI())) {
                featureTypesPerNamespace.put(featureType.getNamespaceURI(), new ArrayList<>());
            }
            featureTypesPerNamespace.get(featureType.getNamespaceURI()).add(featureType.getLocalPart());
        }

        return featureTypesPerNamespace;
    }


    @Override
    public FeatureProviderSchemaConsumer getMappingGenerator(FeatureProviderDataWfs data, List<TargetMappingProviderFromGml> mappingProviders) {
        return new FeatureProviderDataWfsFromSchema((ModifiableFeatureProviderDataWfs) data, mappingProviders);
    }
}
