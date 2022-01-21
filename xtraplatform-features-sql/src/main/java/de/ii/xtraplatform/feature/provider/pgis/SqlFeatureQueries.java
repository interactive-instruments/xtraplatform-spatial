/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.pgis;

import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public abstract class SqlFeatureQueries {

    protected abstract List<String> getPaths();

    protected abstract Set<String> getMultiTables();

    protected abstract SqlPathTree getSqlPaths();

    @Value.Derived
    public List<SqlFeatureQuery> getQueries() {
        return getPaths().stream()
                         .collect(Collectors.collectingAndThen(groupByPath(), this::buildQueries));
    }

    public SqlFeatureQuery getMainQuery() {
        //TODO:
        return getQueries().stream().filter(query -> query.getPaths().get(query.getPaths().size()-1).lastIndexOf("/") == 0)
                .findFirst().orElse(null);
    }

    //TODO
    public List<SqlFeatureQuery> getSubQueries() {
        return getQueries().subList(1, getQueries().size());
    }

    private Collector<String, ?, LinkedHashMap<String, List<String>>> groupByPath() {
        return Collectors.groupingBy(matchPath(), LinkedHashMap::new, Collectors.toList());
    }

    private Function<String, String> matchPath() {
        return path -> path.lastIndexOf("/") > 0 ? path.substring(0, path.lastIndexOf("/")) : path;
    }

    private SqlFeatureQuery buildQuery(List<String> paths) {
        return ImmutableSqlFeatureQuery.builder()
                                       .paths(paths)
                                       .sqlPaths(getSqlPaths())
                                       .build();
    }

    private List<SqlFeatureQuery> buildQueries(Map<String, List<String>> queryGroups) {
        // TODO: is sorted by sortPriority???
        Stream<List<String>> valueStream = /*queryGroups.keySet()
                                                      .iterator()
                                                      .next()
                                                      .lastIndexOf("/") > 0
                ? queryGroups.entrySet()
                             .stream()
                             .sorted(Comparator.comparingInt(group -> group.getKey()
                                                                           .split("/").length))
                             .map(Map.Entry::getValue)
                : */queryGroups.values()
                             .stream();

        return valueStream
                .map(this::buildQuery)
                .collect(Collectors.toList());
    }
}
