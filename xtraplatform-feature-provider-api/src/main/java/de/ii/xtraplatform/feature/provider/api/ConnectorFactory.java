/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.api;

import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV1;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;

/**
 * @author zahnen
 */
public interface ConnectorFactory {

    FeatureProviderConnector<?, ?, ?> createConnector(FeatureProviderDataV2 featureProviderData);

    void disposeConnector(FeatureProviderConnector<?, ?, ?> connector);
}
