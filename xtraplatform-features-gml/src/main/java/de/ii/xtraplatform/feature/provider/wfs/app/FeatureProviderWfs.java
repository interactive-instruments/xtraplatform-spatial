/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.app;

import akka.NotUsed;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.feature.provider.wfs.FeatureTokenDecoderGml;
import de.ii.xtraplatform.feature.provider.wfs.domain.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.feature.provider.wfs.domain.FeatureProviderWfsData;
import de.ii.xtraplatform.feature.provider.wfs.domain.WfsConnector;
import de.ii.xtraplatform.features.domain.AbstractFeatureProvider;
import de.ii.xtraplatform.features.domain.ConnectorFactory;
import de.ii.xtraplatform.features.domain.ExtentReader;
import de.ii.xtraplatform.features.domain.FeatureConsumer;
import de.ii.xtraplatform.features.domain.FeatureCrs;
import de.ii.xtraplatform.features.domain.FeatureExtents;
import de.ii.xtraplatform.features.domain.FeatureMetadata;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureQueries;
import de.ii.xtraplatform.features.domain.FeatureQueriesPassThrough;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureQueryTransformer;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureSourceStream;
import de.ii.xtraplatform.features.domain.FeatureStorePathParser;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.features.domain.FeatureStream2.ResultOld;
import de.ii.xtraplatform.features.domain.FeatureTokenDecoder;
import de.ii.xtraplatform.features.domain.Metadata;
import de.ii.xtraplatform.store.domain.entities.EntityComponent;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.store.domain.entities.handler.Entity;
import de.ii.xtraplatform.streams.domain.Reactive;
import de.ii.xtraplatform.streams.domain.Reactive.Stream;
import de.ii.xtraplatform.xml.domain.XMLNamespaceNormalizer;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.ws.rs.core.MediaType;
import javax.xml.namespace.QName;
import org.apache.felix.ipojo.annotations.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

