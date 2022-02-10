/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import de.ii.xtraplatform.cql.domain.And;
import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.cql.domain.ImmutableCqlPredicate;
import de.ii.xtraplatform.feature.provider.sql.app.SqlQueryTemplates.MetaQueryTemplate;
import de.ii.xtraplatform.feature.provider.sql.app.SqlQueryTemplates.ValueQueryTemplate;
import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialect;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaVisitorWithFinalizer;
import de.ii.xtraplatform.features.domain.SortKey;
import de.ii.xtraplatform.features.domain.SortKey.Direction;
import de.ii.xtraplatform.features.domain.Tuple;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SqlQueryTemplatesDeriver implements
    SchemaVisitorWithFinalizer<SchemaSql, List<ValueQueryTemplate>, SqlQueryTemplates> {

  private static final String SKEY = "SKEY";
  private static final String CSKEY = "CSKEY";
  private static final String TAB = "  ";

  private final SqlDialect sqlDialect;
  private final FilterEncoderSql filterEncoder;
  private final AliasGenerator aliasGenerator;
  private final JoinGenerator joinGenerator;
  private final boolean computeNumberMatched;

  public SqlQueryTemplatesDeriver(FilterEncoderSql filterEncoder, SqlDialect sqlDialect,
      boolean computeNumberMatched) {
    this.sqlDialect = sqlDialect;
    this.filterEncoder = filterEncoder;
    this.aliasGenerator = new AliasGenerator();
    this.joinGenerator = new JoinGenerator();
    this.computeNumberMatched = computeNumberMatched;
  }

  @Override
  public List<ValueQueryTemplate> visit(SchemaSql schema, List<SchemaSql> parents,
      List<List<ValueQueryTemplate>> visitedProperties) {

    Stream<ValueQueryTemplate> current = schema.isObject()
        && schema.getProperties().stream().anyMatch(SchemaBase::isValue)
        ? Stream.of(createValueQueryTemplate(schema, parents))
        : Stream.empty();

    return Stream.concat(
        current,
        visitedProperties.stream().flatMap(Collection::stream)
    ).collect(Collectors.toList());
  }

  @Override
  public SqlQueryTemplates finalize(SchemaSql schema,
      List<ValueQueryTemplate> valueQueryTemplates) {
    return new ImmutableSqlQueryTemplates.Builder()
        .metaQueryTemplate(createMetaQueryTemplate(schema))
        .valueQueryTemplates(valueQueryTemplates)
        .addAllQuerySchemas(schema.getAllObjects())
        .build();
  }

  MetaQueryTemplate createMetaQueryTemplate(SchemaSql schema) {
    return (limit, offset, additionalSortKeys, cqlFilter, virtualTables) -> {
      String limitSql = limit > 0 ? String.format(" LIMIT %d", limit) : "";
      String offsetSql = offset > 0 ? String.format(" OFFSET %d", offset) : "";
      Optional<String> filter = getFilter(schema, cqlFilter);
      String where = filter.isPresent() ? String.format(" WHERE %s", filter.get()) : "";

      String tableName = virtualTables.containsKey(schema.getName()) ? virtualTables.get(schema.getName()) : schema.getName();
      String table = String.format("%s A", tableName);
      String columns = "";
      for (int i = 0; i < additionalSortKeys.size(); i++) {
        SortKey sortKey = additionalSortKeys.get(i);

        columns += "A." + sortKey.getField() + " AS CSKEY_" + i + ", ";
      }
      columns += String.format("A.%s AS " + SKEY, schema.getSortKey().get());
      String orderBy = getOrderBy(additionalSortKeys);
      String minMaxColumns = getMinMaxColumns(additionalSortKeys);

      String numberReturned = String.format(
          "SELECT %7$s, count(*) AS numberReturned FROM (SELECT %2$s FROM %1$s%6$s ORDER BY %3$s%4$s%5$s) AS IDS",
          table, columns, orderBy, limitSql, offsetSql, where, minMaxColumns);

      if (computeNumberMatched) {
        String numberMatched = String.format(
            "SELECT count(*) AS numberMatched FROM (SELECT A.%2$s AS %4$s FROM %1$s A%3$s ORDER BY 1) AS IDS",
            tableName, schema.getSortKey().get(), where, SKEY);
        return String
            .format("WITH\n%3$s%3$sNR AS (%s),\n%3$s%3$sNM AS (%s) \n%3$sSELECT * FROM NR, NM",
                numberReturned, numberMatched, TAB);
      }

      return String
          .format("WITH\n%2$s%2$sNR AS (%s)\n%2$sSELECT *, -1::bigint AS numberMatched FROM NR",
              numberReturned, TAB);
    };
  }

  ValueQueryTemplate createValueQueryTemplate(SchemaSql schema, List<SchemaSql> parents) {
    return (limit, offset, additionalSortKeys, filter, minMaxKeys, virtualTables) -> {
      boolean isIdFilter = filter.flatMap(CqlFilter::getInOperator).isPresent();
      List<String> aliases = aliasGenerator.getAliases(schema);

      SchemaSql rootSchema = parents.isEmpty() ? schema : parents.get(0);
      Optional<String> sqlFilter = getFilter(rootSchema, filter);
      Optional<String> whereClause = isIdFilter
          ? sqlFilter
          : toWhereClause(aliases.get(0), rootSchema.getSortKey().get(), additionalSortKeys,
              minMaxKeys, sqlFilter);
      Optional<String> pagingClause = additionalSortKeys.isEmpty() || (limit == 0 && offset == 0)
          ? Optional.empty()
          : Optional.of((limit > 0 ? String.format(" LIMIT %d", limit) : "") + (offset > 0 ? String
              .format(" OFFSET %d", offset) : ""));

      return getTableQuery(schema, whereClause, pagingClause, additionalSortKeys, parents, virtualTables);
    };
  }

  private String getTableQuery(SchemaSql schema,
      Optional<String> whereClause,
      Optional<String> pagingClause, List<SortKey> additionalSortKeys,
      List<SchemaSql> parents, Map<String, String> virtualTables) {
    List<String> aliases = aliasGenerator.getAliases(parents, schema);
    String attributeContainerAlias = aliases.get(aliases.size() - 1);

    String mainTableName = parents.isEmpty()
        ? schema.getName()
        : parents.get(0).getName();
    if (virtualTables.containsKey(mainTableName)) {
      mainTableName = virtualTables.get(mainTableName);
    }
    String mainTableSortKey = parents.isEmpty()
        ? schema.getSortKey().get()
        : parents.get(0).getSortKey().get();
    String mainTable = String
        .format("%s %s", mainTableName, aliases.get(0));
    List<String> sortFields = getSortFields(schema, parents, aliases, additionalSortKeys);

    String columns = Stream.concat(sortFields.stream(), schema.getProperties()
        .stream()
        .filter(SchemaBase::isValue)
        .map(column -> {
          String name =
              column.isConstant() ? "'" + column.getConstantValue().get() + "'" + " AS " + column
                  .getName()
                  : getQualifiedColumn(attributeContainerAlias, column.getName());
          if (column.isSpatial()) {
            return sqlDialect.applyToWkt(name, column.isForcePolygonCCW());
          }
          if (column.isTemporal()) {
            return sqlDialect.applyToDatetime(name);
          }

          return name;
        }))
        .collect(Collectors.joining(", "));

    Optional<String> instanceFilter = parents.isEmpty()
        ? Optional.empty()
        : parents.get(0).getFilter().map(filter -> filterEncoder.encode(filter, parents.get(0)));

    List<Optional<String>> relationFilters = Stream.concat(
        parents.stream().flatMap(parent -> parent.getRelation().stream()),
        schema.getRelation().stream())
        .map(sqlRelation -> sqlRelation.getTargetFilter().flatMap(
            filter -> filterEncoder.encodeRelationFilter(Optional.of(schema), Optional.empty())))
        .collect(Collectors.toList());

    String join = joinGenerator
        .getJoins(schema, parents, aliases, relationFilters, Optional.empty(), Optional.empty(),
            instanceFilter);

    String where = whereClause.map(w -> " WHERE " + w).orElse("");
    String paging = pagingClause.filter(p -> join.isEmpty()).orElse("");

    if (!join.isEmpty() && pagingClause.isPresent()) {
      String where2 = " WHERE ";
      List<String> aliasesNested = aliasGenerator.getAliases(schema, where.isEmpty() ? 1 : 2);
      String orderBy = IntStream.range(0, sortFields.size())
          .boxed()
          .map(index -> {
            if (index < additionalSortKeys.size()
                && additionalSortKeys.get(index).getDirection() == Direction.DESCENDING) {
              return sortFields.get(index) + " DESC";
            }
            return sortFields.get(index);
          })
          .filter(sortField -> sortField.startsWith("A."))
          .map(sortField -> sortField.replace("A.", aliasesNested.get(0) + "."))
          .map(sortField -> sortField.replaceAll(" AS \\w+", ""))
          .collect(Collectors.joining(","));
      where2 += String.format("(A.%3$s IN (SELECT %2$s.%3$s FROM %1$s %2$s%4$s ORDER BY %5$s%6$s))",
          mainTableName, aliasesNested.get(0), mainTableSortKey,
          where.replace("(A.", "(" + aliasesNested.get(0) + "."), orderBy, pagingClause.get());

      where = where2;
    }

    String orderBy = IntStream.rangeClosed(1, sortFields.size())
        .boxed()
        .map(index -> {
          if (index <= additionalSortKeys.size()
              && additionalSortKeys.get(index - 1).getDirection() == Direction.DESCENDING) {
            return index + " DESC";
          }
          return String.valueOf(index);
        })
        .collect(Collectors.joining(","));

    return String.format("SELECT %s FROM %s%s%s%s ORDER BY %s%s", columns, mainTable,
        join.isEmpty() ? "" : " ", join, where, orderBy, paging);
  }


  private Optional<String> toWhereClause(String alias, String keyField,
      List<SortKey> additionalSortKeys, Optional<Tuple<Object, Object>> minMaxKeys,
      Optional<String> additionalFilter) {
    StringBuilder filter = new StringBuilder();

    if (minMaxKeys.isPresent() && additionalSortKeys.isEmpty()) {
      filter.append("(");
      addMinMaxFilter(filter, alias, keyField, minMaxKeys.get().first(), minMaxKeys.get().second());
      filter.append(")");
    }

    if (additionalFilter.isPresent()) {
      if (minMaxKeys.isPresent() && additionalSortKeys.isEmpty()) {
        filter.append(" AND ");
      }
      filter.append("(")
          .append(additionalFilter.get())
          .append(")");
    }

    if (filter.length() == 0) {
      return Optional.empty();
    }

    return Optional.of(filter.toString());
  }

  private StringBuilder addMinMaxFilter(StringBuilder whereClause, String alias, String keyField,
      Object minKey,
      Object maxKey) {
    return whereClause
        .append(alias)
        .append(".")
        .append(keyField)
        .append(" >= ")
        .append(formatLiteral(minKey))
        .append(" AND ")
        .append(alias)
        .append(".")
        .append(keyField)
        .append(" <= ")
        .append(formatLiteral(maxKey));
  }

  private String formatLiteral(Object literal) {
    if (Objects.isNull(literal)) {
      return "NULL";
    }
    if (literal instanceof Number) {
      return String.valueOf(literal);
    }

    String literalString = literal instanceof Timestamp
        ? String.valueOf(((Timestamp) literal).toInstant())
        : String.valueOf(literal);

    return String.format("'%s'", sqlDialect.escapeString(literalString));
  }

  private List<String> getSortFields(SchemaSql schema, List<SchemaSql> parents,
      List<String> aliases, List<SortKey> additionalSortKeys) {

    final int[] i = {0};
    Stream<String> customSortKeys = additionalSortKeys.stream().map(
        sortKey -> String.format("%s.%s AS CSKEY_%s", aliases.get(0), sortKey.getField(), i[0]++));

    if (!schema.getRelation().isEmpty() || !parents.isEmpty()) {
      ListIterator<String> aliasesIterator = aliases.listIterator();

      List<String> parentSortKeys = parents.stream()
          .flatMap(parent -> parent.getSortKeys(aliasesIterator, true, 0).stream())
          .collect(Collectors.toList());

      return Stream.of(customSortKeys, parentSortKeys.stream(),
          schema.getSortKeys(aliasesIterator, false, parentSortKeys.size()).stream())
          .flatMap(s -> s)
          .collect(Collectors.toList());
    } else {
      return Stream.concat(customSortKeys,
          Stream.of(String.format("%s.%s AS SKEY", aliases.get(0), schema.getSortKey().get())))
          .collect(Collectors.toList());
    }
  }

  private String getQualifiedColumn(String table, String column) {
    return column.contains("(")
        ? column.replaceAll("((?:\\w+\\()+)(\\w+)((?:\\))+)", "$1" + table + ".$2$3 AS $2")
        : String.format("%s.%s", table, column);
  }

  private Optional<String> getFilter(SchemaSql schema,
      Optional<CqlFilter> userFilter) {
    if (schema.getFilter().isEmpty() && userFilter.isEmpty()) {
      return Optional.empty();
    }
    if (schema.getFilter().isPresent() && schema.getRelation().isEmpty() && userFilter.isEmpty()) {
      return Optional
          .of(filterEncoder.encode(schema.getFilter().get(), schema));
    }
    if (schema.getFilter().isEmpty() && schema.getRelation().isEmpty() && userFilter.isPresent()) {
      return Optional.of(filterEncoder.encode(userFilter.get(), schema));
    }
    if (schema.getFilter().isPresent() && schema.getRelation().isEmpty() && userFilter
        .isPresent()) {
      CqlFilter mergedFilter = CqlFilter.of(
          And.of(
              ImmutableCqlPredicate.copyOf(schema.getFilter().get()),
              ImmutableCqlPredicate.copyOf(userFilter.get())
          )
      );

      return Optional.of(filterEncoder.encode(mergedFilter, schema));
    }
    // TODO what to do, if schema.getRelation().isPresent() ?

    return Optional.empty();
  }

  private String getOrderBy(List<SortKey> sortKeys) {
    String orderBy = "";

    for (int i = 0; i < sortKeys.size(); i++) {
      SortKey sortKey = sortKeys.get(i);

      orderBy +=
          CSKEY + "_" + i + (sortKey.getDirection() == Direction.DESCENDING ? " DESC" : "") + ", ";
    }

    orderBy += SKEY;

    return orderBy;
  }

  private String getMinMaxColumns(List<SortKey> sortKeys) {
    String minMaxKeys = "";

    if (!sortKeys.isEmpty()) {
      minMaxKeys += "NULL AS minKey, ";
      minMaxKeys += "NULL AS maxKey";
    } else {
      minMaxKeys += "MIN(" + SKEY + ") AS minKey, ";
      minMaxKeys += "MAX(" + SKEY + ") AS maxKey";
    }

    return minMaxKeys;
  }

}
