/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import java.util.Map;

public interface FeatureQueryEncoder<T, V extends FeatureProviderConnector.QueryOptions> {

  String PROPERTY_NOT_AVAILABLE = "PROPERTY_NOT_AVAILABLE";

  T encode(Query query, Map<String, String> additionalQueryParameters);

  V getOptions(TypeQuery typeQuery);
}
