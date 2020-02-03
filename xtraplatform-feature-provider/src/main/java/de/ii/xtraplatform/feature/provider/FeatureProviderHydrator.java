/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.crs.api.CrsTransformerFactory;
import de.ii.xtraplatform.event.store.EntityHydrator;
import de.ii.xtraplatform.feature.provider.api.FeatureProviderConnector;
import de.ii.xtraplatform.feature.provider.api.FeatureProviderDataV1;
import de.ii.xtraplatform.feature.provider.api.FeatureProviderRegistry;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Component
@Provides(properties = {
        //TODO: how to connect to entity
        @StaticServiceProperty(name = "entityType", type = "java.lang.String", value = "providers")
})
@Instantiate
public class FeatureProviderHydrator implements EntityHydrator<FeatureProviderDataV1> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderHydrator.class);

    @Requires
    private FeatureProviderRegistry featureProviderFactory;

    @Requires
    private CrsTransformerFactory crsTransformerFactory;

    @Override
    public Map<String, Object> getInstanceConfiguration(FeatureProviderDataV1 data) {
        try {
            FeatureProviderConnector connector = featureProviderFactory.createConnector(data);

                return ImmutableMap.<String, Object>builder()
                        .put(".connector", connector)
                        .build();


        } catch (IllegalStateException e) {
            LOGGER.error("Service with id '{}' could not be created: {}", data.getId(), e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exception:", e);
            }
        }

        throw new IllegalStateException();
    }

}
