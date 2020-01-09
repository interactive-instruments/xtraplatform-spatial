/**
 * Copyright 2019 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs;

import akka.Done;
import akka.stream.javadsl.Sink;
import akka.util.ByteString;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.api.exceptions.BadRequest;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.feature.provider.api.AbstractFeatureProvider;
import de.ii.xtraplatform.feature.provider.api.Feature;
import de.ii.xtraplatform.feature.provider.api.FeatureConsumer;
import de.ii.xtraplatform.feature.provider.api.FeatureMetadata;
import de.ii.xtraplatform.feature.provider.api.FeatureProperty;
import de.ii.xtraplatform.feature.provider.api.FeatureProvider2;
import de.ii.xtraplatform.feature.provider.api.FeatureProviderMetadataConsumer;
import de.ii.xtraplatform.feature.provider.api.FeatureProviderSchemaConsumer;
import de.ii.xtraplatform.feature.provider.api.FeatureQueries;
import de.ii.xtraplatform.feature.provider.api.FeatureQueriesPassThrough;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
import de.ii.xtraplatform.feature.provider.api.FeatureSchema;
import de.ii.xtraplatform.feature.provider.api.FeatureSourceStream;
import de.ii.xtraplatform.feature.provider.api.FeatureStream2;
import de.ii.xtraplatform.feature.provider.api.FeatureTransformer2;
import de.ii.xtraplatform.feature.provider.api.FeatureType;
import de.ii.xtraplatform.feature.provider.api.ImmutableFeatureQuery;
import de.ii.xtraplatform.feature.provider.api.MappingStatus;
import de.ii.xtraplatform.feature.transformer.api.FeatureProviderDataTransformer;
import de.ii.xtraplatform.feature.transformer.api.FeatureProviderGenerator;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import de.ii.xtraplatform.feature.transformer.api.GmlStreamParser;
import de.ii.xtraplatform.feature.transformer.api.ImmutableFeatureProviderDataTransformer;
import de.ii.xtraplatform.feature.transformer.api.ImmutableFeatureTypeMapping;
import de.ii.xtraplatform.feature.transformer.api.ImmutableSourcePathMapping;
import de.ii.xtraplatform.feature.transformer.api.OnTheFly;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.exceptions.ParseError;
import de.ii.xtraplatform.ogc.api.exceptions.WFSException;
import de.ii.xtraplatform.ogc.api.wfs.DescribeFeatureType;
import de.ii.xtraplatform.ogc.api.wfs.GetCapabilities;
import de.ii.xtraplatform.ogc.api.wfs.WfsRequestEncoder;
import de.ii.xtraplatform.scheduler.api.TaskProgress;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.codehaus.staxmate.SMInputFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.xml.namespace.QName;
import java.io.InputStream;
import java.net.URI;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.ii.xtraplatform.feature.provider.wfs.FeatureProviderWfs.PROVIDER_TYPE;

/**
 * @author zahnen
 */
