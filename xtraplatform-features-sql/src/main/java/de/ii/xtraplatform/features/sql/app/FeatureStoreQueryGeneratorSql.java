/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.app;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.cql.domain.And;
import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.cql.domain.ImmutableCqlPredicate;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.sql.domain.FilterEncoderSqlNewNew;
import de.ii.xtraplatform.features.sql.domain.SqlDialect;
import de.ii.xtraplatform.features.domain.FeatureStoreAttributesContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreQueryGenerator;
import de.ii.xtraplatform.features.domain.FeatureStoreRelatedContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreRelation;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureStoreQueryGeneratorSql implements FeatureStoreQueryGenerator<String> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureStoreQueryGeneratorSql.class);

  private final FilterEncoderSqlNewNew filterEncoder;
  private final SqlDialect sqlDialect;

  public FeatureStoreQueryGeneratorSql(SqlDialect sqlDialect, EpsgCrs nativeCrs,
      CrsTransformerFactory crsTransformerFactory) {
    this.sqlDialect = sqlDialect;
    this.filterEncoder = new FilterEncoderSqlNewNewImpl(this::getAliases,
        (attributeContainer, aliases) -> (userFilterAttributeContainer, userFilter) -> (instanceFilter)
            -> getJoins(attributeContainer, userFilterAttributeContainer, aliases,
              userFilter, instanceFilter), nativeCrs, sqlDialect, crsTransformerFactory);
  }

  @Override
  public String getExtentQuery(FeatureStoreInstanceContainer instanceContainer, FeatureStoreAttributesContainer attributesContainer) {

    List<String> aliases = getAliases(attributesContainer);
    String attributeContainerAlias = aliases.get(aliases.size() - 1);

    String mainTable = String
        .format("%s %s", attributesContainer.getInstanceContainerName(), aliases.get(0));

    String column = attributesContainer.getSpatialAttribute()
        .map(attribute -> sqlDialect
            .applyToExtent(getQualifiedColumn(attributeContainerAlias, attribute.getName())))
        .get();

    String join = getJoins(attributesContainer, aliases, Optional.empty());

    Optional<String> filter = getFilter(instanceContainer);
    String where = filter.isPresent() ? String.format(" WHERE %s", filter.get()) : "";

    return String
        .format("SELECT %s FROM %s%s%s%s", column, mainTable, join.isEmpty() ? "" : " ", join, where);
  }

  public String getTemporalExtentQuery(FeatureStoreInstanceContainer instanceContainer, FeatureStoreAttributesContainer attributesContainer,
      String property) {
    List<String> aliases = getAliases(attributesContainer);
    String attributeContainerAlias = aliases.get(aliases.size() - 1);

    String table = String
        .format("%s %s", attributesContainer.getInstanceContainerName(), aliases.get(0));
    String column = attributesContainer.getTemporalAttribute(property)
        .map(attribute -> sqlDialect
            .applyToDatetime(getQualifiedColumn(attributeContainerAlias, attribute.getName())))
        .get();
    String join = getJoins(attributesContainer, aliases, Optional.empty());

    Optional<String> filter = getFilter(instanceContainer);
    String where = filter.isPresent() ? String.format(" WHERE %s", filter.get()) : "";

    return String.format("SELECT MIN(%s), MAX(%s) FROM %s%s%s%s", column, column, table,
        join.isEmpty() ? "" : " ", join, where);
  }

  public String getTemporalExtentQuery(FeatureStoreInstanceContainer instanceContainer, FeatureStoreAttributesContainer startAttributesContainer,
      FeatureStoreAttributesContainer endAttributesContainer,
      String startProperty, String endProperty) {
    if (startAttributesContainer.equals(endAttributesContainer)) {
      List<String> aliases = getAliases(startAttributesContainer);
      String attributeContainerAlias = aliases.get(aliases.size() - 1);

      String table = String
          .format("%s %s", startAttributesContainer.getInstanceContainerName(), aliases.get(0));
      String startColumn = startAttributesContainer.getTemporalAttribute(startProperty)
          .map(attribute -> sqlDialect
              .applyToDatetime(getQualifiedColumn(attributeContainerAlias, attribute.getName())))
          .get();
      String endColumn = endAttributesContainer.getTemporalAttribute(endProperty)
          .map(attribute -> sqlDialect
              .applyToDatetime(getQualifiedColumn(attributeContainerAlias, attribute.getName())))
          .get();
      String join = getJoins(startAttributesContainer, aliases, Optional.empty());

      Optional<String> filter = getFilter(instanceContainer);
      String where = filter.isPresent() ? String.format(" WHERE %s", filter.get()) : "";

      return String.format("SELECT MIN(%s), MAX(%s) FROM %s%s%s%s", startColumn, endColumn, table,
          join.isEmpty() ? "" : " ", join, where);

    } else {
      List<String> startAliases = getAliases(startAttributesContainer);
      String startAttributeContainerAlias = startAliases.get(startAliases.size() - 1);
      String startTable = String
          .format("%s %s", startAttributesContainer.getInstanceContainerName(),
              startAliases.get(0));

      List<String> endAliases = getAliases(endAttributesContainer);
      String endAttributeContainerAlias = endAliases.get(endAliases.size() - 1);
      String endTable = String
          .format("%s %s", endAttributesContainer.getInstanceContainerName(), endAliases.get(0));

      String startColumn = startAttributesContainer.getTemporalAttribute(startProperty)
          .map(attribute -> sqlDialect.applyToDatetime(
              getQualifiedColumn(startAttributeContainerAlias, attribute.getName())))
          .get();
      String endColumn = endAttributesContainer.getTemporalAttribute(endProperty)
          .map(attribute -> sqlDialect
              .applyToDatetime(getQualifiedColumn(endAttributeContainerAlias, attribute.getName())))
          .get();

      String startTableJoin = getJoins(startAttributesContainer, startAliases, Optional.empty());
      String startTableWithJoins = String
          .format("%s%s%s", startTable, startTableJoin.isEmpty() ? "" : " ", startTableJoin);
      String endTableJoin = getJoins(endAttributesContainer, endAliases, Optional.empty());
      String endTableWithJoins = String
          .format("%s%s%s", endTable, endTableJoin.isEmpty() ? "" : " ", endTableJoin);

      Optional<String> filter = getFilter(instanceContainer);
      String where = filter.isPresent() ? String.format(" WHERE %s", filter.get()) : "";

      return String
          .format("SELECT * FROM (SELECT MIN(%s) FROM %s%s) AS A, (SELECT MAX(%s) from %s%s) AS B;",
              startColumn, startTableWithJoins, where, endColumn, endTableWithJoins, where);
    }

  }

  private List<String> getAliases(FeatureStoreAttributesContainer attributeContainer) {
    char alias = 'A';

    if (!(attributeContainer instanceof FeatureStoreRelatedContainer)) {
      return ImmutableList.of(String.valueOf(alias));
    }

    FeatureStoreRelatedContainer relatedContainer = (FeatureStoreRelatedContainer) attributeContainer;
    ImmutableList.Builder<String> aliases = new ImmutableList.Builder<>();

    for (FeatureStoreRelation relation : relatedContainer.getInstanceConnection()) {
      aliases.add(String.valueOf(alias++));
      if (relation.isM2N()) {
        aliases.add(String.valueOf(alias++));
      }
    }

    aliases.add(String.valueOf(alias++));

    return aliases.build();
  }

  private String getJoins(FeatureStoreAttributesContainer attributeContainer, List<String> aliases,
      Optional<CqlFilter> userFilter) {

    if (!(attributeContainer instanceof FeatureStoreRelatedContainer)) {
      return "";
    }

    FeatureStoreRelatedContainer relatedContainer = (FeatureStoreRelatedContainer) attributeContainer;

    ListIterator<String> aliasesIterator = aliases.listIterator();
    return relatedContainer.getInstanceConnection()
        .stream()
        .flatMap(relation -> toJoins(relation, aliasesIterator,
            getFilter(attributeContainer, relation, userFilter), Optional.empty()))
        .collect(Collectors.joining(" "));
  }

  private String getJoins(FeatureStoreAttributesContainer attributeContainer,
      FeatureStoreAttributesContainer userFilterAttributeContainer, List<String> aliases,
      Optional<CqlFilter> userFilter, Optional<String> instanceFilter) {

    if (!(attributeContainer instanceof FeatureStoreRelatedContainer)) {
      return "";
    }
    ListIterator<String> aliasesIterator = aliases.listIterator();

    FeatureStoreRelatedContainer relatedUserFilterContainer = (FeatureStoreRelatedContainer) userFilterAttributeContainer;
    String userFilterJoin = userFilter.isPresent() ? toJoins(relatedUserFilterContainer.getInstanceConnection().get(0), aliasesIterator,
            getFilter(userFilterAttributeContainer, relatedUserFilterContainer.getInstanceConnection().get(0), userFilter), instanceFilter).collect(Collectors.joining(" ")) : "";
    String userFilterTargetField = userFilter.isPresent() ? relatedUserFilterContainer.getInstanceConnection().get(0).getTargetField() : "";

    FeatureStoreRelatedContainer relatedContainer = (FeatureStoreRelatedContainer) attributeContainer;
    String join = relatedContainer.getInstanceConnection()
            .stream()
            .filter(container -> !container.getTargetField().equals(userFilterTargetField))
            .flatMap(relation -> toJoins(relation, aliasesIterator,
                    getFilter(attributeContainer, relation, Optional.empty()), instanceFilter))
            .collect(Collectors.joining(" "));
    return String.format("%1$s%3$s%2$s", userFilterJoin, join, userFilterJoin.isEmpty() || join.isEmpty() ? "" : " ");
  }

  private Stream<String> toJoins(FeatureStoreRelation relation, ListIterator<String> aliases,
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

  private Optional<String> getFilter(FeatureStoreInstanceContainer instanceContainer) {
    if (instanceContainer.getFilter().isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(filterEncoder.encode(instanceContainer.getFilter().get(), instanceContainer));
  }

  private Optional<String> getFilter(FeatureStoreAttributesContainer attributesContainer,
      FeatureStoreRelation relation, Optional<CqlFilter> cqlFilter) {
    if (relation.getFilter().isEmpty() && cqlFilter.isEmpty()) {
      return Optional.empty();
    }
    if (relation.getFilter().isPresent() && cqlFilter.isEmpty()) {
      return Optional
          .of(filterEncoder.encodeNested(relation.getFilter().get(), attributesContainer, false));
    }
    if (relation.getFilter().isEmpty() && cqlFilter.isPresent()) {
      return Optional.of(filterEncoder.encodeNested(cqlFilter.get(), attributesContainer, true));
    }

    CqlFilter mergedFilter = CqlFilter.of(And.of(
        ImmutableCqlPredicate.copyOf(relation.getFilter()
            .get()),
        ImmutableCqlPredicate.copyOf(cqlFilter.get())
    ));

    return Optional.of(filterEncoder.encodeNested(mergedFilter, attributesContainer, true));
  }

  private String getQualifiedColumn(String table, String column) {
    return column.contains("(")
        ? column.replaceAll("((?:\\w+\\()+)(\\w+)((?:\\))+)", "$1" + table + ".$2$3 AS $2")
        : String.format("%s.%s", table, column);
  }
}
