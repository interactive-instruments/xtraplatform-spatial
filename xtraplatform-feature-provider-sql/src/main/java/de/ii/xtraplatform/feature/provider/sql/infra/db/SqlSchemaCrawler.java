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
import de.ii.xtraplatform.feature.provider.api.FeatureProviderSchemaConsumer;
import de.ii.xtraplatform.feature.provider.sql.app.SimpleFeatureGeometryFromToWkt;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.features.domain.ImmutableFeatureProperty;
import de.ii.xtraplatform.features.domain.ImmutableFeatureType;
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
    private final ConnectionInfoSql connectionInfo;
    private Connection connection;

    public SqlSchemaCrawler(ConnectionInfoSql connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    public List<FeatureType> parseSchema(String schemaName, FeatureProviderSchemaConsumer schemaConsumer) {

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

    private List<FeatureType> getFeatureTypes(Catalog catalog, Map<String, List<String>> geometry) {
        ImmutableList.Builder<FeatureType> featureTypes = new ImmutableList.Builder<>();

        for (final Schema schema : catalog.getSchemas()) {

            for (final Table table : catalog.getTables(schema)) {
                ImmutableFeatureType.Builder featureType = new ImmutableFeatureType.Builder()
                                                                    .name(table.getName());

                for (final Column column : table.getColumns()) {
                    FeatureProperty.Type featurePropertyType = getFeaturePropertyType(column.getColumnDataType());
                    if (featurePropertyType != FeatureProperty.Type.UNKNOWN) {
                        ImmutableFeatureProperty.Builder featureProperty = new ImmutableFeatureProperty.Builder()
                                .name(column.getName())
                                .path(column.getName())
                                .type(featurePropertyType);
                        if (column.isPartOfPrimaryKey()) {
                            featureProperty.role(FeatureProperty.Role.ID);
                        }
                        if (featurePropertyType == FeatureProperty.Type.GEOMETRY && !Objects.isNull(geometry.get(table.getName()))) {
                            List<String> geometryInfo = geometry.get(table.getName());
                            String geometryType = SimpleFeatureGeometryFromToWkt.fromString(geometryInfo.get(0))
                                    .toSimpleFeatureGeometry()
                                    .toString();
                            String crs = geometryInfo.get(1);
                            featureProperty.additionalInfo(ImmutableMap.of("geometryType", geometryType, "crs", crs));
                        }
                        featureType.putProperties(column.getName(), featureProperty.build());
                    }
                }

                featureTypes.add(featureType.build());
            }
        }
        return featureTypes.build();
    }

    private FeatureProperty.Type getFeaturePropertyType(ColumnDataType columnDataType) {

        if ("geometry".equals(columnDataType.getName())) {
            return FeatureProperty.Type.GEOMETRY;
        }

        switch (columnDataType.getJavaSqlType().getJavaSqlTypeGroup()) {
            case bit:
                return FeatureProperty.Type.BOOLEAN;
            case character:
                return FeatureProperty.Type.STRING;
            case integer:
                return FeatureProperty.Type.INTEGER;
            case real:
                return FeatureProperty.Type.FLOAT;
            case temporal:
                return FeatureProperty.Type.DATETIME;
            default:
                return FeatureProperty.Type.UNKNOWN;
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
