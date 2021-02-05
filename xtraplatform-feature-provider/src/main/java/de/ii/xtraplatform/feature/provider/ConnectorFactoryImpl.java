/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.feature.provider.api.ConnectorFactory;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.ii.xtraplatform.features.domain.FeatureProvider2.PROVIDER_TYPE_KEY;
import static de.ii.xtraplatform.features.domain.FeatureProviderConnector.CONNECTOR_TYPE_KEY;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
@Wbp(
        filter = Registry.FACTORY_FILTER_PREFIX + ConnectorFactoryImpl.FACTORY_TYPE + Registry.FACTORY_FILTER_SUFFIX,
        onArrival = Registry.ON_ARRIVAL_METHOD,
        onDeparture = Registry.ON_DEPARTURE_METHOD
)

public class ConnectorFactoryImpl implements ConnectorFactory, Registry<Factory> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorFactoryImpl.class);

    static final String FACTORY_TYPE = "de.ii.xtraplatform.features.domain.FeatureProviderConnector";

    private final BundleContext context;
    private final FactoryRegistry<FeatureProviderConnector<?,?,?>> connectorFactories;

    public ConnectorFactoryImpl(@Context BundleContext context) {
        this.context = context;
        this.connectorFactories = new FactoryRegistryState<>(FACTORY_TYPE, "connector", context, PROVIDER_TYPE_KEY, CONNECTOR_TYPE_KEY);
    }

    @Override
    public Registry.State<Factory> getRegistryState() {
        return connectorFactories;
    }

    @Validate
    void onStart() {
        if (!connectorFactories.ensureTypeExists()) {
            LOGGER.error("Connector factory target class does not exist: {}", FACTORY_TYPE);
        }
    }

    @Override
    public FeatureProviderConnector<?, ?, ?> createConnector(FeatureProviderDataV2 featureProviderData) {
        final String providerType = featureProviderData.getFeatureProviderType();
        final String connectorType = featureProviderData.getConnectionInfo()
                                                        .getConnectorType();

        if (!connectorFactories.get(providerType, connectorType).isPresent()) {
            throw new IllegalStateException(String.format("Connector with type %s for provider type %s is not supported.", connectorType, providerType));
        }

        try {
            return connectorFactories.createInstance(ImmutableMap.of(".data", featureProviderData), providerType, connectorType);

        } catch (Throwable e) {
            throw new IllegalStateException(String.format("Connector with type %s for provider type %s could not be created.", connectorType, providerType), e);
        }
    }

    @Override
    public void disposeConnector(FeatureProviderConnector<?, ?, ?> connector) {
        connectorFactories.disposeInstance(connector);
    }
}
