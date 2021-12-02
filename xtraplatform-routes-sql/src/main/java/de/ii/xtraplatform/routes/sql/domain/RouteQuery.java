/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.routes.sql.domain;

import de.ii.xtraplatform.cql.domain.Geometry.Point;
import de.ii.xtraplatform.cql.domain.Geometry.MultiPolygon;
import de.ii.xtraplatform.features.domain.FeatureQueryExtension;
import java.util.List;
import java.util.Optional;

import org.immutables.value.Value;

@Value.Immutable
public interface RouteQuery extends FeatureQueryExtension {

  Point getStart();

  Point getEnd();

  List<Point> getWayPoints();

  String getCostColumn();

  String getReverseCostColumn();

  Optional<Double> getWeight();

  Optional<Double> getHeight();

  Optional<String> getObstacles(); // TODO use MultiPolygon geometry class and do the WKT conversion here (after consolidation of geometry types); for now we expect WKT

  List<String> getFlags();
}
