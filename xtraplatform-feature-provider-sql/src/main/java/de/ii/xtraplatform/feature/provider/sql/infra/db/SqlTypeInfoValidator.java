/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.infra.db;

import com.google.common.base.Joiner;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlClient;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2.VALIDATION;
import de.ii.xtraplatform.features.domain.FeatureStoreAttributesContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreRelatedContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreRelation;
import de.ii.xtraplatform.features.domain.ImmutableValidationResult;
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
  public ValidationResult validate(
      String typeName, FeatureStoreAttributesContainer attributesContainer, VALIDATION mode) {
    try (SqlSchemaCrawler schemaCrawler = new SqlSchemaCrawler(sqlClient.getConnection())) {
      Catalog catalog = schemaCrawler.getCatalog(schemas, getUsedTables(attributesContainer));
      Collection<Table> tables = catalog.getTables();

      return validate(typeName, attributesContainer, mode, new SchemaInfo(tables));
    } catch (SchemaCrawlerException | IOException e) {
      throw new IllegalStateException("could not parse schema");
    }
  }

  private ValidationResult validate(
      String typeName, FeatureStoreAttributesContainer attributesContainer, VALIDATION mode, SchemaInfo schemaInfo) {
    ImmutableValidationResult.Builder result = ImmutableValidationResult.builder().mode(mode);

    if (attributesContainer instanceof FeatureStoreRelatedContainer) {
      List<FeatureStoreRelation> relations =
          ((FeatureStoreRelatedContainer) attributesContainer).getInstanceConnection();

      for (int i = 0; i < relations.size(); i++) {
        FeatureStoreRelation relation = relations.get(i);
        String context =
            String.format(
                "Invalid sourcePath '%s' in type '%s'",
                Joiner.on('/').join(relation.asPath()), typeName);

        if (i > 0 && !schemaInfo.tableExists(relation.getSourceContainer())) {
          result.addErrors(String.format(TABLE_DOES_NOT_EXIST, context, relation.getSourceContainer()));
        } else if (!schemaInfo.columnExists(relation.getSourceField(), relation.getSourceContainer())) {
          result.addErrors(
              String.format(
                  COLUMN_DOES_NOT_EXIST,
                  context,
                  relation.getSourceField(),
                  relation.getSourceContainer()));
        }

        if (!schemaInfo.tableExists(relation.getTargetContainer())) {
          result.addErrors(String.format(TABLE_DOES_NOT_EXIST, context, relation.getTargetContainer()));
        } else if (!schemaInfo.columnExists(relation.getTargetField(), relation.getTargetContainer())) {
          result.addErrors(
              String.format(
                  COLUMN_DOES_NOT_EXIST,
                  context,
                  relation.getTargetField(),
                  relation.getTargetContainer()));
        }

        if (relation.getJunction().isPresent() && !schemaInfo.tableExists(relation.getJunction().get())) {
          result.addErrors(String.format(TABLE_DOES_NOT_EXIST, context, relation.getJunction().get()));
        } else {
          if (relation.getJunctionSource().isPresent() && relation.getJunction().isPresent()) {
            if (!schemaInfo.columnExists(
                relation.getJunctionSource().get(), relation.getJunction().get())) {
              result.addErrors(
                  String.format(
                      COLUMN_DOES_NOT_EXIST,
                      context,
                      relation.getJunctionSource().get(),
                      relation.getJunction().get()));
            }
          }

          if (relation.getJunctionTarget().isPresent() && relation.getJunction().isPresent()) {
            if (!schemaInfo.columnExists(
                relation.getJunctionTarget().get(), relation.getJunction().get())) {
              result.addErrors(
                  String.format(
                      COLUMN_DOES_NOT_EXIST,
                      context,
                      relation.getJunctionTarget().get(),
                      relation.getJunction().get()));
            }
          }
        }
      }
    } else if (!schemaInfo.tableExists(attributesContainer.getName())) {
      String context = String.format("Invalid sourcePath in type '%s'", typeName);
      result.addErrors(String.format(TABLE_DOES_NOT_EXIST, context, attributesContainer.getName()));
    }

    ValidationResult intermediateResult = result.build();
    if (!intermediateResult.isSuccess()) {
      return intermediateResult;
    }

    String context = String.format("Invalid sort key for type '%s'", typeName);
    if (!schemaInfo.columnExists(attributesContainer.getSortKey(), attributesContainer.getName())) {
      result.addErrors(
          String.format(
              COLUMN_DOES_NOT_EXIST,
              context,
              attributesContainer.getSortKey(),
              attributesContainer.getName()));
    } else if (!schemaInfo.isColumnUnique(
        attributesContainer.getSortKey(), attributesContainer.getName())) {
      result.addStrictErrors(
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

                if (!schemaInfo.columnExists(attribute.getName(), attributesContainer.getName())) {
                  result.addErrors(
                      String.format(
                          COLUMN_DOES_NOT_EXIST,
                          context2,
                          attribute.getName(),
                          attributesContainer.getName()));
                } else {
                  if (attribute.isId()
                      && !schemaInfo.isColumnUnique(attribute.getName(), attributesContainer.getName())) {
                    String context3 =
                        String.format(
                            "Invalid role ID for property '%s' in type '%s'",
                            attribute.getQueryable(), typeName);
                    result.addStrictErrors(
                        String.format(
                            COLUMN_NOT_UNIQUE,
                            context3,
                            attribute.getName(),
                            attributesContainer.getName(),
                            "feature id"));
                  }

                  if (attribute.isSpatial()
                      && !schemaInfo.isColumnSpatial(
                      attributesContainer.getName(), attribute.getName())) {
                    result.addErrors(
                        String.format(
                            COLUMN_CANNOT_BE_USED_AS,
                            context2,
                            attribute.getName(),
                            attributesContainer.getName(),
                            "geometry"));
                  }

                  //TODO: strictError on string
                  if (attribute.isTemporal()
                      && !schemaInfo.isColumnTemporal(
                      attributesContainer.getName(), attribute.getName())) {
                    result.addErrors(
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

    return result.build();
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
