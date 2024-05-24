/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.oracle.app;

import com.google.common.base.Splitter;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.sql.domain.SchemaSql.PropertyTypeInfo;
import de.ii.xtraplatform.features.sql.domain.SqlDialect;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.threeten.extra.Interval;

public class SqlDialectOras implements SqlDialect {

  private static final Splitter BBOX_SPLITTER =
      Splitter.onPattern("[(), ]").omitEmptyStrings().trimResults();

  @Override
  public String applyToWkt(String column, boolean forcePolygonCCW, boolean linearizeCurves) {
    if (!forcePolygonCCW) {
      return String.format("SDO_UTIL.TO_WKTGEOMETRY(%s)", column);
    }
    return String.format("SDO_UTIL.TO_WKTGEOMETRY(SDO_UTIL.RECTIFY_GEOMETRY(%s, 0.005))", column);
  }

  @Override
  public String applyToExtent(String column, boolean is3d) {
    // SDO_AGGR_MBR() results in a 2D Polygon in Oracle Spatial
    return String.format("SDO_UTIL.TO_WKTGEOMETRY(SDO_AGGR_MBR(%s))", column);
  }

  @Override
  public Optional<BoundingBox> parseExtent(String extent, EpsgCrs crs) {
    if (Objects.isNull(extent)) {
      return Optional.empty();
    }

    List<String> bbox = BBOX_SPLITTER.splitToList(extent);

    if (bbox.size() > 6) {
      return Optional.of(
          BoundingBox.of(
              Double.parseDouble(bbox.get(1)),
              Double.parseDouble(bbox.get(2)),
              Double.parseDouble(bbox.get(5)),
              Double.parseDouble(bbox.get(6)),
              crs));
    }

    return Optional.empty();
  }

  @Override
  public Optional<Interval> parseTemporalExtent(String start, String end) {
    if (Objects.isNull(start)) {
      return Optional.empty();
    }
    DateTimeFormatter parser =
        DateTimeFormatter.ofPattern("yyyy-MM-dd[['T'][' ']HH:mm:ss][.SSS][X]")
            .withZone(ZoneOffset.UTC);
    Instant parsedStart = parser.parse(start, Instant::from);
    if (Objects.isNull(end)) {
      return Optional.of(Interval.of(parsedStart, Instant.MAX));
    }
    Instant parsedEnd = parser.parse(end, Instant::from);
    return Optional.of(Interval.of(parsedStart, parsedEnd));
  }

  @Override
  public String applyToString(String string) {
    return String.format("to_char(%s)", string);
  }

  @Override
  public String applyToDate(String column) {
    return String.format("to_char(%s, 'YYYY-MM-DD\"Z\"')", column);
  }

  @Override
  public String applyToDatetime(String column) {
    return String.format(
        "to_char(to_timestamp(%s, 'YYYY-MM-DD\"Z\"'),'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')", column);
  }

  @Override
  public String applyToDateLiteral(String date) {
    return String.format("to_timestamp('%s', 'YYYY-MM-DD\"Z\"')", date);
  }

  @Override
  public String applyToDatetimeLiteral(String datetime) {
    return String.format("to_timestamp('%s', 'YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')", datetime);
  }

  @Override
  public String applyToInstantMin() {
    return "0001-01-01T00:00:00Z";
  }
  ;

  @Override
  public String applyToInstantMax() {
    return "9999-12-31T23:59:59Z";
  }

  @Override
  public String applyToDiameter(String geomExpression, boolean is3d) {
    throw new IllegalArgumentException(
        "DIAMETER2D()/DIAMETER3D() is not supported for Oracle feature providers.");
  }

  @Override
  public String applyToJsonValue(
      String alias, String column, String path, PropertyTypeInfo typeInfo) {
    throw new IllegalArgumentException("JSON is not supported for Oracle feature providers.");
  }

  @Override
  public String applyToLimit(long limit) {
    return String.format(" FETCH NEXT %d ROWS ONLY", limit);
  }

  @Override
  public String applyToOffset(long offset) {
    return String.format(" OFFSET %d ROWS", offset);
  }

  @Override
  public String applyToNoTable(String select) {
    return String.format("%s FROM dual", select);
  }

  @Override
  public String castToBigInt(int value) {
    return String.format("%d", value);
  }

  @Override
  public String escapeString(String value) {
    return value.replaceAll("'", "''");
  }
}
