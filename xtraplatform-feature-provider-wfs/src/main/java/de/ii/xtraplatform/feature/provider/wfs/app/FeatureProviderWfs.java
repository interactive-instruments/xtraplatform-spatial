/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.app;

import akka.util.ByteString;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.akka.ActorSystemProvider;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.entities.domain.EntityComponent;
import de.ii.xtraplatform.entities.domain.handler.Entity;
import de.ii.xtraplatform.feature.provider.api.ConnectorFactory;
import de.ii.xtraplatform.feature.provider.wfs.domain.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.feature.provider.wfs.domain.WfsConnector;
import de.ii.xtraplatform.features.app.FeatureSchemaToTypeVisitor;
import de.ii.xtraplatform.features.domain.AbstractFeatureProvider;
import de.ii.xtraplatform.features.domain.ExtentReader;
import de.ii.xtraplatform.features.domain.FeatureCrs;
import de.ii.xtraplatform.features.domain.FeatureExtents;
import de.ii.xtraplatform.features.domain.FeatureMetadata;
import de.ii.xtraplatform.features.domain.FeatureNormalizer;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureQueries;
import de.ii.xtraplatform.features.domain.FeatureQueryTransformer;
import de.ii.xtraplatform.features.domain.FeatureStorePathParser;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.features.domain.Metadata;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@EntityComponent
@Entity(type = FeatureProvider2.ENTITY_TYPE, subType = FeatureProviderWfs.ENTITY_SUB_TYPE, dataClass = FeatureProviderDataV2.class, dataSubClass = FeatureProviderDataV2.class)
public class FeatureProviderWfs extends AbstractFeatureProvider<ByteString, String, FeatureProviderConnector.QueryOptions> implements FeatureProvider2, FeatureQueries, FeatureCrs, FeatureExtents, FeatureMetadata {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderWfs.class);

    static final String ENTITY_SUB_TYPE = "feature/wfs";
    public static final String PROVIDER_TYPE = "WFS";

    private final CrsTransformerFactory crsTransformerFactory;
    private final WfsConnector connector;
    private final FeatureQueryTransformerWfs queryTransformer;
    private final FeatureNormalizerWfs featureNormalizer;
    private final ExtentReader extentReader;

    public FeatureProviderWfs(@Context BundleContext context,
                              @Requires ActorSystemProvider actorSystemProvider,
                              @Requires CrsTransformerFactory crsTransformerFactory,
                              @Requires ConnectorFactory connectorFactory,
                              @Property(name = Entity.DATA_KEY) FeatureProviderDataV2 data) {
        super(context, actorSystemProvider, data, createPathParser((ConnectionInfoWfsHttp) data.getConnectionInfo()));

        Map<String, FeatureType> types = data.getTypes()
                                             .entrySet()
                                             .stream()
                                             .map(entry -> new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue()
                                                                                                                       .accept(new FeatureSchemaToTypeVisitor(entry.getKey()))))
                                             .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

        this.crsTransformerFactory = crsTransformerFactory;
        this.connector = (WfsConnector) connectorFactory.createConnector(data);
        this.queryTransformer = new FeatureQueryTransformerWfs(getTypeInfos(), types, data.getTypes(), (ConnectionInfoWfsHttp) data.getConnectionInfo(), data.getNativeCrs().orElse(OgcCrs.CRS84));
        this.featureNormalizer = new FeatureNormalizerWfs(getTypeInfos(), types, data.getTypes(), ((ConnectionInfoWfsHttp) data.getConnectionInfo()).getNamespaces());
        this.extentReader = new ExtentReaderWfs(connector, crsTransformerFactory, data.getNativeCrs().orElse(OgcCrs.CRS84));
    }

    private static FeatureStorePathParser createPathParser(ConnectionInfoWfsHttp connectionInfoWfsHttp) {
        return new FeatureStorePathParserWfs(connectionInfoWfsHttp.getNamespaces());
    }

    @Override
    public FeatureProviderDataV2 getData() {
        return super.getData();
    }

    @Override
    protected FeatureQueryTransformer<String> getQueryTransformer() {
        return queryTransformer;
    }

    @Override
    protected FeatureProviderConnector<ByteString, String, FeatureProviderConnector.QueryOptions> getConnector() {
        return connector;
    }

    @Override
    protected FeatureNormalizer<ByteString> getNormalizer() {
        return featureNormalizer;
    }


    @Override
    public boolean supportsCrs() {
        return FeatureProvider2.super.supportsCrs() && getData().getNativeCrs().isPresent();
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
            return extentReader.getExtent(typeInfo.get())
                               .run(getMaterializer())
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
    public Optional<Metadata> getMetadata() {
        return connector.getMetadata();
    }
}
