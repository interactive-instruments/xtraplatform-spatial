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
import de.ii.xtraplatform.features.domain.FeatureStoreRelation;
import de.ii.xtraplatform.features.domain.FeatureStoreRelation.CARDINALITY;
import de.ii.xtraplatform.features.domain.FeatureTypeV2;
import de.ii.xtraplatform.features.domain.ImmutableFeatureStoreAttribute;
import de.ii.xtraplatform.features.domain.ImmutableFeatureStoreInstanceContainer;
import de.ii.xtraplatform.features.domain.ImmutableFeatureStoreRelatedContainer;
import de.ii.xtraplatform.features.domain.ImmutableFeatureStoreRelation;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.sql.ImmutableSqlPath;
import de.ii.xtraplatform.features.sql.SqlFeatureTypeParser2;
import de.ii.xtraplatform.features.sql.SqlPath;
import de.ii.xtraplatform.features.sql.SqlPathSyntax;
import de.ii.xtraplatform.features.sql.domain.ImmutableSchemaSql.Builder;
import de.ii.xtraplatform.features.sql.domain.SchemaSql;
import de.ii.xtraplatform.features.sql.domain.SqlRelation;
import java.util.AbstractMap;
import java.util.ArrayList;
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

public class FeatureSchemaSwapperSql {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureSchemaSwapperSql.class);

  private final SqlPathSyntax syntax;
  private final SqlFeatureTypeParser2 mappingParser;
  private final Cql cql;

  public FeatureSchemaSwapperSql(SqlPathSyntax syntax, Cql cql) {
    this.syntax = syntax;
    this.mappingParser = new SqlFeatureTypeParser2(syntax);
    this.cql = cql;
  }

  public SchemaSql swap(FeatureSchema featureType) {

    // TODO: build SchemaMapping

    List<Builder> builders =
        swapProperties(featureType.getProperties(), featureType.getSourcePath().orElse(""));
    return new Builder()
        .name(featureType.getName())
        .type(SchemaBase.Type.OBJECT)
        // .target(featureType)
        .addAllPropertiesBuilders(builders)
        .build();
  }

  private List<Builder> swapProperties(List<FeatureSchema> properties, String parentPath) {

    Map<String, Builder> objectPropertiesCache = new LinkedHashMap<>();

    return properties.stream()
        .flatMap(
            originalProperty ->
                createPropertyTree(
                    objectPropertiesCache,
                    originalProperty,
                    parentPath,
                    ImmutableList.of(parentPath))
                    .stream())
        .collect(Collectors.toList());
  }

  private List<Builder> createPropertyTree(
      Map<String, Builder> objectPropertiesCache,
      FeatureSchema originalProperty,
      String parentPath,
      List<String> parentFullPath) {

    boolean isRoot = parentPath.startsWith("/");

    boolean isColumn =
        originalProperty.getType() != SchemaBase.Type.OBJECT
            && originalProperty.getType() != SchemaBase.Type.OBJECT_ARRAY /*&&
                originalProperty.getType() != FeaturePropertyV2.Type.VALUE_ARRAY*/;

    Optional<SqlPath> sqlPath =
        toSqlPath(parentPath + "/" + originalProperty.getSourcePath().orElse(""), isColumn);

    // TODO: column?
    if (!sqlPath.isPresent()) {
      throw new IllegalArgumentException(
          "Parse error for SQL path: "
              + parentPath
              + "/"
              + originalProperty.getSourcePath().orElse(""));
    }

    List<String> tablePathAsList = syntax.asList(sqlPath.get().getTablePath());

    boolean hasRelation = tablePathAsList.size() > 1; // (isRoot ? 1 : 0);

    List<SqlRelation> featureStoreRelations =
        ImmutableList.of(); // TODO hasRelation ? toRelations(tablePathAsList, ImmutableMap.of()) :
    // ImmutableList.of();

    return createPropertyTree(
        objectPropertiesCache,
        sqlPath.get(),
        featureStoreRelations,
        originalProperty,
        parentFullPath);
  }

  private List<Builder> createPropertyTree(
      Map<String, Builder> objectPropertiesCache,
      SqlPath sqlPath,
      List<SqlRelation> relations,
      FeatureSchema originalProperty,
      List<String> parentFullPath) {

    List<Builder> swapped = new ArrayList<>();

    if (!relations.isEmpty()) {
      SqlRelation relation = relations.get(0);
      String name = relation.getTargetContainer();
      SchemaBase.Type type =
          relation.isOne2One()
              ? SchemaBase.Type.OBJECT
              : sqlPath.getColumns().isEmpty()
                  ? SchemaBase.Type.OBJECT_ARRAY
                  : SchemaBase.Type.VALUE_ARRAY;

      Builder objectProperty =
          objectPropertiesCache.computeIfAbsent(
              name,
              cleanName -> {
                LOGGER.debug("OBJ {}", relation);
                Builder objectProperty1 =
                    new Builder()
                        .name(relation.getTargetContainer())
                        .addAllParentPath(parentFullPath)
                        .type(type)
                        .addRelation(relation)
                        .valueType(originalProperty.getValueType())
                        .geometryType(originalProperty.getGeometryType());

                swapped.add(objectProperty1);

                return objectProperty1;
              });

      List<String> fullPath =
          new ImmutableList.Builder<String>()
              .addAll(parentFullPath)
              .addAll(relation.asPath())
              .build();

      if (relations.size() > 1) {
        LOGGER.debug("REL");

        List<Builder> childTrees =
            createPropertyTree(
                objectPropertiesCache,
                sqlPath,
                relations.subList(1, relations.size()),
                originalProperty,
                fullPath);

        objectProperty.addAllPropertiesBuilders(childTrees);

        return swapped;
      }

      originalProperty.getProperties().stream()
          .flatMap(
              childProperty -> {
                LOGGER.debug("NES");

                return createPropertyTree(
                    objectPropertiesCache,
                    childProperty,
                    fullPath.get(fullPath.size() - 1),
                    fullPath)
                    .stream();
              })
          .forEach(objectProperty::addAllPropertiesBuilders);

      if (!sqlPath.getColumns().isEmpty()) {
        LOGGER.debug("COL {}", sqlPath);

        Builder property =
            new Builder()
                .name(sqlPath.getColumns().get(0))
                .addAllParentPath(fullPath)
                .type(
                    originalProperty
                        .getValueType()
                        .orElse(originalProperty.getType())); // .target(originalProperty);

        objectProperty.addAllPropertiesBuilders(property);
      }

      return swapped;
    }

    if (!sqlPath.getColumns().isEmpty()) {
      LOGGER.debug("COL {}", sqlPath);

      Builder property =
          new Builder()
              .name(sqlPath.getColumns().get(0))
              .addAllParentPath(parentFullPath)
              .type(
                  originalProperty
                      .getValueType()
                      .orElse(originalProperty.getType())); // .target(originalProperty);

      swapped.add(property);
    }

    return swapped;
  }

  public List<FeatureStoreInstanceContainer> parse(FeatureTypeV2 featureType) {

    List<String> paths = mappingParser.parse(featureType);

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

  // TODO: test with mappings without sortPriority, e.g. daraa
  private int sortByPriority(String path1, String path2) {
    OptionalInt priority1 = syntax.getPriorityFlag(path1);
    OptionalInt priority2 = syntax.getPriorityFlag(path2);

    return !priority1.isPresent()
        ? 1
        : !priority2.isPresent() ? -1 : priority1.getAsInt() - priority2.getAsInt();
  }

  private Optional<SqlPath> toSqlPath(String path) {
    return toSqlPath(path, true);
  }

  // TODO: merge into toInstanceContainers
  private Optional<SqlPath> toSqlPath(String path, boolean isColumn) {
    Matcher matcher =
        isColumn
            ? syntax.getPartialColumnPathPattern().matcher(path)
            : syntax.getPathPattern().matcher(path);

    if (matcher.find()) {
      String tablePath = matcher.group(SqlPathSyntax.MatcherGroups.PATH);

      // TODO: full parent path?
      Map<String, String> tableFlags = new LinkedHashMap<>();
      Matcher tableMatcher = syntax.getTablePattern().matcher(tablePath);
      while (tableMatcher.find()) {
        String flags = tableMatcher.group(SqlPathSyntax.MatcherGroups.TABLE_FLAGS);
        tablePath = tablePath.replace(flags, "");
        String pathWithoutFlags = tableMatcher.group(0).replace(flags, "");
        tableFlags.putIfAbsent(pathWithoutFlags, flags);
      }

      String column = isColumn ? matcher.group(SqlPathSyntax.MatcherGroups.COLUMNS) : null;
      List<String> columns =
          Objects.nonNull(column)
              ? syntax.getMultiColumnSplitter().splitToList(column)
              : ImmutableList.of();

      String flags = isColumn ? matcher.group(SqlPathSyntax.MatcherGroups.PATH_FLAGS) : "";
      OptionalInt priority = syntax.getPriorityFlag(flags);
      boolean hasOid = syntax.getOidFlag(flags);
      List<String> tablePathAsList = syntax.asList(tablePath);
      boolean isRoot = tablePathAsList.size() == 1;
      boolean isJunction = syntax.isJunctionTable(tablePathAsList.get(tablePathAsList.size() - 1));
      Optional<String> queryable =
          syntax.getQueryableFlag(flags).map(q -> q.replaceAll("\\[", "").replaceAll("]", ""));
      boolean isSpatial = syntax.getSpatialFlag(flags);

      return Optional.of(
          new ImmutableSqlPath.Builder()
              .tablePath(tablePath)
              .tableFlags(tableFlags)
              .columns(columns)
              .hasOid(hasOid)
              .sortPriority(priority)
              .isRoot(isRoot)
              .isJunction(isJunction)
              .queryable("" /*queryable.get()*/)
              .isSpatial(isSpatial)
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
                    SqlPath::getTablePath, LinkedHashMap::new, Collectors.toList()));

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
                          sqlPath ->
                              sqlPath.getColumns().stream()
                                  .map(
                                      name ->
                                          ImmutableFeatureStoreAttribute.builder()
                                              .name(name)
                                              .path(tablePathAsList)
                                              .addPath(name)
                                              .queryable(sqlPath.getQueryable())
                                              .isId(sqlPath.hasOid())
                                              .isSpatial(sqlPath.isSpatial())
                                              .build()))
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
                throw new IllegalStateException(
                    "Unexpected error generating the provider transactions mapping");
              }
              String instanceContainerName =
                  instanceContainerNameMatcher.group(SqlPathSyntax.MatcherGroups.TABLE);
              Matcher attributesContainerNameMatcher =
                  syntax.getTablePattern().matcher(tablePathAsList.get(tablePathAsList.size() - 1));
              if (!attributesContainerNameMatcher.find()) {
                throw new IllegalStateException(
                    "Unexpected error generating the provider transactions mapping");
              }
              String attributesContainerName =
                  attributesContainerNameMatcher.group(SqlPathSyntax.MatcherGroups.TABLE);

              if (!instanceContainerBuilders.containsKey(instanceContainerName)) {
                instanceContainerBuilders.put(
                    instanceContainerName, ImmutableFeatureStoreInstanceContainer.builder());
              }

              Map<String, Cql2Expression> filters =
                  columnPaths.stream()
                      .flatMap(sqlPath -> sqlPath.getTableFlags().entrySet().stream())
                      .filter(entry2 -> syntax.getFilterFlag(entry2.getValue()).isPresent())
                      .map(
                          entry2 ->
                              new AbstractMap.SimpleImmutableEntry<>(
                                  entry2.getKey(), syntax.getFilterFlag(entry2.getValue()).get()))
                      .distinct()
                      .map(
                          entry2 ->
                              new AbstractMap.SimpleImmutableEntry<>(
                                  entry2.getKey(), cql.read(entry2.getValue(), Cql.Format.TEXT)))
                      .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

              if (isRoot) {
                ImmutableFeatureStoreInstanceContainer.Builder instanceContainerBuilder =
                    instanceContainerBuilders.get(instanceContainerName);

                // TODO: if multiple it should be different instance containers
                Optional<Cql2Expression> filter =
                    columnPaths.stream()
                        .flatMap(sqlPath -> sqlPath.getTableFlags().values().stream())
                        .map(syntax::getFilterFlag)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .distinct()
                        .map(filterText -> cql.read(filterText, Cql.Format.TEXT))
                        .findFirst();

                instanceContainerBuilder
                    .name(instanceContainerName)
                    .sortKey(syntax.getOptions().getSortKey())
                    .attributes(attributes)
                    .attributesPosition(instancePos[0])
                    .filter(filter);

                instancePos[0] = 0;
              } else {
                List<FeatureStoreRelation> instanceConnection =
                    toRelations(tablePathAsList, filters);
                String sortKey =
                    syntax.isJunctionTable(attributesContainerName)
                        // TODO: oneo uses columns.get(columns.size()-1) instead, thats not a good
                        // default value
                        // TODO: support flag {orderBy=btkomplex_id}{orderDir=ASC}
                        ? instanceConnection.get(instanceConnection.size() - 1).getTargetField()
                        : syntax.getOptions().getSortKey();

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
      List<String> path, Map<String, Cql2Expression> filters) {

    if (path.size() < 2) {
      throw new IllegalArgumentException(String.format("not a valid relation path: %s", path));
    }

    if (path.size() > 2) {
      return IntStream.range(2, path.size())
          .mapToObj(
              i ->
                  toRelations(
                      path.get(i - 2), path.get(i - 1), path.get(i), i == path.size() - 1, filters))
          .flatMap(Function.identity())
          .collect(Collectors.toList());
    }

    return IntStream.range(1, path.size())
        .mapToObj(i -> toRelation(path.get(i - 1), path.get(i), filters))
        .collect(Collectors.toList());
  }

  private Stream<FeatureStoreRelation> toRelations(
      String source,
      String link,
      String target,
      boolean isLast,
      Map<String, Cql2Expression> filters) {
    if (syntax.isJunctionTable(source)) {
      if (isLast) {
        return Stream.of(toRelation(link, target, filters));
      } else {
        return Stream.empty();
      }
    }
    if (syntax.isJunctionTable(target) && !isLast) {
      return Stream.of(toRelation(source, link, filters));
    }

    if (syntax.isJunctionTable(link)) {
      return Stream.of(toRelation(source, link, target));
    }

    return Stream.of(toRelation(source, link, filters), toRelation(link, target, filters));
  }

  // TODO: support sortKey flag on table instead of getDefaultPrimaryKey
  private FeatureStoreRelation toRelation(
      String source, String target, Map<String, Cql2Expression> filters) {
    Matcher sourceMatcher = syntax.getTablePattern().matcher(source);
    Matcher targetMatcher = syntax.getJoinedTablePattern().matcher(target);
    if (sourceMatcher.find() && targetMatcher.find()) {
      String sourceField = targetMatcher.group(SqlPathSyntax.MatcherGroups.SOURCE_FIELD);
      String targetField = targetMatcher.group(SqlPathSyntax.MatcherGroups.TARGET_FIELD);
      boolean isOne2One = Objects.equals(targetField, syntax.getOptions().getPrimaryKey());

      Optional<Cql2Expression> filter = Optional.ofNullable(filters.get(target));

      return ImmutableFeatureStoreRelation.builder()
          .cardinality(isOne2One ? CARDINALITY.ONE_2_ONE : CARDINALITY.ONE_2_N)
          .sourceContainer(sourceMatcher.group(SqlPathSyntax.MatcherGroups.TABLE))
          .sourceField(sourceField)
          .sourceSortKey(syntax.getOptions().getPrimaryKey())
          .targetContainer(targetMatcher.group(SqlPathSyntax.MatcherGroups.TABLE))
          .targetField(targetField)
          .filter(filter)
          .build();
    }

    throw new IllegalArgumentException(
        String.format("not a valid relation path: %s/%s", source, target));
  }

  private FeatureStoreRelation toRelation(String source, String link, String target) {
    Matcher sourceMatcher = syntax.getTablePattern().matcher(source);
    Matcher junctionMatcher = syntax.getJoinedTablePattern().matcher(link);
    Matcher targetMatcher = syntax.getJoinedTablePattern().matcher(target);
    if (sourceMatcher.find() && junctionMatcher.find() && targetMatcher.find()) {
      return ImmutableFeatureStoreRelation.builder()
          .cardinality(CARDINALITY.M_2_N)
          .sourceContainer(sourceMatcher.group(SqlPathSyntax.MatcherGroups.TABLE))
          .sourceField(junctionMatcher.group(SqlPathSyntax.MatcherGroups.SOURCE_FIELD))
          .sourceSortKey(syntax.getOptions().getPrimaryKey())
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
