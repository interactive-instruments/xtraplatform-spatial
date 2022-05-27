/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.domain;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.cql.domain.TemporalOperation;
import de.ii.xtraplatform.cql.domain.TemporalOperator;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.threeten.extra.Interval;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class SqlDialectPostGis implements SqlDialect {

  private final static Splitter BBOX_SPLITTER = Splitter.onPattern("[(), ]")
      .omitEmptyStrings()
      .trimResults();
  public final static Map<TemporalOperator, String> TEMPORAL_OPERATORS = new ImmutableMap.Builder<TemporalOperator, String>()
      .put(TemporalOperator.T_INTERSECTS, "OVERLAPS") // "({start1},{end1}) OVERLAPS ({start2},{end2})"
      .build();

  @Override
  public String applyToWkt(String column, boolean forcePolygonCCW) {
    if (!forcePolygonCCW) {
      return String.format("ST_AsText(%s)", column);
    }
    return String.format("ST_AsText(ST_ForcePolygonCCW(%s))", column);
  }

  @Override
  public String applyToExtent(String column, boolean is3d) {
    return is3d
        ? String.format("ST_3DExtent(%s)", column)
        : String.format("ST_Extent(%s)", column);
  }

  @Override
  public Optional<BoundingBox> parseExtent(String extent, EpsgCrs crs) {
    List<String> bbox = BBOX_SPLITTER.splitToList(extent);

    if (bbox.size() > 6) {
      return Optional.of(BoundingBox
                             .of(Double.parseDouble(bbox.get(1)), Double.parseDouble(bbox.get(2)),
                                 Double.parseDouble(bbox.get(3)), Double.parseDouble(bbox.get(4)),
                                 Double.parseDouble(bbox.get(5)), Double.parseDouble(bbox.get(6)), crs));
    } else if (bbox.size() > 4) {
      return Optional.of(BoundingBox
                             .of(Double.parseDouble(bbox.get(1)), Double.parseDouble(bbox.get(2)),
                                 Double.parseDouble(bbox.get(3)), Double.parseDouble(bbox.get(4)), crs));
    }

    return Optional.empty();
  }

  @Override
  public Optional<Interval> parseTemporalExtent(String start, String end) {
    if (Objects.isNull(start)) {
      return Optional.empty();
    }
    DateTimeFormatter parser = DateTimeFormatter
        .ofPattern("yyyy-MM-dd[['T'][' ']HH:mm:ss][.SSS][X]").withZone(ZoneOffset.UTC);
    Instant parsedStart = parser.parse(start, Instant::from);
    if (Objects.isNull(end)) {
      return Optional.of(Interval.of(parsedStart, Instant.MAX));
    }
    Instant parsedEnd = parser.parse(end, Instant::from);
    return Optional.of(Interval.of(parsedStart, parsedEnd));
  }

  @Override
  public String getTemporalOperator(TemporalOperator temporalOperator) {
    return TEMPORAL_OPERATORS.get(temporalOperator);
  }

  @Override
  public Set<TemporalOperator> getTemporalOperators() {
    return TEMPORAL_OPERATORS.keySet();
  }

  @Override
  public String applyToString(String string) {
    return String.format("%s::varchar", string);
  }

  @Override
  public String applyToDate(String column) {
    return String.format("%s::date", column);
  }

  @Override
  public String applyToDatetime(String column) {
    return String.format("%s::timestamp(0)", column);
  }

  @Override
  public String applyToDateLiteral(String date) {
    return String.format("DATE '%s'", date);
  }

  @Override
  public String applyToDatetimeLiteral(String datetime) {
    return String.format("TIMESTAMP '%s'", datetime);
  }

  @Override
  public String applyToInstantMin() {
    return "-infinity";
  };

  @Override
  public String applyToInstantMax() {
    return "infinity";
  };

  @Override
  public String escapeString(String value) {
    return value.replaceAll("'", "''");
  }

  @Override
  public String geometryInfoQuery(Map<String, String> dbInfo) {
    return String.format(
        "SELECT f_table_schema AS \"%s\", f_table_name AS \"%s\", f_geometry_column AS \"%s\", coord_dimension AS \"%s\", srid AS \"%s\", type AS \"%s\" FROM geometry_columns;",
        GeoInfo.SCHEMA, GeoInfo.TABLE, GeoInfo.COLUMN, GeoInfo.DIMENSION, GeoInfo.SRID,
        GeoInfo.TYPE);
  }

  @Override
  public List<String> getSystemTables() {
    return ImmutableList
        .of("spatial_ref_sys", "geography_columns", "geometry_columns", "raster_columns",
            "raster_overviews");
  }

}
