/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SchemaToSourcePathsVisitor<T extends SchemaBase<T>> implements SchemaVisitor<T, Multimap<List<String>, T>> {

    static final Splitter SPLITTER = Splitter.on('/')
                                             .omitEmptyStrings();

    private final boolean useTargetPath;
    private int counter;

    public SchemaToSourcePathsVisitor() {
        this(false);
    }

    public SchemaToSourcePathsVisitor(boolean useTargetPath) {
        this.useTargetPath = useTargetPath;
        this.counter = 0;
    }


    @Override
    public Multimap<List<String>, T> visit(T schema, List<Multimap<List<String>, T>> visitedProperties) {
        List<List<String>> paths = useTargetPath
            ? ImmutableList.of(schema.getPath())
            //TODO: static cleanup method in PathParser
            : schema.getSourcePaths().stream()
                .map(sourcePath -> {
                    int i = sourcePath.indexOf('{');

                    if (i > 0) {
                        List<String> p = new ArrayList<>(SPLITTER.splitToList(sourcePath.substring(0, i)));
                        p.set(p.size()-1, p.get(p.size()-1)
                            + sourcePath.substring(i).replaceAll("\\{sortKey=.*?\\}", "").replaceAll("\\{primaryKey=.*?\\}", "")
                            + (schema.isValue() ? "{priority=" + (counter++) + "}" : ""));
                        return p;
                    }

                    return SPLITTER.splitToList(sourcePath + (schema.isValue() ? "{priority=" + (counter++) + "}" : ""));
                })
                .collect(Collectors.toList());

        return (paths.isEmpty()
        ? visitedProperties.stream().flatMap(map -> map.asMap()
            .entrySet()
            .stream()
            .flatMap(entry -> prependToKey(entry, ImmutableList.of())))
        : paths.stream().flatMap(path -> {
            return Stream.concat(
                Stream.of(new AbstractMap.SimpleImmutableEntry<>(path, schema)),
                visitedProperties.stream()
                    .flatMap(map -> map.asMap()
                        .entrySet()
                        .stream()
                        .flatMap(entry -> prependToKey(entry, path))));
        }))
            .collect(ImmutableListMultimap.toImmutableListMultimap(Map.Entry::getKey, Map.Entry::getValue));

        /*return Stream.concat(
                path.isEmpty()
                        ? Stream.empty()
                        : Stream.of(new AbstractMap.SimpleImmutableEntry<>(path, schema)),
                visitedProperties.stream()
                                 .flatMap(map -> map.asMap()
                                                    .entrySet()
                                                    .stream()
                                                    .flatMap(entry -> prependToKey(entry, path)))
        )
                     .collect(ImmutableListMultimap.toImmutableListMultimap(Map.Entry::getKey, Map.Entry::getValue));*/
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
