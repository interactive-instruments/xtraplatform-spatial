/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import static de.ii.xtraplatform.cql.domain.In.ID_PLACEHOLDER;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableSqlQueries;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableSqlQueryOptions;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueries;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlQueryOptions;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRowMeta;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureQueryTransformer;
import de.ii.xtraplatform.features.domain.FeatureStoreAttribute;
import de.ii.xtraplatform.features.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.features.domain.ImmutableSortKey;
import de.ii.xtraplatform.features.domain.SortKey;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FeatureQueryTransformerSql implements FeatureQueryTransformer<SqlQueries, SqlQueryOptions> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureQueryTransformerSql.class);

  private final Map<String, FeatureStoreTypeInfo> typeInfos;
  private final FeatureStoreQueryGeneratorSql queryGenerator;
  private final boolean computeNumberMatched;

  FeatureQueryTransformerSql(Map<String, FeatureStoreTypeInfo> typeInfos,
      FeatureStoreQueryGeneratorSql queryGenerator, boolean computeNumberMatched) {
    this.typeInfos = typeInfos;
    this.queryGenerator = queryGenerator;
    this.computeNumberMatched = computeNumberMatched;
  }

  //TODO: rest of code in this class, so mainly the query multiplexing, goes to SqlConnector
  //TODO: should merge QueryTransformer with QueryGenerator
  @Override
  public SqlQueries transformQuery(FeatureQuery featureQuery,
      Map<String, String> additionalQueryParameters) {
    //TODO: either pass as parameter, or check for null here
    FeatureStoreTypeInfo typeInfo = typeInfos.get(featureQuery.getType());

    //TODO: implement for multiple main tables
    FeatureStoreInstanceContainer mainTable = typeInfo.getInstanceContainers()
        .get(0);

    List<SortKey> sortKeys = transformSortKeys(featureQuery.getSortKeys(), mainTable);

    Optional<String> metaQuery = featureQuery.hasIdFilter()
        ? Optional.empty()
        : Optional.of(queryGenerator
            .getMetaQuery(mainTable, featureQuery.getLimit(), featureQuery.getOffset(),
                featureQuery.getFilter(), sortKeys, computeNumberMatched));

    Function<SqlRowMeta, Stream<String>> valueQueries = metaResult -> queryGenerator
        .getInstanceQueries(mainTable, featureQuery.getFilter(), sortKeys,
            metaResult.getMinKey(), metaResult.getMaxKey(), metaResult.getCustomMinKeys(),
            metaResult.getCustomMaxKeys());

    return ImmutableSqlQueries.builder()
        .metaQuery(metaQuery)
        .valueQueries(valueQueries)
        .instanceContainers(typeInfo.getInstanceContainers())
        .build();
  }

  @Override
  public SqlQueryOptions getOptions(FeatureQuery featureQuery) {
    //TODO: either pass as parameter, or check for null here
    FeatureStoreTypeInfo typeInfo = typeInfos.get(featureQuery.getType());

    //TODO: implement for multiple main tables
    FeatureStoreInstanceContainer mainTable = typeInfo.getInstanceContainers()
        .get(0);

    List<SortKey> sortKeys = transformSortKeys(featureQuery.getSortKeys(), mainTable);

    return ImmutableSqlQueryOptions.builder()
        .customSortKeys(sortKeys).build();
  }

  private List<SortKey> transformSortKeys(List<SortKey> sortKeys,
      FeatureStoreInstanceContainer instanceContainer) {
    return sortKeys.stream().map(sortKey -> {
      //TODO: fast enough? maybe pass all typeInfos to constructor and create map?
      Predicate<FeatureStoreAttribute> propertyMatches = attribute -> Objects
          .equals(sortKey.getField(), attribute.getQueryable()) || (
          Objects.equals(sortKey.getField(), ID_PLACEHOLDER) && attribute.isId());

      Optional<String> column = instanceContainer.getAttributes()
          .stream()
          .filter(propertyMatches)
          .filter(attribute -> !attribute.isSpatial() && !attribute.isConstant())
          .findFirst()
          .map(FeatureStoreAttribute::getName);

      if (!column.isPresent()) {
        throw new IllegalArgumentException(
            String.format("Sort key is invalid, property '%s' is either unknown or inapplicable.",
                sortKey.getField(), instanceContainer.getName()));
      }

      return ImmutableSortKey.builder().from(sortKey).field(column.get()).build();
    }).collect(ImmutableList.toImmutableList());
  }
}
