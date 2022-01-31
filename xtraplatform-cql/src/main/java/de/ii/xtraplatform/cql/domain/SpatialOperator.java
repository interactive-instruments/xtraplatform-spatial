/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

public enum SpatialOperator implements CqlNode {
    S_INTERSECTS,
    S_EQUALS,
    S_DISJOINT,
    S_TOUCHES,
    S_WITHIN,
    S_OVERLAPS,
    S_CROSSES,
    S_CONTAINS
}
