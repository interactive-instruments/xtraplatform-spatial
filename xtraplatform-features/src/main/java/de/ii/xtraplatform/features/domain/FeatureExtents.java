/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.threeten.extra.Interval;

import java.util.Optional;

public interface FeatureExtents {

    Optional<BoundingBox> getSpatialExtent(String typeName);

    Optional<BoundingBox> getSpatialExtent(String typeName, EpsgCrs crs);

    Optional<Interval> getTemporalExtent(String typeName, String property);

    Optional<Interval> getTemporalExtent(String typeName, String startProperty, String endProperty);
}
