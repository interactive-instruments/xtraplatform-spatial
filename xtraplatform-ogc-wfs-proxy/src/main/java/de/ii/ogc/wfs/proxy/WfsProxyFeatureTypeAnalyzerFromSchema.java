/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogc.wfs.proxy;

import de.ii.xtraplatform.feature.transformer.api.GmlFeatureTypeAnalyzer;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformerService;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLSchemaAnalyzer;

import java.util.List;

/**
 * @author zahnen
 */
public class WfsProxyFeatureTypeAnalyzerFromSchema extends GmlFeatureTypeAnalyzer implements GMLSchemaAnalyzer {

    public WfsProxyFeatureTypeAnalyzerFromSchema(FeatureTransformerService proxyService, List<TargetMappingProviderFromGml> mappingProviders) {
        super(proxyService, mappingProviders);
    }


    @Override
    public boolean analyzeNamespaceRewrite(String oldNamespace, String newNamespace, String featureTypeName) {
        return super.analyzeNamespaceRewrite(oldNamespace, newNamespace, featureTypeName);
    }

    @Override
    public void analyzeFeatureType(String nsUri, String localName) {
        super.analyzeFeatureType(nsUri, localName);
    }

    @Override
    public void analyzeAttribute(String nsUri, String localName, String type, boolean required) {
        super.analyzeAttribute(nsUri, localName, type);
    }

    @Override
    public void analyzeProperty(String nsUri, String localName, String type, long minOccurs, long maxOccurs, int depth, boolean isParentMultiple, boolean isComplex, boolean isObject) {
        super.analyzeProperty(nsUri, localName, type, depth, isObject);
    }
}
