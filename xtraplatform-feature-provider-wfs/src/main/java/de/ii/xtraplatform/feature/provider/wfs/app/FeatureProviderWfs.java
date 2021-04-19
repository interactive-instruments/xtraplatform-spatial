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
import akka.util.ByteString;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
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
import de.ii.xtraplatform.features.domain.FeatureNormalizer;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureQueries;
import de.ii.xtraplatform.features.domain.FeatureQueriesPassThrough;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureQueryTransformer;
import de.ii.xtraplatform.features.domain.FeatureSchemaToTypeVisitor;
import de.ii.xtraplatform.features.domain.FeatureSourceStream;
import de.ii.xtraplatform.features.domain.FeatureStorePathParser;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.features.domain.FeatureStream2.Result;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.features.domain.Metadata;
import de.ii.xtraplatform.store.domain.entities.EntityComponent;
import de.ii.xtraplatform.store.domain.entities.handler.Entity;
import de.ii.xtraplatform.streams.domain.ActorSystemProvider;
import de.ii.xtraplatform.streams.domain.RunnableGraphWithMdc;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.ws.rs.core.MediaType;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

@EntityComponent
@Entity(type = FeatureProvider2.ENTITY_TYPE, subType = FeatureProviderWfs.ENTITY_SUB_TYPE, dataClass = FeatureProviderDataV2.class, dataSubClass = FeatureProviderWfsData.class)
public class FeatureProviderWfs extends AbstractFeatureProvider<ByteString, String, FeatureProviderConnector.QueryOptions> implements FeatureProvider2, FeatureQueries, FeatureCrs, FeatureExtents, FeatureMetadata,
    FeatureQueriesPassThrough<ByteString> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderWfs.class);

    static final String ENTITY_SUB_TYPE = "feature/wfs";
    public static final String PROVIDER_TYPE = "WFS";
    private static final MediaType MEDIA_TYPE = new MediaType("application", "gml+xml");

    private final CrsTransformerFactory crsTransformerFactory;
    private FeatureQueryTransformerWfs queryTransformer;
    private FeatureNormalizerWfs featureNormalizer;
    private ExtentReader extentReader;
    private FeatureStorePathParser pathParser;

    public FeatureProviderWfs(@Context BundleContext context,
                              @Requires ActorSystemProvider actorSystemProvider,
                              @Requires CrsTransformerFactory crsTransformerFactory,
                              @Requires ConnectorFactory connectorFactory) {
        super(context, actorSystemProvider, connectorFactory);

        this.crsTransformerFactory = crsTransformerFactory;
    }

    @Override
    protected boolean onStartup() throws InterruptedException {
        this.pathParser = createPathParser(getData().getConnectionInfo());


        boolean success = super.onStartup();

        if (!success) {
            return false;
        }

        Map<String, FeatureType> types = getData().getTypes()
            .entrySet()
            .stream()
            .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue()
                .accept(new FeatureSchemaToTypeVisitor(entry.getKey()))))
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        this.queryTransformer = new FeatureQueryTransformerWfs(getTypeInfos(), types, getData().getTypes(),
            getData().getConnectionInfo(), getData().getNativeCrs().orElse(OgcCrs.CRS84));
        this.featureNormalizer = new FeatureNormalizerWfs(getTypeInfos(), types, getData().getTypes(), getData().getConnectionInfo()
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
    protected FeatureNormalizer<ByteString> getNormalizer() {
        return featureNormalizer;
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
        return Objects.equals(getNativeCrs(), crs) || crsTransformerFactory.isCrsSupported(crs);
    }

    @Override
    public Optional<BoundingBox> getSpatialExtent(String typeName) {
        Optional<FeatureStoreTypeInfo> typeInfo = Optional.ofNullable(getTypeInfos().get(typeName));

        if (!typeInfo.isPresent()) {
            return Optional.empty();
        }

        try {
            RunnableGraphWithMdc<CompletionStage<Optional<BoundingBox>>> extentGraph = extentReader.getExtent(typeInfo.get());
            return getStreamRunner().run(extentGraph)
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
        return getSpatialExtent(typeName).flatMap(boundingBox -> crsTransformerFactory.getTransformer(getNativeCrs(), crs)
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
    public FeatureSourceStream<ByteString> getFeatureSourceStream(FeatureQuery query) {
        return new FeatureSourceStream<>() {
            @Override
            public CompletionStage<Result> runWith(FeatureConsumer consumer) {
                Optional<FeatureStoreTypeInfo> typeInfo = Optional
                    .ofNullable(getTypeInfos().get(query.getType()));

                if (!typeInfo.isPresent()) {
                    //TODO: put error message into Result, complete successfully
                    CompletableFuture<Result> promise = new CompletableFuture<>();
                    promise.completeExceptionally(
                        new IllegalStateException("No features available for type"));
                    return promise;
                }

                String transformedQuery = getQueryTransformer()
                    .transformQuery(query, ImmutableMap.of());

                FeatureProviderConnector.QueryOptions options = getQueryTransformer()
                    .getOptions(query);

                Source<ByteString, NotUsed> sourceStream = getConnector()
                    .getSourceStream(transformedQuery, options);

                Sink<ByteString, CompletionStage<Result>> sink = featureNormalizer
                    .normalizeAndConsume(consumer, query);

                return getStreamRunner().run(sourceStream, sink);
            }

            @Override
            public CompletionStage<Result> runWith2(
                Sink<ByteString, CompletionStage<Result>> consumer) {
                return null;
            }
        };
    }
}
