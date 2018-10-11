/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureProviderDataTransformer;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import de.ii.xtraplatform.feature.transformer.api.SourcePathMapping;
import org.apache.http.client.utils.URIBuilder;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * @author zahnen
 */
//TODO
//@Value.Style(validationMethod = Value.Style.ValidationMethod.NONE)
@Value.Immutable
@Value.Modifiable
@JsonDeserialize(as = ModifiableFeatureProviderDataWfs.class)
public abstract class FeatureProviderDataWfs extends FeatureProviderDataTransformer {

    @Value.Derived
    @Override
    public String getProviderType() {
        return FeatureProviderWfs.PROVIDER_TYPE;
    }

    public abstract ConnectionInfo getConnectionInfo();

    @Value.Default
    public MappingStatus getMappingStatus() {
        return ImmutableMappingStatus.builder().build();
    }

    @Override
    @Value.Derived
    public Optional<String> getDataSourceUrl() {
        URIBuilder uriBuilder = new URIBuilder(getConnectionInfo().getUri());
        return Optional.of(uriBuilder.addParameter("SERVICE", "WFS").addParameter("REQUEST", "GetCapabilities").toString());
    }

    @Override
    public boolean isFeatureTypeEnabled(String featureType) {
        if (!getMappingStatus().getEnabled()) {
            return true;
        }

        //TODO: better way to detect mapping for feature type
        FeatureTypeMapping featureTypeMapping = getMappings().get(featureType);
        if (featureTypeMapping != null) {
            SourcePathMapping firstMapping = featureTypeMapping.getMappings()
                                                       .values()
                                                       .iterator()
                                                       .next();
            return firstMapping.getMappingForType(TargetMapping.BASE_TYPE).isEnabled();
        }
        return false;
    }
}
