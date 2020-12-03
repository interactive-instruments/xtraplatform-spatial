package de.ii.xtraplatform.feature.provider.sql.infra.db;

import com.google.common.base.Joiner;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlClient;
import de.ii.xtraplatform.features.domain.FeatureStoreAttributesContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreRelatedContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreRelation;
import de.ii.xtraplatform.features.domain.TypeInfoValidator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.Table;
import schemacrawler.schemacrawler.SchemaCrawlerException;

public class SqlTypeInfoValidator implements TypeInfoValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(SqlTypeInfoValidator.class);
  public static final String TABLE_DOES_NOT_EXIST = "%s: table '%s' does not exist";
  public static final String COLUMN_DOES_NOT_EXIST = "%s: column '%s' in table '%s' does not exist";
  public static final String COLUMN_NOT_UNIQUE =
      "%s: column '%s' in table '%s' should not be used as %s, neither a primary key nor a unique constraint can be found";
  public static final String COLUMN_CANNOT_BE_USED_AS =
      "%s: column '%s' in table '%s' cannot be used as %s";

  private final List<String> schemas;
  private SqlClient sqlClient;

  public SqlTypeInfoValidator(List<String> schemas, SqlClient sqlClient) {
    this.schemas = schemas;
    this.sqlClient = sqlClient;
  }

  @Override
  public List<String> validate(
      String typeName, FeatureStoreAttributesContainer attributesContainer) {
    try (SqlSchemaCrawler schemaCrawler = new SqlSchemaCrawler(sqlClient.getConnection())) {
      Catalog catalog = schemaCrawler.getCatalog(schemas, getUsedTables(attributesContainer));
      Collection<Table> tables = catalog.getTables();

      return validate(typeName, attributesContainer, new SchemaInfo(tables));
    } catch (SchemaCrawlerException | IOException e) {
      throw new IllegalStateException("could not parse schema");
    }
  }

  private List<String> validate(
      String typeName, FeatureStoreAttributesContainer attributesContainer, SchemaInfo test) {
    List<String> errors = new ArrayList<>();

    if (attributesContainer instanceof FeatureStoreRelatedContainer) {
      List<FeatureStoreRelation> relations =
          ((FeatureStoreRelatedContainer) attributesContainer).getInstanceConnection();

      for (int i = 0; i < relations.size(); i++) {
        FeatureStoreRelation relation = relations.get(i);
        String context =
            String.format(
                "Invalid sourcePath '%s' in type '%s'",
                Joiner.on('/').join(relation.asPath()), typeName);

        if (i > 0 && !test.tableExists(relation.getSourceContainer())) {
          errors.add(String.format(TABLE_DOES_NOT_EXIST, context, relation.getSourceContainer()));
        } else if (!test.columnExists(relation.getSourceField(), relation.getSourceContainer())) {
          errors.add(
              String.format(
                  COLUMN_DOES_NOT_EXIST,
                  context,
                  relation.getSourceField(),
                  relation.getSourceContainer()));
        }

        if (!test.tableExists(relation.getTargetContainer())) {
          errors.add(String.format(TABLE_DOES_NOT_EXIST, context, relation.getTargetContainer()));
        } else if (!test.columnExists(relation.getTargetField(), relation.getTargetContainer())) {
          errors.add(
              String.format(
                  COLUMN_DOES_NOT_EXIST,
                  context,
                  relation.getTargetField(),
                  relation.getTargetContainer()));
        }

        if (relation.getJunction().isPresent() && !test.tableExists(relation.getJunction().get())) {
          errors.add(String.format(TABLE_DOES_NOT_EXIST, context, relation.getJunction().get()));
        } else {
          if (relation.getJunctionSource().isPresent() && relation.getJunction().isPresent()) {
            if (!test.columnExists(
                relation.getJunctionSource().get(), relation.getJunction().get())) {
              errors.add(
                  String.format(
                      COLUMN_DOES_NOT_EXIST,
                      context,
                      relation.getJunctionSource().get(),
                      relation.getJunction().get()));
            }
          }

          if (relation.getJunctionTarget().isPresent() && relation.getJunction().isPresent()) {
            if (!test.columnExists(
                relation.getJunctionTarget().get(), relation.getJunction().get())) {
              errors.add(
                  String.format(
                      COLUMN_DOES_NOT_EXIST,
                      context,
                      relation.getJunctionTarget().get(),
                      relation.getJunction().get()));
            }
          }
        }
      }
    } else if (!test.tableExists(attributesContainer.getName())) {
      String context = String.format("Invalid sourcePath in type '%s'", typeName);
      errors.add(String.format(TABLE_DOES_NOT_EXIST, context, attributesContainer.getName()));
    }

    if (!errors.isEmpty()) {
      return errors;
    }

    String context = String.format("Invalid sort key for type '%s'", typeName);
    if (!test.columnExists(attributesContainer.getSortKey(), attributesContainer.getName())) {
      errors.add(
          String.format(
              COLUMN_DOES_NOT_EXIST,
              context,
              attributesContainer.getSortKey(),
              attributesContainer.getName()));
    } else if (!test.isColumnUnique(
        attributesContainer.getSortKey(), attributesContainer.getName())) {
      errors.add(
          String.format(
              COLUMN_NOT_UNIQUE,
              context,
              attributesContainer.getSortKey(),
              attributesContainer.getName(),
              "sort key"));
    }

    attributesContainer
        .getAttributes()
        .forEach(
            attribute -> {
              if (!attribute.isConstant()) {
                String context2 =
                    String.format(
                        "Invalid sourcePath for property '%s' in type '%s'",
                        attribute.getQueryable(), typeName);

                if (!test.columnExists(attribute.getName(), attributesContainer.getName())) {
                  errors.add(
                      String.format(
                          COLUMN_DOES_NOT_EXIST,
                          context2,
                          attribute.getName(),
                          attributesContainer.getName()));
                } else {
                  if (attribute.isId()
                      && !test.isColumnUnique(attribute.getName(), attributesContainer.getName())) {
                    String context3 =
                        String.format(
                            "Invalid role ID for property '%s' in type '%s'",
                            attribute.getQueryable(), typeName);
                    errors.add(
                        String.format(
                            COLUMN_NOT_UNIQUE,
                            context3,
                            attribute.getName(),
                            attributesContainer.getName(),
                            "feature id"));
                  }

                  if (attribute.isSpatial()
                      && !test.isColumnSpatial(
                      attributesContainer.getName(), attribute.getName())) {
                    errors.add(
                        String.format(
                            COLUMN_CANNOT_BE_USED_AS,
                            context2,
                            attribute.getName(),
                            attributesContainer.getName(),
                            "geometry"));
                  }

                  if (attribute.isTemporal()
                      && !test.isColumnTemporal(
                      attributesContainer.getName(), attribute.getName())) {
                    errors.add(
                        String.format(
                            COLUMN_CANNOT_BE_USED_AS,
                            context2,
                            attribute.getName(),
                            attributesContainer.getName(),
                            "datetime"));
                  }
                }
              }
            });

    return errors;
  }

  private List<String> getUsedTables(FeatureStoreAttributesContainer attributesContainer) {
    List<String> usedTables = new ArrayList<>();

    usedTables.add(attributesContainer.getName());
    if (attributesContainer instanceof FeatureStoreRelatedContainer) {
      List<FeatureStoreRelation> relations =
          ((FeatureStoreRelatedContainer) attributesContainer).getInstanceConnection();

      for (int i = 0; i < relations.size(); i++) {
        FeatureStoreRelation relation = relations.get(i);
        usedTables.add(relation.getSourceContainer());
        usedTables.add(relation.getTargetContainer());
        if (relation.getJunction().isPresent()) {
          usedTables.add(relation.getJunction().get());
        }
      }
    }

    return usedTables;
  }

}
