package de.ii.xtraplatform.feature.provider.sql.infra.db;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.feature.provider.sql.domain.ConnectionInfoSql;
import de.ii.xtraplatform.features.domain.FeatureStoreAttributesContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreRelatedContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreRelation;
import de.ii.xtraplatform.features.domain.TypeInfoValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.Column;
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

    private final ConnectionInfoSql connectionInfo;
    private final ClassLoader classLoader;
    private Connection connection;
    private final Catalog catalog;
    private final Collection<Table> tables;

    public SqlTypeInfoValidator(ConnectionInfoSql connectionInfo) {
        this(connectionInfo, Thread.currentThread().getContextClassLoader());
    }

    public SqlTypeInfoValidator(ConnectionInfoSql connectionInfo, ClassLoader classLoader) {
        this.connectionInfo = connectionInfo;
        this.classLoader = classLoader;

        try {
            this.catalog = getCatalog();
        } catch (SchemaCrawlerException e) {
            throw new IllegalStateException("could not parse schema");
        }
        this.tables = catalog.getTables();
    }

    @Override
    public List<String> validate(FeatureStoreAttributesContainer attributesContainer) {
        List<String> errors = new ArrayList<>();

        if (!tableExists(attributesContainer.getName())){
            errors.add(String.format("table '%s' does not exist", attributesContainer.getName()));
        }

        if (!columnExists(attributesContainer.getSortKey(), attributesContainer.getName())) {
            errors.add(String.format("column '%s' in table '%s' does not exist", attributesContainer.getSortKey(), attributesContainer.getName()));
        }

        if (!isColumnUnique(attributesContainer.getSortKey(), attributesContainer.getName())) {
            errors.add(String.format("column %s in table %s is not a primary key or a unique index for the column does not exist",
                    attributesContainer.getSortKey(), attributesContainer.getName()));
        }


        attributesContainer.getAttributes().forEach(attribute -> {
            if (!attribute.isConstant()) {
                if (!columnExists(attribute.getName(), attributesContainer.getName())) {
                    errors.add(String.format("column '%s' in table '%s' does not exist", attribute.getName(), attributesContainer.getName()));
                }

                if (attribute.isId() && !isColumnUnique(attribute.getName(), attributesContainer.getName())) {
                    errors.add(String.format("column %s in table %s is not a primary key or a unique index for the column does not exist",
                            attributesContainer.getSortKey(), attributesContainer.getName()));
                }

                if (attribute.isSpatial() && !isColumnSpatial(attribute.getName(), attributesContainer.getName())) {
                    errors.add(String.format("column %s in table %s is not of type geometry", attribute.getName(), attributesContainer.getName()));
                }
            }
        });

        if (attributesContainer instanceof FeatureStoreRelatedContainer) {
            List<FeatureStoreRelation> relations = ((FeatureStoreRelatedContainer) attributesContainer).getInstanceConnection();

            relations.forEach(relation -> {
                //TODO: tableExists for relation.getSourceContainer(), relation.getTargetContainer() and relation.getJunction() (if present)
                if (!tableExists(relation.getSourceContainer())) {
                    errors.add(String.format("table '%s' does not exist", relation.getSourceContainer()));
                }

                if (!tableExists(relation.getTargetContainer())) {
                    errors.add(String.format("table '%s' does not exist", relation.getTargetContainer()));
                }

                //TODO: columnExists for relation.getSourceField(), relation.getSourceSortKey() and relation.getTargetField()
                if (relation.getJunction().isPresent() && !tableExists(relation.getJunction().get())) {
                    errors.add(String.format("table '%s' does not exist", relation.getJunction().get()));
                }

                if (!columnExists(relation.getSourceField(), relation.getSourceSortKey())) {
                    errors.add(String.format("column '%s' in table '%s' does not exist", relation.getSourceField(), relation.getSourceSortKey()));
                }

                if (!columnExists(relation.getTargetField(), relation.getSourceSortKey())) {
                    errors.add(String.format("column '%s' in table '%s' does not exist", relation.getTargetField(), relation.getSourceSortKey()));
                }

                //TODO: columnExists for relation.getJunctionSource() and relation.getJunctionTarget() if present
                if (relation.getJunctionSource().isPresent() && relation.getJunctionTarget().isPresent()) {
                    if (!columnExists(relation.getJunctionSource().get(), relation.getJunctionTarget().get())) {
                        errors.add(String.format("column '%s' in table '%s' does not exist",
                                relation.getJunctionSource().get(), relation.getJunctionTarget().get()));
                    }
                }

                //TODO: isColumnUnique for relation.getSourceSortKey()
                if (!isColumnUnique(relation.getSourceField(), relation.getSourceSortKey())) {
                    errors.add(String.format("column %s in table %s is not a primary key or a unique index for the column does not exist",
                            relation.getSourceField(), relation.getSourceSortKey()));
                }

                if (!isColumnUnique(relation.getTargetField(), relation.getSourceSortKey())) {
                    errors.add(String.format("column %s in table %s is not a primary key or a unique index for the column does not exist",
                            relation.getTargetField(), relation.getSourceSortKey()));
                }

            });
        }

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
                .findFirst();
        return column.isPresent() && (column.get().isPartOfPrimaryKey() || column.get().isPartOfUniqueIndex());
    }

    private boolean isColumnSpatial(String name, String table) {
        return tables.stream()
                .filter(t -> t.getName().equals(table))
                .flatMap(t -> t.getColumns().stream())
                .filter(c -> c.getName().equals(name))
                .anyMatch(c -> "geometry".equals(c.getColumnDataType().getName()));
    }


    private Catalog getCatalog() throws SchemaCrawlerException {
        List<String> schemas = connectionInfo.getSchemas().isEmpty() ? ImmutableList.of("public") : connectionInfo.getSchemas();
        String schemaNames = Joiner.on('|').join(schemas);

        final SchemaCrawlerOptionsBuilder optionsBuilder = SchemaCrawlerOptionsBuilder.builder()
                                                                                      .withSchemaInfoLevel(SchemaInfoLevelBuilder.maximum())
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
