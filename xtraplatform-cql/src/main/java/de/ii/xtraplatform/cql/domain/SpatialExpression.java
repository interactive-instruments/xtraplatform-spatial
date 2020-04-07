/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import java.util.Optional;

public interface SpatialExpression {

    Optional<Equals> getEquals();

    Optional<Disjoint> getDisjoint();

    Optional<Touches> getTouches();

    Optional<Within> getWithin();

    Optional<Overlaps> getOverlaps();

    Optional<Crosses> getCrosses();

    Optional<Intersects> getIntersects();

    Optional<Contains> getContains();

}
