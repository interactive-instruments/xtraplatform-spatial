package de.ii.xtraplatform.features.domain;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class SchemaToMappingVisitor<T extends SchemaBase<T>> implements SchemaVisitor<T, Multimap<List<String>, T>> {

    static final Splitter SPLITTER = Splitter.on('/')
                                             .omitEmptyStrings();

    @Override
    public Multimap<List<String>, T> visit(T schema, List<Multimap<List<String>, T>> visitedProperties) {
        List<String> path = SPLITTER.splitToList(schema.getSourcePath().orElse(""));

        return Stream.concat(
                path.isEmpty()
                        ? Stream.empty()
                        : Stream.of(new AbstractMap.SimpleImmutableEntry<>(path, schema)),
                visitedProperties.stream()
                                 .flatMap(map -> map.asMap()
                                                    .entrySet()
                                                    .stream()
                                                    .flatMap(entry -> path.isEmpty() && entry.getKey()
                                                                                             .size() < 2
                                                            ? Stream.concat(
                                                            Stream.of(new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), schema)), prependToKey(entry, path))
                                                            : prependToKey(entry, path)))
        )
                     .collect(ImmutableListMultimap.toImmutableListMultimap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Stream<Map.Entry<List<String>, T>> prependToKey(Map.Entry<List<String>, Collection<T>> property, List<String> parentPath) {
        return property.getValue()
                       .stream()
                       .map(value -> new AbstractMap.SimpleImmutableEntry<>(merge(parentPath, property.getKey()), value));
    }

    private List<String> merge(List<String> parentPath, List<String> path) {
        if (parentPath.isEmpty()) {
            return path;
        }

        return ImmutableList.<String>builder().addAll(parentPath)
                                              .addAll(path)
                                              .build();
    }
}
