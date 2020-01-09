package de.ii.xtraplatform.feature.provider.sql.infra.db;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.feature.provider.api.FeatureProperty;
import de.ii.xtraplatform.feature.provider.api.FeatureProviderSchemaConsumer;
import de.ii.xtraplatform.feature.provider.api.FeatureType;
import de.ii.xtraplatform.feature.provider.api.ImmutableFeatureProperty;
import de.ii.xtraplatform.feature.provider.api.ImmutableFeatureType;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql;
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
import java.util.List;

public class SqlSchemaCrawler {

    private final ConnectionInfoSql connectionInfo;

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
        return getFeatureTypes(catalog);

    }

    private List<FeatureType> getFeatureTypes(Catalog catalog) {
        ImmutableList.Builder<FeatureType> featureTypes = new ImmutableList.Builder<>();

        for (final Schema schema : catalog.getSchemas()) {

            for (final Table table : catalog.getTables(schema)) {
                ImmutableFeatureType.Builder featureType = new ImmutableFeatureType.Builder()
                                                                    .name(table.getName());

                for (final Column column : table.getColumns()) {
                    FeatureProperty.Type featurePropertyType = getFeaturePropertyType(column.getColumnDataType());
                    if (featurePropertyType != FeatureProperty.Type.UNKNOWN) {
                        ImmutableFeatureProperty featureProperty = new ImmutableFeatureProperty.Builder()
                                .name(column.getName())
                                .type(featurePropertyType)
                                .build();
                        featureType.putProperties(featureProperty.getName(), featureProperty);
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
                                                                                      .withSchemaInfoLevel(SchemaInfoLevelBuilder.standard())
                                                                                      .includeSchemas(new RegularExpressionInclusionRule(schemaNames));

        final SchemaCrawlerOptions options = optionsBuilder.toOptions();

        // Get the schema definition
        Catalog catalog = SchemaCrawlerUtility.getCatalog(getConnection(), options);

        return catalog;
    }


    private Connection getConnection() {
        final String connectionUrl = String.format("jdbc:postgresql://%1$s/%2$s", connectionInfo.getHost(), connectionInfo.getDatabase());
        final DatabaseConnectionSource dataSource = new DatabaseConnectionSource(connectionUrl);
        dataSource.setUserCredentials(new SingleUseUserCredentials(connectionInfo.getUser(), connectionInfo.getPassword()));
        return dataSource.get();
    }
}
