/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.app;

import static de.ii.xtraplatform.cql.domain.In.ID_PLACEHOLDER;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureQueryEncoder;
import de.ii.xtraplatform.features.domain.FeatureSchema.Scope;
import de.ii.xtraplatform.features.domain.ImmutableSortKey;
import de.ii.xtraplatform.features.domain.SortKey;
import de.ii.xtraplatform.features.domain.Tuple;
import de.ii.xtraplatform.features.sql.domain.ImmutableSqlQueries.Builder;
import de.ii.xtraplatform.features.sql.domain.ImmutableSqlQueryOptions;
import de.ii.xtraplatform.features.sql.domain.SchemaSql;
import de.ii.xtraplatform.features.sql.domain.SqlQueries;
import de.ii.xtraplatform.features.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.features.sql.domain.SqlRowMeta;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FeatureQueryEncoderSql implements FeatureQueryEncoder<SqlQueries, SqlQueryOptions> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureQueryEncoderSql.class);

  private final Map<String, List<SqlQueryTemplates>> allQueryTemplates;
  private final Map<String, List<SqlQueryTemplates>> allQueryTemplatesMutations;
  private final Map<String, List<SchemaSql>> tableSchemas;

  FeatureQueryEncoderSql(
      Map<String, List<SqlQueryTemplates>> allQueryTemplates,
      Map<String, List<SqlQueryTemplates>> allQueryTemplatesMutations,
      Map<String, List<SchemaSql>> tableSchemas) {
    this.allQueryTemplates = allQueryTemplates;
    this.allQueryTemplatesMutations = allQueryTemplatesMutations;
    this.tableSchemas = tableSchemas;
  }

  // TODO: rest of code in this class, so mainly the query multiplexing, goes to SqlConnector
  // TODO: should merge QueryTransformer with QueryGenerator
  @Override
  public SqlQueries encode(
      FeatureQuery featureQuery, Map<String, String> additionalQueryParameters) {
    // TODO: either pass as parameter, or check for null here
    List<SchemaSql> typeInfo = tableSchemas.get(featureQuery.getType());
    List<SqlQueryTemplates> queryTemplates =
        featureQuery.getSchemaScope() == Scope.QUERIES
            ? allQueryTemplates.get(featureQuery.getType())
            : allQueryTemplatesMutations.get(featureQuery.getType());

    // TODO: implement for multiple main tables
    SchemaSql mainTable = typeInfo.get(0);
    SqlQueryTemplates queries = queryTemplates.get(0);

    List<SortKey> sortKeys = transformSortKeys(featureQuery.getSortKeys(), mainTable);

    Optional<String> metaQuery =
        featureQuery.returnsSingleFeature()
            ? Optional.empty()
            : Optional.of(
                queries
                    .getMetaQueryTemplate()
                    .generateMetaQuery(
                        featureQuery.getLimit(),
                        featureQuery.getOffset(),
                        sortKeys,
                        featureQuery.getFilter(),
                        additionalQueryParameters));

    Function<SqlRowMeta, Stream<String>> valueQueries =
        metaResult ->
            queries.getValueQueryTemplates().stream()
                .map(
                    valueQueryTemplate ->
                        valueQueryTemplate.generateValueQuery(
                            featureQuery.getLimit(),
                            featureQuery.getOffset(),
                            sortKeys,
                            featureQuery.getFilter(),
                            ((Objects.nonNull(metaResult.getMinKey())
                                        && Objects.nonNull(metaResult.getMaxKey()))
                                    || metaResult.getNumberReturned() == 0)
                                ? Optional.of(
                                    Tuple.of(metaResult.getMinKey(), metaResult.getMaxKey()))
                                : Optional.empty(),
                            additionalQueryParameters));

    return new Builder()
        .metaQuery(metaQuery)
        .valueQueries(valueQueries)
        .tableSchemas(queries.getQuerySchemas())
        .build();
  }

  @Override
  public SqlQueryOptions getOptions(FeatureQuery featureQuery) {
    // TODO: either pass as parameter, or check for null here
    List<SchemaSql> typeInfo = tableSchemas.get(featureQuery.getType());

    // TODO: implement for multiple main tables
    SchemaSql mainTable = typeInfo.get(0);

    List<SortKey> sortKeys = transformSortKeys(featureQuery.getSortKeys(), mainTable);

    return new ImmutableSqlQueryOptions.Builder().customSortKeys(sortKeys).build();
  }

  private List<SortKey> transformSortKeys(List<SortKey> sortKeys, SchemaSql mainTable) {
    return sortKeys.stream()
        .map(
            sortKey -> {
              // TODO: fast enough? maybe pass all typeInfos to constructor and create map?
              Predicate<SchemaSql> propertyMatches =
                  attribute ->
                      (!attribute.getEffectiveSourcePaths().isEmpty()
                              && Objects.equals(
                                  sortKey.getField(),
                                  String.join(".", attribute.getEffectiveSourcePaths().get(0))))
                          || (Objects.equals(sortKey.getField(), ID_PLACEHOLDER)
                              && attribute.isId());

              Optional<String> column =
                  mainTable.getProperties().stream()
                      .filter(propertyMatches)
                      .filter(attribute -> !attribute.isSpatial() && !attribute.isConstant())
                      .findFirst()
                      .map(SchemaSql::getName);

              if (!column.isPresent()) {
                throw new IllegalArgumentException(
                    String.format(
                        "Sort key is invalid, property '%s' is either unknown or inapplicable.",
                        sortKey.getField()));
              }

              return ImmutableSortKey.builder().from(sortKey).field(column.get()).build();
            })
        .collect(ImmutableList.toImmutableList());
  }
}
