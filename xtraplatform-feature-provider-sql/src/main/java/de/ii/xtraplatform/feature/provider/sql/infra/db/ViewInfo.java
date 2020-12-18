/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.infra.db;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.feature.provider.sql.app.ImmutableTuple;
import de.ii.xtraplatform.feature.provider.sql.app.Tuple;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.ParseException;
import net.sf.jsqlparser.parser.StringProvider;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItemVisitorAdapter;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitorAdapter;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.util.TablesNamesFinder;

public class ViewInfo {

  public static List<String> getOriginalTables(String viewDefinition) {
    try {
      PlainSelect select = parse(viewDefinition);

      TablesNamesFinder tablesNamesFinder = new TablesNamesFinder() {
        @Override
        protected String extractTableName(Table table) {
          return table.getName();
        }
      };

      return tablesNamesFinder.getTableList(statementVisitor -> select.accept(tablesNamesFinder));

    } catch (JSQLParserException | ParseException e) {
      // ignore
      boolean br = true;
    }

    return ImmutableList.of();
  }

  public static Optional<Tuple<String, String>> getOriginalTableAndColumn(
      String viewDefinition, String columnName) {
    try {
      PlainSelect select = parse(viewDefinition);

      return getOriginalTableAndColumn(select, columnName)
          .map(tableAndColumn -> resolveTableAlias(select, tableAndColumn));

    } catch (JSQLParserException | ParseException e) {
      // ignore
      boolean br = true;
    }

    return Optional.empty();
  }

  private static Optional<ImmutableTuple<String, String>> getOriginalTableAndColumn(
      PlainSelect select, String columnName) {
    ImmutableTuple.Builder<String, String> builder =
        ImmutableTuple.<String, String>builder().second(columnName);

    for (SelectItem selectItem : select.getSelectItems()) {
      selectItem.accept(
          new SelectItemVisitorAdapter() {
            @Override
            public void visit(SelectExpressionItem item) {
              if (item.getExpression() instanceof Column) {
                Column column = (Column) item.getExpression();

                if (Objects.nonNull(item.getAlias())
                    && Objects.equals(item.getAlias().getName(), columnName)) {
                  builder.first(column.getTable().getName()).second(column.getColumnName());
                } else if (Objects.equals(column.getColumnName(), columnName)) {
                  builder.first(column.getTable().getName());
                }
              }
            }
          });
    }
    try {
      return Optional.of(builder.build());
    } catch (Throwable e) {
      // column not found
    }

    return Optional.empty();
  }

  private static Tuple<String, String> resolveTableAlias(
      PlainSelect select, ImmutableTuple<String, String> tableAndColumn) {
    ImmutableTuple.Builder<String, String> builder =
        ImmutableTuple.<String, String>builder().from(tableAndColumn);

    select
        .getFromItem()
        .accept(
            new FromItemVisitorAdapter() {
              @Override
              public void visit(Table tableName) {
                if (Objects.nonNull(tableName.getAlias())
                    && Objects.equals(tableName.getAlias().getName(), tableAndColumn.first())) {
                  builder.first(tableName.getName());
                }
              }

              @Override
              public void visit(SubJoin subjoin) {
                subjoin.getLeft().accept(this);
                subjoin.getJoinList().forEach(join -> join.getRightItem().accept(this));
              }
            });

    return builder.build();
  }

  private static PlainSelect parse(String select) throws JSQLParserException, ParseException {
    Select statement =
        (Select)
            CCJSqlParserUtil.parse(select, ccjSqlParser -> ccjSqlParser.setErrorRecovery(true));

    PlainSelect selectBody = (PlainSelect) statement.getSelectBody();

    if (Objects.isNull(selectBody.getFromItem())) {
      CCJSqlParser from =
          new CCJSqlParser(new StringProvider(select.substring(select.lastIndexOf("FROM") + 5)));

      selectBody.setFromItem(from.FromItem());
    }

    return selectBody;
  }
}
