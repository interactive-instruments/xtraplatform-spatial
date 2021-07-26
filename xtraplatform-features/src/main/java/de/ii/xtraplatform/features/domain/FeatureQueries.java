/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

public interface FeatureQueries {

    FeatureStream2 getFeatureStream2(FeatureQuery query);

    long getFeatureCount(FeatureQuery featureQuery);

    default FeatureStream getFeatureStream(FeatureQuery query) {
        throw new UnsupportedOperationException();
    }
}
