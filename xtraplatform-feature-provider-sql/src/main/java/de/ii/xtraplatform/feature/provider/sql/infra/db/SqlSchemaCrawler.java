package de.ii.xtraplatform.feature.provider.sql.infra.db;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.feature.provider.api.FeatureProviderSchemaConsumer;
import de.ii.xtraplatform.feature.provider.api.FeatureType;
import de.ii.xtraplatform.feature.provider.api.ImmutableFeatureProperty;
import de.ii.xtraplatform.feature.provider.api.ImmutableFeatureType;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.Column;
import schemacrawler.schema.Schema;
import schemacrawler.schema.Table;
import schemacrawler.schema.View;
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

        //TODO:
        // - create a FeatureType for every Table using ImmutableFeatureType.builder()
        // - create a FeatureProperty for every Column using ImmutableFeatureProperty.builder() and add it to FeatureType
        // - map every possible type of column.getColumnDataType() to FeatureProperty.Type

        for (final Schema schema : catalog.getSchemas()) {
            System.out.println(schema);
            for (final Table table : catalog.getTables(schema)) {
                System.out.print("o--> " + table);
                if (table instanceof View) {
                    System.out.println(" (VIEW)");
                } else {
                    System.out.println();
                }

                for (final Column column : table.getColumns()) {
                    System.out.println(
                            "     o--> " + column + " (" + column.getColumnDataType() + ")");
                }
            }
        }

        return ImmutableList.of();
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
