/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableSchemaSql;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableSchemaSql.Builder;
import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlPath;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlPathParser;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlRelation;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.MappedSchemaDeriver;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QuerySchemaDeriver implements MappedSchemaDeriver<SchemaSql, SqlPath> {
  private final SqlPathParser pathParser;

  public QuerySchemaDeriver(SqlPathParser pathParser) {
    this.pathParser = pathParser;
  }

  @Override
  public List<SqlPath> parseSourcePaths(FeatureSchema sourceSchema) {
    return sourceSchema.getSourcePaths()
        .stream()
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
      List<SqlPath> parentPaths) {

    List<String> fullParentPath =
        targetSchema.isObject()
            ? parentPaths.isEmpty()
              ? ImmutableList.of()
              : parentPaths.get(0).getFullPath()
            : parentPaths.stream()
              .flatMap(sqlPath -> sqlPath.getFullPath().stream())
              .collect(Collectors.toList());

    List<SqlRelation> relations =
        parentPaths.isEmpty()
            ? ImmutableList.of()
            : pathParser.extractRelations(parentPaths.get(parentPaths.size() - 1), path);

    Map<List<SqlRelation>, List<SchemaSql>> propertiesGroupedByRelation = visitedProperties.stream()
        .collect(Collectors.groupingBy(SchemaSql::getRelation,
            LinkedHashMap::new, Collectors.toList()));

    List<SchemaSql> newVisitedProperties = propertiesGroupedByRelation.entrySet().stream()
        .flatMap(entry -> {
          if (entry.getKey().isEmpty()) {
            return entry.getValue().stream()
                .map(prop -> targetSchema.isFeature() ? prop : new Builder().from(prop)
                    .sourcePath(prop.getSourcePath().map(sourcePath -> targetSchema.getName() + "." + sourcePath))
                    .sourcePaths(prop.getSourcePaths()
                        .stream()
                        .map(sourcePath -> targetSchema.getName() + "." + sourcePath)
                        .collect(Collectors.toList()))
                  .build());
          }

          if (entry.getValue().stream().noneMatch(SchemaBase::isValue)) {
            List<SqlPath> pp = parentPaths;
            SqlPath p = path;

            //TODO: recurse into properties, change sourcePaths
            return entry.getValue()
                .stream()
                .map(prop -> new Builder().from(prop)
                    .relation(ImmutableList.of())
                    .addAllRelation(relations)
                    .addAllRelation(entry.getKey())
                    .sourcePath(targetSchema.isFeature() ? prop.getSourcePath() : prop.getSourcePath().map(sourcePath -> targetSchema.getName() + "." + sourcePath))
                    .sourcePaths(targetSchema.isFeature() ? prop.getSourcePaths() : prop.getSourcePaths()
                        .stream()
                        .map(sourcePath -> targetSchema.getName() + "." + sourcePath)
                        .collect(Collectors.toList()))
                    .build());
          }

          List<String> newParentPath = entry.getKey()
              .stream()
              .flatMap(rel -> rel.asPath().stream())
              .collect(Collectors.toList());

          List<ImmutableSchemaSql> newProperties = entry.getValue()
              .stream()
              .map(prop -> new Builder().from(prop)
                  .type(prop.getValueType().orElse(prop.getType()))
                  .valueType(Optional.empty())
                  .addAllParentPath(newParentPath)
                  .relation(ImmutableList.of())
                  .build())
              .collect(Collectors.toList());

          boolean isArray = entry.getValue()
              .stream()
              .anyMatch(SchemaBase::isArray);

          SqlPath tablePath = pathParser.parseTablePath(newParentPath.get(newParentPath.size() - 1));

          return Stream.of(new Builder()
              .name(entry.getKey().get(entry.getKey().size()-1).getTargetContainer())
              .type(isArray ? Type.OBJECT_ARRAY : Type.OBJECT)
              .parentPath(entry.getValue().get(0).getParentPath())
              .addAllRelation(relations)
              .addAllRelation(entry.getKey())
              .properties(newProperties)
              .sortKey(tablePath.getSortKey())
              .primaryKey(tablePath.getPrimaryKey())
              .build());
        })
        .collect(Collectors.toList());

    Builder builder =
          new Builder()
              .name(path.getName())
              .parentPath(fullParentPath)
              .type(targetSchema.getType())
              .valueType(targetSchema.getValueType())
              .geometryType(targetSchema.getGeometryType())
              .role(targetSchema.getRole())
              .sourcePath(targetSchema.getName())
              .relation(relations)
              .properties(newVisitedProperties)
              .constantValue(targetSchema.getConstantValue())
              .forcePolygonCCW(targetSchema.getForcePolygonCCW());

      if (targetSchema.isObject()) {
        builder
            .sortKey(path.getSortKey())
            .primaryKey(path.getPrimaryKey())
            .filter(path.getFilter())
            .filterString(path.getFilterString());
      }

    return builder.build();
  }

  @Override
  public List<SchemaSql> merge(
      FeatureSchema targetSchema, SqlPath parentPath, List<SchemaSql> visitedProperties) {
    return visitedProperties.stream()
        .map(property -> new Builder()
            .from(property)
            .sourcePath(property.getSourcePath().map(sourcePath -> targetSchema.getName() + "." + sourcePath))
            .sourcePaths(property.getSourcePaths()
                .stream()
                .map(sourcePath -> targetSchema.getName() + "." + sourcePath)
                .collect(Collectors.toList()))
            .build())
        .collect(Collectors.toList());
  }
}
