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
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaBase.Type;
import de.ii.xtraplatform.features.domain.SchemaDeriver;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QuerySchemaDeriver implements SchemaDeriver<SchemaSql, SqlPath> {
  private final SqlPathParser pathParser;

  public QuerySchemaDeriver(SqlPathParser pathParser) {
    this.pathParser = pathParser;
  }

  @Override
  public Optional<SqlPath> parseSourcePath(FeatureSchema sourceSchema) {
    return sourceSchema
        .getSourcePath()
        .map(
            sourcePath ->
                sourceSchema.isValue()
                    ? pathParser.parseColumnPath(sourcePath)
                    : pathParser.parseTablePath(sourcePath));
  }

  @Override
  public SchemaSql create(
      FeatureSchema targetSchema,
      SqlPath path,
      List<SchemaSql> visitedProperties,
      List<SqlPath> parentPaths) {

    Tuple<Stream<SchemaSql>, Stream<SchemaSql>> splitProperties =
        splitStream(visitedProperties, SchemaBase::isValue);
    List<SchemaSql> valueProperties = splitProperties.first()
        .collect(Collectors.groupingBy(SchemaSql::getRelation))
        .entrySet()
        .stream()
        .flatMap(entry -> entry.getValue().stream())
        .collect(Collectors.toList());
    List<SchemaSql> objectProperties = splitProperties.second().collect(Collectors.toList());

    //List<SchemaSql> finalProperties = Stream.concat()

    List<String> fullParentPath =
        parentPaths.stream()
            .flatMap(sqlPath -> sqlPath.getFullPath().stream())
            .collect(Collectors.toList());

    List<SqlRelation> relations =
        parentPaths.isEmpty()
            ? ImmutableList.of()
            : pathParser.extractRelations(parentPaths.get(parentPaths.size() - 1), path);

    Builder builder;

    // TODO: walk over visitedProperties, on columns with matching relations create common parent

    if (targetSchema.getType() == Type.VALUE_ARRAY
        && targetSchema.getValueType().isPresent()
        && !relations.isEmpty()) {
      String propertyParentPath =
          relations
              .get(relations.size() - 1)
              .asPath()
              .get(relations.get(relations.size() - 1).asPath().size() - 1);

      List<SchemaSql> properties =
          ImmutableList.of(
              new Builder()
                  .name(path.getName())
                  .parentPath(fullParentPath)
                  .addParentPath(propertyParentPath)
                  .type(targetSchema.getValueType().get())
                  .geometryType(targetSchema.getGeometryType())
                  .role(targetSchema.getRole())
                  .sourcePath(targetSchema.getName())
                  .build());

      SqlPath tablePath = path.getParentTables().get(path.getParentTables().size() - 1);

      builder =
          new Builder()
              .name(tablePath.getName())
              .parentPath(fullParentPath)
              .type(targetSchema.getType())
              .relation(relations)
              .properties(properties)
              .sortKey(tablePath.getSortKey())
              .primaryKey(tablePath.getPrimaryKey())
              .filter(tablePath.getFilter());
    } else {
      builder =
          new Builder()
              .name(path.getName())
              .parentPath(fullParentPath)
              .type(targetSchema.getType())
              .valueType(targetSchema.getValueType())
              .geometryType(targetSchema.getGeometryType())
              .role(targetSchema.getRole())
              .sourcePath(targetSchema.getName())
              .relation(relations)
              .properties(visitedProperties);

      if (targetSchema.isObject() || targetSchema.isArray()) {
        builder
            .sortKey(path.getSortKey())
            .primaryKey(path.getPrimaryKey())
            .filter(path.getFilter());
      }
    }

    return builder.build();
  }

  @Override
  public SchemaSql merge(
      FeatureSchema targetSchema, SqlPath parentPath, List<SchemaSql> visitedProperties) {
    return null;
  }

  private SchemaSql mergeChild(SchemaSql parent, FeatureSchema child) {
    return new ImmutableSchemaSql.Builder()
        .from(parent)
        .sourcePath(parent.getSourcePath() + ". " + child.getName())
        .build();
  }

  private SchemaSql createParent(List<SqlRelation> relations, List<SchemaSql> children) {
    if (children.isEmpty()) {
      throw new IllegalArgumentException();
    }

    return null;
  }

  private <T> Tuple<Stream<T>, Stream<T>> splitStream(List<T> list, Predicate<T> predicate) {
    return ImmutableTuple.of(
        list.stream().filter(predicate), list.stream().filter(Predicate.not(predicate)));
  }
}
