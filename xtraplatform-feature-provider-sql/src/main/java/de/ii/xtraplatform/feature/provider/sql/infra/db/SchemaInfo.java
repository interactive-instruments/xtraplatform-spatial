/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.infra.db;

import de.ii.xtraplatform.feature.provider.sql.app.Tuple;
import de.ii.xtraplatform.feature.provider.sql.domain.ValueTypeMapping;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import schemacrawler.schema.Column;
import schemacrawler.schema.Index;
import schemacrawler.schema.Table;
import schemacrawler.schema.View;

class SchemaInfo {

  private static final Logger LOGGER = LoggerFactory.getLogger(SchemaInfo.class);

  private static final String VIEW_COLUMN_NOT_ANALYZABLE =
      "Cannot analyze constraints and indices for column '{}' in view '{}', most likely it is composed of multiple actual columns.";

  private final Collection<Table> tables;

  SchemaInfo(Collection<Table> tables) {
    this.tables = tables;
  }

  public boolean tableExists(String name) {
    return tables.stream().anyMatch(t -> t.getName().equals(name));
  }

  public boolean columnExists(String name, String table) {
    return tables.stream()
        .filter(t -> t.getName().equals(table))
        .flatMap(t -> t.getColumns().stream())
        .anyMatch(c -> c.getName().equals(name));
  }

  //TODO: unique may be either column constraint, table constraint or index plus primary key
  //TODO: NOT NULL constraints
  public boolean isColumnUnique(String columnName, String tableName) {
    Optional<Column> optionalColumn = getColumn(tableName, columnName, true);

    if (optionalColumn.isPresent()) {
      Column column = optionalColumn.get();
      Table table = column.getParent();

      boolean isPrimaryKey =
          table.hasPrimaryKey()
              && table.getPrimaryKey().getColumns().size() == 1
              && Objects.equals(table.getPrimaryKey().getColumns().get(0), column);

      boolean isUniqueIndex =
          table.getIndexes().stream()
              .filter(Index::isUnique)
              .anyMatch(
                  index ->
                      index.getColumns().size() == 1
                          && Objects.equals(index.getColumns().get(0), column));

      return isPrimaryKey || isUniqueIndex;
    }

    return false;
  }

  public boolean isColumnSpatial(String table, String name) {
    return getColumn(table, name)
        .filter(c -> ValueTypeMapping.matches(c.getColumnDataType().getJavaSqlType(),
            c.getColumnDataType().getName(), Type.GEOMETRY))
        .isPresent();
  }

  public boolean isColumnTemporal(String table, String name) {
    return getColumn(table, name)
        .filter(c -> ValueTypeMapping.matches(c.getColumnDataType().getJavaSqlType(),
            c.getColumnDataType().getDatabaseSpecificTypeName(), Type.DATETIME))
        .isPresent();
  }

  public Optional<Column> getColumn(String tableName, String columnName) {
    return getColumn(tableName, columnName, false);
  }

  public Optional<Column> getColumn(String tableName, String columnName, boolean resolveViews) {
    Optional<Column> optionalColumn =
        tables.stream()
            .filter(t -> t.getName().equals(tableName))
            .flatMap(t -> t.getColumns().stream())
            .filter(c -> c.getName().equals(columnName))
            .findFirst();

    if (resolveViews && optionalColumn.isPresent()) {
      Table table = optionalColumn.get().getParent();

      if (table instanceof View) {
        Optional<Tuple<String, String>> originalColumn =
            ViewInfo.getOriginalTableAndColumn(table.getDefinition(), columnName);

        if (originalColumn.isPresent()) {
          return getColumn(originalColumn.get().first(), originalColumn.get().second());
        }

        LOGGER.warn(VIEW_COLUMN_NOT_ANALYZABLE, columnName, tableName);
      }
    }

    return optionalColumn;
  }
}
