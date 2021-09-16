/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRelation;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JoinGenerator {

  public String getJoins(SchemaSql table, List<String> aliases, List<Optional<String>> relationFilters, Optional<SchemaSql> userFilterTable,
      Optional<String> userFilter, Optional<String> instanceFilter) {

    if (table.getRelation().isEmpty()) {
      return "";
    }
    ListIterator<String> aliasesIterator = aliases.listIterator();

    Optional<SqlRelation> userFilterRelation = userFilterTable
        .map(t -> t.getRelation().get(t.getRelation().size() - 1));
    String userFilterJoin = userFilter.isPresent() ? toJoins(userFilterRelation.get(), aliasesIterator,
        userFilter, instanceFilter).collect(
        Collectors.joining(" ")) : "";
    String userFilterTargetField = userFilterRelation.map(SqlRelation::getTargetField).orElse("");

    final int[] i = {0};
    String join = table.getRelation()
        .stream()
        .filter(t -> !t.getTargetField().equals(userFilterTargetField))
        .flatMap(relation -> toJoins(relation, aliasesIterator,
            relationFilters.get(i[0]++), instanceFilter))
        .collect(Collectors.joining(" "));
    return String.format("%1$s%3$s%2$s", userFilterJoin, join, userFilterJoin.isEmpty() || join.isEmpty() ? "" : " ");
  }

  private Stream<String> toJoins(SqlRelation relation, ListIterator<String> aliases,
      Optional<String> sqlFilter, Optional<String> sourceFilter) {
    List<String> joins = new ArrayList<>();

    if (relation.isM2N()) {
      String sourceAlias = aliases.next();
      String junctionAlias = aliases.next();
      String targetAlias = aliases.next();
      aliases.previous();

      joins.add(toJoin(relation.getJunction()
          .get(), junctionAlias, relation.getJunctionSource()
          .get(), relation.getSourceContainer(), sourceAlias, relation.getSourceField(), sqlFilter, sourceFilter));
      joins.add(toJoin(relation.getTargetContainer(), targetAlias, relation.getTargetField(),
          relation.getJunctionSource().get(), junctionAlias, relation.getJunctionTarget()
              .get(), sqlFilter, Optional.empty()));

    } else {
      String sourceAlias = aliases.next();
      String targetAlias = aliases.next();
      aliases.previous();

      joins.add(
          toJoin(relation.getTargetContainer(), targetAlias, relation.getTargetField(), relation.getSourceContainer(), sourceAlias,
              relation.getSourceField(), sqlFilter, sourceFilter));
    }

    return joins.stream();
  }

  private String toJoin(String targetContainer, String targetAlias, String targetField,
      String sourceContainer, String sourceAlias, String sourceField,
      Optional<String> sqlFilter, Optional<String> sourceFilter) {
    String additionalFilter = sqlFilter.map(s -> " AND (" + s + ")")
        .orElse("");
    String targetTable = targetContainer;

    if (additionalFilter.contains(FilterEncoderSql.ROW_NUMBER)) {
      String sourceFilterPart = sourceFilter.isPresent() ? String.format(" WHERE %s ORDER BY 1", sourceFilter.get()) : "";
      targetTable = String.format("(SELECT A.%1$s AS A%1$s, B.*, %6$s() OVER (PARTITION BY B.%2$s ORDER BY B.%2$s) AS %6$s FROM %3$s A JOIN %4$s B ON (A.%1$s=B.%2$s)%5$s)",
          sourceField, targetField, sourceContainer, targetContainer, sourceFilterPart, FilterEncoderSql.ROW_NUMBER);
    }

    return String.format("JOIN %1$s %2$s ON (%4$s.%5$s=%2$s.%3$s%6$s)", targetTable, targetAlias,
        targetField, sourceAlias, sourceField, additionalFilter);
  }

}
