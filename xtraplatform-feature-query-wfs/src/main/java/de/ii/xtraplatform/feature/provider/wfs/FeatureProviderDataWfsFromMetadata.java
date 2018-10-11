/**
 * Copyright 2018 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs;

import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.feature.query.api.AbstractFeatureProviderMetadataConsumer;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import org.apache.http.client.utils.URIBuilder;

import javax.xml.namespace.QName;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author zahnen
 */
public class FeatureProviderDataWfsFromMetadata extends AbstractFeatureProviderMetadataConsumer {
    private final ModifiableFeatureProviderDataWfs featureProviderDataWfs;
    private final ModifiableConnectionInfo connectionInfo;
    private final XMLNamespaceNormalizer namespaceNormalizer;

    public FeatureProviderDataWfsFromMetadata(ModifiableFeatureProviderDataWfs featureProviderDataWfs) {
        this.featureProviderDataWfs = featureProviderDataWfs;
        this.connectionInfo = (ModifiableConnectionInfo) featureProviderDataWfs.getConnectionInfo();
        URI cleanUri = parseAndCleanWfsUrl(connectionInfo.getUri());
        connectionInfo.setUri(cleanUri);
        this.namespaceNormalizer = new XMLNamespaceNormalizer();
    }

    @Override
    public void analyzeNamespace(String prefix, String uri) {
        namespaceNormalizer.addNamespace(prefix, uri);
        connectionInfo.putNamespaces(prefix, uri);
    }

    @Override
    public void analyzeVersion(String version) {
        if (!connectionInfo.versionIsSet() || version.compareTo(connectionInfo.getVersion()) > 0) {
            connectionInfo.setVersion(version);
        }
    }

    @Override
    public void analyzeFeatureType(String featureTypeName) {
        if (featureTypeName.contains(":")) {
            String[] name = featureTypeName.split(":");
            String namespace = namespaceNormalizer.getNamespaceURI(name[0]);
            featureProviderDataWfs.putFeatureTypes(name[1].toLowerCase(), new QName(namespace, name[1], name[0]));
        }
    }

    @Override
    public void analyzeFeatureTypeDefaultCrs(String featureTypeName, String crs) {
        featureProviderDataWfs.setNativeCrs(new EpsgCrs(crs));
    }

    @Override
    public void analyzeOperationGetUrl(String operation, String url) {
        connectionInfo.putOtherUrls(operation, url);
    }

    @Override
    public void analyzeOperationPostUrl(String operation, String url) {
        //TODO GET/POST
        //connectionInfo.putOtherUrls(operation, url);
    }

    static URI parseAndCleanWfsUrl(URI inUri) {
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
