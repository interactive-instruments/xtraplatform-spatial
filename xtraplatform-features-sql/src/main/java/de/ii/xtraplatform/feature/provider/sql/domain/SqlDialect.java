/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.domain;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.cql.domain.ImmutableTAfter;
import de.ii.xtraplatform.cql.domain.ImmutableTBefore;
import de.ii.xtraplatform.cql.domain.ImmutableSContains;
import de.ii.xtraplatform.cql.domain.ImmutableSCrosses;
import de.ii.xtraplatform.cql.domain.ImmutableSDisjoint;
import de.ii.xtraplatform.cql.domain.ImmutableTDuring;
import de.ii.xtraplatform.cql.domain.ImmutableTEquals;
import de.ii.xtraplatform.cql.domain.ImmutableSIntersects;
import de.ii.xtraplatform.cql.domain.ImmutableSOverlaps;
import de.ii.xtraplatform.cql.domain.ImmutableSEquals;
import de.ii.xtraplatform.cql.domain.ImmutableSTouches;
import de.ii.xtraplatform.cql.domain.ImmutableSWithin;
import de.ii.xtraplatform.cql.domain.ImmutableTIntersects;
import de.ii.xtraplatform.cql.domain.SpatialOperation;
import de.ii.xtraplatform.cql.domain.TemporalOperation;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.threeten.extra.Interval;

public interface SqlDialect {

  String applyToWkt(String column, boolean forcePolygonCCW);

  String applyToExtent(String column);

  String applyToDatetime(String column);

  Optional<BoundingBox> parseExtent(String extent, EpsgCrs crs);

  Optional<Interval> parseTemporalExtent(String start, String end);

  String escapeString(String value);

  String geometryInfoQuery(Map<String, String> dbInfo);

  List<String> getSystemTables();

 default String getSpatialOperator(Class<? extends SpatialOperation> clazz) {
   return SPATIAL_OPERATORS.get(clazz);
 }

  default String getTemporalOperator(Class<? extends TemporalOperation> clazz) {
    return TEMPORAL_OPERATORS.get(clazz);
  }

  interface GeoInfo {

    String SCHEMA = "schema";
    String TABLE = "table";
    String COLUMN = "column";
    String DIMENSION = "dimension";
    String SRID = "srid";
    String TYPE = "type";
  }

  Map<Class<?>, String> TEMPORAL_OPERATORS = new ImmutableMap.Builder<Class<?>, String>()
      .put(ImmutableTAfter.class, ">")
      .put(ImmutableTBefore.class, "<")
      .put(ImmutableTDuring.class, "BETWEEN")
      .put(ImmutableTEquals.class, "=")
      .put(ImmutableTIntersects.class, "OVERLAPS")
      .build();

  Map<Class<?>, String> SPATIAL_OPERATORS = new ImmutableMap.Builder<Class<?>, String>()
      .put(ImmutableSEquals.class, "ST_Equals")
      .put(ImmutableSDisjoint.class, "ST_Disjoint")
      .put(ImmutableSTouches.class, "ST_Touches")
      .put(ImmutableSWithin.class, "ST_Within")
      .put(ImmutableSOverlaps.class, "ST_Overlaps")
      .put(ImmutableSCrosses.class, "ST_Crosses")
      .put(ImmutableSIntersects.class, "ST_Intersects")
      .put(ImmutableSContains.class, "ST_Contains")
      .build();

}
