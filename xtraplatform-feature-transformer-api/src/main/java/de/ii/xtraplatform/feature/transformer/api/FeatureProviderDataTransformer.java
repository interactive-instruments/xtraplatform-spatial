/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.feature.query.api.FeatureProviderData;
import org.immutables.value.Value;

import javax.xml.namespace.QName;
import java.util.Map;

/**
 * @author zahnen
 */
public abstract class FeatureProviderDataTransformer extends FeatureProviderData {

    public abstract Map<String, QName> getFeatureTypes();

    @JsonMerge
    public abstract Map<String, FeatureTypeMapping> getMappings();

    public abstract boolean isFeatureTypeEnabled(final String featureType);

    public abstract EpsgCrs getNativeCrs();

    @JsonIgnore
    @Value.Default
    public boolean supportsTransactions() {
        return false;
    }
}
