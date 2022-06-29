/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.app;

public enum Type {
  String,
  Boolean,
  Integer,
  Long,
  Double,
  LocalDate,
  Instant,
  Interval,
  OPEN,
  Geometry,
  List,
  UNKNOWN;

  public String schemaType() {
    switch (this) {
      case String:
        return "STRING";
      case Boolean:
        return "BOOLEAN";
      case Integer:
      case Long:
        return "INTEGER";
      case Double:
        return "FLOAT";
      case LocalDate:
        return "DATE";
      case OPEN:
        return "UNBOUNDED_START_OR_END";
      case Instant:
        return "DATETIME";
      case Interval:
        return "INTERVAL";
      case Geometry:
        return "GEOMETRY";
      case List:
        return "ARRAY";
      default:
        return "unknown";
    }
  }
}
