/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.infra.db;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.feature.provider.sql.app.SimpleFeatureGeometryFromToWkt;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlClient;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaGenerator;
import java.io.Closeable;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.Column;
import schemacrawler.schema.ColumnDataType;
import schemacrawler.schema.Schema;
import schemacrawler.schema.Table;
import schemacrawler.schemacrawler.SchemaCrawlerException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchemaGeneratorSql implements SchemaGenerator, Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaGeneratorSql.class);

    private final Connection connection;
    private final SqlSchemaCrawler schemaCrawler;
    private final List<String> schemas;

    public SchemaGeneratorSql(SqlClient sqlClient, List<String> schemas) {
        this.connection = sqlClient.getConnection();
        this.schemaCrawler = new SqlSchemaCrawler(sqlClient.getConnection());
        this.schemas = schemas;
    }

    @Override
    public List<FeatureSchema> generate() {

        Catalog catalog;
        try {
            catalog = schemaCrawler.getCatalog(schemas);

            Map<String, List<String>> geometryInfos = getGeometryInfos();

            return getFeatureTypes(catalog, geometryInfos);
        } catch (SchemaCrawlerException e) {
            throw new IllegalStateException("could not parse schema");
        }
    }

    private List<FeatureSchema> getFeatureTypes(Catalog catalog, Map<String, List<String>> geometryInfos) {
        ImmutableList.Builder<FeatureSchema> featureTypes = new ImmutableList.Builder<>();

        for (final Schema schema : catalog.getSchemas()) {

            for (final Table table : catalog.getTables(schema)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Generating type '{}'", table.getName());
                }

                ImmutableFeatureSchema.Builder featureType = new ImmutableFeatureSchema.Builder()
                                                                    .name(table.getName())
                                                                    .sourcePath("/" + table.getName().toLowerCase());

                for (final Column column : table.getColumns()) {
                    SchemaBase.Type featurePropertyType = getFeaturePropertyType(column.getColumnDataType());
                    if (featurePropertyType != SchemaBase.Type.UNKNOWN) {
                        ImmutableFeatureSchema.Builder featureProperty = new ImmutableFeatureSchema.Builder()
                                .name(column.getName())
                                .sourcePath(column.getName())
                                .type(featurePropertyType);
                        if (column.isPartOfPrimaryKey()) {
                            featureProperty.role(SchemaBase.Role.ID);
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
                                //ignore
                            }

                            featureProperty.geometryType(SimpleFeatureGeometryFromToWkt.fromString(geometryInfo.get(0))
                                            .toSimpleFeatureGeometry());
                        }
                        featureType.putPropertyMap(column.getName(), featureProperty.build());
                    }
                }

                featureTypes.add(featureType.build());
            }
        }
        return featureTypes.build();
    }

    private SchemaBase.Type getFeaturePropertyType(ColumnDataType columnDataType) {

        if ("geometry".equals(columnDataType.getName())) {
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

    //TODO: use SqlClient, query is PGIS specific
    private Map<String, List<String>> getGeometryInfos() {
        Map<String, List<String>> geometry = new HashMap<>();
        String query = "SELECT f_table_name, type, srid FROM public.geometry_columns;";

        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                geometry.put(rs.getString("f_table_name"), ImmutableList.of(rs.getString("type"), rs.getString("srid")));
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
