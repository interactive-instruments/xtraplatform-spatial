/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.api;

import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureConsumer;
import de.ii.xtraplatform.features.domain.FeatureQuery;

/**
 * @author zahnen
 */
public interface FeatureProvider<T extends FeatureConsumer> {

    FeatureStream<T> getFeatureStream(FeatureQuery query);

    String getSourceFormat();

    default BoundingBox getSpatialExtent(String featureTypeId) {
        return new BoundingBox(-180.0D, -90.0D, 180.0D, 90.0D, EpsgCrs.of(4326));
    }

}
