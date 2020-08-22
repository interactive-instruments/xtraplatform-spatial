/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.infra;

import com.google.common.base.Strings;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.feature.provider.api.AbstractFeatureProviderMetadataConsumer;
import de.ii.xtraplatform.features.domain.ImmutableMetadata;
import de.ii.xtraplatform.features.domain.Metadata;
import de.ii.xtraplatform.xml.domain.XMLNamespaceNormalizer;

import javax.xml.namespace.QName;
import java.util.Objects;

public class WfsCapabilitiesAnalyzer extends AbstractFeatureProviderMetadataConsumer {

    private final XMLNamespaceNormalizer namespaceNormalizer;
    private final ImmutableMetadata.Builder metadata;
    private String lastVersion;

    public WfsCapabilitiesAnalyzer() {
        this.namespaceNormalizer = new XMLNamespaceNormalizer();
        this.metadata = new ImmutableMetadata.Builder();
    }

    public Metadata getMetadata() {
        return metadata.build();
    }

    @Override
    public void analyzeNamespace(String prefix, String uri) {
        if (!namespaceNormalizer.getNamespaces().containsKey(prefix)) {
            metadata.putNamespaces(prefix, uri);
            namespaceNormalizer.addNamespace(prefix, uri);
        }
    }

    @Override
    public void analyzeVersion(String version) {
        if (Objects.isNull(lastVersion) || version.compareTo(lastVersion) > 0) {
            this.lastVersion = version;
            metadata.version(version);
        }
    }

    @Override
    public void analyzeFeatureType(String featureTypeName) {
        if (featureTypeName.contains(":")) {
            String prefix = namespaceNormalizer.extractPrefix(featureTypeName);
            String namespace = namespaceNormalizer.getNamespaceURI(prefix);
            String localName = namespaceNormalizer.getLocalName(featureTypeName);

            metadata.putFeatureTypes(getFeatureTypeId(featureTypeName), new QName(namespace, localName, prefix));
        }
    }

    @Override
    public void analyzeFeatureTypeDefaultCrs(String featureTypeName, String crs) {
        metadata.putFeatureTypesCrs(getFeatureTypeId(featureTypeName), crs);
    }

    @Override
    public void analyzeFeatureTypeBoundingBox(String featureTypeName, String xmin, String ymin, String xmax,
                                              String ymax) {
        metadata.putFeatureTypesBoundingBox(getFeatureTypeId(featureTypeName), new BoundingBox(Double.parseDouble(xmin), Double.parseDouble(ymin), Double.parseDouble(xmax), Double.parseDouble(ymax), OgcCrs.CRS84));
    }

    @Override
    public void analyzeTitle(String title) {
        metadata.label(title);
    }

    @Override
    public void analyzeAbstract(String abstrct) {
        metadata.description(abstrct);
    }

    @Override
    public void analyzeKeywords(String... keywords) {
        metadata.addKeywords(keywords);
    }

    @Override
    public void analyzeFees(String fees) {
        if (!Strings.isNullOrEmpty(fees)) {
            metadata.fees(fees);
        }
    }

    @Override
    public void analyzeAccessConstraints(String accessConstraints) {
        if (!Strings.isNullOrEmpty(accessConstraints) && !Objects.equals(accessConstraints.toLowerCase(), "none")) {
            metadata.accessConstraints(accessConstraints);
        }
    }

    @Override
    public void analyzeProviderName(String providerName) {
        metadata.contactName(providerName);
    }

    @Override
    public void analyzeProviderSite(String providerSite) {
        if (!Strings.isNullOrEmpty(providerSite)) {
            metadata.contactUrl(providerSite);
        }
    }

    @Override
    public void analyzeServiceContactEmail(String email) {
        metadata.contactEmail(email);
    }

    @Override
    public void analyzeServiceContactOnlineResource(String onlineResource) {
        if (!metadata.build()
                            .getContactUrl()
                            .isPresent() && !Strings.isNullOrEmpty(onlineResource)) {
            metadata.contactUrl(onlineResource);
        }
    }

    private String getFeatureTypeId(String qualifiedName) {
        return namespaceNormalizer.getLocalName(qualifiedName).toLowerCase();
    }
}
