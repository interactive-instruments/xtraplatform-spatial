/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.api;

/**
 * @author zahnen
 */
public interface FeatureProviderRegistry {
    boolean isSupported(String providerType, String connectorType);

    FeatureProvider2 createFeatureProvider(FeatureProviderData featureProviderData);
}
