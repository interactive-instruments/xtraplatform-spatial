package de.ii.xtraplatform.feature.provider.sql.infra.db;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.features.domain.FeatureStoreAttributesContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreRelatedContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreRelation;
import de.ii.xtraplatform.features.domain.TypeInfoValidator;
import java.util.Base64;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.Column;
import schemacrawler.schema.Index;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SqlTypeInfoValidator implements TypeInfoValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlTypeInfoValidator.class);
    public static final String TABLE_DOES_NOT_EXIST = "%s: table '%s' does not exist";
    public static final String COLUMN_DOES_NOT_EXIST = "%s: column '%s' in table '%s' does not exist";
    public static final String COLUMN_NOT_UNIQUE = "%s: column '%s' in table '%s' should not be used as %s, neither a primary key nor a unique constraint can be found";
    public static final String COLUMN_CANNOT_BE_USED_AS = "%s: column '%s' in table '%s' cannot be used as %s";


    private final ConnectionInfoSql connectionInfo;
    private final ClassLoader classLoader;
    private Connection connection;
    private /*final*/ Catalog catalog;
    private /*final*/ Collection<Table> tables;

    public SqlTypeInfoValidator(ConnectionInfoSql connectionInfo) {
        this(connectionInfo, Thread.currentThread().getContextClassLoader());
    }

    public SqlTypeInfoValidator(ConnectionInfoSql connectionInfo, ClassLoader classLoader) {
        this.connectionInfo = connectionInfo;
        this.classLoader = classLoader;

        /*try {
            this.catalog = getCatalog();
        } catch (SchemaCrawlerException e) {
            throw new IllegalStateException("could not parse schema");
        }
        this.tables = catalog.getTables();*/
    }

    @Override
    public List<String> validate(String typeName, FeatureStoreAttributesContainer attributesContainer) {
        List<String> currentTables = new ArrayList<>();
        currentTables.add(attributesContainer.getName());
        if (attributesContainer instanceof FeatureStoreRelatedContainer) {
            List<FeatureStoreRelation> relations = ((FeatureStoreRelatedContainer) attributesContainer).getInstanceConnection();

            for (int i = 0; i < relations.size(); i++) {
                FeatureStoreRelation relation = relations.get(i);
                currentTables.add(relation.getSourceContainer());
                currentTables.add(relation.getTargetContainer());
                if (relation.getJunction().isPresent()) {
                    currentTables.add(relation.getJunction().get());
                }
            }
        }

        try {
            this.catalog = getCatalog(currentTables);
        } catch (SchemaCrawlerException e) {
            throw new IllegalStateException("could not parse schema");
        }
        this.tables = catalog.getTables();


        List<String> errors = new ArrayList<>();

        if (attributesContainer instanceof FeatureStoreRelatedContainer) {
            List<FeatureStoreRelation> relations = ((FeatureStoreRelatedContainer) attributesContainer).getInstanceConnection();

            for (int i = 0; i < relations.size(); i++) {
                FeatureStoreRelation relation = relations.get(i);
                String context = String.format("Invalid sourcePath '%s' in type '%s'", Joiner.on('/')
                                                                                               .join(relation.asPath()), typeName);

                if (i > 0 && !tableExists(relation.getSourceContainer())) {
                    errors.add(String.format(TABLE_DOES_NOT_EXIST, context, relation.getSourceContainer()));
                } else if (!columnExists(relation.getSourceField(), relation.getSourceContainer())) {
                    errors.add(String.format(COLUMN_DOES_NOT_EXIST, context, relation.getSourceField(), relation.getSourceContainer()));
                }

                if (!tableExists(relation.getTargetContainer())) {
                    errors.add(String.format(TABLE_DOES_NOT_EXIST, context, relation.getTargetContainer()));
                } else if (!columnExists(relation.getTargetField(), relation.getTargetContainer())) {
                    errors.add(String.format(COLUMN_DOES_NOT_EXIST, context, relation.getTargetField(), relation.getTargetContainer()));
                }

                if (relation.getJunction()
                            .isPresent() && !tableExists(relation.getJunction()
                                                                 .get())) {
                    errors.add(String.format(TABLE_DOES_NOT_EXIST, context, relation.getJunction()
                                                                                     .get()));
                } else {
                    if (relation.getJunctionSource()
                                .isPresent() && relation.getJunction()
                                                        .isPresent()) {
                        if (!columnExists(relation.getJunctionSource()
                                                  .get(), relation.getJunction()
                                                                  .get())) {
                            errors.add(String.format(COLUMN_DOES_NOT_EXIST,
                                    context, relation.getJunctionSource()
                                            .get(), relation.getJunction()
                                                            .get()));
                        }
                    }

                    if (relation.getJunctionTarget()
                                .isPresent() && relation.getJunction()
                                                        .isPresent()) {
                        if (!columnExists(relation.getJunctionTarget()
                                                  .get(), relation.getJunction()
                                                                  .get())) {
                            errors.add(String.format(COLUMN_DOES_NOT_EXIST,
                                    context, relation.getJunctionTarget()
                                            .get(), relation.getJunction()
                                                            .get()));
                        }
                    }
                }
            }
        } else if (!tableExists(attributesContainer.getName())){
            String context = String.format("Invalid sourcePath in type '%s'", typeName);
            errors.add(String.format(TABLE_DOES_NOT_EXIST, context, attributesContainer.getName()));
        }

        if (!errors.isEmpty()) {
            return errors;
        }

        String context = String.format("Invalid sort key for type '%s'", typeName);
        if (!columnExists(attributesContainer.getSortKey(), attributesContainer.getName())) {
            errors.add(String.format(COLUMN_DOES_NOT_EXIST, context, attributesContainer.getSortKey(), attributesContainer.getName()));
        }
        else if (!isColumnUnique(attributesContainer.getSortKey(), attributesContainer.getName())) {
            errors.add(String.format(COLUMN_NOT_UNIQUE,
                    context, attributesContainer.getSortKey(), attributesContainer.getName(), "sort key"));
        }


        attributesContainer.getAttributes().forEach(attribute -> {
            if (!attribute.isConstant()) {
                String context2 = String.format("Invalid sourcePath for property '%s' in type '%s'", attribute.getQueryable(), typeName);

                if (!columnExists(attribute.getName(), attributesContainer.getName())) {
                    errors.add( String.format(COLUMN_DOES_NOT_EXIST, context2, attribute.getName(), attributesContainer.getName()));
                } else {
                    if (attribute.isId() && !isColumnUnique(attribute.getName(), attributesContainer.getName())) {
                        String context3 = String.format("Invalid role ID for property '%s' in type '%s'", attribute.getQueryable(), typeName);
                        errors.add(String.format(COLUMN_NOT_UNIQUE,
                                context3, attribute.getName(), attributesContainer.getName(), "feature id"));
                    }

                    if (attribute.isSpatial() && !isColumnSpatial(attribute.getName(), attributesContainer.getName())) {
                        errors.add(String.format(COLUMN_CANNOT_BE_USED_AS, context2, attribute.getName(), attributesContainer.getName(), "geometry"));
                    }

                    if (attribute.isTemporal() && !isColumnTemporal(attribute.getName(), attributesContainer.getName())) {
                        errors.add(String.format(COLUMN_CANNOT_BE_USED_AS, context2, attribute.getName(), attributesContainer.getName(), "datetime"));
                    }
                }
            }
        });

        return errors;
    }

    private boolean tableExists(String name) {
        return tables.stream().anyMatch(t -> t.getName().equals(name));
    }

    private boolean columnExists(String name, String table) {
        return tables.stream()
                .filter(t -> t.getName().equals(table))
                .flatMap(t -> t.getColumns().stream())
                .anyMatch(c -> c.getName().equals(name));
    }

    private boolean isColumnUnique(String name, String table) {
        Optional<Column> column = tables.stream()
                .filter(t -> t.getName().equals(table))
                .flatMap(t -> t.getColumns().stream())
                .filter(c -> c.getName().equals(name))
                .findAny();
        if (column.isPresent()) {
            boolean isPrimaryKey = tables.stream()
                    .filter(t -> Objects.equals(t.getName(), table) && t.hasPrimaryKey())
                    .anyMatch(t -> t.getPrimaryKey().getColumns().size() == 1 && Objects.equals(t.getPrimaryKey()
                                                                                                 .getColumns()
                                                                                                 .get(0), column.get()));
            boolean isUniqueIndex = tables.stream()
                    .filter(t -> t.getName().equals(table))
                    .flatMap(t -> t.getIndexes().stream())
                    .filter(Index::isUnique)
                    .anyMatch(index -> index.getColumns().size() == 1 && Objects.equals(index.getColumns()
                                                                                             .get(0), column.get()));

            return isPrimaryKey || isUniqueIndex;
        }

        return false;
    }

    private boolean isColumnSpatial(String name, String table) {
        return tables.stream()
                .filter(t -> t.getName().equals(table))
                .flatMap(t -> t.getColumns().stream())
                .filter(c -> c.getName().equals(name))
                .anyMatch(c -> "geometry".equals(c.getColumnDataType().getName()));
    }

    private boolean isColumnTemporal(String name, String table) {
        return tables.stream()
                .filter(t -> t.getName().equals(table))
                .flatMap(t -> t.getColumns().stream())
                .filter(c -> c.getName().equals(name))
                .anyMatch(c -> c.getColumnDataType().getName().startsWith("timestamp") ||
                        "date".equals(c.getColumnDataType().getName()) ||
                        "string".equals(c.getColumnDataType().getName()));
    }


    private Catalog getCatalog(List<String> currentTables) throws SchemaCrawlerException {
        String schemasPattern = Stream.concat(
                Stream.of("public"),
                connectionInfo.getSchemas().stream())
            .distinct()
            .collect(Collectors.joining("|", "(", ")"));
        String tablesPattern = currentTables.stream()
            .distinct()
            .collect(Collectors.joining("|.*\\.", "(.*\\.", ")"));

        final SchemaCrawlerOptionsBuilder optionsBuilder = SchemaCrawlerOptionsBuilder.builder()
            .withSchemaInfoLevel(SchemaInfoLevelBuilder.detailed())
            //TODO: does the include pattern work, also for multiple schemas?
            .includeSchemas(new RegularExpressionInclusionRule(schemasPattern))
            .includeTables(new RegularExpressionInclusionRule(tablesPattern));

        final SchemaCrawlerOptions options = optionsBuilder.toOptions();

        // Get the schema definition
        Catalog catalog = SchemaCrawlerUtility.getCatalog(getConnection(), options);

        return catalog;
    }

    private Catalog getCatalog() throws SchemaCrawlerException {
        List<String> schemas = connectionInfo.getSchemas().isEmpty() ? ImmutableList.of("public") : connectionInfo.getSchemas();
        String schemaNames = Joiner.on('|').join(schemas);

        final SchemaCrawlerOptionsBuilder optionsBuilder = SchemaCrawlerOptionsBuilder.builder()
                                                                                      .withSchemaInfoLevel(SchemaInfoLevelBuilder.detailed())
                                                                                      //TODO: does the include pattern work, also for multiple schemas?
                                                                                      .includeSchemas(new RegularExpressionInclusionRule("(" + schemaNames + ")"));

        final SchemaCrawlerOptions options = optionsBuilder.toOptions();

        // Get the schema definition
        Catalog catalog = SchemaCrawlerUtility.getCatalog(getConnection(), options);

        return catalog;
    }

    private Connection getConnection() {
        try {
            if (Objects.isNull(connection) || connection.isClosed()) {
                //Thread.currentThread().setContextClassLoader(classLoader);
                //LOGGER.debug("CLASSL {}", classLoader);
                final String connectionUrl = String.format("jdbc:postgresql://%1$s/%2$s", connectionInfo.getHost(), connectionInfo.getDatabase());
                final DatabaseConnectionSource dataSource = new DatabaseConnectionSource(connectionUrl);
                dataSource.setUserCredentials(new SingleUseUserCredentials(connectionInfo.getUser(), getPassword(connectionInfo)));
                connection = dataSource.get();
            }
        } catch (SQLException e) {
            LOGGER.debug("SQL CONNECTION ERROR", e);
        }
        return connection;
    }

    //TODO: does SqlSchemaCrawler use this?
    //TODO: factor out common SchemaCrawler
    private static String getPassword(ConnectionInfoSql connectionInfo) {
        String password = connectionInfo.getPassword();
        try {
            password = new String(Base64.getDecoder()
                .decode(password), Charsets.UTF_8);
        } catch (IllegalArgumentException e) {
            //ignore if not valid base64
        }

        return password;
    }
}