@EntityComponent
@Entity(type = FeatureProvider2.ENTITY_TYPE, subType = FeatureProviderWfs.ENTITY_SUB_TYPE, dataClass = FeatureProviderDataV2.class, dataSubClass = FeatureProviderWfsData.class)
public class FeatureProviderWfs extends AbstractFeatureProvider<byte[], String, FeatureProviderConnector.QueryOptions> implements FeatureProvider2, FeatureQueries, FeatureCrs, FeatureExtents, FeatureMetadata,
    FeatureQueriesPassThrough<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderWfs.class);

    static final String ENTITY_SUB_TYPE = "feature/wfs";
    public static final String PROVIDER_TYPE = "WFS";
    private static final MediaType MEDIA_TYPE = new MediaType("application", "gml+xml");

    private final CrsTransformerFactory crsTransformerFactory;
    private final EntityRegistry entityRegistry;

    private FeatureQueryTransformerWfs queryTransformer;
    private FeatureNormalizerWfs featureNormalizer;
    private ExtentReader extentReader;
    private FeatureStorePathParser pathParser;

    public FeatureProviderWfs(@Requires CrsTransformerFactory crsTransformerFactory,
                              @Requires ConnectorFactory connectorFactory,
                              @Requires Reactive reactive,
                              @Requires EntityRegistry entityRegistry) {
        super(connectorFactory, reactive, crsTransformerFactory);

        this.crsTransformerFactory = crsTransformerFactory;
        this.entityRegistry = entityRegistry;
    }

    @Override
    protected boolean onStartup() throws InterruptedException {
        this.pathParser = createPathParser(getData().getConnectionInfo());


        boolean success = super.onStartup();

        if (!success) {
            return false;
        }

        //TODO: remove FeatureSchemaToTypeVisitor
        /*Map<String, FeatureType> types = getData().getTypes()
            .entrySet()
            .stream()
            .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue()
                .accept(new FeatureSchemaToTypeVisitor(entry.getKey()))))
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));*/

        this.queryTransformer = new FeatureQueryTransformerWfs(getTypeInfos(), getData().getTypes(),
            getData().getConnectionInfo(), getData().getNativeCrs().orElse(OgcCrs.CRS84));
        this.featureNormalizer = new FeatureNormalizerWfs(getData().getTypes(), getData().getConnectionInfo()
            .getNamespaces());
        this.extentReader = new ExtentReaderWfs(this, crsTransformerFactory, getData().getNativeCrs().orElse(OgcCrs.CRS84));

        return true;
    }

    private static FeatureStorePathParser createPathParser(ConnectionInfoWfsHttp connectionInfoWfsHttp) {
        return new FeatureStorePathParserWfs(connectionInfoWfsHttp.getNamespaces());
    }

    @Override
    public FeatureProviderWfsData getData() {
        return (FeatureProviderWfsData) super.getData();
    }

    @Override
    protected WfsConnector getConnector() {
        return (WfsConnector) super.getConnector();
    }

    @Override
    protected FeatureStorePathParser getPathParser() {
        return pathParser;
    }

    @Override
    protected FeatureQueryTransformer<String, FeatureProviderConnector.QueryOptions> getQueryTransformer() {
        return queryTransformer;
    }

    @Override
    protected FeatureTokenDecoder<byte[]> getDecoder(FeatureQuery query) {
        Map<String, String> namespaces = getData().getConnectionInfo()
            .getNamespaces();
        XMLNamespaceNormalizer namespaceNormalizer = new XMLNamespaceNormalizer(namespaces);
        FeatureSchema featureSchema = getData().getTypes().get(query.getType());
        String name = featureSchema.getSourcePath().map(sourcePath -> sourcePath.substring(1)).orElse(null);
        QName qualifiedName = new QName(namespaceNormalizer.getNamespaceURI(namespaceNormalizer.extractURI(name)), namespaceNormalizer.getLocalName(name));
        return new FeatureTokenDecoderGml(namespaces, ImmutableList.of(qualifiedName), featureSchema, query);
    }

    @Override
    protected Map<String, Codelist> getCodelists() {
        //TODO
        getData().getCodelists();

        return entityRegistry.getEntitiesForType(Codelist.class).stream().map(codelist -> new SimpleImmutableEntry<>(codelist.getId(), codelist)).collect(
            ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }


    @Override
    public boolean supportsCrs() {
        return super.supportsCrs() && getData().getNativeCrs().isPresent();
    }

    @Override
    public EpsgCrs getNativeCrs() {
        return getData().getNativeCrs().get();
    }

    @Override
    public boolean isCrsSupported(EpsgCrs crs) {
        return Objects.equals(getNativeCrs(), crs) || crsTransformerFactory.isSupported(crs);
    }

    @Override
    public Optional<BoundingBox> getSpatialExtent(String typeName) {
        Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(typeName));

        if (!typeInfo.isPresent()) {
            return Optional.empty();
        }

        try {
            Stream<Optional<BoundingBox>> extentGraph = extentReader.getExtent(typeInfo.get());

            return extentGraph.on(getStreamRunner()).run()
                                    .exceptionally(throwable -> Optional.empty())
                                    .toCompletableFuture()
                                    .join();
        } catch (Throwable e) {
            //continue
            boolean br = true;
        }

        return Optional.empty();
    }

    @Override
    public Optional<BoundingBox> getSpatialExtent(String typeName, EpsgCrs crs) {
        return getSpatialExtent(typeName).flatMap(boundingBox -> crsTransformerFactory.getTransformer(getNativeCrs(), crs, true)
                                                                                      .flatMap(crsTransformer -> {
                                                                                          try {
                                                                                              return Optional.of(crsTransformer.transformBoundingBox(boundingBox));
                                                                                          } catch (CrsTransformationException e) {
                                                                                              return Optional.empty();
                                                                                          }
                                                                                      }));
    }

    @Override
    public Optional<Interval> getTemporalExtent(String typeName, String property) {
        return Optional.empty();
    }

    @Override
    public Optional<Interval> getTemporalExtent(String typeName, String startProperty, String endProperty) {
        return Optional.empty();
    }

    @Override
    public Optional<Metadata> getMetadata() {
        return getConnector().getMetadata();
    }

    @Override
    public MediaType getMediaType() {
        return MEDIA_TYPE;
    }

    @Override
    public FeatureSourceStream<byte[]> getFeatureSourceStream(FeatureQuery query) {
        return new FeatureSourceStream<>() {
            @Override
            public CompletionStage<ResultOld> runWith(FeatureConsumer consumer) {
                Optional<FeatureStoreTypeInfo> typeInfo = Optional
                    .ofNullable(getTypeInfos().get(query.getType()));

                if (!typeInfo.isPresent()) {
                    //TODO: put error message into Result, complete successfully
                    CompletableFuture<ResultOld> promise = new CompletableFuture<>();
                    promise.completeExceptionally(
                        new IllegalStateException("No features available for type"));
                    return promise;
                }

                String transformedQuery = getQueryTransformer()
                    .transformQuery(query, ImmutableMap.of());

                FeatureProviderConnector.QueryOptions options = getQueryTransformer()
                    .getOptions(query);

                Source<byte[], NotUsed> sourceStream = getConnector()
                    .getSourceStream(transformedQuery, options);

                Sink<byte[], CompletionStage<ResultOld>> sink = featureNormalizer
                    .normalizeAndConsume(consumer, query);

                //TODO: use Reactive
                return getStreamRunner().run(sourceStream, sink);
            }

            @Override
            public CompletionStage<ResultOld> runWith2(
                Sink<byte[], CompletionStage<ResultOld>> consumer) {
                return null;
            }
        };
    }
}
