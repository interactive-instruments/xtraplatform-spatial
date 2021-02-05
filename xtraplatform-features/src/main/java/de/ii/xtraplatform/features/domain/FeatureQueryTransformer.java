/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import java.util.Map;

public interface FeatureQueryTransformer<T> {

    String PROPERTY_NOT_AVAILABLE = "PROPERTY_NOT_AVAILABLE";

    T transformQuery(FeatureQuery featureQuery,
                     Map<String, String> additionalQueryParameters);
}
