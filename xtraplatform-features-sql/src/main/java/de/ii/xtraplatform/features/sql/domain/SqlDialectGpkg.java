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
import de.ii.xtraplatform.crs.domain.EpsgCrs.Force;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.sql.domain.SchemaSql.PropertyTypeInfo;
import de.ii.xtraplatform.features.sql.domain.SqlDialectGpkg.DbInfoGpkg.SpatialMetadata;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.immutables.value.Value;
import org.threeten.extra.Interval;

public class SqlDialectGpkg implements SqlDialect {

  private static final Splitter BBOX_SPLITTER =
      Splitter.onPattern("[(), ]").omitEmptyStrings().trimResults();

  @Override
  public String applyToWkt(String column, boolean forcePolygonCCW, boolean linearizeCurves) {
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
  public String applyToDiameter(String geomExpression, boolean is3d) {
    throw new IllegalArgumentException(
        "DIAMETER2D()/DIAMETER3D() is not supported for GeoPackage feature providers.");
  }

  @Override
  public String applyToJsonValue(
      String alias, String column, String path, PropertyTypeInfo typeInfo) {

    if (typeInfo.getInArray()) {
      throw new IllegalArgumentException(
          "Queryables that are values in an array are not supported for GeoPackage feature providers.");
    }

    String cast = "";
    if (Objects.nonNull(typeInfo.getType())) {
      switch (typeInfo.getType()) {
        case STRING:
        case FLOAT:
        case INTEGER:
        case BOOLEAN:
          cast = getCast(typeInfo.getType());
          break;
        case VALUE:
        case FEATURE_REF:
          cast = typeInfo.getValueType().map(this::getCast).orElse(getCast(Type.STRING));
          break;
        case VALUE_ARRAY:
        case FEATURE_REF_ARRAY:
          throw new IllegalArgumentException(
              "Arrays as queryables are not supported for GeoPackage feature providers.");
      }
    }

    if (Objects.isNull(path)) {
      if (cast.isEmpty()) {
        return String.format("%s.%s", alias, column);
      }
      return String.format("cast(%s.%s as %s)", alias, column, cast);
    }
    if (cast.isEmpty()) {
      return String.format("%s.%s ->> '$.%s'", alias, column, path);
    }
    return String.format("cast((%s.%s ->> '$.%s') as %s)", alias, column, path, cast);
  }

  private String getCast(Type valueType) {
    switch (valueType) {
      case FLOAT:
        return "real";
      case INTEGER:
      case BOOLEAN:
        return "integer";
      default:
      case STRING:
        return "text";
    }
  }

  @Override
  public String castToBigInt(int value) {
    return String.format("CAST(%d AS BIGINT)", value);
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
  public Map<String, GeoInfo> getGeoInfo(Connection connection, DbInfo dbInfo) throws SQLException {
    if (!(dbInfo instanceof DbInfoGpkg)
        || ((DbInfoGpkg) dbInfo).getSpatialMetadata() == SpatialMetadata.UNSUPPORTED) {
      throw new SQLException("Not a valid spatial SQLite database.");
    }
    String query =
        ((DbInfoGpkg) dbInfo).getSpatialMetadata() == SpatialMetadata.GPKG
            ? String.format(
                "SELECT table_name AS \"%s\", column_name AS \"%s\", CASE z WHEN 1 THEN 3 ELSE 2 END AS \"%s\", srs_id AS \"%s\", geometry_type_name AS \"%s\" FROM gpkg_geometry_columns;",
                GeoInfo.TABLE, GeoInfo.COLUMN, GeoInfo.DIMENSION, GeoInfo.SRID, GeoInfo.TYPE)
            : String.format(
                "SELECT f_table_name AS \"%s\", f_geometry_column AS \"%s\", coord_dimension AS \"%s\", srid AS \"%s\", geometry_type AS \"%s\" FROM geometry_columns;",
                GeoInfo.TABLE, GeoInfo.COLUMN, GeoInfo.DIMENSION, GeoInfo.SRID, GeoInfo.TYPE);

    Statement stmt = connection.createStatement();
    ResultSet rs = stmt.executeQuery(query);
    Map<String, GeoInfo> result = new LinkedHashMap<>();

    while (rs.next()) {
      result.put(
          rs.getString(GeoInfo.TABLE),
          ImmutableGeoInfo.of(
              null,
              rs.getString(GeoInfo.TABLE),
              rs.getString(GeoInfo.COLUMN),
              rs.getString(GeoInfo.DIMENSION),
              rs.getString(GeoInfo.SRID),
              forceAxisOrder(dbInfo).name(),
              rs.getString(GeoInfo.TYPE)));
    }

    return result;
  }

  @Override
  public DbInfo getDbInfo(Connection connection) throws SQLException {
    String query =
        "SELECT sqlite_version(),spatialite_version(),CASE CheckSpatialMetaData() WHEN 4 THEN 'GPKG' WHEN 3 THEN 'SPATIALITE' ELSE 'UNSUPPORTED' END;";

    Statement stmt = connection.createStatement();
    ResultSet rs = stmt.executeQuery(query);
    rs.next();

    return ImmutableDbInfoGpkg.of(
        rs.getString(1), rs.getString(2), SpatialMetadata.valueOf(rs.getString(3)));
  }

  @Value.Immutable
  public interface DbInfoGpkg extends DbInfo {
    enum SpatialMetadata {
      GPKG,
      SPATIALITE,
      UNSUPPORTED
    }

    @Value.Parameter
    String getSqliteVersion();

    @Value.Parameter
    String getSpatialiteVersion();

    @Value.Parameter
    SpatialMetadata getSpatialMetadata();
  }

  @Override
  public EpsgCrs.Force forceAxisOrder(DbInfo dbInfo) {
    if (dbInfo instanceof DbInfoGpkg
        && ((DbInfoGpkg) dbInfo).getSpatialMetadata() == SpatialMetadata.GPKG) {
      return Force.LON_LAT;
    }

    return Force.NONE;
  }

  @Override
  public List<String> getSystemSchemas() {
    return ImmutableList.of();
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
