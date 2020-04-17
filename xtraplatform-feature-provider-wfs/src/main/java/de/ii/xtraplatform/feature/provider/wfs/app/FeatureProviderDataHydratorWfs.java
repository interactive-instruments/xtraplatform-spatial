/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.app;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.entity.api.handler.Entity;
import de.ii.xtraplatform.event.store.EntityHydrator;
import de.ii.xtraplatform.feature.provider.api.ConnectorFactory;
import de.ii.xtraplatform.feature.provider.wfs.domain.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.feature.provider.wfs.infra.WfsConnectorHttp;
import de.ii.xtraplatform.feature.provider.wfs.infra.WfsSchemaCrawler;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV1;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.features.domain.ImmutableFeatureProviderDataV1;
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
public class FeatureProviderDataHydratorWfs implements EntityHydrator<FeatureProviderDataV1> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderDataHydratorWfs.class);

    private final ConnectorFactory connectorFactory;

    public FeatureProviderDataHydratorWfs(@Requires ConnectorFactory connectorFactory) {
        this.connectorFactory = connectorFactory;
    }

    @Override
    public FeatureProviderDataV1 hydrateData(FeatureProviderDataV1 data) {

        try {
            WfsConnectorHttp connector = (WfsConnectorHttp) connectorFactory.createConnector(data);

            if (data.isAuto()) {
                LOGGER.info("Feature provider with id '{}' is in auto mode, generating configuration ...", data.getId());
            }

            FeatureProviderDataV1 hydrated = completeConnectionInfoIfNecessary(connector,
                    generateNativeCrsIfNecessary(
                            generateTypesIfNecessary(connector, data)
                    )
            );

            connectorFactory.disposeConnector(connector);

            return hydrated;


        } catch (Throwable e) {
            LOGGER.error("Feature provider with id '{}' could not be hydrated: {}", data.getId(), e.getMessage());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exception:", e);
            }
        }

        throw new IllegalStateException();
    }

    private FeatureProviderDataV1 completeConnectionInfoIfNecessary(WfsConnectorHttp connector,
                                                                    FeatureProviderDataV1 data) {
        ConnectionInfoWfsHttp connectionInfo = (ConnectionInfoWfsHttp) data.getConnectionInfo();

        if (data.isAuto() && connectionInfo.getNamespaces()
                                           .isEmpty()) {

            WfsSchemaCrawler schemaCrawler = new WfsSchemaCrawler(connector, connectionInfo);

            ConnectionInfoWfsHttp connectionInfoWfsHttp = schemaCrawler.completeConnectionInfo();

            return new ImmutableFeatureProviderDataV1.Builder()
                    .from(data)
                    .connectionInfo(connectionInfoWfsHttp)
                    .build();
        }

        return data;
    }

    private FeatureProviderDataV1 generateTypesIfNecessary(WfsConnectorHttp connector, FeatureProviderDataV1 data) {
        if (data.isAuto() && data.getTypes()
                                 .isEmpty()) {

            ConnectionInfoWfsHttp connectionInfo = (ConnectionInfoWfsHttp) data.getConnectionInfo();

            WfsSchemaCrawler schemaCrawler = new WfsSchemaCrawler(connector, connectionInfo);

            List<FeatureType> types = schemaCrawler.parseSchema();

            ImmutableMap<String, FeatureType> typeMap = types.stream()
                                                             .map(type -> new AbstractMap.SimpleImmutableEntry<>(type.getName(), type))
                                                             .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

            return new ImmutableFeatureProviderDataV1.Builder()
                    .from(data)
                    .types(typeMap)
                    .build();
        }

        return data;
    }

    private FeatureProviderDataV1 generateNativeCrsIfNecessary(FeatureProviderDataV1 data) {
        if (data.isAuto() && !data.getNativeCrs()
                                  .isPresent()) {
            EpsgCrs nativeCrs = data.getTypes()
                                    .values()
                                    .stream()
                                    .flatMap(type -> type.getProperties()
                                                         .values()
                                                         .stream())
                                    .filter(property -> property.isSpatial() && property.getAdditionalInfo()
                                                                                        .containsKey("crs"))
                                    .findFirst()
                                    .map(property -> EpsgCrs.fromString(property.getAdditionalInfo()
                                                                                .get("crs")))
                                    .orElseGet(() -> OgcCrs.CRS84);

            return new ImmutableFeatureProviderDataV1.Builder()
                    .from(data)
                    .nativeCrs(nativeCrs)
                    .build();

        }

        return data;
    }
}
