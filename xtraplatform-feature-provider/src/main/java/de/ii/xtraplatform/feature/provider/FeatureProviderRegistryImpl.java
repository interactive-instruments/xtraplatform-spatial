/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.feature.provider.api.FeatureProvider;
import de.ii.xtraplatform.feature.provider.api.FeatureProviderConnector;
import de.ii.xtraplatform.feature.provider.api.FeatureProviderData;
import de.ii.xtraplatform.feature.provider.api.FeatureProviderRegistry;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Context;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.whiteboard.Wbp;
import org.apache.felix.ipojo.whiteboard.Whiteboards;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zahnen
 */
@Component
@Provides
@Instantiate
@Whiteboards(whiteboards = {
        @Wbp(
                filter = "(&(objectClass=org.apache.felix.ipojo.Factory)(component.providedServiceSpecifications=de.ii.xtraplatform.feature.provider.api.FeatureProvider))",
                onArrival = "onFactoryArrival",
                onDeparture = "onFactoryDeparture"),
        @Wbp(
                filter = "(&(objectClass=org.apache.felix.ipojo.Factory)(component.providedServiceSpecifications=de.ii.xtraplatform.feature.provider.api.FeatureProviderConnector))",
                onArrival = "onFactoryArrival",
                onDeparture = "onFactoryDeparture")
})

public class FeatureProviderRegistryImpl implements FeatureProviderRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderRegistryImpl.class);

    @Context
    private BundleContext context;

    private final Map<String, Factory> providerFactories;
    private final Map<String, Map<String, Factory>> connectorFactories;

    public FeatureProviderRegistryImpl() {
        this.providerFactories = new ConcurrentHashMap<>();
        this.connectorFactories = new ConcurrentHashMap<>();
    }

    @Override
    public boolean isSupported(String providerType, String connectorType) {
        return providerFactories.containsKey(providerType) && connectorFactories.containsKey(providerType) && connectorFactories.get(providerType).containsKey(connectorType);
    }

    @Override
    public FeatureProvider createFeatureProvider(FeatureProviderData featureProviderData) {
        if (!isSupported(featureProviderData.getProviderType(), featureProviderData.getConnectorType())) {
            throw new IllegalStateException("FeatureProvider with type " + featureProviderData.getProviderType() + " and connector " + featureProviderData.getConnectorType() + " is not supported");
        }

        try {
            ComponentInstance connectorInstance =  connectorFactories.get(featureProviderData.getProviderType()).get(featureProviderData.getConnectorType()).createComponentInstance(new Hashtable<>(ImmutableMap.of(".data", featureProviderData)));

            ServiceReference[] connectorRefs = context.getServiceReferences(FeatureProviderConnector.class.getName(), "(instance.name=" + connectorInstance.getInstanceName() +")");
            FeatureProviderConnector connector = (FeatureProviderConnector) context.getService(connectorRefs[0]);

            ComponentInstance instance =  providerFactories.get(featureProviderData.getProviderType()).createComponentInstance(new Hashtable<>(ImmutableMap.of(".data", featureProviderData, ".connector", connector)));

            ServiceReference[] refs = context.getServiceReferences(FeatureProvider.class.getName(), "(instance.name=" + instance.getInstanceName() +")");
            return (FeatureProvider) context.getService(refs[0]);

        } catch (UnacceptableConfiguration | MissingHandlerException | ConfigurationException | InvalidSyntaxException | NullPointerException e) {
            throw new IllegalStateException("FeatureProvider with type " + featureProviderData.getProviderType() + " could not be created", e);
        }
    }

    private synchronized void onFactoryArrival(ServiceReference<Factory> ref) {
        Optional<String> providerType = Arrays.stream((PropertyDescription[]) ref.getProperty("component.properties"))
                                              .filter(pd -> pd.getName().equals("providerType"))
                                                .map(PropertyDescription::getValue)
                                                .findFirst();

        Optional<String> connectorType = Arrays.stream((PropertyDescription[]) ref.getProperty("component.properties"))
                                              .filter(pd -> pd.getName().equals("connectorType"))
                                              .map(PropertyDescription::getValue)
                                              .findFirst();

        if (providerType.isPresent() && !connectorType.isPresent()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("FEATURE PROVIDER FACTORY {}", providerType.get());
            }
            this.providerFactories.put(providerType.get(), context.getService(ref));
        }
        else if (providerType.isPresent() && connectorType.isPresent()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("FEATURE PROVIDER CONNECTOR FACTORY {} {}", providerType.get(), connectorType.get());
            }
            this.connectorFactories.putIfAbsent(providerType.get(), new ConcurrentHashMap<>());
            this.connectorFactories.get(providerType.get()).put(connectorType.get(), context.getService(ref));
        }
    }

    private synchronized void onFactoryDeparture(ServiceReference<Factory> ref) {
        Optional<String> providerType = Arrays.stream((PropertyDescription[]) ref.getProperty("component.properties"))
                                              .filter(pd -> pd.getName().equals("providerType"))
                                              .map(PropertyDescription::getValue)
                                              .findFirst();

        Optional<String> connectorType = Arrays.stream((PropertyDescription[]) ref.getProperty("component.properties"))
                                               .filter(pd -> pd.getName().equals("connectorType"))
                                               .map(PropertyDescription::getValue)
                                               .findFirst();

        if (providerType.isPresent() && !connectorType.isPresent()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("REMOVE FEATURE PROVIDER FACTORY {}", providerType.get());
            }
            this.providerFactories.remove(providerType.get());
        }
        else if (providerType.isPresent() && connectorType.isPresent()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("REMOVE FEATURE PROVIDER CONNECTOR FACTORY {} {}", providerType.get(), connectorType.get());
            }
            this.providerFactories.remove(providerType.get());
            this.connectorFactories.get(providerType.get()).remove(connectorType.get());
        }
    }

}
