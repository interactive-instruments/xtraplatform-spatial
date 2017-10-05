/**
 * Copyright 2017 European Union, interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.ogc.wfs.proxy;

/**
 * @author zahnen
 */
public class WFSProxyServiceProperties {
    private int maxFeatures;
    private WfsProxyMappingStatus mappingStatus;

    public WFSProxyServiceProperties() {
        this.mappingStatus = new WfsProxyMappingStatus();
    }

    public int getMaxFeatures() {
        return maxFeatures;
    }

    public void setMaxFeatures(int maxFeatures) {
        this.maxFeatures = maxFeatures;
    }

    public WfsProxyMappingStatus getMappingStatus() {
        return mappingStatus;
    }

    public void setMappingStatus(WfsProxyMappingStatus mappingStatus) {
        this.mappingStatus = mappingStatus;
    }
}
