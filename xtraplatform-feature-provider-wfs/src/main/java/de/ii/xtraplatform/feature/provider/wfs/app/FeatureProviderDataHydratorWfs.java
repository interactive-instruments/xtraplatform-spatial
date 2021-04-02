/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.app;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.store.domain.entities.EntityHydrator;
import de.ii.xtraplatform.store.domain.entities.handler.Entity;
import de.ii.xtraplatform.feature.provider.api.ConnectorFactory;
import de.ii.xtraplatform.feature.provider.wfs.domain.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.feature.provider.wfs.infra.WfsConnectorHttp;
import de.ii.xtraplatform.feature.provider.wfs.infra.WfsSchemaCrawler;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureProviderDataV2;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Optional;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.StaticServiceProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

@Component
@Provides(properties = {
        @StaticServiceProperty(name = Entity.TYPE_KEY, type = "java.lang.String", value = FeatureProvider2.ENTITY_TYPE),
        @StaticServiceProperty(name = Entity.SUB_TYPE_KEY, type = "java.lang.String", value = FeatureProviderWfs.ENTITY_SUB_TYPE)
})
@Instantiate
public class FeatureProviderDataHydratorWfs implements EntityHydrator<FeatureProviderDataV2> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderDataHydratorWfs.class);

    private final ConnectorFactory connectorFactory;

    public FeatureProviderDataHydratorWfs(@Requires ConnectorFactory connectorFactory) {
        this.connectorFactory = connectorFactory;
    }

    @Override
    public FeatureProviderDataV2 hydrateData(FeatureProviderDataV2 data) {

        try {
            WfsConnectorHttp connector = (WfsConnectorHttp) connectorFactory.createConnector(data);

            if (data.isAuto()) {
                LOGGER.info("Feature provider with id '{}' is in auto mode, generating configuration ...", data.getId());
            }

            FeatureProviderDataV2 hydrated = cleanupAutoPersist(cleanupAdditionalInfo(completeConnectionInfoIfNecessary(connector,
                    generateNativeCrsIfNecessary(
                            generateTypesIfNecessary(connector, data)
                    )
            )));

            connectorFactory.disposeConnector(connector);

            return hydrated;


        } catch (Throwable e) {
            LOGGER.error("Feature provider with id '{}' could not be hydrated: {}", data.getId(), e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exception:", e);
            }
        }

        throw new IllegalStateException(String.format("Feature provider with id %s   could not be hydrated", data.getId()));
    }

    private FeatureProviderDataV2 completeConnectionInfoIfNecessary(WfsConnectorHttp connector,
                                                                    FeatureProviderDataV2 data) {
        ConnectionInfoWfsHttp connectionInfo = (ConnectionInfoWfsHttp) data.getConnectionInfo();

        if (data.isAuto() && connectionInfo.getNamespaces()
                                           .isEmpty()) {

            WfsSchemaCrawler schemaCrawler = new WfsSchemaCrawler(connector, connectionInfo);

            ConnectionInfoWfsHttp connectionInfoWfsHttp = schemaCrawler.completeConnectionInfo();

            return new ImmutableFeatureProviderDataV2.Builder()
                    .from(data)
                    .connectionInfo(connectionInfoWfsHttp)
                    .build();
        }

        return data;
    }

    private FeatureProviderDataV2 generateTypesIfNecessary(WfsConnectorHttp connector, FeatureProviderDataV2 data) {
        if (data.isAuto() && data.getTypes()
                                 .isEmpty()) {

            ConnectionInfoWfsHttp connectionInfo = (ConnectionInfoWfsHttp) data.getConnectionInfo();

            WfsSchemaCrawler schemaCrawler = new WfsSchemaCrawler(connector, connectionInfo);

            List<FeatureSchema> types = schemaCrawler.parseSchema();

            ImmutableMap<String, FeatureSchema> typeMap = types.stream()
                                                             .map(type -> new AbstractMap.SimpleImmutableEntry<>(type.getName(), type))
                                                             .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

            return new ImmutableFeatureProviderDataV2.Builder()
                    .from(data)
                    .types(typeMap)
                    .build();
        }

        return data;
    }

    private FeatureProviderDataV2 generateNativeCrsIfNecessary(FeatureProviderDataV2 data) {
        if (data.isAuto() && !data.getNativeCrs()
                                  .isPresent()) {
            EpsgCrs nativeCrs = data.getTypes()
                                    .values()
                                    .stream()
                                    .flatMap(type -> type.getProperties()
                                                         .stream())
                                    .filter(property -> property.isSpatial() && property.getAdditionalInfo()
                                                                                        .containsKey("crs"))
                                    .findFirst()
                                    .map(property -> EpsgCrs.fromString(property.getAdditionalInfo()
                                                                                .get("crs")))
                                    .orElseGet(() -> OgcCrs.CRS84);

            return new ImmutableFeatureProviderDataV2.Builder()
                    .from(data)
                    .nativeCrs(nativeCrs)
                    .build();

        }

        return data;
    }

    private FeatureProviderDataV2 cleanupAdditionalInfo(FeatureProviderDataV2 data) {
        if (data.isAuto()) {
            return new ImmutableFeatureProviderDataV2.Builder()
                .from(data)
                .types(data.getTypes().entrySet().stream()
                    .map(entry -> new SimpleImmutableEntry<>(entry.getKey(), new ImmutableFeatureSchema.Builder()
                        .from(entry.getValue())
                        .additionalInfo(ImmutableMap.of())
                        .propertyMap(entry.getValue().getPropertyMap().entrySet().stream()
                            .map(entry2 -> new SimpleImmutableEntry<>(entry2.getKey(), new ImmutableFeatureSchema.Builder()
                                .from(entry2.getValue())
                                .additionalInfo(ImmutableMap.of())
                                .build()))
                            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)))
                        .build()))
                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)))
                .build();

        }

        return data;
    }

    private FeatureProviderDataV2 cleanupAutoPersist(FeatureProviderDataV2 data) {
        if (data.isAuto() && data.isAutoPersist()) {
            return new ImmutableFeatureProviderDataV2.Builder()
                .from(data)
                .auto(Optional.empty())
                .autoPersist(Optional.empty())
                .build();
        }

        return data;
    }
}
