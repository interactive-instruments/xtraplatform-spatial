package de.ii.xtraplatform.feature.provider.sql.app;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreAttribute;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreAttributesContainer;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreQueryGenerator;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreRelatedContainer;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreRelation;
import de.ii.xtraplatform.feature.provider.sql.domain.FilterEncoderSqlNew;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlCondition;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialect;
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

    private final FilterEncoderSqlNew filterEncoder;
    private final SqlDialect sqlDialect;

    public FeatureStoreQueryGeneratorSql(FilterEncoderSqlNew filterEncoder,
                                         SqlDialect sqlDialect) {
        this.filterEncoder = filterEncoder;
        this.sqlDialect = sqlDialect;
    }

    @Override
    public String getMetaQuery(FeatureStoreInstanceContainer instanceContainer, int limit, int offset, String filter,
                               boolean computeNumberMatched) {
        String limitSql = limit > 0 ? String.format(" LIMIT %d", limit) : "";
        String offsetSql = offset > 0 ? String.format(" OFFSET %d", offset) : "";
        String where = !Strings.isNullOrEmpty(filter) ? String.format(" WHERE %s", getFilter(instanceContainer, filter)) : "";

        String numberReturned = String.format("SELECT MIN(SKEY) AS minKey, MAX(SKEY) AS maxKey, count(*) AS numberReturned FROM (SELECT A.%2$s AS SKEY FROM %1$s A%5$s ORDER BY 1%3$s%4$s) AS NR", instanceContainer.getName(), instanceContainer.getSortKey(), limitSql, offsetSql, where);

        if (computeNumberMatched) {
            String numberMatched = String.format("SELECT count(*) AS numberMatched FROM (SELECT A.%2$s AS SKEY FROM %1$s A%3$s ORDER BY 1) AS NM", instanceContainer.getName(), instanceContainer.getSortKey(), where);
            return String.format("SELECT * FROM (%s) AS NR2, (%s) AS NM2", numberReturned, numberMatched);
        } else {
            return String.format("SELECT *,-1 FROM (%s) AS META", numberReturned);
        }
    }

    @Override
    public Stream<String> getInstanceQueries(FeatureStoreInstanceContainer instanceContainer, String cqlFilter,
                                             long minKey, long maxKey) {

        Optional<String> filter = Optional.ofNullable(Strings.emptyToNull(cqlFilter));
        boolean isIdFilter = filter.isPresent() && isIdFilter(filter.get());
        List<String> aliases = getAliases(instanceContainer);
        Optional<String> sqlFilter = filter.map(f -> getFilter(instanceContainer, f));

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

        String join = getJoins(attributesContainer, aliases);

        return String.format("SELECT %s FROM %s%s%s", column, mainTable, join.isEmpty() ? "" : " ", join);
    }

    private String getTableQuery(FeatureStoreAttributesContainer attributeContainer, Optional<String> whereClause) {
        List<String> aliases = getAliases(attributeContainer);
        String attributeContainerAlias = aliases.get(aliases.size() - 1);

        String mainTable = String.format("%s %s", attributeContainer.getInstanceContainerName(), aliases.get(0));
        List<String> sortFields = getSortFields(attributeContainer, aliases);

        String columns = Stream.concat(sortFields.stream(), attributeContainer.getAttributes()
                                                                              .stream()
                                                                              .map(column -> {
                                                                                  String name = getQualifiedColumn(attributeContainerAlias, column.getName());
                                                                                  return column.isSpatial() ? sqlDialect.applyToWkt(name) : name;
                                                                              }))
                               .collect(Collectors.joining(", "));

        String join = getJoins(attributeContainer, aliases);

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

    private String getJoins(FeatureStoreAttributesContainer attributeContainer, List<String> aliases) {

        if (!(attributeContainer instanceof FeatureStoreRelatedContainer)) {
            return "";
        }

        FeatureStoreRelatedContainer relatedContainer = (FeatureStoreRelatedContainer) attributeContainer;

        ListIterator<String> aliasesIterator = aliases.listIterator();
        return relatedContainer.getInstanceConnection()
                               .stream()
                               .flatMap(relation -> toJoins(relation, aliasesIterator))
                               .collect(Collectors.joining(" "));
    }

    private Stream<String> toJoins(FeatureStoreRelation relation, ListIterator<String> aliases) {
        List<String> joins = new ArrayList<>();

        if (relation.isM2N()) {
            String sourceAlias = aliases.next();
            String junctionAlias = aliases.next();
            String targetAlias = aliases.next();
            aliases.previous();

            joins.add(toJoin(relation.getJunction()
                                     .get(), junctionAlias, relation.getJunctionSource()
                                                                    .get(), sourceAlias, relation.getSourceField()));
            joins.add(toJoin(relation.getTargetContainer(), targetAlias, relation.getTargetField(), junctionAlias, relation.getJunctionTarget()
                                                                                                                           .get()));

        } else {
            String sourceAlias = aliases.next();
            String targetAlias = aliases.next();
            aliases.previous();

            joins.add(toJoin(relation.getTargetContainer(), targetAlias, relation.getTargetField(), sourceAlias, relation.getSourceField()));
        }

        return joins.stream();
    }

    private String toJoin(String targetContainer, String targetAlias, String targetField, String sourceContainer,
                          String sourceField) {
        return String.format("JOIN %1$s %2$s ON %4$s.%5$s=%2$s.%3$s", targetContainer, targetAlias, targetField, sourceContainer, sourceField);
    }

    private String getFilter(FeatureStoreInstanceContainer instanceContainer, String cqlFilter) {
        List<SqlCondition> sqlConditions = filterEncoder.encode(cqlFilter, instanceContainer);

        String sqlFilter = sqlConditions.stream()
                                        .map(sqlCondition -> {

                                            List<String> aliases = getAliases(sqlCondition.getTable()).stream()
                                                                                                      .map(s -> "A" + s)
                                                                                                      .collect(Collectors.toList());
                                            String join = getJoins(sqlCondition.getTable(), aliases);
                                            String property = String.format("%s.%s", aliases.get(aliases.size() - 1), sqlCondition.getColumn());
                                            String expression = sqlCondition.getExpression()
                                                                            .replace("{{prop}}", property);

                                            return String.format("A.%3$s IN (SELECT %2$s.%3$s FROM %1$s %2$s %4$s WHERE %5$s)", instanceContainer.getName(), aliases.get(0), instanceContainer.getSortKey(), join, expression);
                                        })
                                        .collect(Collectors.joining(") AND (", "(", ")"));

        return sqlFilter;
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

    //TODO: test after encoding on List<SqlCondition> instead
    private boolean isIdFilter(String filter) {
        return Strings.nullToEmpty(filter)
                      .startsWith("IN ('");
    }

    private String toWhereClause(String alias, String keyField, long minKey, long maxKey,
                                 Optional<String> additionalFilter) {
        StringBuilder filter = new StringBuilder()
                .append("(")
                .append(alias)
                .append(".")
                .append(keyField)
                .append(" >= ")
                .append(minKey)
                .append(" AND ")
                .append(alias)
                .append(".")
                .append(keyField)
                .append(" <= ")
                .append(maxKey)
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
