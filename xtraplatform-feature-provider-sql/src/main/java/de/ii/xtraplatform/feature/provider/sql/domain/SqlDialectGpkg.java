/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.domain;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

public class SqlDialectGpkg implements SqlDialect {

  private final static Splitter BBOX_SPLITTER = Splitter.onPattern("[(), ]")
      .omitEmptyStrings()
      .trimResults();

  @Override
  public String applyToWkt(String column) {
    return String.format("ST_AsText(ST_ForcePolygonCCW(%s))", column);
  }

  @Override
  public String applyToExtent(String column) {
    return String.format("ST_AsText(Extent(%s))", column);
  }

  @Override
  public Optional<BoundingBox> parseExtent(String extent, EpsgCrs crs) {
    List<String> bbox = BBOX_SPLITTER.splitToList(extent);

    if (bbox.size() > 6) {
      return Optional.of(BoundingBox
          .of(Double.parseDouble(bbox.get(1)), Double.parseDouble(bbox.get(2)),
              Double.parseDouble(bbox.get(5)), Double.parseDouble(bbox.get(6)), crs));
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
  public String applyToDatetime(String column) {
    return String.format("datetime(%s)", column);
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
    return ImmutableList
        .of("gpkg_.*", "sqlite_.*", "rtree_.*", "spatial_ref_sys.*", "geometry_columns.*",
            "geom_cols.*", "views_geometry_columns.*", "virts_geometry_columns.*",
            "vector_layers.*", "spatialite_.*", "sql_statements_log", "sqlite_sequence",
            "ElementaryGeometries", "SpatialIndex");
  }
}
