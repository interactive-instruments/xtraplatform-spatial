package de.ii.xtraplatform.feature.provider.sql.app;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.feature.provider.sql.SqlPath;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableSchemaSql;
import de.ii.xtraplatform.feature.provider.sql.domain.SchemaSql;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureStoreRelation;
import de.ii.xtraplatform.features.domain.ReverseSchemaBuilder;
import de.ii.xtraplatform.features.domain.SchemaBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SchemaBuilderSql implements ReverseSchemaBuilder<SchemaSql> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReverseSchemaBuilder.class);

    private static final Joiner JOINER = Joiner.on('/')
                                               .skipNulls();

    private final PathParserSql pathParser;

    public SchemaBuilderSql(PathParserSql pathParser) {
        this.pathParser = pathParser;
    }

    @Override
    public SchemaSql create(List<String> path, FeatureSchema targetSchema) {
        LOGGER.debug("{} {}", targetSchema.isObject() ? "OBJECT" : "VALUE", path);
        return new ImmutableSchemaSql.Builder()
                .name(path.get(path.size() - 1))
                .parentPath(path.subList(0, path.size() - 1))
                .type(targetSchema.getType())
                .valueType(targetSchema.getValueType())
                .geometryType(targetSchema.getGeometryType())
                .role(targetSchema.getRole())
                .target(targetSchema.getFullPath())
                .sourcePath(targetSchema.getName())
                .build();
    }

    @Override
    public List<SchemaSql> createParents(String parentParentPath, SchemaSql child, Map<List<String>, SchemaSql> objectCache) {

        Optional<SqlPath> sqlPath = pathParser.parse(parentParentPath + "/" + JOINER.join(child.getFullPath()), child.isValue());

        //TODO: column?
        if (!sqlPath.isPresent()) {
            throw new IllegalArgumentException();
        }

        List<String> tablePathAsList = ReverseSchemaBuilder.SPLITTER.splitToList(sqlPath.get()
                                                                                        .getTablePath());

        boolean hasRelation = tablePathAsList.size() > 1;//(isRoot ? 1 : 0);

        List<FeatureStoreRelation> relations = hasRelation ? pathParser.toRelations(tablePathAsList, ImmutableMap.of()) : ImmutableList.of();

        SchemaSql currentChild = child;

        List<SchemaSql> parents = new ArrayList<>();

        for (int i = relations.size() - 1; i >= 0; i--) {
            FeatureStoreRelation relation = relations.get(i);

            SchemaBase.Type type = relation.isOne2One()
                    ? SchemaBase.Type.OBJECT
                    : SchemaBase.Type.OBJECT_ARRAY;

            boolean replace = currentChild.isObject() && parents.isEmpty();

            SchemaSql parent = objectCache.computeIfAbsent(relation.asPath(), ignore -> createParent(relation, type, child, replace));

            currentChild = replace ? parent : addChild(parent, currentChild);

            parents.add(0, currentChild);
        }

        return parents;
    }

    private SchemaSql createParent(FeatureStoreRelation relation, SchemaBase.Type type, SchemaSql child, boolean replace) {
        LOGGER.debug("OBJECT {}", relation);

        List<String> targetPath = (List<String>) child.getTarget()
                                                      .get();
        ImmutableSchemaSql.Builder builder = new ImmutableSchemaSql.Builder()
                .name(relation.getTargetContainer())
                .type(type)
                .relation(relation)
                .parentPath(child.getParentPath()
                                 .subList(0, child.getParentPath()
                                                  .size() - 1))
                .target(targetPath.subList(0, targetPath.size() - 1))
                .sourcePath("");

        if (replace) {
            builder.parentPath(child.getParentPath()
                                    .subList(0, child.getParentPath()
                                                     .size() - 1))
                   .properties(child.getProperties())
                   .sourcePath(child.getSourcePath())
                   .target(child.getTarget());
        }

        return builder.build();
    }

    private SchemaSql addChild(SchemaSql parent, SchemaSql child) {
        LOGGER.debug("CHILD {} {}", parent.getName(), child.getName());

        SchemaSql toAdd = child;

        if (child.getValueType()
                 .isPresent()) {
            toAdd = new ImmutableSchemaSql.Builder().from(child)
                                                    .type(child.getValueType()
                                                                     .get())
                                                    .valueType(Optional.empty())
                                                    .build();
        }

        return new ImmutableSchemaSql.Builder().from(parent)
                                               .addProperties(toAdd)
                                               .build();
    }

    @Override
    public SchemaSql addChildren(SchemaSql parent, List<SchemaSql> children) {
        return new ImmutableSchemaSql.Builder().from(parent)
                                               .addAllProperties(children)
                                               .build();
    }

    @Override
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
}