@Component
@Provides(properties = {@StaticServiceProperty(name = "providerType", type = "java.lang.String", value = PROVIDER_TYPE)})
public class FeatureProviderWfs extends AbstractFeatureProvider implements FeatureProvider2, FeatureQueries, FeatureQueriesPassThrough, FeatureMetadata, FeatureSchema, FeatureProviderGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderWfs.class);

    private static final String SOURCE_FORMAT = "application/gml+xml";
    private static final MediaType MEDIA_TYPE = new MediaType("application", "gml+xml");

    static final String PROVIDER_TYPE = "WFS";

    private final WfsConnector connector;
    private final WfsRequestEncoder wfsRequestEncoder;
    private final Map<String, QName> featureTypeNames;
    private final Map<String, FeatureTypeMapping> featureTypeMappings;
    private final Map<String, FeatureType> featureTypes;
    private final FeatureQueryEncoderWfs queryEncoder;
    private final MappingStatus mappingStatus;
    private final FeatureProviderDataTransformer data;

    FeatureProviderWfs(@Property(name = ".data") FeatureProviderDataTransformer data,
                       @Property(name = ".connector") WfsConnector connector) {
        ConnectionInfoWfsHttp connectionInfo = (ConnectionInfoWfsHttp) data.getConnectionInfo();

        this.wfsRequestEncoder = new WfsRequestEncoder();
        wfsRequestEncoder.setVersion(connectionInfo.getVersion());
        wfsRequestEncoder.setGmlVersion(connectionInfo.getGmlVersion());
        wfsRequestEncoder.setUrls(ImmutableMap.of("default", ImmutableMap.of(WFS.METHOD.GET, FeatureProviderDataWfsFromMetadata.parseAndCleanWfsUrl(connectionInfo.getUri()), WFS.METHOD.POST, FeatureProviderDataWfsFromMetadata.parseAndCleanWfsUrl(connectionInfo.getUri()))));
        wfsRequestEncoder.setNsStore(new XMLNamespaceNormalizer(connectionInfo.getNamespaces()));

        this.featureTypeNames = !data.getLocalFeatureTypeNames()
                                     .isEmpty() ? data.getLocalFeatureTypeNames() : data.getMappings()
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
        //TODO
        this.featureTypes = new HashMap<>();

        Map<String, FeatureTypeMapping> queryMappings;
        if (data.getMappingStatus()
                .getEnabled()) {
            queryMappings = featureTypeMappings;
        } else {
            queryMappings = getOnTheFlyMappings(data.getLocalFeatureTypeNames()
                                                    .keySet());
        }

        //TODO: if mapping disabled, create dummy mappings for gml:id, depending on version (TODO: set gmlVersion depending on wfsVersion)
        this.queryEncoder = new FeatureQueryEncoderWfs(featureTypeNames, queryMappings, wfsRequestEncoder.getNsStore(), wfsRequestEncoder);

        this.connector = connector;
        connector.setQueryEncoder(queryEncoder);

        this.mappingStatus = data.getMappingStatus();

        this.data = data;
    }

    //@Override
    /*public FeatureStream<FeatureConsumer> getFeatureStream(FeatureQuery query) {
        if (!queryEncoder.isValid(query)) {
            throw new IllegalArgumentException("Feature type '" + query.getType() + "' not found");
        }

        return (featureConsumer, timer) -> {
            Optional<FeatureTypeMapping> featureTypeMapping = getFeatureTypeMapping(query.getType());
            Map<QName, List<String>> resolvableTypes = featureTypeMapping.isPresent() ? getResolvableTypes(featureTypeMapping.get()) : ImmutableMap.of();

            List<QName> featureTypes = resolvableTypes.isEmpty() ? ImmutableList.of(queryEncoder.getFeatureTypeName(query)) : ImmutableList.<QName>builder().add(queryEncoder.getFeatureTypeName(query))
                                                                                                                                                            .addAll(resolvableTypes.keySet())
                                                                                                                                                            .build();

            Sink<ByteString, CompletionStage<Done>> parser = GmlStreamParser.consume(featureTypes, featureConsumer);

            Map<String, String> additionalQueryParameters;

            if (!resolvableTypes.isEmpty()) {
                //TODO depth???
                additionalQueryParameters = ImmutableMap.of("resolve", "local", "resolvedepth", "1");
            } else {
                additionalQueryParameters = ImmutableMap.of();
            }

            return connector.runQuery(query, parser, additionalQueryParameters);
        };
    }

    //TODO interface ResolveRelations
    //@Override
    public FeatureStream<FeatureTransformer> getFeatureTransformStream(FeatureQuery query) {
        // if query crs is native or not supported by provider, remove from query
        boolean useProviderDefaultCrs = data.getNativeCrs()
                                            .getCode() == query.getCrs()
                                                               .getCode()
                || !supportsCrs(query.getCrs());

        FeatureQuery finalQuery = useProviderDefaultCrs ? ImmutableFeatureQuery.builder()
                                                                               .from(query)
                                                                               .crs(null)
                                                                               .build() : query;

        return (featureTransformer, timer) -> {
            Optional<FeatureTypeMapping> featureTypeMapping = getFeatureTypeMapping(finalQuery.getType());

            if (!featureTypeMapping.isPresent()) {
                try {
                    OnTheFly onTheFly = (OnTheFly) featureTransformer;
                } catch (ClassCastException e) {

                    CompletableFuture<Done> promise = new CompletableFuture<>();
                    promise.completeExceptionally(new IllegalStateException("No features available for type"));
                    return promise;
                }
            }

            Map<QName, List<String>> resolvableTypes = getResolvableTypes(featureTypeMapping.get());


            Sink<ByteString, CompletionStage<Done>> transformer = GmlStreamParser.transform(queryEncoder.getFeatureTypeName(finalQuery), featureTypeMapping.orElse(null), featureTransformer, finalQuery.getFields(), resolvableTypes);

            Map<String, String> additionalQueryParameters;

            if (!resolvableTypes.isEmpty()) {
                //TODO depth???
                additionalQueryParameters = ImmutableMap.of("resolve", "local", "resolvedepth", "1");
            } else {
                additionalQueryParameters = ImmutableMap.of();
            }

            if (Objects.nonNull(timer))
                timer.stop();

            return connector.runQuery(finalQuery, transformer, additionalQueryParameters);
        };
    }*/

    private Map<String, FeatureTypeMapping> getOnTheFlyMappings(Set<String> featureTypes) {
        return featureTypes.stream()
                           .map(featureType -> new AbstractMap.SimpleEntry<>(featureType, new ImmutableFeatureTypeMapping.Builder()
                                   .mappings(ImmutableMap.of("http://www.opengis.net/gml/3.2:@id", new ImmutableSourcePathMapping.Builder()
                                           .build()))
                                   .build()))
                           .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<QName, List<String>> getResolvableTypes(FeatureType featureType) {
        // TODO factor out, move into derived in FeatureTypeMapping
        List<List<String>> embedRefs = featureType.getProperties()
                                                  .values()
                                                  .stream()
                                                  .filter(FeatureProperty::isReferenceEmbed)
                                                  //TODO
                                                  .map(featureProperty -> Splitter.on('/')
                                                                                  .omitEmptyStrings()
                                                                                  .splitToList(featureProperty.getPath()))
                                                  .collect(Collectors.toList());

        List<List<String>> embedRoots = embedRefs.stream()
                                                 .map(path -> path.subList(0, path.size() - 1))
                                                 .collect(Collectors.toList());

        return featureType.getProperties()
                          .values()
                          .stream()
                          //TODO
                          .map(featureProperty -> Splitter.on('/')
                                                          .omitEmptyStrings()
                                                          .splitToList(featureProperty.getPath()))
                          .filter(path -> embedRoots.stream()
                                                    .anyMatch(root -> path.subList(0, root.size())
                                                                          .equals(root)) && embedRefs.stream()
                                                                                                     .noneMatch(ref -> ref.equals(path)))
                          .map(path -> embedRoots.stream()
                                                 .filter(root -> path.subList(0, root.size())
                                                                     .equals(root))
                                                 .findFirst()
                                                 .map(root -> path.subList(0, root.size() + 1))
                                                 .get())
                          .distinct()
                          .map(path -> {
                              String type = path.get(path.size() - 1);
                              QName qn = new QName(type.substring(0, type.lastIndexOf(":")), type.substring(type.lastIndexOf(":") + 1));
                              return new AbstractMap.SimpleImmutableEntry<>(qn, path);
                          })
                          .filter(entry -> featureTypeNames.values()
                                                           .stream()
                                                           .anyMatch(ft -> ft.equals(entry.getKey())))
                          .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public boolean supportsCrs(EpsgCrs crs) {
        return data.getNativeCrs()
                   .getCode() == crs.getCode()
                || data.getOtherCrs()
                       .stream()
                       .anyMatch(epsgCrs -> epsgCrs.getCode() == crs.getCode());
    }

    //TODO: can't the crs transformer handle the swapping, then we can remove this
    @Override
    public boolean shouldSwapCoordinates(EpsgCrs crs) {
        return supportsCrs(crs) && Stream.concat(Stream.of(data.getNativeCrs()), data.getOtherCrs()
                                                                                     .stream())
                                         .filter(epsgCrs -> epsgCrs.getCode() == crs.getCode())
                                         .findFirst()
                                         .map(epsgCrs -> epsgCrs.isForceLongitudeFirst() != crs.isForceLongitudeFirst())
                                         .orElse(false);
    }

    //@Override
    public String getSourceFormat() {
        return SOURCE_FORMAT;
    }

    private Optional<FeatureTypeMapping> getFeatureTypeMapping(final String typeName) {
        return Optional.ofNullable(featureTypeMappings.get(typeName));
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

            BadRequest pe = new BadRequest("Retrieving or parsing of GetCapabilities failed.");
            pe.addDetail(ex.getMsg());
            for (String det : ex.getDetails()) {
                pe.addDetail(det);
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Retrieving or parsing of GetCapabilities failed: {}", ex.getMessage(), ex.getCause());
            }
            throw pe;
        }
    }

    private static SMInputFactory staxFactory = new SMInputFactory(new InputFactoryImpl());

    private void analyzeCapabilities(FeatureProviderMetadataConsumer metadataConsumer,
                                     WFS.VERSION version) throws ParseError {

        GetCapabilities getCapabilities;

        if (version == null) {
            LOGGER.debug("Analyzing Capabilities");
            getCapabilities = new GetCapabilities();
        } else {
            LOGGER.debug("Analyzing Capabilities (version: {})", version.toString());
            getCapabilities = new GetCapabilities(version);
        }
        InputStream source = connector.runWfsOperation(getCapabilities);

        WFSCapabilitiesParser wfsParser = new WFSCapabilitiesParser(metadataConsumer, staxFactory);

        wfsParser.parse(source);
    }

    @Override
    public FeatureProviderMetadataConsumer getDataGenerator(FeatureProviderDataTransformer data,
                                                            ImmutableFeatureProviderDataTransformer.Builder dataBuilder) {
        return new FeatureProviderDataWfsFromMetadata(data, dataBuilder);
    }

    @Override
    public void getSchema(FeatureProviderSchemaConsumer schemaConsumer, Map<String, QName> featureTypes,
                          TaskProgress taskProgress) {
        analyzeFeatureTypes(schemaConsumer, featureTypes, taskProgress);
    }

    public void analyzeFeatureTypes(FeatureProviderSchemaConsumer schemaConsumer, Map<String, QName> featureTypes,
                                    TaskProgress taskProgress) {
        if (mappingStatus.getEnabled() && mappingStatus.getLoading()) {

            Map<String, List<String>> featureTypesByNamespace = getSupportedFeatureTypesPerNamespace(featureTypes);

            if (!featureTypesByNamespace.isEmpty()) {
                analyzeFeatureTypesWithDescribeFeatureType(schemaConsumer, featureTypesByNamespace, taskProgress);
            }

            // only log warnings about timeouts in the analysis phase
            //wfsAdapter.setIgnoreTimeouts(true);
        }
    }

    private void analyzeFeatureTypesWithDescribeFeatureType(FeatureProviderSchemaConsumer schemaConsumer,
                                                            Map<String, List<String>> featureTypesByNamespace,
                                                            TaskProgress taskProgress) {
        URI baseUri = wfsRequestEncoder.findUrl(WFS.OPERATION.DESCRIBE_FEATURE_TYPE, WFS.METHOD.GET);

        InputStream source = connector.runWfsOperation(new DescribeFeatureType());

        // create mappings
        GMLSchemaParser gmlSchemaParser = new GMLSchemaParser(ImmutableList.of(schemaConsumer), baseUri);

        gmlSchemaParser.parse(source, featureTypesByNamespace, taskProgress);
    }

    private Map<String, List<String>> getSupportedFeatureTypesPerNamespace(Map<String, QName> featureTypes) {
        Map<String, List<String>> featureTypesPerNamespace = new HashMap<>();

        for (QName featureType : featureTypes.values()) {
            if (!featureTypesPerNamespace.containsKey(featureType.getNamespaceURI())) {
                featureTypesPerNamespace.put(featureType.getNamespaceURI(), new ArrayList<>());
            }
            featureTypesPerNamespace.get(featureType.getNamespaceURI())
                                    .add(featureType.getLocalPart());
        }

        return featureTypesPerNamespace;
    }


    @Override
    public FeatureProviderSchemaConsumer getMappingGenerator(
            FeatureProviderDataTransformer data,
            ImmutableFeatureProviderDataTransformer.Builder dataBuilder,
            List<TargetMappingProviderFromGml> mappingProviders) {
        return new FeatureProviderDataWfsFromSchema(data, dataBuilder, mappingProviders);
    }

    //TODO interface FeatureRelations
    @Override
    public FeatureStream2 getFeatureStream2(FeatureQuery query) {
        return new FeatureStream2() {
            @Override
            public CompletionStage<Result> runWith(FeatureTransformer2 featureTransformer) {
                // if query crs is native or not supported by provider, remove from query
                boolean useProviderDefaultCrs = data.getNativeCrs()
                                                    .getCode() == query.getCrs()
                                                                       .getCode()
                        || !supportsCrs(query.getCrs());

                FeatureQuery finalQuery = useProviderDefaultCrs ? ImmutableFeatureQuery.builder()
                                                                                       .from(query)
                                                                                       .crs(null)
                                                                                       .build() : query;
                //Optional<FeatureTypeMapping> featureTypeMapping = getFeatureTypeMapping(finalQuery.getType());
                Optional<FeatureType> featureType = Optional.ofNullable(featureTypes.get(finalQuery.getType()));

                if (!featureType.isPresent()) {
                    try {
                        OnTheFly onTheFly = (OnTheFly) featureTransformer;
                    } catch (ClassCastException e) {
                        //TODO: put error message into Result, complete successfully
                        CompletableFuture<Result> promise = new CompletableFuture<>();
                        promise.completeExceptionally(new IllegalStateException("No features available for type"));
                        return promise;
                    }
                }

                Map<QName, List<String>> resolvableTypes = getResolvableTypes(featureType.get());


                Sink<ByteString, CompletionStage<Done>> transformer = GmlStreamParser.transform(queryEncoder.getFeatureTypeName(finalQuery), featureType.orElse(null), featureTransformer, finalQuery.getFields(), resolvableTypes);

                Map<String, String> additionalQueryParameters;

                if (!resolvableTypes.isEmpty()) {
                    //TODO depth???
                    additionalQueryParameters = ImmutableMap.of("resolve", "local", "resolvedepth", "1");
                } else {
                    additionalQueryParameters = ImmutableMap.of();
                }


                return connector.runQuery(finalQuery, transformer, additionalQueryParameters)
                                .thenApply(done -> () -> true);

            }

            @Override
            public CompletionStage<Result> runWith(Sink<Feature, CompletionStage<Result>> transformer) {
                return null;
            }
        };
    }

    @Override
    public long getFeatureCount(FeatureQuery featureQuery) {
        return 0;
    }

    @Override
    public MediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public FeatureSourceStream getFeatureSourceStream(FeatureQuery query) {
        return new FeatureSourceStream() {
            @Override
            public CompletionStage<FeatureStream2.Result> runWith(FeatureConsumer featureConsumer) {
                if (!queryEncoder.isValid(query)) {
                    throw new IllegalArgumentException("Feature type '" + query.getType() + "' not found");
                }

                // if query crs is native or not supported by provider, remove from query
                boolean useProviderDefaultCrs = data.getNativeCrs()
                                                    .getCode() == query.getCrs()
                                                                       .getCode()
                        || !supportsCrs(query.getCrs());

                FeatureQuery finalQuery = useProviderDefaultCrs ? ImmutableFeatureQuery.builder()
                                                                                       .from(query)
                                                                                       .crs(null)
                                                                                       .build() : query;

                Optional<FeatureType> featureType = Optional.ofNullable(featureTypes.get(finalQuery.getType()));

                Map<QName, List<String>> resolvableTypes = featureType.isPresent() ? getResolvableTypes(featureType.get()) : ImmutableMap.of();

                List<QName> featureTypes = resolvableTypes.isEmpty() ? ImmutableList.of(queryEncoder.getFeatureTypeName(finalQuery)) : ImmutableList.<QName>builder().add(queryEncoder.getFeatureTypeName(finalQuery))
                                                                                                                                                                     .addAll(resolvableTypes.keySet())
                                                                                                                                                                     .build();

                Sink<ByteString, CompletionStage<Done>> parser = GmlStreamParser.consume(featureTypes, featureConsumer);//TODO

                Map<String, String> additionalQueryParameters;

                if (!resolvableTypes.isEmpty()) {
                    //TODO depth???
                    additionalQueryParameters = ImmutableMap.of("resolve", "local", "resolvedepth", "1");
                } else {
                    additionalQueryParameters = ImmutableMap.of();
                }

                return connector.runQuery(finalQuery, parser, additionalQueryParameters)
                                .thenApply(done -> () -> true);
            }

            @Override
            public CompletionStage<FeatureStream2.Result> runWith2(Sink consumer) {
                return null;
            }
        };
    }
}
