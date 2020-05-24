/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableSchemaSql;
import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql;
import de.ii.xtraplatform.features.domain.FeatureStoreRelation;
import de.ii.xtraplatform.features.domain.ImmutableFeatureStoreRelation;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.features.domain.SchemaVisitor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class MutationSchemaBuilderSql implements SchemaVisitor<SchemaSql, SchemaSql> {

    @Override
    public SchemaSql visit(SchemaSql schema, List<SchemaSql> visitedProperties) {

        SchemaSql current = schema;
        SchemaSql newParent = null;
        List<SchemaSql> currentChildren = visitedProperties;

        Optional<SchemaSql> mainTable = visitedProperties.stream()
                                                         .filter(this::isMerge)
                                                         .filter(this::isMain)
                                                         .findFirst();

        if (mainTable.isPresent()) {
            newParent = switchRelation(current, mainTable.get(), currentChildren);
            current = newParent.getProperties()
                               .get(newParent.getProperties()
                                             .size() - 1);
            currentChildren = current.getProperties();
        }

        while (currentChildren.stream()
                              .anyMatch(this::isMerge)) {
            SchemaSql child = currentChildren.stream()
                                             .filter(this::isMerge)
                                             .findFirst()
                                             .get();

            SchemaSql switched = switchRelation(current, child, currentChildren);

            if (Objects.isNull(newParent)) {
                newParent = switched;
            } else {
                newParent = replaceChild(newParent, current, switched);
            }

            current = switched.getProperties()
                              .get(switched.getProperties()
                                           .size() - 1);
            currentChildren = current.getProperties();
        }

        return Optional.ofNullable(newParent)
                       .orElse(schema);
    }

    private SchemaSql replaceChild(SchemaSql parent, SchemaSql child, SchemaSql newChild) {
        List<SchemaSql> newChildren = parent.getProperties()
                                            .stream()
                                            .map(schemaSql -> {
                                                if (Objects.equals(schemaSql, child)) {
                                                    return newChild;
                                                }
                                                return schemaSql;
                                            })
                                            .collect(Collectors.toList());

        return new ImmutableSchemaSql.Builder().from(parent)
                                               .properties(newChildren)
                                               .build();
    }

    private SchemaSql switchRelation(SchemaSql parent, SchemaSql child, List<SchemaSql> allChildren) {
        FeatureStoreRelation childRelation = child.getRelation()
                                                  .get();

        //TODO: rebuild schema without mainTable and with reverse relation
        FeatureStoreRelation newChildRelation = ImmutableFeatureStoreRelation.builder()
                                                                             .from(childRelation)
                                                                             .sourceContainer(childRelation.getTargetContainer())
                                                                             .sourceField(childRelation.getTargetField())
                                                                             .targetContainer(childRelation.getSourceContainer())
                                                                             .targetField(childRelation.getSourceField())
                                                                             .build();

        List<SchemaSql> newProperties = allChildren.stream()
                                                   .filter(property -> !Objects.equals(property, child))
                                                   .map(property -> replaceInParentPath(1, newChildRelation.asPath(), property))
                                                   .collect(Collectors.toList());


        SchemaSql newChild = new ImmutableSchemaSql.Builder().from(parent)
                                                             .properties(newProperties)
                                                             .relation(newChildRelation)
                                                             .sourcePath(child.getSourcePath())
                                                             .build();

        List<String> newChildPath = parent.getRelation().isPresent()
                ? Lists.newArrayList(Iterables.concat(parent.getParentPath(), childRelation.asPath()))
                : ImmutableList.of(child.getName());

        newChild = replaceInParentPath(parent.getRelation().isPresent() ? 1 : 0, newChildPath, newChild);

        //TODO: rebuild mainTable without relation, change nested parentPaths

        //TODO: make schema child of mainTable, change nested parentPaths

        Optional<FeatureStoreRelation> newParentRelation = parent.getRelation()
                                                                 .isPresent()
                ? Optional.of(ImmutableFeatureStoreRelation.builder()
                                                           .from(parent.getRelation().get())
                                                           .sourceContainer(parent.getRelation().get().getSourceContainer())
                                                           .sourceField(parent.getRelation().get().getSourceField())
                                                           .targetContainer(childRelation.getTargetContainer())
                                                           .targetField(childRelation.getTargetField())
                                                           .build())
                : parent.getRelation();

        List<String> newParentPath = newParentRelation.isPresent()
                ? parent.getParentPath()
                : ImmutableList.of(child.getName());

        SchemaSql newParent = replaceInParentPath(2, newParentPath, child);

        newParent = new ImmutableSchemaSql.Builder().from(newParent)
                                                    .addProperties(newChild)
                                                    .parentPath(newParentRelation.isPresent() ? newParentPath : ImmutableList.of())
                                                    .relation(newParentRelation)
                                                    .sourcePath(parent.getSourcePath())
                                                    .build();


        return newParent;
    }

    private boolean isMain(SchemaSql schemaSql) {
        return schemaSql.getProperties()
                        .stream()
                        .anyMatch(property -> property.getRole()
                                                      .isPresent() && property.getRole()
                                                                              .get() == SchemaBase.Role.ID);
    }

    //TODO
    private boolean isMerge(SchemaSql schemaSql) {
        return schemaSql.getRelation()
                        .isPresent()
                && schemaSql.getRelation()
                            .get()
                            .isOne2One()
                && Objects.equals(schemaSql.getRelation()
                                           .get()
                                           .getSourceSortKey(), schemaSql.getRelation()
                                                                         .get()
                                                                         .getSourceField());
    }

    public SchemaSql prependToParentPath(List<String> path, SchemaSql schema) {
        return new ImmutableSchemaSql.Builder().from(schema)
                                               .parentPath(path)
                                               .addAllParentPath(schema.getParentPath())
                                               .properties(schema.getProperties()
                                                                 .stream()
                                                                 .map(prop -> prependToParentPath(path, prop))
                                                                 .collect(Collectors.toList()))
                                               .build();
    }

    public SchemaSql replaceInParentPath(int numElemsFromStart, List<String> to, SchemaSql schema) {

        List<String> newParentPath = numElemsFromStart > schema.getParentPath()
                                                               .size()
                ? schema.getParentPath()
                : ImmutableList.<String>builder().addAll(to)
                                                 .addAll(schema.getParentPath()
                                                               .subList(numElemsFromStart, schema.getParentPath()
                                                                                                 .size()))
                                                 .build();
        return new ImmutableSchemaSql.Builder().from(schema)
                                               .parentPath(newParentPath)
                                               .properties(schema.getProperties()
                                                                 .stream()
                                                                 .map(prop -> replaceInParentPath(numElemsFromStart, to, prop))
                                                                 .collect(Collectors.toList()))
                                               .build();
    }
}
