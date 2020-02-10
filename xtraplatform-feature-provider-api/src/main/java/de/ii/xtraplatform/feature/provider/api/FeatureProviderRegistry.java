/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.api;

import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV1;

/**
 * @author zahnen
 */
public interface FeatureProviderRegistry {
    boolean isSupported(String providerType, String connectorType);

    //FeatureProvider2 createFeatureProvider(FeatureProviderDataV1 featureProviderData);

    FeatureProviderConnector createConnector(FeatureProviderDataV1 featureProviderData);
}
