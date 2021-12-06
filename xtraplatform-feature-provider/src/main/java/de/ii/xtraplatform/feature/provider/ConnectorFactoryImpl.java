/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.ConnectionInfo;
import de.ii.xtraplatform.features.domain.ConnectorFactory;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.Tuple;
import de.ii.xtraplatform.features.domain.WithConnectionInfo;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private final Map<FeatureProviderConnector<?,?,?>, Set<Runnable>> disposeListeners;

    public ConnectorFactoryImpl(@Context BundleContext context) {
        this.context = context;
        this.connectorFactories = new FactoryRegistryState<>(FACTORY_TYPE, "connector", context, PROVIDER_TYPE_KEY, CONNECTOR_TYPE_KEY);
        this.disposeListeners = new HashMap<>();
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
    public synchronized FeatureProviderConnector<?, ?, ?> createConnector(FeatureProviderDataV2 featureProviderData) {
        final String instanceId = "connectors/" + featureProviderData.getId();
        final String providerType = featureProviderData.getFeatureProviderType();
        ConnectionInfo connectionInfo = ((WithConnectionInfo<?>) featureProviderData)
            .getConnectionInfo();
        final String connectorType = connectionInfo.getConnectorType();

        if (connectorFactories.hasInstance(instanceId)) {
            return connectorFactories.getInstance(instanceId);
        }

        if (connectionInfo.isShared()) {
            Optional<FeatureProviderConnector<?, ?, ?>> match = connectorFactories.getInstances()
                .stream()
                .filter(connector -> connector.canBeSharedWith(connectionInfo, false).first())
                .findFirst();

            if (match.isPresent()) {
                Tuple<Boolean, String> fullMatch = match.get()
                    .canBeSharedWith(connectionInfo, true);

                if (fullMatch.first()) {
                    return match.get();
                } else {
                    throw new IllegalStateException(String.format("Connection pool cannot be shared with provider %s: %s", match.get().getProviderId(), fullMatch.second()));
                }
            }
        }

        if (!connectorFactories.get(providerType, connectorType).isPresent()) {
            throw new IllegalStateException(String.format("Connector with type %s for provider type %s is not supported.", connectorType, providerType));
        }

        try {
            return connectorFactories.createInstance(ImmutableMap.of(Factory.INSTANCE_NAME_PROPERTY, instanceId, ".data", featureProviderData), providerType, connectorType);

        } catch (Throwable e) {
            throw new IllegalStateException(String.format("Connector with type %s for provider type %s could not be created.", connectorType, providerType), e);
        }
    }

    @Override
    public synchronized void disposeConnector(FeatureProviderConnector<?, ?, ?> connector) {
        connectorFactories.disposeInstance(connector);
        if (disposeListeners.containsKey(connector)) {
            disposeListeners.get(connector).forEach(Runnable::run);
            disposeListeners.get(connector).clear();
        }
    }

    @Override
    public synchronized void onDispose(FeatureProviderConnector<?, ?, ?> connector, Runnable runnable) {
        if (!disposeListeners.containsKey(connector)) {
            disposeListeners.put(connector, new HashSet<>());
        }
        disposeListeners.get(connector).add(runnable);
    }
}
