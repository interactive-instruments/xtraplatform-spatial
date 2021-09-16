/**
 * Copyright 2021 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
 * the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.domain;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.cql.domain.ImmutableAfter;
import de.ii.xtraplatform.cql.domain.ImmutableAnyInteracts;
import de.ii.xtraplatform.cql.domain.ImmutableBefore;
import de.ii.xtraplatform.cql.domain.ImmutableContains;
import de.ii.xtraplatform.cql.domain.ImmutableCrosses;
import de.ii.xtraplatform.cql.domain.ImmutableDisjoint;
import de.ii.xtraplatform.cql.domain.ImmutableDuring;
import de.ii.xtraplatform.cql.domain.ImmutableEquals;
import de.ii.xtraplatform.cql.domain.ImmutableIntersects;
import de.ii.xtraplatform.cql.domain.ImmutableOverlaps;
import de.ii.xtraplatform.cql.domain.ImmutableTEquals;
import de.ii.xtraplatform.cql.domain.ImmutableTouches;
import de.ii.xtraplatform.cql.domain.ImmutableWithin;
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
      .put(ImmutableAfter.class, ">")
      .put(ImmutableBefore.class, "<")
      .put(ImmutableDuring.class, "BETWEEN")
      .put(ImmutableTEquals.class, "=")
      .put(ImmutableAnyInteracts.class, "OVERLAPS")
      .build();

  Map<Class<?>, String> SPATIAL_OPERATORS = new ImmutableMap.Builder<Class<?>, String>()
      .put(ImmutableEquals.class, "ST_Equals")
      .put(ImmutableDisjoint.class, "ST_Disjoint")
      .put(ImmutableTouches.class, "ST_Touches")
      .put(ImmutableWithin.class, "ST_Within")
      .put(ImmutableOverlaps.class, "ST_Overlaps")
      .put(ImmutableCrosses.class, "ST_Crosses")
      .put(ImmutableIntersects.class, "ST_Intersects")
      .put(ImmutableContains.class, "ST_Contains")
      .build();

}
