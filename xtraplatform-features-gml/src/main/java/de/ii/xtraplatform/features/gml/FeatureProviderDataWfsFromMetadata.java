/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.gml;

import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.AbstractFeatureProviderMetadataConsumer;
import de.ii.xtraplatform.features.gml.domain.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.features.gml.domain.ImmutableConnectionInfoWfsHttp;
import de.ii.xtraplatform.feature.transformer.api.FeatureProviderDataTransformer;
import de.ii.xtraplatform.feature.transformer.api.ImmutableFeatureProviderDataTransformer;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.xml.domain.XMLNamespaceNormalizer;
import org.apache.http.client.utils.URIBuilder;

import javax.xml.namespace.QName;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * @author zahnen
 */
public class FeatureProviderDataWfsFromMetadata extends AbstractFeatureProviderMetadataConsumer {
    private final FeatureProviderDataTransformer featureProviderData;
    private final ImmutableFeatureProviderDataTransformer.Builder featureProviderDataBuilder;
    private final ImmutableConnectionInfoWfsHttp.Builder connectionInfoBuilder;
    private final XMLNamespaceNormalizer namespaceNormalizer;
    private String lastVersion;

    public FeatureProviderDataWfsFromMetadata(FeatureProviderDataTransformer featureProviderData,
                                              ImmutableFeatureProviderDataTransformer.Builder dataBuilder) {
        this.featureProviderData = featureProviderData;
        this.featureProviderDataBuilder = dataBuilder;
        this.connectionInfoBuilder = new ImmutableConnectionInfoWfsHttp.Builder().from(featureProviderData.getConnectionInfo());
        URI cleanUri = parseAndCleanWfsUrl(((ConnectionInfoWfsHttp)featureProviderData.getConnectionInfo()).getUri());
        connectionInfoBuilder.uri(cleanUri);
        this.namespaceNormalizer = new XMLNamespaceNormalizer();
    }

    @Override
    public void analyzeNamespace(String prefix, String uri) {
        if (!namespaceNormalizer.getNamespaces().containsKey(prefix)) {
            namespaceNormalizer.addNamespace(prefix, uri);
            connectionInfoBuilder.putNamespaces(prefix, uri);
        }
    }

    @Override
    public void analyzeVersion(String version) {
        if (Objects.isNull(lastVersion) || version.compareTo(lastVersion) > 0) {
            connectionInfoBuilder.version(version);
        }
    }

    @Override
    public void analyzeFeatureType(String featureTypeName) {
        //TODO usage with Immutable Builder
        if (featureTypeName.contains(":")) {
            String[] name = featureTypeName.split(":");
            String namespace = namespaceNormalizer.getNamespaceURI(name[0]);
            featureProviderDataBuilder.putLocalFeatureTypeNames(name[1].toLowerCase(), new QName(namespace, name[1], name[0]));
        }
    }

    @Override
    public void analyzeFeatureTypeDefaultCrs(String featureTypeName, String crs) {
        //TODO usage with Immutable Builder
        featureProviderDataBuilder.nativeCrs(EpsgCrs.fromString(crs));
    }

    @Override
    public void analyzeOperationGetUrl(String operation, String url) {
        connectionInfoBuilder.putOtherUrls(operation, url);
    }

    @Override
    public void analyzeOperationPostUrl(String operation, String url) {
        //TODO GET/POST
        //connectionInfoBuilder.putOtherUrls(operation, url);
    }

    @Override
    public void analyzeEnd() {
        featureProviderDataBuilder.connectionInfo(connectionInfoBuilder.build());
    }

    @Override
    public void analyzeFailed(Exception ex) {
        featureProviderDataBuilder.connectionInfo(connectionInfoBuilder.build());

        throw new IllegalStateException(ex.getMessage(), ex);
    }

    public static URI parseAndCleanWfsUrl(URI inUri) {
        URIBuilder outUri = new URIBuilder(inUri).removeQuery();

        if (inUri.getQuery() != null && !inUri.getQuery()
                                              .isEmpty()) {
            for (String inParam : inUri.getQuery()
                                       .split("&")) {
                String[] param = inParam.split("=");
                if (!WFS.hasKVPKey(param[0].toUpperCase())) {
                    outUri.addParameter(param[0], param[1]);
                }
            }
        }

        try {
            return outUri.build();
        } catch (URISyntaxException e) {
            return inUri;
        }
    }
}
