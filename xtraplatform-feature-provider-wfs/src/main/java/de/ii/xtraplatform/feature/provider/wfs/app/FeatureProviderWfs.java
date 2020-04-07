/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.app;

import akka.util.ByteString;
import de.ii.xtraplatform.akka.ActorSystemProvider;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.entity.api.EntityComponent;
import de.ii.xtraplatform.entity.api.handler.Entity;
import de.ii.xtraplatform.feature.provider.wfs.domain.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.feature.provider.wfs.domain.WfsConnector;
import de.ii.xtraplatform.features.domain.AbstractFeatureProvider;
import de.ii.xtraplatform.features.domain.FeatureCrs;
import de.ii.xtraplatform.features.domain.FeatureNormalizer;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV1;
import de.ii.xtraplatform.features.domain.FeatureQueries;
import de.ii.xtraplatform.features.domain.FeatureQueryTransformer;
import de.ii.xtraplatform.features.domain.FeatureStorePathParser;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Property;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EntityComponent
@Entity(entityType = FeatureProvider2.class, dataType = FeatureProviderDataV1.class, type = "providers", subType = "feature/wfs")
public class FeatureProviderWfs extends AbstractFeatureProvider<ByteString, String, FeatureProviderConnector.QueryOptions> implements FeatureProvider2, FeatureQueries, FeatureCrs {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderWfs.class);

    private final CrsTransformerFactory crsTransformerFactory;
    private final WfsConnector connector;
    private final FeatureQueryTransformerWfs queryTransformer;
    private final FeatureNormalizerWfs featureNormalizer;

    public FeatureProviderWfs(@Context BundleContext context,
                              @Requires ActorSystemProvider actorSystemProvider,
                              @Requires CrsTransformerFactory crsTransformerFactory,
                              @Property(name = "data") FeatureProviderDataV1 data,
                              @Property(name = ".connector") WfsConnector wfsConnector) {
        super(context, actorSystemProvider, data, createPathParser((ConnectionInfoWfsHttp) data.getConnectionInfo()));

        this.crsTransformerFactory = crsTransformerFactory;
        this.connector = wfsConnector;
        this.queryTransformer = new FeatureQueryTransformerWfs(getTypeInfos(), getData().getTypes(), (ConnectionInfoWfsHttp) getData().getConnectionInfo(), getData().getNativeCrs());
        this.featureNormalizer = new FeatureNormalizerWfs(getTypeInfos(), getData().getTypes(), ((ConnectionInfoWfsHttp) getData().getConnectionInfo()).getNamespaces());
    }

    private static FeatureStorePathParser createPathParser(ConnectionInfoWfsHttp connectionInfoWfsHttp) {
        return new FeatureStorePathParserWfs(connectionInfoWfsHttp.getNamespaces());
    }

    @Override
    public FeatureProviderDataV1 getData() {
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
    public boolean isCrsSupported(EpsgCrs crs) {
        return getData().getNativeCrs()
                        .equals(crs) || crsTransformerFactory.isCrsSupported(crs);
    }
}
