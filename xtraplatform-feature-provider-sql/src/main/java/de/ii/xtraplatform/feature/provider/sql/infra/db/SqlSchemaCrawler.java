/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.infra.db;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.feature.provider.sql.app.SimpleFeatureGeometryFromToWkt;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.Column;
import schemacrawler.schema.ColumnDataType;
import schemacrawler.schema.Schema;
import schemacrawler.schema.Table;
import schemacrawler.schemacrawler.RegularExpressionInclusionRule;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder;
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder;
import schemacrawler.tools.databaseconnector.DatabaseConnectionSource;
import schemacrawler.tools.databaseconnector.SingleUseUserCredentials;
import schemacrawler.utility.SchemaCrawlerUtility;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SqlSchemaCrawler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlSchemaCrawler.class);
    private static final List<String> TABLE_BLACKLIST = ImmutableList.of("spatial_ref_sys", "geography_columns", "geometry_columns", "raster_columns", "raster_overviews");

    private final ConnectionInfoSql connectionInfo;
    private final ClassLoader classLoader;
    private Connection connection;

    public SqlSchemaCrawler(ConnectionInfoSql connectionInfo) {
        this.connectionInfo = connectionInfo;
        this.classLoader = Thread.currentThread().getContextClassLoader();
    }

    public SqlSchemaCrawler(ConnectionInfoSql connectionInfo, ClassLoader classLoader) {
        this.connectionInfo = connectionInfo;
        this.classLoader = classLoader;
    }

    public List<FeatureSchema> parseSchema(String schemaName) {

        Catalog catalog;
        try {
            catalog = getCatalog(schemaName);
        } catch (SchemaCrawlerException e) {
            e.printStackTrace();
            throw new IllegalStateException("could not parse schema");
        }
        Map<String, List<String>> geometry = getGeometry();

        return getFeatureTypes(catalog, geometry);

    }

    private List<FeatureSchema> getFeatureTypes(Catalog catalog, Map<String, List<String>> geometry) {
        ImmutableList.Builder<FeatureSchema> featureTypes = new ImmutableList.Builder<>();

        for (final Schema schema : catalog.getSchemas()) {

            for (final Table table : catalog.getTables(schema)) {
                if (TABLE_BLACKLIST.contains(table.getName())) {
                    continue;
                }

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
                            if (Objects.isNull(geometry.get(table.getName()))) {
                                continue;
                            }
                            List<String> geometryInfo = geometry.get(table.getName());
                            String crs = geometryInfo.get(1);
                            featureProperty.geometryType(SimpleFeatureGeometryFromToWkt.fromString(geometryInfo.get(0))
                                            .toSimpleFeatureGeometry())
                                    .additionalInfo(ImmutableMap.of("crs", crs));
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

    private Catalog getCatalog(String schemaNames) throws SchemaCrawlerException {
        final SchemaCrawlerOptionsBuilder optionsBuilder = SchemaCrawlerOptionsBuilder.builder()
                                                                                      .withSchemaInfoLevel(SchemaInfoLevelBuilder.maximum())
                                                                                      .includeSchemas(new RegularExpressionInclusionRule(schemaNames));

        final SchemaCrawlerOptions options = optionsBuilder.toOptions();

        // Get the schema definition
        Catalog catalog = SchemaCrawlerUtility.getCatalog(getConnection(), options);

        return catalog;
    }

    private Map<String, List<String>> getGeometry() {
        Map<String, List<String>> geometry = new HashMap<>();
        Connection con = getConnection();
        Statement stmt;
        String query = "SELECT * FROM public.geometry_columns;";
        try {
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                geometry.put(rs.getString("f_table_name"), ImmutableList.of(rs.getString("type"), rs.getString("srid")));
            }
        } catch (SQLException e) {
            LOGGER.debug("SQL QUERY EXCEPTION", e);
        }
        return geometry;
    }


    private Connection getConnection() {
        try {
            if (Objects.isNull(connection) || connection.isClosed()) {
                Thread.currentThread().setContextClassLoader(classLoader);
                //LOGGER.debug("CLASSL {}", classLoader);
                final String connectionUrl = String.format("jdbc:postgresql://%1$s/%2$s", connectionInfo.getHost(), connectionInfo.getDatabase());
                final DatabaseConnectionSource dataSource = new DatabaseConnectionSource(connectionUrl);
                dataSource.setUserCredentials(new SingleUseUserCredentials(connectionInfo.getUser(), connectionInfo.getPassword()));
                connection = dataSource.get();
            }
        } catch (SQLException e) {
            LOGGER.debug("SQL CONNECTION ERROR", e);
        }
        return connection;
    }
}
