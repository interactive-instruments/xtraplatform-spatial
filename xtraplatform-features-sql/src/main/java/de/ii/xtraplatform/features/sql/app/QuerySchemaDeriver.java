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
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.MappedSchemaDeriver;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.sql.domain.ImmutableSchemaSql.Builder;
import de.ii.xtraplatform.features.sql.domain.ImmutableSqlRelation;
import de.ii.xtraplatform.features.sql.domain.SchemaSql;
import de.ii.xtraplatform.features.sql.domain.SchemaSql.PropertyTypeInfo;
import de.ii.xtraplatform.features.sql.domain.SqlPath;
import de.ii.xtraplatform.features.sql.domain.SqlPathParser;
import de.ii.xtraplatform.features.sql.domain.SqlRelation;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QuerySchemaDeriver implements MappedSchemaDeriver<SchemaSql, SqlPath> {

  private final SqlPathParser pathParser;

  public QuerySchemaDeriver(SqlPathParser pathParser) {
    this.pathParser = pathParser;
  }

  @Override
  public List<SqlPath> parseSourcePaths(FeatureSchema sourceSchema, List<List<SqlPath>> parents) {
    boolean inConnector =
        parents.stream()
            .map(list -> list.stream().filter(p -> p.getConnector().isPresent()).findFirst())
            .anyMatch(Optional::isPresent);

    if (inConnector) {
      return sourceSchema.getEffectiveSourcePaths().stream()
          .map(pathParser::parseColumnPath)
          .collect(Collectors.toList());
    }

    if (sourceSchema.getType() == Type.OBJECT_ARRAY && !sourceSchema.getConcat().isEmpty()) {
      return List.of();
    }

    return sourceSchema.getEffectiveSourcePaths().stream()
        .map(
            sourcePath ->
                sourceSchema.isValue()
                    ? pathParser.parseColumnPath(sourcePath)
                    : pathParser.parseTablePath(sourcePath))
        .collect(Collectors.toList());
  }

  @Override
  public SchemaSql create(
      FeatureSchema targetSchema,
      SqlPath path,
      List<SchemaSql> visitedProperties,
      List<SqlPath> parentPaths,
      boolean nestedArray) {

    String fullSchemaPath = targetSchema.getFullPathAsString();

    List<String> fullParentPath =
        parentPaths.stream()
            .flatMap(sqlPath -> sqlPath.getFullPath().stream())
            .collect(Collectors.toList());

    List<SqlRelation> relations =
        parentPaths.isEmpty()
            ? ImmutableList.of()
            : pathParser.extractRelations(parentPaths.get(parentPaths.size() - 1), path);

    Optional<String> connector = path.getConnector();

    List<String> sortKeys =
        Stream.concat(
                relations.stream()
                    .filter(relation -> relation.getSourceSortKey().isPresent())
                    .map(
                        relation ->
                            String.format(
                                "%s.%s",
                                relation.getSourceContainer(), relation.getSourceSortKey().get())),
                targetSchema.isObject()
                        && targetSchema.getProperties().stream()
                            .anyMatch(
                                prop -> prop.isValue() || prop.getEffectiveSourcePaths().isEmpty())
                    ? Stream.of(String.format("%s.%s", path.getName(), path.getSortKey()))
                    : Stream.empty())
            .collect(Collectors.toList());

    List<String> parentSortKeys =
        path.getParentTables().stream().map(SqlPath::getSortKey).collect(Collectors.toList());

    Map<List<SqlRelation>, List<SchemaSql>> propertiesGroupedByRelation =
        visitedProperties.stream()
            .collect(
                Collectors.groupingBy(
                    SchemaSql::getRelation, LinkedHashMap::new, Collectors.toList()));

    Map<List<SqlRelation>, List<SchemaSql>> propertiesGroupedByRelation2 =
        propertiesGroupedByRelation.entrySet().stream()
            .flatMap(
                entry -> {
                  List<SqlRelation> relation1 = entry.getKey();
                  List<SchemaSql> mergeable =
                      propertiesGroupedByRelation.entrySet().stream()
                          .filter(
                              entry2 ->
                                  relation1.size() > 0
                                      && relation1.size() < entry2.getKey().size()
                                      && Objects.equals(
                                          relation1, entry2.getKey().subList(0, relation1.size())))
                          .flatMap(entry2 -> entry2.getValue().stream())
                          .map(
                              prop ->
                                  new Builder()
                                      .from(prop)
                                      .relation(
                                          prop.getRelation()
                                              .subList(relation1.size(), prop.getRelation().size()))
                                      .addAllParentPath(
                                          prop.getRelation().subList(0, relation1.size()).stream()
                                              .flatMap(s -> s.asPath().stream())
                                              .collect(Collectors.toList()))
                                      .build())
                          .collect(Collectors.toList());

                  if (!mergeable.isEmpty()) {
                    List<SchemaSql> newProps =
                        Stream.concat(entry.getValue().stream(), mergeable.stream())
                            .collect(Collectors.toList());

                    return Stream.of(new SimpleImmutableEntry<>(relation1, newProps));
                  } else if (propertiesGroupedByRelation.keySet().stream()
                      .anyMatch(
                          relation2 ->
                              relation2.size() > 0
                                  && relation2.size() < relation1.size()
                                  && Objects.equals(
                                      relation2, relation1.subList(0, relation2.size())))) {
                    return Stream.empty();
                  }

                  return Stream.of(new SimpleImmutableEntry<>(relation1, entry.getValue()));
                })
            .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));

    List<SchemaSql> newVisitedProperties =
        propertiesGroupedByRelation2.entrySet().stream()
            .flatMap(
                entry -> {
                  if (entry.getKey().isEmpty()) {
                    return entry.getValue().stream()
                        .map(
                            prop ->
                                targetSchema.isFeature() || prop.getSubDecoder().isPresent()
                                    ? prop
                                    : new Builder()
                                        .from(prop)
                                        .sourcePath(
                                            prop.getSourcePath()
                                                .map(
                                                    sourcePath ->
                                                        targetSchema.getName() + "." + sourcePath))
                                        .sourcePaths(
                                            prop.getSourcePaths().stream()
                                                .map(
                                                    sourcePath ->
                                                        targetSchema.getName() + "." + sourcePath)
                                                .collect(Collectors.toList()))
                                        .build());
                  }

                  if (entry.getValue().stream().noneMatch(SchemaBase::isValue)) {
                    boolean hasValueSiblings =
                        visitedProperties.stream().anyMatch(SchemaBase::isValue)
                            && entry.getKey().size() == 1;
                    List<SqlRelation> childRelations =
                        hasValueSiblings
                            ? entry.getKey()
                            : !relations.isEmpty()
                                ? entry.getKey().stream()
                                    .map(
                                        rel ->
                                            new ImmutableSqlRelation.Builder()
                                                .from(rel)
                                                .sourceSortKey(Optional.empty())
                                                .sourcePrimaryKey(Optional.empty())
                                                .build())
                                    .collect(Collectors.toList())
                                : entry.getKey().size() > 1
                                    ? Stream.concat(
                                            Stream.of(entry.getKey().get(0)),
                                            entry
                                                .getKey()
                                                .subList(1, entry.getKey().size())
                                                .stream()
                                                .map(
                                                    rel ->
                                                        new ImmutableSqlRelation.Builder()
                                                            .from(rel)
                                                            .sourceSortKey(Optional.empty())
                                                            .sourcePrimaryKey(Optional.empty())
                                                            .build()))
                                        .collect(Collectors.toList())
                                    : entry.getKey();

                    List<String> childSortKeys =
                        Stream.concat(
                                sortKeys.stream(),
                                childRelations.stream()
                                    .filter(relation -> relation.getSourceSortKey().isPresent())
                                    .map(
                                        relation ->
                                            String.format(
                                                "%s.%s",
                                                relation.getSourceContainer(),
                                                relation.getSourceSortKey().get())))
                            .distinct()
                            .collect(Collectors.toList());

                    return entry.getValue().stream()
                        .map(
                            prop ->
                                new Builder()
                                    .from(prop)
                                    .relation(ImmutableList.of())
                                    // .addAllRelation(relations)
                                    .addAllRelation(childRelations)
                                    .parentSortKeys(sortKeys)
                                    .sourcePath(
                                        targetSchema.isFeature()
                                            ? prop.getSourcePath()
                                            : prop.getSourcePath()
                                                .map(
                                                    sourcePath ->
                                                        targetSchema.getName() + "." + sourcePath))
                                    .sourcePaths(
                                        targetSchema.isFeature()
                                            ? prop.getSourcePaths()
                                            : prop.getSourcePaths().stream()
                                                .map(
                                                    sourcePath ->
                                                        targetSchema.getName() + "." + sourcePath)
                                                .collect(Collectors.toList()))
                                    .properties(
                                        adjustParentSortKeys(
                                            targetSchema.isFeature()
                                                ? prop.getProperties()
                                                : prefixSourcePath(
                                                    prop.getProperties(),
                                                    targetSchema.getName() + "."),
                                            childSortKeys))
                                    .build());
                  }

                  List<String> newParentPath =
                      entry.getKey().stream()
                          .flatMap(rel -> rel.asPath().stream())
                          .collect(Collectors.toList());

                  List<SchemaSql> newProperties =
                      entry.getValue().stream()
                          .flatMap(
                              prop -> {
                                if (Objects.equals(newParentPath, prop.getPath())
                                    && prop.isObject()) {
                                  return prop.getProperties().stream();
                                }
                                if (prop.isObject() && prop.isArray()) {
                                  return Stream.of(
                                      new Builder().from(prop).parentSortKeys(sortKeys).build());
                                }
                                return Stream.of(
                                    new Builder()
                                        .from(prop)
                                        .type(prop.getValueType().orElse(prop.getType()))
                                        .valueType(Optional.empty())
                                        .addAllParentPath(newParentPath)
                                        .relation(ImmutableList.of())
                                        .sourcePath(
                                            prop.getSourcePath()
                                                .map(
                                                    sourcePath ->
                                                        !targetSchema.isFeature()
                                                            ? targetSchema.getName()
                                                                + "."
                                                                + sourcePath
                                                            : sourcePath))
                                        .sourcePaths(
                                            prop.getSourcePaths().stream()
                                                .map(
                                                    sourcePath ->
                                                        !targetSchema.isFeature()
                                                            ? targetSchema.getName()
                                                                + "."
                                                                + sourcePath
                                                            : sourcePath)
                                                .collect(Collectors.toList()))
                                        .build());
                              })
                          .collect(Collectors.toList());

                  boolean isArray = entry.getValue().stream().anyMatch(SchemaBase::isArray);

                  SqlPath tablePath =
                      entry.getValue().get(0).getSortKey().isPresent()
                          ? pathParser.parseTablePath(
                              newParentPath.get(newParentPath.size() - 1)
                                  + "{sortKey="
                                  + entry.getValue().get(0).getSortKey().get()
                                  + "}")
                          : pathParser.parseTablePath(newParentPath.get(newParentPath.size() - 1));

                  return Stream.of(
                      new Builder()
                          .name(entry.getKey().get(entry.getKey().size() - 1).getTargetContainer())
                          .type(isArray ? Type.OBJECT_ARRAY : Type.OBJECT)
                          .parentPath(entry.getValue().get(0).getParentPath())
                          .parentSortKeys(sortKeys)
                          // .addAllRelation(relations)
                          .addAllRelation(entry.getKey())
                          .properties(newProperties)
                          .sortKey(tablePath.getSortKey())
                          .sortKeyUnique(tablePath.getSortKeyUnique())
                          .primaryKey(tablePath.getPrimaryKey())
                          .sourcePath(
                              !targetSchema.isFeature()
                                  ? Optional.of(targetSchema.getName())
                                  : Optional.empty())
                          .build());
                })
            .collect(Collectors.toList());

    Set<String> connected = new HashSet<>();
    Map<String, Map<String, String>> subConnectorPaths = new HashMap<>();
    Map<String, Map<String, PropertyTypeInfo>> subConnectorTypes = new HashMap<>();

    newVisitedProperties.forEach(
        prop -> {
          String jsonColumn = null;
          Map<String, String> subPaths = null;
          Map<String, PropertyTypeInfo> subTypes = null;
          if (prop.getSubDecoder().isPresent()) {
            jsonColumn = prop.getName();
            subPaths = prop.getSubDecoderPaths();
            subTypes = prop.getSubDecoderTypes();
          } else if (prop.getFullPathAsString().matches(".+?\\[[^=\\]]+].+")) {
            String fullPath = prop.getFullPathAsString();
            jsonColumn = fullPath.substring(fullPath.indexOf("[JSON]") + 6);
            if (jsonColumn.contains(".")) {
              jsonColumn = jsonColumn.substring(0, jsonColumn.indexOf('.'));
            }
            if (prop.getSourcePath().isPresent()) {
              subPaths =
                  ImmutableMap.of(
                      prop.getSourcePath().get(),
                      prop.getSourcePath().get().replace(path.getName() + ".", ""));
              subTypes =
                  ImmutableMap.of(
                      prop.getSourcePath().get(),
                      getTypeInfo(prop, nestedArray || targetSchema.isArray()));
            }
          }
          if (Objects.nonNull(subPaths)) {
            if (!subConnectorPaths.containsKey(jsonColumn)) {
              subConnectorPaths.put(jsonColumn, new HashMap<>());
            }
            if (!subConnectorTypes.containsKey(jsonColumn)) {
              subConnectorTypes.put(jsonColumn, new HashMap<>());
            }
            String finalJsonColumn = jsonColumn;
            subPaths.forEach((q, p) -> subConnectorPaths.get(finalJsonColumn).put(q, p));
            subTypes.forEach((q, p) -> subConnectorTypes.get(finalJsonColumn).put(q, p));
          }
        });

    List<SchemaSql> newVisitedProperties2 =
        newVisitedProperties.stream()
            .flatMap(
                prop -> {
                  if (prop.getSubDecoder().isEmpty()) {
                    return Stream.of(prop);
                  }

                  boolean isFirst = connected.add(prop.getSubDecoder().get() + prop.getName());

                  return isFirst
                      ? Stream.of(
                          new Builder()
                              .from(prop)
                              .sourcePath(prop.getName())
                              .primaryKey(Optional.empty())
                              .sortKey(Optional.empty())
                              .subDecoderPaths(subConnectorPaths.get(prop.getName()))
                              .subDecoderTypes(subConnectorTypes.get(prop.getName()))
                              .build())
                      : Stream.empty();
                })
            .collect(Collectors.toList());

    Optional<String> sortKey =
        parentSortKeys.isEmpty() || !targetSchema.isValue()
            ? Optional.empty()
            : Optional.of(parentSortKeys.get(parentSortKeys.size() - 1));

    Type type = connector.isPresent() ? Type.STRING : targetSchema.getType();
    Optional<Type> valueType = targetSchema.getValueType();

    if (!targetSchema.getConcat().isEmpty()) {
      if (!relations.isEmpty()) {
        sortKey = Optional.empty();
      } else if (type == Type.VALUE_ARRAY) {
        type = valueType.orElse(Type.STRING);
        valueType = Optional.empty();
      }
    }

    Map<String, String> subConnectorPaths1 =
        Objects.requireNonNullElse(subConnectorPaths.get(path.getName()), Map.of());
    Map<String, String> subDecoderPaths =
        connector.isPresent()
            ? (subConnectorPaths1.isEmpty()
                ? (path.getPathInConnector().isPresent()
                    ? ImmutableMap.of(fullSchemaPath, path.getPathInConnector().get())
                    : Map.of())
                : subConnectorPaths1)
            : Map.of();
    Map<String, PropertyTypeInfo> subConnectorTypes1 =
        Objects.requireNonNullElse(subConnectorTypes.get(path.getName()), Map.of());
    Map<String, PropertyTypeInfo> subDecoderTypes =
        connector.isPresent()
            ? (subConnectorTypes1.isEmpty()
                ? ImmutableMap.of(
                    fullSchemaPath,
                    PropertyTypeInfo.of(
                        targetSchema.getType(), targetSchema.getValueType(), nestedArray))
                : subConnectorTypes1)
            : Map.of();

    Builder builder =
        new Builder()
            .name(path.getName())
            .parentPath(fullParentPath)
            .sortKey(sortKey)
            .type(type)
            .valueType(valueType)
            .geometryType(targetSchema.getGeometryType())
            .role(targetSchema.getRole())
            .format(targetSchema.getFormat())
            .excludedScopes(targetSchema.getExcludedScopes())
            .sourcePath(targetSchema.getName())
            .relation(relations)
            .subDecoder(connector)
            .subDecoderPaths(subDecoderPaths)
            .subDecoderTypes(subDecoderTypes)
            .isExpression(
                connector.isPresent()
                    && Objects.equals(
                        connector.get(), DecoderFactorySqlExpression.CONNECTOR_STRING))
            .properties(connector.isPresent() ? List.of() : newVisitedProperties2)
            .constantValue(targetSchema.getConstantValue())
            .forcePolygonCCW(
                targetSchema.isForcePolygonCCW() ? Optional.empty() : Optional.of(false))
            .linearizeCurves(
                targetSchema.shouldLinearizeCurves() ? Optional.of(true) : Optional.empty())
            .constraints(targetSchema.getConstraints());

    if (targetSchema.isObject()) {
      if (targetSchema.getProperties().stream()
          .anyMatch(prop -> prop.isValue() || prop.getEffectiveSourcePaths().isEmpty())) {
        builder
            .sortKey(path.getSortKey())
            .sortKeyUnique(path.getSortKeyUnique())
            .primaryKey(path.getPrimaryKey());
      }
      builder.filter(path.getFilter()).filterString(path.getFilterString());
    }

    return builder.build();
  }

  private PropertyTypeInfo getTypeInfo(SchemaSql column, boolean nestedArray) {
    return PropertyTypeInfo.of(column.getType(), column.getValueType(), nestedArray);
  }

  @Override
  public List<SchemaSql> merge(
      FeatureSchema targetSchema, List<String> parentPath, List<SchemaSql> visitedProperties) {
    return visitedProperties.stream()
        .map(
            property ->
                new Builder()
                    .from(property)
                    .sourcePath(
                        property
                            .getSourcePath()
                            .map(sourcePath -> targetSchema.getName() + "." + sourcePath))
                    .sourcePaths(
                        property.getSourcePaths().stream()
                            .map(sourcePath -> targetSchema.getName() + "." + sourcePath)
                            .collect(Collectors.toList()))
                    .build())
        .collect(Collectors.toList());
  }

  private List<SchemaSql> prefixSourcePath(List<SchemaSql> schemas, String prefix) {
    return schemas.stream()
        .map(
            schema ->
                new Builder()
                    .from(schema)
                    .sourcePath(schema.getSourcePath().map(sourcePath -> prefix + sourcePath))
                    .sourcePaths(
                        schema.getSourcePaths().stream()
                            .map(sourcePath -> prefix + sourcePath)
                            .collect(Collectors.toList()))
                    .build())
        .collect(Collectors.toList());
  }

  private List<SchemaSql> adjustParentSortKeys(
      List<SchemaSql> schemas, List<String> parentSortKeys) {
    ArrayList<String> keys = new ArrayList<>(parentSortKeys);

    return schemas.stream()
        .map(
            schema ->
                schema.isObject()
                    ? new Builder().from(schema).parentSortKeys(keys).build()
                    : schema)
        .collect(Collectors.toList());
  }
}
