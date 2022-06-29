/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

/**
 * @author zahnen
 */
public interface ConnectorFactory {

  FeatureProviderConnector<?, ?, ?> createConnector(FeatureProviderDataV2 featureProviderData);

  void disposeConnector(FeatureProviderConnector<?, ?, ?> connector);

  void onDispose(FeatureProviderConnector<?, ?, ?> connector, Runnable runnable);
}
