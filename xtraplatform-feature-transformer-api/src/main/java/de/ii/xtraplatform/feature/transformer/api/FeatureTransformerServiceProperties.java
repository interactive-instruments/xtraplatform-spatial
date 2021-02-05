/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.feature.transformer.api;

/**
 * @author zahnen
 */
public class FeatureTransformerServiceProperties {
    private int maxFeatures;
    private FeatureTypeMappingStatus mappingStatus;

    public FeatureTransformerServiceProperties() {
        this.mappingStatus = new FeatureTypeMappingStatus();
    }

    public int getMaxFeatures() {
        return maxFeatures;
    }

    public void setMaxFeatures(int maxFeatures) {
        this.maxFeatures = maxFeatures;
    }

    public FeatureTypeMappingStatus getMappingStatus() {
        return mappingStatus;
    }

    public void setMappingStatus(FeatureTypeMappingStatus mappingStatus) {
        this.mappingStatus = mappingStatus;
    }
}
