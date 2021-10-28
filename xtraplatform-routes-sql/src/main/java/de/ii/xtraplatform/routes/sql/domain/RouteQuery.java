/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.routes.sql.domain;

import de.ii.xtraplatform.cql.domain.Geometry.Point;
import de.ii.xtraplatform.features.domain.FeatureQueryExtension;
import java.util.List;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
public interface RouteQuery extends FeatureQueryExtension {

  Point getStart();

  Point getEnd();

  List<Point> getWayPoints();

  List<String> getFlags();
}
