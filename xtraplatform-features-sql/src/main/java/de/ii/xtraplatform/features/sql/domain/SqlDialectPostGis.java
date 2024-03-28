/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.domain;

import static de.ii.xtraplatform.cql.domain.ArrayFunction.A_CONTAINEDBY;
import static de.ii.xtraplatform.cql.domain.ArrayFunction.A_CONTAINS;
import static de.ii.xtraplatform.cql.domain.ArrayFunction.A_EQUALS;
import static de.ii.xtraplatform.cql.domain.ArrayFunction.A_OVERLAPS;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.cql.domain.ArrayFunction;
import de.ii.xtraplatform.cql.domain.SpatialFunction;
import de.ii.xtraplatform.cql.domain.TemporalFunction;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.sql.domain.SchemaSql.PropertyTypeInfo;
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
import java.util.Set;
import org.immutables.value.Value;
import org.threeten.extra.Interval;

public class SqlDialectPostGis implements SqlDialect {

  private static final Splitter BBOX_SPLITTER =
      Splitter.onPattern("[(), ]").omitEmptyStrings().trimResults();
  private static final Map<SpatialFunction, String> SPATIAL_OPERATORS_3D =
      new ImmutableMap.Builder<SpatialFunction, String>()
          .put(SpatialFunction.S_INTERSECTS, "ST_3DIntersects")
          .build();
  public static final Map<TemporalFunction, String> TEMPORAL_OPERATORS =
      new ImmutableMap.Builder<TemporalFunction, String>()
          .put(
              TemporalFunction.T_INTERSECTS,
              "OVERLAPS") // "({start1},{end1}) OVERLAPS ({start2},{end2})"
          .build();

  @Override
  public String applyToWkt(String column, boolean forcePolygonCCW, boolean linearizeCurves) {
    StringBuilder queryBuilder = new StringBuilder("ST_AsText(");
    if (linearizeCurves) {
      queryBuilder.append("ST_CurveToLine(");
    }
    if (forcePolygonCCW) {
      queryBuilder.append("ST_ForcePolygonCCW(");
    }
    queryBuilder.append(column);
    if (forcePolygonCCW) {
      queryBuilder.append(")");
    }
    if (linearizeCurves) {
      queryBuilder.append(",32,0,1)");
    }
    return queryBuilder.append(")").toString();
  }

  @Override
  public String applyToExtent(String column, boolean is3d) {
    return is3d ? String.format("ST_3DExtent(%s)", column) : String.format("ST_Extent(%s)", column);
  }

