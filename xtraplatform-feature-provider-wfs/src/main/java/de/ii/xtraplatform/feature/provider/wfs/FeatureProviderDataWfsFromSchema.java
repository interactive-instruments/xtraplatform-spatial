/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs;

import de.ii.xtraplatform.feature.transformer.api.FeatureProviderSchemaConsumer;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;

import java.util.List;

/**
 * @author zahnen
 */
public class FeatureProviderDataWfsFromSchema extends GmlFeatureTypeAnalyzer implements FeatureProviderSchemaConsumer {

    public FeatureProviderDataWfsFromSchema(FeatureProviderDataWfs featureProviderDataWfs, List<TargetMappingProviderFromGml> mappingProviders) {
        super(featureProviderDataWfs, mappingProviders);

    }

    @Override
    public boolean analyzeNamespaceRewrite(String oldNamespace, String newNamespace, String featureTypeName) {
        return super.analyzeNamespaceRewrite(oldNamespace, newNamespace, featureTypeName);
    }

    @Override
    public void analyzeFailure(Exception e) {
        /*TODO ModifiableMappingStatus mappingStatus = ModifiableMappingStatus.create().from(providerDataWfs.getMappingStatus());
        mappingStatus.setErrorMessage(e.getMessage());
        if (e.getClass() == SchemaParseException.class) {
            mappingStatus.setErrorMessageDetails(((SchemaParseException)e).getDetails());
        }*/

        //TODO providerDataWfs.setMappingStatus(mappingStatus);
    }

    @Override
    public void analyzeSuccess() {
        ModifiableConnectionInfo connectionInfo = ModifiableConnectionInfo.create()
                                                                .from(providerDataWfs.getConnectionInfo());
        connectionInfo.putAllNamespaces(super.getNamespaces());
        //TODO providerDataWfs.setConnectionInfo(connectionInfo);

        //TODO ModifiableMappingStatus mappingStatus = ModifiableMappingStatus.create().from(providerDataWfs.getMappingStatus());
        //TODO mappingStatus.setSupported(true);
        //TODO providerDataWfs.setMappingStatus(mappingStatus);
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
