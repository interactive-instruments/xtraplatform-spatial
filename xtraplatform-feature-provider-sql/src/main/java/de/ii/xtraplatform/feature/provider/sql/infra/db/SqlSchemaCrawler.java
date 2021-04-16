/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.infra.db;

import com.google.common.collect.ImmutableList;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import schemacrawler.inclusionrule.InclusionRule;
import schemacrawler.inclusionrule.RegularExpressionExclusionRule;
import schemacrawler.inclusionrule.RegularExpressionInclusionRule;
import schemacrawler.inclusionrule.RegularExpressionRule;
import schemacrawler.schema.Catalog;
import schemacrawler.schemacrawler.InfoLevel;
import schemacrawler.schemacrawler.LimitOptionsBuilder;
import schemacrawler.schemacrawler.LoadOptionsBuilder;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder;
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder;
import schemacrawler.utility.SchemaCrawlerUtility;

public class SqlSchemaCrawler implements Closeable {

  private final Connection connection;

  public SqlSchemaCrawler(Connection connection) {
    this.connection = connection;
  }

  public Catalog getCatalog(List<String> schemas, List<String> excludeTables)
      throws SchemaCrawlerException {
    return getCatalog(schemas, ImmutableList.of(), excludeTables);
  }

  public Catalog getCatalog(List<String> schemas, List<String> includeTables,
      List<String> excludeTables)
      throws SchemaCrawlerException {
    Catalog catalog = crawlSchema(schemas, includeTables, excludeTables);

    if (!includeTables.isEmpty() && catalog.getTables().stream()
        .anyMatch(table -> table.getTableType().isView())) {
      List<String> additionalTables = Stream.concat(includeTables.stream(), catalog.getTables()
          .stream()
          .filter(table -> table.getTableType().isView())
          .flatMap(table -> ViewInfo.getOriginalTables(table.getDefinition()).stream()))
          .collect(Collectors.toList());

      if (additionalTables.size() > includeTables.size()) {
        return crawlSchema(schemas, additionalTables, excludeTables);
      }
    }

    return catalog;
  }

  private Catalog crawlSchema(List<String> schemas, List<String> includeTables,
      List<String> excludeTables)
      throws SchemaCrawlerException {
    String includeSchemas = schemas.stream()
        .distinct()
        .collect(Collectors.joining("|", "(", ")"));

    Collector<CharSequence, ?, String> tableCollector = schemas.isEmpty()
        ? Collectors.joining("|", "(", ")")
        : Collectors.joining("|.*\\.", "(.*\\.", ")");
    String includeTablesPattern = includeTables.stream()
        .distinct()
        .collect(tableCollector);
    String excludeTablesPattern = excludeTables.stream()
        .distinct()
        .collect(tableCollector);

    InclusionRule tablesRule = includeTables.isEmpty() && !excludeTables.isEmpty()
        ? new RegularExpressionExclusionRule(excludeTablesPattern)
        : !includeTables.isEmpty() && excludeTables.isEmpty()
            ? new RegularExpressionInclusionRule(includeTablesPattern)
            : new RegularExpressionRule(includeTablesPattern, excludeTablesPattern);

    LimitOptionsBuilder limitOptionsBuilder =
        LimitOptionsBuilder.builder()
            //.tableTypes()
            .includeTables(tablesRule);
    if (!schemas.isEmpty()) {
      limitOptionsBuilder.includeSchemas(new RegularExpressionInclusionRule(includeSchemas));
    }
    LoadOptionsBuilder loadOptionsBuilder =
        LoadOptionsBuilder.builder()
            .withSchemaInfoLevel(SchemaInfoLevelBuilder.builder()
                .setRetrieveColumnDataTypes(true)
                .setRetrieveIndexes(true)
                .setRetrieveIndexInformation(true)
                .setRetrieveTableColumns(true)
                .setRetrieveTableConstraintInformation(true)
                .setRetrieveTableDefinitionsInformation(true)
                .setRetrieveTables(true)
                .setRetrieveViewInformation(true)
                .setRetrieveAdditionalColumnAttributes(false)
                .setRetrieveAdditionalColumnMetadata(false)
                .setRetrieveAdditionalDatabaseInfo(false)
                .setRetrieveAdditionalJdbcDriverInfo(false)
                .setRetrieveAdditionalTableAttributes(false)
                .setRetrieveDatabaseInfo(false)
                .setRetrieveDatabaseUsers(false)
                .setRetrieveForeignKeys(false)
                .setRetrieveRoutineInformation(false)
                .setRetrieveRoutineParameters(false)
                .setRetrieveRoutines(false)
                .setRetrieveSequenceInformation(false)
                .setRetrieveServerInfo(false)
                .setRetrieveSynonymInformation(false)
                .setRetrieveTableColumnPrivileges(false)
                .setRetrieveTableConstraintDefinitions(false)
                .setRetrieveTablePrivileges(false)
                .setRetrieveTriggerInformation(false)
                .setRetrieveUserDefinedColumnDataTypes(true)
                .setRetrieveViewViewTableUsage(false)
                .setRetrieveWeakAssociations(false)
                .toOptions());

    SchemaCrawlerOptions options =
        SchemaCrawlerOptionsBuilder.newSchemaCrawlerOptions()
            .withLimitOptions(limitOptionsBuilder.toOptions())
            .withLoadOptions(loadOptionsBuilder.toOptions());

    return SchemaCrawlerUtility.getCatalog(connection, options);
  }

  @Override
  public void close() throws IOException {
    try {
      connection.close();
    } catch (SQLException exception) {
      throw new IOException(exception);
    }
  }
}
