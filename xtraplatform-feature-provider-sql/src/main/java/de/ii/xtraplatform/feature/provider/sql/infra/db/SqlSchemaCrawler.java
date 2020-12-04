package de.ii.xtraplatform.feature.provider.sql.infra.db;

import com.google.common.collect.ImmutableList;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import schemacrawler.inclusionrule.InclusionRule;
import schemacrawler.inclusionrule.RegularExpressionExclusionRule;
import schemacrawler.inclusionrule.RegularExpressionInclusionRule;
import schemacrawler.schema.Catalog;
import schemacrawler.schemacrawler.LimitOptionsBuilder;
import schemacrawler.schemacrawler.LoadOptionsBuilder;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder;
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder;
import schemacrawler.utility.SchemaCrawlerUtility;

public class SqlSchemaCrawler implements Closeable {

  private static final List<String> TABLE_BLACKLIST = ImmutableList
      .of("spatial_ref_sys", "geography_columns", "geometry_columns", "raster_columns",
          "raster_overviews");

  private final Connection connection;

  public SqlSchemaCrawler(Connection connection) {
    this.connection = connection;
  }

  public Catalog getCatalog(List<String> schemas)
      throws SchemaCrawlerException {
    return getCatalog(schemas, ImmutableList.of());
  }

  public Catalog getCatalog(List<String> schemas, List<String> tables)
      throws SchemaCrawlerException {
    Catalog catalog = crawlSchema(schemas, tables);

    if (!tables.isEmpty() && catalog.getTables().stream()
        .anyMatch(table -> table.getTableType().isView())) {
      List<String> additionalTables = Stream.concat(tables.stream(), catalog.getTables()
          .stream()
          .filter(table -> table.getTableType().isView())
          .flatMap(table -> ViewInfo.getOriginalTables(table.getDefinition()).stream()))
          .collect(Collectors.toList());

      if (additionalTables.size() > tables.size()) {
        return crawlSchema(schemas, additionalTables);
      }
    }

    return catalog;
  }

  private Catalog crawlSchema(List<String> schemas, List<String> tables)
      throws SchemaCrawlerException {
    String includeSchemas = Stream.concat(
        Stream.of("public"),
        schemas.stream())
        .distinct()
        .collect(Collectors.joining("|", "(", ")"));
    String includeTables = tables.stream()
        .distinct()
        .collect(Collectors.joining("|.*\\.", "(.*\\.", ")"));
    String excludeTables = TABLE_BLACKLIST.stream()
        .distinct()
        .collect(Collectors.joining("|.*\\.", "(.*\\.", ")"));

    InclusionRule tablesRule = tables.isEmpty() ? new RegularExpressionExclusionRule(excludeTables)
        : new RegularExpressionInclusionRule(includeTables);

    LimitOptionsBuilder limitOptionsBuilder =
        LimitOptionsBuilder.builder()
            .includeSchemas(new RegularExpressionInclusionRule(includeSchemas))
            .includeTables(tablesRule);
    LoadOptionsBuilder loadOptionsBuilder =
        LoadOptionsBuilder.builder()
            .withSchemaInfoLevel(SchemaInfoLevelBuilder.maximum());
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
