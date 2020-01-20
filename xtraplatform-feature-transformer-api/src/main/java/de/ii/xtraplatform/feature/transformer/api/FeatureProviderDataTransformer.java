/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.entity.api.maptobuilder.ValueBuilderMap;
import de.ii.xtraplatform.entity.api.maptobuilder.encoding.ValueBuilderMapEncodingEnabled;
import de.ii.xtraplatform.feature.provider.api.ConnectionInfo;
import de.ii.xtraplatform.feature.provider.api.FeatureProviderData;
import de.ii.xtraplatform.feature.provider.api.ImmutableMappingStatus;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author zahnen
 */
@Deprecated
@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true, attributeBuilderDetection = true)
@ValueBuilderMapEncodingEnabled
@JsonDeserialize(builder = ImmutableFeatureProviderDataTransformer.Builder.class)
public abstract class FeatureProviderDataTransformer extends FeatureProviderData {

    //@JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // means only read from json
    @Override
    public abstract String getProviderType();

    //@JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // means only read from json
    @Value.Default
    @Override
    public String getConnectorType() {
        return getProviderType().equals("PGIS") ? "SLICK" : "HTTP";
    }

    @JsonAlias(value = {"featureTypes"})
    //@JsonIgnore
    public abstract Map<String, QName> getLocalFeatureTypeNames();

    //@JsonIgnore
    public abstract ConnectionInfo getConnectionInfo();

    //@JsonMerge
    //public abstract Map<String, FeatureTypeMapping> getMappings();

    //behaves exactly like Map<String, FeatureTypeMapping>, but supports mergeable builder deserialization
    // (immutables attributeBuilder does not work with maps yet)
    @JsonMerge
    public abstract ValueBuilderMap<FeatureTypeMapping, ImmutableFeatureTypeMapping.Builder> getMappings();

    @Value.Default
    public ImmutableMappingStatus getMappingStatus() {
        return new ImmutableMappingStatus.Builder()
                .enabled(true)
                .supported(true)
                .refined(true)
                .build();
    }

    @Override
    @Value.Derived
    public Optional<String> getDataSourceUrl() {
        return Optional.ofNullable(getConnectionInfo()).flatMap(ConnectionInfo::getConnectionUri);
                       /*.map(connectionInfo -> new URIBuilder(connectionInfo.getUri()))
                       .map(uriBuilder -> uriBuilder.addParameter("SERVICE", "WFS")
                                                    .addParameter("REQUEST", "GetCapabilities")
                                                    .toString());*/
    }

    public boolean isFeatureTypeEnabled(String featureType) {
        if (!getMappingStatus().getEnabled() || Objects.nonNull(getMappingStatus().getErrorMessage())) {
            return true;
        }

        //TODO: better way to detect mapping for feature type
        FeatureTypeMapping featureTypeMapping = getMappings().get(featureType);
        if (featureTypeMapping != null) {
            SourcePathMapping firstMapping = featureTypeMapping.getMappings()
                                                               .values()
                                                               .iterator()
                                                               .next();
            return firstMapping.getMappingForType(TargetMapping.BASE_TYPE)
                               .isEnabled();
        }
        return false;
    }

    public abstract EpsgCrs getNativeCrs();

    public abstract List<EpsgCrs> getOtherCrs();

    //TODO: pgis stuff
    @Value.Default
    public boolean computeNumberMatched() {
        return true;
    }

    @Nullable
    public abstract Object getTrigger();

    @JsonIgnore
    @Value.Default
    public boolean supportsTransactions() {
        return false;
    }
}
