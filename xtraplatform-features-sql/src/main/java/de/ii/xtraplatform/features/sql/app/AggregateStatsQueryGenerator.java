/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.app;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.features.domain.Tuple;
import de.ii.xtraplatform.features.sql.domain.SchemaSql;
import de.ii.xtraplatform.features.sql.domain.SqlDialect;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AggregateStatsQueryGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(AggregateStatsQueryGenerator.class);

  private final SqlDialect sqlDialect;
  private final FilterEncoderSql filterEncoder;

  public AggregateStatsQueryGenerator(SqlDialect sqlDialect, FilterEncoderSql filterEncoder) {
    this.sqlDialect = sqlDialect;
    this.filterEncoder = filterEncoder;
  }

  public String getCountQuery(SchemaSql sourceSchema) {

    List<String> aliases = AliasGenerator.getAliases(sourceSchema);

    String mainTable = String.format("%s %s", sourceSchema.getName(), aliases.get(0));

    Optional<String> filter = getFilter(sourceSchema);
    String where = filter.isPresent() ? String.format(" WHERE %s", filter.get()) : "";

    return String.format("SELECT COUNT(*) FROM %s%s", mainTable, where);
  }

  public String getSpatialExtentQuery(SchemaSql mainSchema, SchemaSql spatialSchema, boolean is3d) {

    List<String> aliases = AliasGenerator.getAliases(spatialSchema);
    String spatialAlias = aliases.get(aliases.size() - 1);

    String mainTable = String.format("%s %s", mainSchema.getName(), aliases.get(0));

    String column =
        spatialSchema
            .getPrimaryGeometry()
            .map(
                attribute ->
                    sqlDialect.applyToExtent(
                        getQualifiedColumn(spatialAlias, attribute.getName()), is3d))
            .get();

    String join =
        JoinGenerator.getJoins(spatialSchema, ImmutableList.of(mainSchema), aliases, filterEncoder);

    Optional<String> filter = getFilter(mainSchema);
    String where = filter.isPresent() ? String.format(" WHERE %s", filter.get()) : "";

    return String.format(
        "SELECT %s FROM %s%s%s%s", column, mainTable, join.isEmpty() ? "" : " ", join, where);
  }

  public String getTemporalExtentQuery(SchemaSql mainSchema, SchemaSql temporalSchema) {
    List<String> aliases = AliasGenerator.getAliases(temporalSchema);
    String temporalAlias = aliases.get(aliases.size() - 1);

    String mainTable = String.format("%s %s", mainSchema.getName(), aliases.get(0));
    String column =
        temporalSchema
            .getPrimaryInstant()
            .map(
                attribute ->
                    sqlDialect.applyToDatetime(
                        getQualifiedColumn(temporalAlias, attribute.getName())))
            .get();

    String join =
        JoinGenerator.getJoins(
            temporalSchema, ImmutableList.of(mainSchema), aliases, filterEncoder);

    Optional<String> filter = getFilter(mainSchema);
    String where = filter.isPresent() ? String.format(" WHERE %s", filter.get()) : "";

    return String.format(
        "SELECT MIN(%s), MAX(%s) FROM %s%s%s%s",
        column, column, mainTable, join.isEmpty() ? "" : " ", join, where);
  }

  public String getTemporalExtentQuery(
      SchemaSql mainSchema, SchemaSql startSchema, SchemaSql endSchema) {
    if (startSchema.equals(endSchema)) {
      List<String> aliases = AliasGenerator.getAliases(startSchema);
      String temporalAlias = aliases.get(aliases.size() - 1);

      String mainTable = String.format("%s %s", mainSchema.getName(), aliases.get(0));
      Optional<Tuple<SchemaSql, SchemaSql>> primaryInterval = startSchema.getPrimaryInterval();
      String startColumn =
          primaryInterval
              .map(
                  attributes ->
                      sqlDialect.applyToDatetime(
                          getQualifiedColumn(temporalAlias, attributes.first().getName())))
              .get();
      String endColumn =
          primaryInterval
              .map(
                  attributes ->
                      sqlDialect.applyToDatetime(
                          getQualifiedColumn(temporalAlias, attributes.second().getName())))
              .get();
      String join =
          JoinGenerator.getJoins(startSchema, ImmutableList.of(mainSchema), aliases, filterEncoder);

      Optional<String> filter = getFilter(mainSchema);
      String where = filter.isPresent() ? String.format(" WHERE %s", filter.get()) : "";

      return String.format(
          "SELECT MIN(%s), MAX(%s) FROM %s%s%s%s",
          startColumn, endColumn, mainTable, join.isEmpty() ? "" : " ", join, where);

    } else {
      List<String> startAliases = AliasGenerator.getAliases(startSchema);
      String startAlias = startAliases.get(startAliases.size() - 1);
      List<String> endAliases = AliasGenerator.getAliases(endSchema);
      String endAlias = endAliases.get(endAliases.size() - 1);

      String mainTable = String.format("%s %s", mainSchema.getName(), startAliases.get(0));

      Optional<Tuple<SchemaSql, SchemaSql>> primaryInterval = startSchema.getPrimaryInterval();
      String startColumn =
          primaryInterval
              .map(
                  attributes ->
                      sqlDialect.applyToDatetime(
                          getQualifiedColumn(startAlias, attributes.first().getName())))
              .get();
      String endColumn =
          primaryInterval
              .map(
                  attributes ->
                      sqlDialect.applyToDatetime(
                          getQualifiedColumn(endAlias, attributes.second().getName())))
              .get();

      String startJoin =
          JoinGenerator.getJoins(
              startSchema, ImmutableList.of(mainSchema), startAliases, filterEncoder);
      String startTableWithJoins =
          String.format("%s%s%s", mainSchema, startJoin.isEmpty() ? "" : " ", startJoin);
      String endJoin =
          JoinGenerator.getJoins(
              endSchema, ImmutableList.of(mainSchema), endAliases, filterEncoder);
      String endTableWithJoins =
          String.format("%s%s%s", mainTable, endJoin.isEmpty() ? "" : " ", endJoin);

      Optional<String> filter = getFilter(mainSchema);
      String where = filter.isPresent() ? String.format(" WHERE %s", filter.get()) : "";

      return String.format(
          "SELECT * FROM (SELECT MIN(%s) FROM %s%s) AS A, (SELECT MAX(%s) from %s%s) AS B;",
          startColumn, startTableWithJoins, where, endColumn, endTableWithJoins, where);
    }
  }

  private Optional<String> getFilter(SchemaSql schemaSql) {
    return schemaSql.getFilter().map(cql -> filterEncoder.encode(cql, schemaSql));
  }

  private String getQualifiedColumn(String table, String column) {
    return column.contains("(")
        ? column.replaceAll("((?:\\w+\\()+)(\\w+)((?:\\))+)", "$1" + table + ".$2$3 AS $2")
        : String.format("%s.%s", table, column);
  }
}
