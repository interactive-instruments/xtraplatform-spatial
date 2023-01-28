/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.infra.db;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaGenerator;
import de.ii.xtraplatform.features.domain.Tuple;
import de.ii.xtraplatform.features.sql.app.SimpleFeatureGeometryFromToWkt;
import de.ii.xtraplatform.features.sql.domain.SqlClient;
import de.ii.xtraplatform.features.sql.domain.SqlDialect;
import de.ii.xtraplatform.features.sql.domain.SqlDialect.GeoInfo;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.Column;
import schemacrawler.schema.ColumnDataType;
import schemacrawler.schema.Schema;
import schemacrawler.schema.Table;

public class SchemaGeneratorSql implements SchemaGenerator, Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(SchemaGeneratorSql.class);

  private final SqlClient sqlClient;
  private final Connection connection;
  private final SqlSchemaCrawler schemaCrawler;
  private final List<String> schemas;
  private final List<String> includeTables;
  private final SqlDialect dialect;

  public SchemaGeneratorSql(
      SqlClient sqlClient, List<String> schemas, List<String> includeTables, SqlDialect dialect) {
    this.sqlClient = sqlClient;
    this.connection = sqlClient.getConnection();
    this.schemaCrawler = new SqlSchemaCrawler(sqlClient.getConnection());
    this.schemas = schemas;
    this.includeTables = includeTables;
    this.dialect = dialect;
  }

  @Override
  public List<FeatureSchema> generate() {
    try {
      LOGGER.debug("Crawling SQL schema");

      Tuple<Catalog, List<String>> catalogAndMatching =
          schemaCrawler.getCatalogAndMatching(schemas, includeTables, dialect.getSystemTables());

      Map<String, List<String>> geometryInfos = getGeometryInfos();

      List<FeatureSchema> featureTypes =
          getFeatureTypes(catalogAndMatching.first(), catalogAndMatching.second(), geometryInfos);

      LOGGER.debug("Finished crawling SQL schema");

      return featureTypes;
    } catch (Throwable e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Stacktrace:", e);
      }
      throw new IllegalStateException("Could not crawl schema: " + e.getMessage());
    }
  }

  private List<FeatureSchema> getFeatureTypes(
      Catalog catalog, List<String> includeTables, Map<String, List<String>> geometryInfos) {
    ImmutableList.Builder<FeatureSchema> featureTypes = new ImmutableList.Builder<>();

    for (final Schema schema : catalog.getSchemas()) {

      SchemaInfo schemaInfo = new SchemaInfo(catalog.getTables(schema));

      for (final Table table : catalog.getTables(schema)) {
        if (!includeTables.isEmpty() && !includeTables.contains(table.getName())) {
          continue;
        }
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Generating type '{}'", table.getName());
        }

        ImmutableFeatureSchema.Builder featureType =
            new ImmutableFeatureSchema.Builder()
                .name(table.getName())
                .sourcePath("/" + table.getName().toLowerCase());

        boolean idFound = false;

        for (final Column column : table.getColumns()) {
          SchemaBase.Type featurePropertyType = getFeaturePropertyType(column.getColumnDataType());
          if (featurePropertyType != SchemaBase.Type.UNKNOWN) {
            ImmutableFeatureSchema.Builder featureProperty =
                new ImmutableFeatureSchema.Builder()
                    .name(column.getName())
                    .sourcePath(column.getName())
                    .type(featurePropertyType);
            if (!idFound && schemaInfo.isColumnUnique(column.getName(), table.getName(), false)) {
              featureProperty.role(SchemaBase.Role.ID);
              idFound = true;
            }
            if (featurePropertyType == SchemaBase.Type.GEOMETRY) {
              if (!geometryInfos.containsKey(table.getName())) {
                continue;
              }
              List<String> geometryInfo = geometryInfos.get(table.getName());

              // if srid=0, do not set, will use default
              try {
                int srid = Integer.parseInt(geometryInfo.get(1));
                if (srid > 0) {
                  featureProperty.additionalInfo(ImmutableMap.of("crs", String.valueOf(srid)));
                }
              } catch (Throwable e) {
                // ignore
              }

              featureProperty.geometryType(
                  SimpleFeatureGeometryFromToWkt.fromString(geometryInfo.get(0))
                      .toSimpleFeatureGeometry());
            }
            featureType.putPropertyMap(column.getName(), featureProperty.build());
          }
        }

        ImmutableFeatureSchema featureSchema = featureType.build();

        if (featureSchema.getProperties().stream().noneMatch(FeatureSchema::isId)) {
          LOGGER.warn(
              "No primary key or unique column found for table '{}', you have to adjust the type configuration manually.",
              table.getName());
        }

        featureTypes.add(featureSchema);
      }
    }
    return featureTypes.build();
  }

  private SchemaBase.Type getFeaturePropertyType(ColumnDataType columnDataType) {
    // TODO: pass GeoInfo to determine geo columns
    if (SimpleFeatureGeometryFromToWkt.fromString(columnDataType.getName())
        != SimpleFeatureGeometryFromToWkt.NONE) {
      return SchemaBase.Type.GEOMETRY;
    }

    switch (columnDataType.getJavaSqlType().getJavaSqlTypeGroup()) {
      case bit:
        return SchemaBase.Type.BOOLEAN;
      case character:
        return SchemaBase.Type.STRING;
      case integer:
        return SchemaBase.Type.INTEGER;
      case real:
        return SchemaBase.Type.FLOAT;
      case temporal:
        return SchemaBase.Type.DATETIME;
      default:
        return SchemaBase.Type.UNKNOWN;
    }
  }

  // TODO: use SqlClient
  private Map<String, List<String>> getGeometryInfos() {
    Map<String, List<String>> geometry = new HashMap<>();
    Map<String, String> dbInfo = sqlClient.getDbInfo();
    String query = dialect.geometryInfoQuery(dbInfo);

    try {
      Statement stmt = connection.createStatement();
      ResultSet rs = stmt.executeQuery(query);
      while (rs.next()) {
        geometry.put(
            rs.getString(GeoInfo.TABLE),
            ImmutableList.of(rs.getString(GeoInfo.TYPE), rs.getString(GeoInfo.SRID)));
      }
    } catch (SQLException e) {
      LOGGER.debug("SQL QUERY EXCEPTION", e);
    }

    return geometry;
  }

  @Override
  public void close() throws IOException {
    try {
      schemaCrawler.close();
      connection.close();
    } catch (SQLException exception) {
      throw new IOException(exception);
    }
  }
}
