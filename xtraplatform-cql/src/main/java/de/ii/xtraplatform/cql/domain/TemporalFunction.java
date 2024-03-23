/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

public enum TemporalFunction implements CqlNode {
  T_AFTER,
  T_BEFORE,
  T_CONTAINS,
  T_DISJOINT,
  T_DURING,
  T_EQUALS,
  T_FINISHEDBY,
  T_FINISHES,
  T_INTERSECTS,
  T_MEETS,
  T_METBY,
  T_OVERLAPPEDBY,
  T_OVERLAPS,
  T_STARTEDBY,
  T_STARTS
}
