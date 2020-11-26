/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.domain;

import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.threeten.extra.Interval;

import java.util.Optional;

public interface SqlDialect {

    String applyToWkt(String column);

    String applyToExtent(String column);

    Optional<BoundingBox> parseExtent(String extent, EpsgCrs crs);

    Optional<Interval> parseTemporalExtent(String start, String end);

    String applyToDatetime(String column);

}
