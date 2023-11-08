/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.domain;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.xtraplatform.cql.domain.ArrayOperator;
import de.ii.xtraplatform.cql.domain.SpatialOperator;
import de.ii.xtraplatform.cql.domain.TemporalOperator;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.EpsgCrs.Force;
import de.ii.xtraplatform.features.sql.domain.SchemaSql.PropertyTypeInfo;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import org.threeten.extra.Interval;

public interface SqlDialect {

  String applyToWkt(String column, boolean forcePolygonCCW);

  String applyToExtent(String column, boolean is3d);

  String applyToString(String string);

  String applyToDate(String column);

  String applyToDatetime(String column);

  String applyToDateLiteral(String date);

  String applyToDatetimeLiteral(String datetime);

  String applyToInstantMin();

  String applyToInstantMax();

  String applyToDiameter(String geomExpression, boolean is3d);

  String applyToJsonValue(String alias, String column, String path, PropertyTypeInfo typeInfo);

  default String applyToJsonArrayOp(
      ArrayOperator op, boolean notInverse, String mainExpression, String jsonValueArray) {
    throw new IllegalArgumentException(
        "Arrays as queryables are not supported for this feature provider.");
  }

  String castToBigInt(int value);

  Optional<BoundingBox> parseExtent(String extent, EpsgCrs crs);

  Optional<Interval> parseTemporalExtent(String start, String end);

  String escapeString(String value);

  String geometryInfoQuery(Map<String, String> dbInfo);

  Map<String, GeoInfo> getGeoInfo(Connection connection, DbInfo dbInfo) throws SQLException;

  DbInfo getDbInfo(Connection connection) throws SQLException;

  default EpsgCrs.Force forceAxisOrder(DbInfo dbInfo) {
    return Force.NONE;
  }

  List<String> getSystemSchemas();

  List<String> getSystemTables();

  default String getSpatialOperator(SpatialOperator spatialOperator, boolean is3d) {
    return is3d && SPATIAL_OPERATORS_3D.containsKey(spatialOperator)
        ? SPATIAL_OPERATORS_3D.get(spatialOperator)
        : SPATIAL_OPERATORS.get(spatialOperator);
  }

  default String getTemporalOperator(TemporalOperator temporalOperator) {
    // this is implementation specific
    return null;
  }

  default Set<TemporalOperator> getTemporalOperators() {
    return ImmutableSet.of();
  }

  interface DbInfo {}

  @Value.Immutable
  interface GeoInfo {

    String SCHEMA = "schema";
    String TABLE = "table";
    String COLUMN = "column";
    String DIMENSION = "dimension";
    String SRID = "srid";
    String TYPE = "type";

    @Nullable
    @Value.Parameter
    String getSchema();

    @Value.Parameter
    String getTable();

    @Value.Parameter
    String getColumn();

    @Value.Parameter
    String getDimension();

    @Value.Parameter
    String getSrid();

    @Value.Parameter
    String getForce();

    @Value.Parameter
    String getType();
  }

  Map<SpatialOperator, String> SPATIAL_OPERATORS =
      new ImmutableMap.Builder<SpatialOperator, String>()
          .put(SpatialOperator.S_EQUALS, "ST_Equals")
          .put(SpatialOperator.S_DISJOINT, "ST_Disjoint")
          .put(SpatialOperator.S_TOUCHES, "ST_Touches")
          .put(SpatialOperator.S_WITHIN, "ST_Within")
          .put(SpatialOperator.S_OVERLAPS, "ST_Overlaps")
          .put(SpatialOperator.S_CROSSES, "ST_Crosses")
          .put(SpatialOperator.S_INTERSECTS, "ST_Intersects")
          .put(SpatialOperator.S_CONTAINS, "ST_Contains")
          .build();

  Map<SpatialOperator, String> SPATIAL_OPERATORS_3D =
      new ImmutableMap.Builder<SpatialOperator, String>().build();
}
