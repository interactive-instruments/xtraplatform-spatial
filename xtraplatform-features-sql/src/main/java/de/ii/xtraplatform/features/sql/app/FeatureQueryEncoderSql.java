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
import de.ii.xtraplatform.features.sql.domain.ImmutableSqlQueryBatch;
import de.ii.xtraplatform.features.sql.domain.ImmutableSqlQueryOptions;
import de.ii.xtraplatform.features.sql.domain.ImmutableSqlQuerySet.Builder;
import de.ii.xtraplatform.features.sql.domain.SchemaSql;
import de.ii.xtraplatform.features.sql.domain.SqlQueryBatch;
import de.ii.xtraplatform.features.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.features.sql.domain.SqlQuerySet;
import de.ii.xtraplatform.features.sql.domain.SqlRowMeta;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.function.TriFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FeatureQueryEncoderSql implements FeatureQueryEncoder<SqlQueryBatch, SqlQueryOptions> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureQueryEncoderSql.class);

  private final Map<String, List<SqlQueryTemplates>> allQueryTemplates;
  private final Map<String, List<SqlQueryTemplates>> allQueryTemplatesMutations;
  private final int chunkSize;

  FeatureQueryEncoderSql(
      Map<String, List<SqlQueryTemplates>> allQueryTemplates,
      Map<String, List<SqlQueryTemplates>> allQueryTemplatesMutations,
      int chunkSize) {
    this.allQueryTemplates = allQueryTemplates;
    this.allQueryTemplatesMutations = allQueryTemplatesMutations;
    this.chunkSize = chunkSize;
  }

  // TODO: options
  public SqlQueryBatch encode(
      FeatureQuery featureQuery, Map<String, String> additionalQueryParameters) {
    List<SqlQueryTemplates> queryTemplates =
        featureQuery.getSchemaScope() == Scope.QUERIES
            ? allQueryTemplates.get(featureQuery.getType())
            : allQueryTemplatesMutations.get(featureQuery.getType());
    int chunks =
        (featureQuery.getLimit() / chunkSize) + (featureQuery.getLimit() % chunkSize > 0 ? 1 : 0);

    List<SqlQuerySet> querySets =
        IntStream.range(0, queryTemplates.size())
            .mapToObj(
                tableIndex -> {
                  return IntStream.range(0, chunks)
                      .mapToObj(
                          chunk ->
                              createQuerySet(
                                  queryTemplates.get(tableIndex),
                                  chunkSize,
                                  featureQuery.getOffset() + (chunk * chunkSize),
                                  featureQuery,
                                  additionalQueryParameters));
                })
            .flatMap(s -> s)
            .collect(Collectors.toList());

    return new ImmutableSqlQueryBatch.Builder()
        .limit(featureQuery.getLimit())
        .chunkSize(chunkSize)
        .querySets(querySets)
        .build();
  }

  private SqlQuerySet createQuerySet(
      SqlQueryTemplates queryTemplates,
      int limit,
      int offset,
      FeatureQuery featureQuery,
      Map<String, String> additionalQueryParameters) {
    SchemaSql mainTable = queryTemplates.getQuerySchemas().get(0);
    List<SortKey> sortKeys = transformSortKeys(featureQuery.getSortKeys(), mainTable);

    BiFunction<Long, Long, Optional<String>> metaQuery =
        (maxLimit, skipped) ->
            featureQuery.returnsSingleFeature()
                ? Optional.empty()
                : Optional.of(
                    queryTemplates
                        .getMetaQueryTemplate()
                        .generateMetaQuery(
                            Math.min(limit, maxLimit),
                            Math.max(0L, offset - skipped),
                            sortKeys,
                            featureQuery.getFilter(),
                            additionalQueryParameters,
                            featureQuery.getOffset() > 0));

    TriFunction<SqlRowMeta, Long, Long, Stream<String>> valueQueries =
        (metaResult, maxLimit, skipped) ->
            queryTemplates.getValueQueryTemplates().stream()
                .map(
                    valueQueryTemplate ->
                        valueQueryTemplate.generateValueQuery(
                            Math.min(limit, maxLimit),
                            Math.max(0L, offset - skipped),
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
        .tableSchemas(queryTemplates.getQuerySchemas())
        .options(getOptions(featureQuery))
        .build();
  }

  @Override
  public SqlQueryOptions getOptions(FeatureQuery featureQuery) {
    // TODO: either pass as parameter, or check for null here
    List<SchemaSql> typeInfo =
        allQueryTemplates.get(featureQuery.getType()).get(0).getQuerySchemas();

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
