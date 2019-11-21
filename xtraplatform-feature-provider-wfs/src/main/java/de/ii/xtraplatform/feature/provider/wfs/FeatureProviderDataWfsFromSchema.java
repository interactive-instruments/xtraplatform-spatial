/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs;

import de.ii.xtraplatform.feature.transformer.api.FeatureProviderDataTransformer;
import de.ii.xtraplatform.feature.provider.api.FeatureProviderSchemaConsumer;
import de.ii.xtraplatform.feature.transformer.api.ImmutableFeatureProviderDataTransformer;
import de.ii.xtraplatform.feature.transformer.api.ImmutableMappingStatus;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import de.ii.xtraplatform.ogc.api.exceptions.SchemaParseException;

import java.util.List;

/**
 * @author zahnen
 */
public class FeatureProviderDataWfsFromSchema extends GmlFeatureTypeAnalyzer implements FeatureProviderSchemaConsumer {

    public FeatureProviderDataWfsFromSchema(
            FeatureProviderDataTransformer data,
            ImmutableFeatureProviderDataTransformer.Builder dataBuilder,
            List<TargetMappingProviderFromGml> mappingProviders) {
        super(data, dataBuilder, mappingProviders);

    }

    @Override
    public boolean analyzeNamespaceRewrite(String oldNamespace, String newNamespace, String featureTypeName) {
        return super.analyzeNamespaceRewrite(oldNamespace, newNamespace, featureTypeName);
    }

    @Override
    public void analyzeFailure(Throwable e) {
        ImmutableMappingStatus.Builder builder = new ImmutableMappingStatus.Builder()
                .from(providerDataWfs.getMappingStatus())
                .errorMessage(e.getMessage());
        if (e.getClass() == SchemaParseException.class) {
            builder.addAllErrorMessageDetails(((SchemaParseException)e).getDetails());
        }
        dataBuilder.mappingStatus(builder.build());
    }

    @Override
    public void analyzeSuccess() {
        super.analyzeSuccess();

        ImmutableConnectionInfoWfsHttp.Builder connectionInfo = new ImmutableConnectionInfoWfsHttp.Builder()
                                                                                .from(providerDataWfs.getConnectionInfo());
        connectionInfo.namespaces(super.getNamespaces());

            dataBuilder.connectionInfo(connectionInfo.build());

            ImmutableMappingStatus mappingStatus = new ImmutableMappingStatus.Builder().from(providerDataWfs.getMappingStatus()).enabled(true)
                                                                              .supported(true)
                                                                              .build();
            dataBuilder.mappingStatus(mappingStatus);
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
        super.analyzeProperty(nsUri, localName, type, depth, isObject, isMultiple(maxOccurs));
    }

    private boolean isMultiple(long maxOccurs) {
        return maxOccurs > 1 || maxOccurs == -1;
    }
}
