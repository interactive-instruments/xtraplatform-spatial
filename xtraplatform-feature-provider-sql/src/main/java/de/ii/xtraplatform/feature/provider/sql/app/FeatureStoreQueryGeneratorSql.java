/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.cql.domain.And;
import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.cql.domain.ImmutableCqlPredicate;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.feature.provider.sql.domain.FilterEncoderSqlNewNew;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialect;
import de.ii.xtraplatform.features.domain.FeatureStoreAttributesContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreQueryGenerator;
import de.ii.xtraplatform.features.domain.FeatureStoreRelatedContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreRelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FeatureStoreQueryGeneratorSql implements FeatureStoreQueryGenerator<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureStoreQueryGeneratorSql.class);

    private final FilterEncoderSqlNewNew filterEncoder;
    private final SqlDialect sqlDialect;

    public FeatureStoreQueryGeneratorSql(SqlDialect sqlDialect, EpsgCrs nativeCrs,
                                         CrsTransformerFactory crsTransformerFactory) {
        //this.filterEncoder = new FilterEncoderSqlNewImpl(nativeCrs);
        this.sqlDialect = sqlDialect;
        this.filterEncoder = new FilterEncoderSqlNewNewImpl(this::getAliases, (attributeContainer, aliases) -> userFilter -> getJoins(attributeContainer, aliases, userFilter), nativeCrs, sqlDialect,crsTransformerFactory);
    }

    @Override
    public String getMetaQuery(FeatureStoreInstanceContainer instanceContainer, int limit, int offset, Optional<CqlFilter> cqlFilter,
                               boolean computeNumberMatched) {
        String limitSql = limit > 0 ? String.format(" LIMIT %d", limit) : "";
        String offsetSql = offset > 0 ? String.format(" OFFSET %d", offset) : "";
        Optional<String> filter = getFilter(instanceContainer, cqlFilter);
        String where = filter.isPresent() ? String.format(" WHERE %s", filter.get()) : "";

        String numberReturned = String.format("SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.%2$s AS SKEY FROM %1$s A%5$s ORDER BY 1%3$s%4$s) AS NR", instanceContainer.getName(), instanceContainer.getSortKey(), limitSql, offsetSql, where);

        if (computeNumberMatched) {
            String numberMatched = String.format("SELECT count(*) AS numberMatched FROM (SELECT A.%2$s AS SKEY FROM %1$s A%3$s ORDER BY 1) AS NM", instanceContainer.getName(), instanceContainer.getSortKey(), where);
            return String.format("SELECT * FROM (%s) AS NR2, (%s) AS NM2", numberReturned, numberMatched);
        } else {
            return String.format("SELECT *,-1 FROM (%s) AS META", numberReturned);
        }
    }

    @Override
    public Stream<String> getInstanceQueries(FeatureStoreInstanceContainer instanceContainer, Optional<CqlFilter> cqlFilter,
                                             Object minKey, Object maxKey) {

        boolean isIdFilter = cqlFilter.flatMap(CqlFilter::getInOperator).isPresent();
        List<String> aliases = getAliases(instanceContainer);
        Optional<String> sqlFilter = getFilter(instanceContainer, cqlFilter);

        Optional<String> whereClause = isIdFilter
                ? sqlFilter
                : Optional.of(toWhereClause(aliases.get(0), instanceContainer.getSortKey(), minKey, maxKey, sqlFilter));

        return instanceContainer.getAllAttributesContainers()
                                .stream()
                                .map(attributeContainer -> getTableQuery(attributeContainer, whereClause));
    }

    @Override
    public String getExtentQuery(FeatureStoreAttributesContainer attributesContainer) {

        List<String> aliases = getAliases(attributesContainer);
        String attributeContainerAlias = aliases.get(aliases.size() - 1);

        String mainTable = String.format("%s %s", attributesContainer.getInstanceContainerName(), aliases.get(0));

        String column = attributesContainer.getSpatialAttribute()
                                            .map(attribute -> sqlDialect.applyToExtent(getQualifiedColumn(attributeContainerAlias, attribute.getName())))
                                            .get();

        String join = getJoins(attributesContainer, aliases, Optional.empty());

        return String.format("SELECT %s FROM %s%s%s", column, mainTable, join.isEmpty() ? "" : " ", join);
    }

    public String getTemporalExtentQuery(FeatureStoreAttributesContainer attributesContainer, String property) {
        List<String> aliases = getAliases(attributesContainer);
        String attributeContainerAlias = aliases.get(aliases.size() - 1);

        String table = String.format("%s %s", attributesContainer.getInstanceContainerName(), aliases.get(0));
        String column = attributesContainer.getTemporalAttribute(property)
                .map(attribute -> sqlDialect.applyToExtent(getQualifiedColumn(attributeContainerAlias, attribute.getName())))
                .get();
        String join = getJoins(attributesContainer, aliases, Optional.empty());

        return String.format("SELECT MIN(%s), MAX(%s) FROM %s%s%s", column, column, table, join.isEmpty() ? "" : " ", join);
    }

    public String getTemporalExtentQuery(FeatureStoreAttributesContainer startAttributesContainer,
                                         FeatureStoreAttributesContainer endAttributesContainer,
                                         String startProperty, String endProperty) {
        if (startAttributesContainer.equals(endAttributesContainer)) {
            List<String> aliases = getAliases(startAttributesContainer);
            String attributeContainerAlias = aliases.get(aliases.size() - 1);

            String table = String.format("%s %s", startAttributesContainer.getInstanceContainerName(), aliases.get(0));
            String startColumn = startAttributesContainer.getTemporalAttribute(startProperty)
                    .map(attribute -> sqlDialect.applyToExtent(getQualifiedColumn(attributeContainerAlias, attribute.getName())))
                    .get();
            String endColumn = endAttributesContainer.getTemporalAttribute(endProperty)
                    .map(attribute -> sqlDialect.applyToExtent(getQualifiedColumn(attributeContainerAlias, attribute.getName())))
                    .get();
            String join = getJoins(startAttributesContainer, aliases, Optional.empty());

            return String.format("SELECT MIN(%s), MAX(%s) FROM %s%s%s", startColumn, endColumn, table, join.isEmpty() ? "" : " ", join);

        } else {
            List<String> startAliases = getAliases(startAttributesContainer);
            String startAttributeContainerAlias = startAliases.get(startAliases.size() - 1);
            String startTable = String.format("%s %s", startAttributesContainer.getInstanceContainerName(), startAliases.get(0));

            List<String> endAliases = getAliases(endAttributesContainer);
            String endAttributeContainerAlias = endAliases.get(endAliases.size() - 1);
            String endTable = String.format("%s %s", endAttributesContainer.getInstanceContainerName(), endAliases.get(0));

            String startColumn = startAttributesContainer.getTemporalAttribute(startProperty)
                    .map(attribute -> sqlDialect.applyToExtent(getQualifiedColumn(startAttributeContainerAlias, attribute.getName())))
                    .get();
            String endColumn = endAttributesContainer.getTemporalAttribute(endProperty)
                    .map(attribute -> sqlDialect.applyToExtent(getQualifiedColumn(endAttributeContainerAlias, attribute.getName())))
                    .get();

            String startTableJoin = getJoins(startAttributesContainer, startAliases, Optional.empty());
            String startTableWithJoins = String.format("%s%s%s", startTable, startTableJoin.isEmpty() ? "" : " ", startTableJoin);
            String endTableJoin = getJoins(endAttributesContainer, endAliases, Optional.empty());
            String endTableWithJoins = String.format("%s%s%s", endTable, endTableJoin.isEmpty() ? "" : " ", endTableJoin);

            return String.format("SELECT * FROM (SELECT MIN(%s) FROM %s) AS A, (SELECT MAX(%s) from %s) AS B;",
                    startColumn, startTableWithJoins, endColumn, endTableWithJoins);
        }

    }

    private String getTableQuery(FeatureStoreAttributesContainer attributeContainer, Optional<String> whereClause) {
        List<String> aliases = getAliases(attributeContainer);
        String attributeContainerAlias = aliases.get(aliases.size() - 1);

        String mainTable = String.format("%s %s", attributeContainer.getInstanceContainerName(), aliases.get(0));
        List<String> sortFields = getSortFields(attributeContainer, aliases);

        String columns = Stream.concat(sortFields.stream(), attributeContainer.getAttributes()
                                                                              .stream()
                                                                              .map(column -> {
                                                                                  String name = column.isConstant() ? column.getConstantValue().get() + " AS " + column.getName() : getQualifiedColumn(attributeContainerAlias, column.getName());
                                                                                  if (column.isSpatial()) {
                                                                                      return sqlDialect.applyToWkt(name);
                                                                                  }
                                                                                  if (column.isTemporal()) {
                                                                                      return sqlDialect.applyToDatetime(name);
                                                                                  }

                                                                                  return name;
                                                                              }))
                               .collect(Collectors.joining(", "));

        String join = getJoins(attributeContainer, aliases, Optional.empty());

        //String limit2 = limit > 0 ? " LIMIT " + limit : "";
        //String offset2 = offset > 0 ? " OFFSET " + offset : "";
        String where = whereClause.map(w -> " WHERE " + w)
                                  .orElse("");
        String orderBy = IntStream.rangeClosed(1, sortFields.size())
                                  .boxed()
                                  .map(String::valueOf)
                                  .collect(Collectors.joining(","));

        return String.format("SELECT %s FROM %s%s%s%s ORDER BY %s", columns, mainTable, join.isEmpty() ? "" : " ", join, where, orderBy);
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
                               .flatMap(relation -> toJoins(relation, aliasesIterator, getFilter(attributeContainer, relation, userFilter)))
                               .collect(Collectors.joining(" "));
    }

    private Stream<String> toJoins(FeatureStoreRelation relation, ListIterator<String> aliases,
                                   Optional<String> sqlFilter) {
        List<String> joins = new ArrayList<>();

        if (relation.isM2N()) {
            String sourceAlias = aliases.next();
            String junctionAlias = aliases.next();
            String targetAlias = aliases.next();
            aliases.previous();

            joins.add(toJoin(relation.getJunction()
                                     .get(), junctionAlias, relation.getJunctionSource()
                                                                    .get(), sourceAlias, relation.getSourceField(), sqlFilter));
            joins.add(toJoin(relation.getTargetContainer(), targetAlias, relation.getTargetField(), junctionAlias, relation.getJunctionTarget()
                                                                                                                           .get(), sqlFilter));

        } else {
            String sourceAlias = aliases.next();
            String targetAlias = aliases.next();
            aliases.previous();

            joins.add(toJoin(relation.getTargetContainer(), targetAlias, relation.getTargetField(), sourceAlias, relation.getSourceField(), sqlFilter));
        }

        return joins.stream();
    }

    private String toJoin(String targetContainer, String targetAlias, String targetField, String sourceContainer,
                          String sourceField, Optional<String> sqlFilter) {
        String additionalFilter = sqlFilter.map(s -> " AND " + s)
                                           .orElse("");
        String targetTable = targetContainer;

        if (additionalFilter.contains("row_number")) {
            targetTable = String.format("(SELECT *,row_number() OVER () FROM %s)", targetContainer);
        }

        return String.format("JOIN %1$s %2$s ON (%4$s.%5$s=%2$s.%3$s%6$s)", targetTable, targetAlias, targetField, sourceContainer, sourceField, additionalFilter);
    }

    private Optional<String> getFilter(FeatureStoreInstanceContainer instanceContainer, Optional<CqlFilter> cqlFilter) {
        if (!instanceContainer.getFilter().isPresent() && !cqlFilter.isPresent()) {
            return Optional.empty();
        }
        if (instanceContainer.getFilter().isPresent() && !cqlFilter.isPresent()) {
            return Optional.of(filterEncoder.encode(instanceContainer.getFilter().get(), instanceContainer));
        }
        if (!instanceContainer.getFilter().isPresent() && cqlFilter.isPresent()) {
            return Optional.of(filterEncoder.encode(cqlFilter.get(), instanceContainer));
        }

        CqlFilter mergedFilter = CqlFilter.of(And.of(
                ImmutableCqlPredicate.copyOf(instanceContainer.getFilter()
                                                              .get()),
                ImmutableCqlPredicate.copyOf(cqlFilter.get())
        ));

        return Optional.of(filterEncoder.encode(mergedFilter, instanceContainer));
    }

    private Optional<String> getFilter(FeatureStoreAttributesContainer attributesContainer, FeatureStoreRelation relation, Optional<CqlFilter> cqlFilter) {
        if (!relation.getFilter().isPresent() && !cqlFilter.isPresent()) {
            return Optional.empty();
        }
        if (relation.getFilter().isPresent() && !cqlFilter.isPresent()) {
            return Optional.of(filterEncoder.encodeNested(relation.getFilter().get(), attributesContainer, false));
        }
        if (!relation.getFilter().isPresent() && cqlFilter.isPresent()) {
            return Optional.of(filterEncoder.encodeNested(cqlFilter.get(), attributesContainer, true));
        }

        CqlFilter mergedFilter = CqlFilter.of(And.of(
                ImmutableCqlPredicate.copyOf(relation.getFilter()
                                                              .get()),
                ImmutableCqlPredicate.copyOf(cqlFilter.get())
        ));

        return Optional.of(filterEncoder.encodeNested(mergedFilter, attributesContainer, true));
    }

    private List<String> getSortFields(FeatureStoreAttributesContainer attributesContainer, List<String> aliases) {
        if (!(attributesContainer instanceof FeatureStoreRelatedContainer)) {
            return ImmutableList.of(String.format("%s.%s AS SKEY", aliases.get(0), attributesContainer.getSortKey()));
        } else {
            FeatureStoreRelatedContainer relatedContainer = (FeatureStoreRelatedContainer) attributesContainer;
            ListIterator<String> aliasesIterator = aliases.listIterator();

            return relatedContainer.getSortKeys(aliasesIterator);
        }
    }

    private String getQualifiedColumn(String table, String column) {
        return column.contains("(")
                ? column.replaceAll("((?:\\w+\\()+)(\\w+)((?:\\))+)", "$1" + table + ".$2$3 AS $2")
                : String.format("%s.%s", table, column);
    }


    private String toWhereClause(String alias, String keyField, Object minKey, Object maxKey,
                                 Optional<String> additionalFilter) {
        StringBuilder filter = new StringBuilder()
                .append("(")
                .append(alias)
                .append(".")
                .append(keyField)
                .append(" >= ")
                .append(minKey instanceof String ? String.format("'%s'", minKey) : minKey)
                .append(" AND ")
                .append(alias)
                .append(".")
                .append(keyField)
                .append(" <= ")
                .append(maxKey instanceof String ? String.format("'%s'", maxKey) : maxKey)
                .append(")");

        if (additionalFilter.isPresent()) {
            filter.append(" AND ")
                  .append(additionalFilter.get());
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("SUB FILTER: {}", filter);
        }

        return filter.toString();
    }
}
