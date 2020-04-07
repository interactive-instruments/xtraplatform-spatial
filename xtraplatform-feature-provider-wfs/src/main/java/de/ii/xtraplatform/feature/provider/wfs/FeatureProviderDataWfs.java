/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.ii.xtraplatform.feature.provider.wfs.infra.WfsConnectorHttp;
import de.ii.xtraplatform.feature.transformer.api.FeatureProviderDataTransformer;
import org.immutables.value.Value;

/**
 * @author zahnen
 */

public abstract class FeatureProviderDataWfs extends FeatureProviderDataTransformer {

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // means only read from json
    @Value.Default
    @Override
    public String getProviderType() {
        return FeatureProviderWfs.PROVIDER_TYPE;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // means only read from json
    @Value.Default
    @Override
    public String getConnectorType() {
        return WfsConnectorHttp.CONNECTOR_TYPE;
    }
}
