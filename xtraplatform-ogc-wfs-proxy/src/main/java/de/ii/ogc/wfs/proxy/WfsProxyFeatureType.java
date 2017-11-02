/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.ogc.wfs.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * @author zahnen
 */
public class WfsProxyFeatureType {
    private String name;
    private String namespace;
    private String displayName;
    private WfsProxyFeatureTypeMapping mappings;

    public WfsProxyFeatureType() {

    }
    public WfsProxyFeatureType(String name, String namespace, String displayName) {
        this.name = name;
        this.namespace = namespace;
        this.displayName = displayName;
        this.mappings = new WfsProxyFeatureTypeMapping();
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

    public WfsProxyFeatureTypeMapping getMappings() {
        return mappings;
    }

    public void setMappings(WfsProxyFeatureTypeMapping mappings) {
        this.mappings = mappings;
    }

    @JsonIgnore
    public boolean isEnabled() {
        List<TargetMapping> baseMapping = mappings.findMappings(namespace + ":" + name, TargetMapping.BASE_TYPE);

        return !baseMapping.isEmpty() && baseMapping.get(0).isEnabled();
    }
}
