/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.domain;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.threeten.extra.Interval;

public class SqlDialectGpkg implements SqlDialect {

  private static final Splitter BBOX_SPLITTER =
      Splitter.onPattern("[(), ]").omitEmptyStrings().trimResults();

  @Override
  public String applyToWkt(String column, boolean forcePolygonCCW) {
    if (!forcePolygonCCW) {
      return String.format("ST_AsText(%s)", column);
    }
    return String.format("ST_AsText(ST_ForcePolygonCCW(%s))", column);
  }

  @Override
  public String applyToExtent(String column, boolean is3d) {
    // Extent() results in a 2D Polygon in Spatialite
    return String.format("ST_AsText(Extent(%s))", column);
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
    return String.format("cast(%s as text)", string);
  }

  @Override
  public String applyToDate(String column) {
    return String.format("date(%s)", column);
  }

  @Override
  public String applyToDatetime(String column) {
    return String.format("datetime(%s)", column);
  }

  @Override
  public String applyToDateLiteral(String date) {
    return String.format("date('%s')", date);
  }

  @Override
  public String applyToDatetimeLiteral(String datetime) {
    return String.format("datetime('%s')", datetime);
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
  public String applyToDiameter(String geomExpression) {
    throw new IllegalArgumentException(
        "DIAMETER() is not supported for GeoPackage feature providers.");
  }

  @Override
  public String escapeString(String value) {
    return value.replaceAll("'", "''");
  }

  @Override
  public String geometryInfoQuery(Map<String, String> dbInfo) {
    if (Objects.equals(dbInfo.get("spatial_metadata"), "GPKG")) {
      return String.format(
          "SELECT table_name AS \"%s\", column_name AS \"%s\", CASE z WHEN 1 THEN 3 ELSE 2 END AS \"%s\", srs_id AS \"%s\", geometry_type_name AS \"%s\" FROM gpkg_geometry_columns;",
          GeoInfo.TABLE, GeoInfo.COLUMN, GeoInfo.DIMENSION, GeoInfo.SRID, GeoInfo.TYPE);
    }

    return String.format(
        "SELECT f_table_name AS \"%s\", f_geometry_column AS \"%s\", coord_dimension AS \"%s\", srid AS \"%s\", geometry_type AS \"%s\" FROM geometry_columns;",
        GeoInfo.TABLE, GeoInfo.COLUMN, GeoInfo.DIMENSION, GeoInfo.SRID, GeoInfo.TYPE);
  }

  @Override
  public List<String> getSystemTables() {
    return ImmutableList.of(
        "gpkg_.*",
        "sqlite_.*",
        "rtree_.*",
        "spatial_ref_sys.*",
        "geometry_columns.*",
        "geom_cols.*",
        "views_geometry_columns.*",
        "virts_geometry_columns.*",
        "vector_layers.*",
        "spatialite_.*",
        "sql_statements_log",
        "sqlite_sequence",
        "ElementaryGeometries",
        "SpatialIndex");
  }
}