  @Override
  public String castToBigInt(int value) {
    return String.format("%d::bigint", value);
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
              Double.parseDouble(bbox.get(3)),
              Double.parseDouble(bbox.get(4)),
              Double.parseDouble(bbox.get(5)),
              Double.parseDouble(bbox.get(6)),
              crs));
    } else if (bbox.size() > 4) {
      return Optional.of(
          BoundingBox.of(
              Double.parseDouble(bbox.get(1)),
              Double.parseDouble(bbox.get(2)),
              Double.parseDouble(bbox.get(3)),
              Double.parseDouble(bbox.get(4)),
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
  public String getSpatialOperator(SpatialFunction spatialFunction, boolean is3d) {
    return is3d && SPATIAL_OPERATORS_3D.containsKey(spatialFunction)
        ? SPATIAL_OPERATORS_3D.get(spatialFunction)
        : SqlDialect.super.getSpatialOperator(spatialFunction, is3d);
  }

  @Override
  public String getTemporalOperator(TemporalFunction temporalFunction) {
    return TEMPORAL_OPERATORS.get(temporalFunction);
  }

  @Override
  public Set<TemporalFunction> getTemporalOperators() {
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
  }
  ;

  @Override
  public String applyToInstantMax() {
    return "infinity";
  }

  @Override
  public String applyToDiameter(String geomExpression, boolean is3d) {
    // the bounding box is transformed to a CRS that uses meter for all axes
    if (is3d) {
      if (geomExpression.contains("%1$s") && geomExpression.contains("%2$s")) {
        return String.format(
            geomExpression,
            "%1$sST_3DLength(ST_BoundingDiagonal(Box3D(ST_Transform(ST_Force3DZ(",
            "),4978))))%2$s");
      }
      return String.format(
          "ST_3DLength(ST_BoundingDiagonal(Box3D(ST_Transform(ST_Force3DZ(%s),4978))))",
          geomExpression);
    }
    if (geomExpression.contains("%1$s") && geomExpression.contains("%2$s")) {
      return String.format(
          geomExpression, "%1$sST_Length(ST_BoundingDiagonal(Box2D(ST_Transform(", ",3857))))%2$s");
    }
    return String.format(
        "ST_Length(ST_BoundingDiagonal(Box2D(ST_Transform(%s,3857))))", geomExpression);
  }

  @Override
  public String applyToJsonValue(
      String alias, String column, String path, PropertyTypeInfo typeInfo) {

    if (typeInfo.getInArray()) {
      return String.format("jsonb_path_query_array(%s.%s::jsonb,'$.%s')", alias, column, path);
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
        case VALUE_ARRAY:
        case FEATURE_REF_ARRAY:
          cast = typeInfo.getValueType().map(this::getCast).orElse(getCast(Type.STRING));
          break;
      }
    }

    String finalAlias = alias.isEmpty() ? alias : String.format("%s.", alias);
    if (typeInfo.getType() == Type.VALUE_ARRAY || typeInfo.getType() == Type.FEATURE_REF_ARRAY) {
      if (Objects.isNull(path)) {
        return String.format("%s.%s::jsonb", alias, column);
      } else if (path.contains(".")) {
        return String.format("(%s%s #> '{%s}')", finalAlias, column, path.replaceAll("\\.", ","));
      }
      return String.format("(%s%s -> '%s')", finalAlias, column, path);
    }

    if (Objects.isNull(path)) {
      return String.format("%s%s%s", finalAlias, column, cast);
    } else if (path.contains(".")) {
      return String.format(
          "(%s%s #>> '{%s}')%s", finalAlias, column, path.replaceAll("\\.", ","), cast);
    }
    return String.format("(%s%s ->> '%s')%s", finalAlias, column, path, cast);
  }

  private String getCast(Type valueType) {
    switch (valueType) {
      case FLOAT:
        return "::double";
      case INTEGER:
        return "::integer";
      case BOOLEAN:
        return "::boolean";
      default:
      case STRING:
        return "::varchar";
    }
  }

  @Override
  public String applyToJsonArrayOp(
      ArrayFunction op, boolean notInverse, String mainExpression, String jsonValueArray) {
    if (notInverse ? op == A_CONTAINS : op == A_CONTAINEDBY) {
      String arrayQuery = String.format(" @> '%s'", jsonValueArray);
      return String.format(mainExpression, "", arrayQuery);
    } else if (op == A_EQUALS) {
      String arrayQuery = String.format(" = '%s'", jsonValueArray);
      return String.format(mainExpression, "", arrayQuery);
    } else if (op == A_OVERLAPS) {
      throw new IllegalArgumentException("A_OVERLAPS is not supported in JSON columns.");
    } else if (notInverse ? op == A_CONTAINEDBY : op == A_CONTAINS) {
      String arrayQuery = String.format(" <@ '%s'", jsonValueArray);
      return String.format(mainExpression, "", arrayQuery);
    }
    throw new IllegalStateException("unexpected array operator: " + op);
  }

  @Override
  public String escapeString(String value) {
    return value.replaceAll("'", "''");
  }

  @Override
  public String geometryInfoQuery(Map<String, String> dbInfo) {
    return String.format(
        "SELECT f_table_schema AS \"%s\", f_table_name AS \"%s\", f_geometry_column AS \"%s\", coord_dimension AS \"%s\", srid AS \"%s\", type AS \"%s\" FROM geometry_columns;",
        GeoInfo.SCHEMA,
        GeoInfo.TABLE,
        GeoInfo.COLUMN,
        GeoInfo.DIMENSION,
        GeoInfo.SRID,
        GeoInfo.TYPE);
  }

  @Override
  public Map<String, GeoInfo> getGeoInfo(Connection connection, DbInfo dbInfo) throws SQLException {
    if (!(dbInfo instanceof DbInfoPgis)) {
      throw new SQLException("Not a valid spatial PostgreSQL database.");
    }
    String query =
        String.format(
            "SELECT f_table_schema AS \"%s\", f_table_name AS \"%s\", f_geometry_column AS \"%s\", coord_dimension AS \"%s\", srid AS \"%s\", type AS \"%s\" FROM geometry_columns;",
            GeoInfo.SCHEMA,
            GeoInfo.TABLE,
            GeoInfo.COLUMN,
            GeoInfo.DIMENSION,
            GeoInfo.SRID,
            GeoInfo.TYPE);

    Statement stmt = connection.createStatement();
    ResultSet rs = stmt.executeQuery(query);
    Map<String, GeoInfo> result = new LinkedHashMap<>();

    while (rs.next()) {
      result.put(
          rs.getString(GeoInfo.TABLE),
          ImmutableGeoInfo.of(
              rs.getString(GeoInfo.SCHEMA),
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
    String query = "SELECT version(), PostGIS_Lib_Version();";

    Statement stmt = connection.createStatement();
    ResultSet rs = stmt.executeQuery(query);
    rs.next();

    return ImmutableDbInfoPgis.of(rs.getString(1), rs.getString(2));
  }

  @Value.Immutable
  interface DbInfoPgis extends DbInfo {

    @Value.Parameter
    String getPostgresVersion();

    @Value.Parameter
    String getPostGisVersion();
  }

  @Override
  public List<String> getSystemSchemas() {
    return ImmutableList.of("information_schema", "pg_catalog", "tiger", "tiger_data", "topology");
  }

  @Override
  public List<String> getSystemTables() {
    return ImmutableList.of(
        "spatial_ref_sys",
        "geography_columns",
        "geometry_columns",
        "raster_columns",
        "raster_overviews");
  }
}
