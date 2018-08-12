/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.feature.transformer.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.feature.query.api.TargetMapping;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @author zahnen
 */
public class FeatureTypeConfigurationOld {
    private String name;
    private String displayName;
    private FeatureTypeMapping mappings;
    //TODO: to wfs source extension
    private String namespace;
    // TODO: to wfs3 target extension
    private TemporalExtent temporalExtent;
    private BoundingBox spatialExtent;

    public FeatureTypeConfigurationOld() {
        this.temporalExtent = new TemporalExtent(Instant.now().toEpochMilli(), 0);

    }
    public FeatureTypeConfigurationOld(String name, String namespace, String displayName) {
        this.name = name;
        this.namespace = namespace;
        this.displayName = displayName;
        //this.mappings = new FeatureTypeMapping();
        this.temporalExtent = new TemporalExtent(Instant.now().toEpochMilli(), 0);
    }

    // TODO: only used for testing, replace by builder
    public FeatureTypeConfigurationOld(String name, String namespace, String displayName, FeatureTypeMapping wfsProxyFeatureTypeMapping) {
        this.name = name;
        this.namespace = namespace;
        this.displayName = displayName;
        this.mappings = wfsProxyFeatureTypeMapping;
        this.temporalExtent = new TemporalExtent(Instant.now().toEpochMilli(), 0);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public FeatureTypeMapping getMappings() {
        return mappings;
    }

    public void setMappings(FeatureTypeMapping mappings) {
        this.mappings = mappings;
    }

    @JsonIgnore
    public boolean isEnabled() {
        Optional<TargetMapping> baseMapping = mappings.findMappings(namespace + ":" + name, TargetMapping.BASE_TYPE);

        return baseMapping.isPresent() && baseMapping.get().isEnabled();
    }

    public TemporalExtent getTemporalExtent() {
        return temporalExtent;
    }

    public void setTemporalExtent(TemporalExtent temporalExtent) {
        this.temporalExtent = temporalExtent;
    }
}
