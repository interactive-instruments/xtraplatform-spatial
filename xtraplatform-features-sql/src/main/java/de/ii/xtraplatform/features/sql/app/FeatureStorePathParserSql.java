/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.sql.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.cql.domain.Cql2Expression;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureStoreAttribute;
import de.ii.xtraplatform.features.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.features.domain.FeatureStorePathParser;
import de.ii.xtraplatform.features.domain.FeatureStoreRelation;
import de.ii.xtraplatform.features.domain.FeatureStoreRelation.CARDINALITY;
import de.ii.xtraplatform.features.domain.ImmutableFeatureStoreAttribute;
import de.ii.xtraplatform.features.domain.ImmutableFeatureStoreInstanceContainer;
import de.ii.xtraplatform.features.domain.ImmutableFeatureStoreRelatedContainer;
import de.ii.xtraplatform.features.domain.ImmutableFeatureStoreRelation;
import de.ii.xtraplatform.features.sql.ImmutableSqlPath.Builder;
import de.ii.xtraplatform.features.sql.SqlFeatureTypeParser;
import de.ii.xtraplatform.features.sql.SqlPath;
import de.ii.xtraplatform.features.sql.SqlPathSyntax;
import de.ii.xtraplatform.features.sql.SqlPathSyntax.MatcherGroups;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureStorePathParserSql implements FeatureStorePathParser {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureStorePathParserSql.class);

  private final SqlPathSyntax syntax;
  private final SqlFeatureTypeParser mappingParser;
  private final Cql cql;

  public FeatureStorePathParserSql(SqlPathSyntax syntax, Cql cql) {
    this.syntax = syntax;
    this.mappingParser = new SqlFeatureTypeParser(syntax);
    this.cql = cql;
  }

  @Override
  public List<FeatureStoreInstanceContainer> parse(FeatureSchema schema) {

    List<String> paths = mappingParser.parse(schema);

    List<SqlPath> sortedPaths =
        paths.stream()
            .sorted(this::sortByPriority)
            .map(this::toSqlPath)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

    List<FeatureStoreInstanceContainer> mergedPaths = toInstanceContainers(sortedPaths);

    // Set<SqlPath> allPaths = fanOutObjectTables(mergedPaths);

    // return toTableTree(ImmutableList.copyOf(allPaths));

    return mergedPaths;
  }

  private int sortByPriority(String path1, String path2) {
    OptionalInt priority1 = syntax.getPriorityFlag(path1);
    OptionalInt priority2 = syntax.getPriorityFlag(path2);

    // TODO: special handling for geoval [observedproperty_fk=code]observedproperty/symbol
    // find more general solution
    if (path1.indexOf('/', 1) < path1.lastIndexOf('/')
        && path2.indexOf('/', 1) < path2.lastIndexOf('/')) {
      String table1 = path1.substring(0, path1.lastIndexOf('/'));
      String table2 = path2.substring(0, path2.lastIndexOf('/'));

      if (table2.indexOf('/', 1) < table2.lastIndexOf('/'))
        if (!Objects.equals(table2, table1) && table2.startsWith(table1)) {
          return -1;
        }
    }

    return priority1.isEmpty()
        ? 1
        : priority2.isEmpty() ? -1 : priority1.getAsInt() - priority2.getAsInt();
  }

  // TODO: merge into toInstanceContainers
  private Optional<SqlPath> toSqlPath(String path) {
    Matcher matcher = syntax.getColumnPathPattern().matcher(path);

    if (matcher.find()) {
      String tablePath = matcher.group(SqlPathSyntax.MatcherGroups.PATH);

      // TODO: full parent path?
      Map<String, String> tableFlags = new LinkedHashMap<>();
      Matcher tableMatcher = syntax.getTablePattern().matcher(tablePath);
      while (tableMatcher.find()) {
        String flags = tableMatcher.group(SqlPathSyntax.MatcherGroups.TABLE_FLAGS);
        String tableName = tableMatcher.group(SqlPathSyntax.MatcherGroups.TABLE);
        tablePath = tablePath.replace(flags, "");
        // String pathWithoutFlags = tableMatcher.group(0)
        //                                      .replace(flags, "");
        tableFlags.putIfAbsent(tableName, flags);
      }

      String columnString = matcher.group(SqlPathSyntax.MatcherGroups.COLUMNS);
      List<String> columns =
          Objects.nonNull(columnString)
              ? syntax.getMultiColumnSplitter().splitToList(columnString)
              : ImmutableList.of();
      String flags = matcher.group(SqlPathSyntax.MatcherGroups.PATH_FLAGS);
      OptionalInt priority = syntax.getPriorityFlag(flags);
      boolean hasOid = syntax.getOidFlag(flags);
      List<String> tablePathAsList = syntax.asList(tablePath);
      boolean isRoot = tablePathAsList.size() == 1;
      boolean isJunction = syntax.isJunctionTable(tablePathAsList.get(tablePathAsList.size() - 1));
      Optional<String> queryable =
          syntax.getQueryableFlag(flags).map(q -> q.replaceAll("\\[", "").replaceAll("]", ""));
      boolean isSpatial = syntax.getSpatialFlag(flags);
      boolean isTemporal = syntax.getTemporalFlag(flags);
      String sortKey =
          tableFlags.values().stream()
              .findFirst()
              .flatMap(syntax::getSortKeyFlag)
              .orElse(syntax.getOptions().getSortKey());
      Optional<String> constant = syntax.getConstantFlag(flags);

      return Optional.of(
          new Builder()
              .tablePath(tablePath)
              .tableFlags(tableFlags)
              .columns(columns)
              .hasOid(hasOid)
              .sortPriority(priority)
              .isRoot(isRoot)
              .isJunction(isJunction)
              .queryable(queryable.get())
              .isSpatial(isSpatial)
              .isTemporal(isTemporal)
              // .sortKey(sortKey)
              .constantValue(constant)
              .build());
    } else {
      LOGGER.warn("Invalid path in provider configuration: {}", path);
    }

    return Optional.empty();
  }

  private List<FeatureStoreInstanceContainer> toInstanceContainers(List<SqlPath> sqlPaths) {
    LinkedHashMap<String, List<SqlPath>> groupedPaths =
        sqlPaths.stream()
            .collect(
                Collectors.groupingBy(
                    SqlPath::getTablePathWithFlags, LinkedHashMap::new, Collectors.toList()));

    LinkedHashMap<String, ImmutableFeatureStoreInstanceContainer.Builder>
        instanceContainerBuilders = new LinkedHashMap<>();

    // TODO
    final int[] instancePos = {0};

    groupedPaths.entrySet().stream()
        // TODO: is this really needed?
        // .sorted(Comparator.comparingInt(entry -> syntax.asList(entry.getKey())
        //                                               .size()))
        .forEach(
            entry -> {
              String tablePath = entry.getKey();
              List<String> tablePathAsList = syntax.asList(tablePath);
              List<SqlPath> columnPaths = entry.getValue();
              List<String> columns =
                  columnPaths.stream()
                      .flatMap(sqlPath -> sqlPath.getColumns().stream())
                      .collect(Collectors.toList());
              List<FeatureStoreAttribute> attributes =
                  columnPaths.stream()
                      .flatMap(
                          sqlPath -> {
                            return sqlPath.getColumns().stream()
                                .map(
                                    name ->
                                        ImmutableFeatureStoreAttribute.builder()
                                            .name(name)
                                            .path(tablePathAsList)
                                            .addPath(name)
                                            .queryable(sqlPath.getQueryable())
                                            .isId(sqlPath.hasOid())
                                            .isSpatial(sqlPath.isSpatial())
                                            .isTemporal(sqlPath.isTemporal())
                                            .constantValue(sqlPath.getConstantValue())
                                            .build());
                          })
                      .collect(Collectors.toList());
              boolean hasOid = columnPaths.stream().anyMatch(SqlPath::hasOid);
              OptionalInt priority =
                  columnPaths.stream()
                      .flatMapToInt(
                          columnPath -> {
                            OptionalInt sortPriority = columnPath.getSortPriority();
                            return sortPriority.isPresent()
                                ? IntStream.of(sortPriority.getAsInt())
                                : IntStream.empty();
                          })
                      .findFirst();
              boolean isRoot = columnPaths.stream().anyMatch(SqlPath::isRoot);
              Matcher instanceContainerNameMatcher =
                  syntax.getTablePattern().matcher(tablePathAsList.get(0));
              if (!instanceContainerNameMatcher.find()) {
                throw new IllegalStateException("Unexpected error parsing the provider schema");
              }
              String instanceContainerName =
                  instanceContainerNameMatcher.group(SqlPathSyntax.MatcherGroups.TABLE);
              Matcher attributesContainerNameMatcher =
                  syntax.getTablePattern().matcher(tablePathAsList.get(tablePathAsList.size() - 1));
              if (!attributesContainerNameMatcher.find()) {
                throw new IllegalStateException("Unexpected error parsing the provider schema");
              }
              String attributesContainerName =
                  attributesContainerNameMatcher.group(SqlPathSyntax.MatcherGroups.TABLE);

              if (!instanceContainerBuilders.containsKey(instanceContainerName)) {
                instanceContainerBuilders.put(
                    instanceContainerName, ImmutableFeatureStoreInstanceContainer.builder());
              }

              Map<String, String> stringFilters =
                  columnPaths.stream()
                      .flatMap(sqlPath -> sqlPath.getTableFlags().entrySet().stream())
                      .filter(entry2 -> syntax.getFilterFlag(entry2.getValue()).isPresent())
                      .map(
                          entry2 ->
                              new AbstractMap.SimpleImmutableEntry<>(
                                  entry2.getKey(), syntax.getFilterFlag(entry2.getValue()).get()))
                      .distinct()
                      .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

              // TODO: needed for SqlConnectorSlick
              Map<String, Cql2Expression> filters =
                  Objects.isNull(cql)
                      ? ImmutableMap.of()
                      : stringFilters.entrySet().stream()
                          .map(
                              entry2 ->
                                  new AbstractMap.SimpleImmutableEntry<>(
                                      entry2.getKey(),
                                      cql.read(entry2.getValue(), Cql.Format.TEXT)))
                          .collect(
                              ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

              Map<String, String> sortKeys =
                  columnPaths.stream()
                      .flatMap(sqlPath -> sqlPath.getTableFlags().entrySet().stream())
                      .filter(entry2 -> syntax.getSortKeyFlag(entry2.getValue()).isPresent())
                      .map(
                          entry2 ->
                              new AbstractMap.SimpleImmutableEntry<>(
                                  entry2.getKey(), syntax.getSortKeyFlag(entry2.getValue()).get()))
                      .distinct()
                      .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

              if (isRoot) {
                ImmutableFeatureStoreInstanceContainer.Builder instanceContainerBuilder =
                    instanceContainerBuilders.get(instanceContainerName);

                // TODO: if multiple it should be different instance containers
                Optional<Cql2Expression> filter =
                    Optional.ofNullable(filters.get(instanceContainerName));

                String sortKey =
                    sortKeys.getOrDefault(instanceContainerName, syntax.getOptions().getSortKey());

                instanceContainerBuilder
                    .name(instanceContainerName)
                    .sortKey(sortKey)
                    .attributes(attributes)
                    .attributesPosition(instancePos[0])
                    .filter(filter);

                instancePos[0] = 0;
              } else {
                List<FeatureStoreRelation> instanceConnection =
                    toRelations(tablePathAsList, filters, stringFilters, sortKeys);

                String defaultSortKey =
                    syntax.isJunctionTable(attributesContainerName)
                        // TODO: oneo uses columns.get(columns.size()-1) instead, thats not a good
                        // default value
                        // TODO: support flag {orderBy=btkomplex_id}{orderDir=ASC}
                        ? instanceConnection.get(instanceConnection.size() - 1).getTargetField()
                        : syntax.getOptions().getSortKey();

                String sortKey = sortKeys.getOrDefault(attributesContainerName, defaultSortKey);

                // TODO: get tableFlags/filters; since right now we can only filter on mapped
                // attributes, we might put the filter on the attributesContainer
                // TODO: better would be to put the filter(s) on FeatureStoreRelation, so pass them
                // to toRelations
                // TODO: that would make them part of the join conditions, which should be
                // easier/cleaner

                ImmutableFeatureStoreRelatedContainer attributesContainer =
                    ImmutableFeatureStoreRelatedContainer.builder()
                        .name(attributesContainerName)
                        .sortKey(sortKey)
                        .instanceConnection(instanceConnection)
                        .attributes(attributes)
                        .build();

                instanceContainerBuilders
                    .get(instanceContainerName)
                    .addRelatedContainers(attributesContainer);

                instancePos[0]++;
              }
            });

    return instanceContainerBuilders.values().stream()
        .map(ImmutableFeatureStoreInstanceContainer.Builder::build)
        .collect(Collectors.toList());
  }

  private List<FeatureStoreRelation> toRelations(
      List<String> path,
      Map<String, Cql2Expression> filters,
      Map<String, String> stringFilters,
      Map<String, String> sortKeys) {

    if (path.size() < 2) {
      throw new IllegalArgumentException(String.format("not a valid relation path: %s", path));
    }

    if (path.size() > 2) {
      return IntStream.range(2, path.size())
          .mapToObj(
              i ->
                  toRelations(
                      path.get(i - 2),
                      path.get(i - 1),
                      path.get(i),
                      i == path.size() - 1,
                      filters,
                      stringFilters,
                      sortKeys))
          .flatMap(Function.identity())
          .collect(Collectors.toList());
    }

    return IntStream.range(1, path.size())
        .mapToObj(i -> toRelation(path.get(i - 1), path.get(i), filters, stringFilters, sortKeys))
        .collect(Collectors.toList());
  }

  private Stream<FeatureStoreRelation> toRelations(
      String source,
      String link,
      String target,
      boolean isLast,
      Map<String, Cql2Expression> filters,
      Map<String, String> stringFilters,
      Map<String, String> sortKeys) {
    if (syntax.isJunctionTable(source)) {
      if (isLast) {
        return Stream.of(toRelation(link, target, filters, stringFilters, sortKeys));
      } else {
        return Stream.empty();
      }
    }
    if (syntax.isJunctionTable(target) && !isLast) {
      return Stream.of(toRelation(source, link, filters, stringFilters, sortKeys));
    }

    if (syntax.isJunctionTable(link)) {
      return Stream.of(toRelation(source, link, target, sortKeys));
    }

    return Stream.of(
        toRelation(source, link, filters, stringFilters, sortKeys),
        toRelation(link, target, filters, stringFilters, sortKeys));
  }

  // TODO: support sortKey flag on table instead of getDefaultPrimaryKey
  private FeatureStoreRelation toRelation(
      String source,
      String target,
      Map<String, Cql2Expression> filters,
      Map<String, String> stringFilters,
      Map<String, String> sortKeys) {
    Matcher sourceMatcher = syntax.getTablePattern().matcher(source);
    Matcher targetMatcher = syntax.getJoinedTablePattern().matcher(target);
    if (sourceMatcher.find() && targetMatcher.find()) {
      String sourceContainer = sourceMatcher.group(SqlPathSyntax.MatcherGroups.TABLE);
      String sourceField = targetMatcher.group(SqlPathSyntax.MatcherGroups.SOURCE_FIELD);
      String targetContainer = targetMatcher.group(MatcherGroups.TABLE);
      String targetField = targetMatcher.group(SqlPathSyntax.MatcherGroups.TARGET_FIELD);
      // TODO: primaryKey flag
      boolean isOne2One = Objects.equals(targetField, syntax.getOptions().getPrimaryKey());

      // TODO: shouldn't this be targetContainer?
      Optional<String> sourceFilter =
          Optional.ofNullable(stringFilters.get(sourceContainer))
              .map(f -> String.format("{filter=%s}", f));
      Optional<String> targetFilter =
          Optional.ofNullable(stringFilters.get(targetContainer))
              .map(f -> String.format("{filter=%s}", f));
      Optional<Cql2Expression> targetFilter2 = Optional.ofNullable(filters.get(targetContainer));

      String sortKey = sortKeys.getOrDefault(sourceContainer, syntax.getOptions().getSortKey());

      return ImmutableFeatureStoreRelation.builder()
          .cardinality(isOne2One ? CARDINALITY.ONE_2_ONE : CARDINALITY.ONE_2_N)
          .sourceContainer(sourceContainer)
          .sourceField(sourceField)
          .sourceSortKey(sortKey)
          .sourceFilter(sourceFilter)
          .targetContainer(targetContainer)
          .targetField(targetField)
          .targetFilter(targetFilter)
          .filter(targetFilter2)
          .build();
    }

    throw new IllegalArgumentException(
        String.format("not a valid relation path: %s/%s", source, target));
  }

  private FeatureStoreRelation toRelation(
      String source, String link, String target, Map<String, String> sortKeys) {
    Matcher sourceMatcher = syntax.getTablePattern().matcher(source);
    Matcher junctionMatcher = syntax.getJoinedTablePattern().matcher(link);
    Matcher targetMatcher = syntax.getJoinedTablePattern().matcher(target);
    if (sourceMatcher.find() && junctionMatcher.find() && targetMatcher.find()) {
      String sourceContainer = sourceMatcher.group(SqlPathSyntax.MatcherGroups.TABLE);
      Optional<String> sourceFilter =
          Optional.ofNullable(sourceMatcher.group(MatcherGroups.TABLE_FLAGS))
              .flatMap(syntax::getFilterFlagExpression);
      String sortKey = sortKeys.getOrDefault(sourceContainer, syntax.getOptions().getSortKey());

      return ImmutableFeatureStoreRelation.builder()
          .cardinality(CARDINALITY.M_2_N)
          .sourceContainer(sourceMatcher.group(SqlPathSyntax.MatcherGroups.TABLE))
          .sourceField(junctionMatcher.group(SqlPathSyntax.MatcherGroups.SOURCE_FIELD))
          .sourceFilter(sourceFilter)
          .sourceSortKey(sortKey)
          .junctionSource(junctionMatcher.group(SqlPathSyntax.MatcherGroups.TARGET_FIELD))
          .junction(junctionMatcher.group(SqlPathSyntax.MatcherGroups.TABLE))
          .junctionTarget(targetMatcher.group(SqlPathSyntax.MatcherGroups.SOURCE_FIELD))
          .targetContainer(targetMatcher.group(SqlPathSyntax.MatcherGroups.TABLE))
          .targetField(targetMatcher.group(SqlPathSyntax.MatcherGroups.TARGET_FIELD))
          .build();
    }

    throw new IllegalArgumentException(
        String.format("not a valid relation path: %s/%s/%s", source, link, target));
  }
}
