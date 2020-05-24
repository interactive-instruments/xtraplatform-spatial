package de.ii.xtraplatform.features.domain;

import com.google.common.base.Splitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface ReverseSchemaBuilder<T extends SchemaBase<T>> extends SchemaVisitor<FeatureSchema, T> {

    @Override
    default T visit(FeatureSchema schema, List<T> visitedProperties) {

        List<String> currentPath = splitPath(schema.getSourcePath().orElse(""));

        T current = create(currentPath, schema);

        Map<List<String>, T> objectCache = new LinkedHashMap<>();

        List<T> properties = visitedProperties.stream()
                .map(sourceSchema -> {
                    List<String> parentPath = sourceSchema.getParentPath();

                    if (parentPath.isEmpty()) {
                        return sourceSchema;
                    }

                    String parentParentPath = currentPath.get(currentPath.size()-1);

                    List<T> parents = createParents(parentParentPath, sourceSchema, objectCache);

                    parents.forEach(t -> objectCache.put(t.getPath(), t));

                    return parents.get(0);
                })
                .collect(Collectors.toList());

        properties = properties.stream()
                .map(sourceSchema -> objectCache.getOrDefault(sourceSchema.getPath(), sourceSchema))
                .distinct()
                .map(sourceSchema -> prependToParentPath(currentPath, sourceSchema))
                .collect(Collectors.toList());

        return addChildren(current, properties);
    }

    List<T> createParents(String parentParentPath, T child, Map<List<String>, T> objectCache);

    T create(List<String> path, FeatureSchema targetSchema);

    T addChildren(T parent, List<T> children);

    T prependToParentPath(List<String> path, T schema);

    Splitter SPLITTER = Splitter.on('/').omitEmptyStrings();

    default List<String> splitPath(String path) {
        return SPLITTER.splitToList(path);
    }
}
