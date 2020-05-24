/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.feature.provider.sql.ImmutableSqlPath;
import de.ii.xtraplatform.feature.provider.sql.SqlPath;
import de.ii.xtraplatform.feature.provider.sql.SqlPathSyntax;
import de.ii.xtraplatform.features.domain.FeatureStoreRelation;
import de.ii.xtraplatform.features.domain.ImmutableFeatureStoreRelation;
import de.ii.xtraplatform.features.domain.ReverseSchemaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class PathParserSql {

    private static final Logger LOGGER = LoggerFactory.getLogger(PathParserSql.class);

    private final SqlPathSyntax syntax;
    private final Cql cql;

    public PathParserSql(SqlPathSyntax syntax, Cql cql) {
        this.syntax = syntax;
        this.cql = cql;
    }

    public Optional<SqlPath> parse(String path, boolean isColumn) {
        Matcher matcher = isColumn
                ? syntax.getPartialColumnPathPattern()
                .matcher(path)
                : syntax.getPathPattern()
                .matcher(path);

        if (matcher.find()) {
            String tablePath = matcher.group(SqlPathSyntax.MatcherGroups.PATH);

            //TODO: full parent path?
            Map<String, String> tableFlags = new LinkedHashMap<>();
            Matcher tableMatcher = syntax.getTablePattern()
                    .matcher(tablePath);
            while (tableMatcher.find()) {
                String flags = tableMatcher.group(SqlPathSyntax.MatcherGroups.TABLE_FLAGS);
                tablePath = tablePath.replace(flags, "");
                String pathWithoutFlags = tableMatcher.group(0)
                        .replace(flags, "");
                tableFlags.putIfAbsent(pathWithoutFlags, flags);
            }

            String column = isColumn ? matcher.group(SqlPathSyntax.MatcherGroups.COLUMNS) : null;
            List<String> columns = Objects.nonNull(column)
                    ? syntax.getMultiColumnSplitter()
                    .splitToList(column)
                    : ImmutableList.of();

            String flags = isColumn ? matcher.group(SqlPathSyntax.MatcherGroups.PATH_FLAGS) : "";
            OptionalInt priority = syntax.getPriorityFlag(flags);
            boolean hasOid = syntax.getOidFlag(flags);
            List<String> tablePathAsList = syntax.asList(tablePath);
            boolean isRoot = tablePathAsList.size() == 1;
            boolean isJunction = syntax.isJunctionTable(tablePathAsList.get(tablePathAsList.size() - 1));
            Optional<String> queryable = syntax.getQueryableFlag(flags)
                    .map(q -> q.replaceAll("\\[", "")
                            .replaceAll("]", ""));
            boolean isSpatial = syntax.getSpatialFlag(flags);

            return Optional.of(ImmutableSqlPath.builder()
                    .tablePath(tablePath)
                    .tableFlags(tableFlags)
                    .columns(columns)
                    .hasOid(hasOid)
                    .sortPriority(priority)
                    .isRoot(isRoot)
                    .isJunction(isJunction)
                    .queryable(""/*queryable.get()*/)
                    .isSpatial(isSpatial)
                    .build());
        } else {
            LOGGER.warn("Invalid path in provider configuration: {}", path);
        }

        return Optional.empty();
    }

    public List<FeatureStoreRelation> toRelations(List<String> path,
                                                    Map<String, CqlFilter> filters) {

        if (path.size() < 2) {
            throw new IllegalArgumentException(String.format("not a valid relation path: %s", path));
        }

        if (path.size() > 2) {
            return IntStream.range(2, path.size())
                    .mapToObj(i -> toRelations(path.get(i - 2), path.get(i - 1), path.get(i), i == path.size() - 1, filters))
                    .flatMap(Function.identity())
                    .collect(Collectors.toList());
        }

        return IntStream.range(1, path.size())
                .mapToObj(i -> toRelation(path.get(i - 1), path.get(i), filters))
                .collect(Collectors.toList());
    }

    private Stream<FeatureStoreRelation> toRelations(String source, String link, String target, boolean isLast,
                                                     Map<String, CqlFilter> filters) {
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

    //TODO: support sortKey flag on table instead of getDefaultPrimaryKey
    private FeatureStoreRelation toRelation(String source, String target,
                                            Map<String, CqlFilter> filters) {
        Matcher sourceMatcher = syntax.getTablePattern()
                .matcher(source);
        Matcher targetMatcher = syntax.getJoinedTablePattern()
                .matcher(target);
        if (sourceMatcher.find() && targetMatcher.find()) {
            String sourceField = targetMatcher.group(SqlPathSyntax.MatcherGroups.SOURCE_FIELD);
            String targetField = targetMatcher.group(SqlPathSyntax.MatcherGroups.TARGET_FIELD);
            boolean isOne2One = Objects.equals(targetField, syntax.getOptions()
                    .getDefaultPrimaryKey());

            Optional<CqlFilter> filter = Optional.ofNullable(filters.get(target));

            return ImmutableFeatureStoreRelation.builder()
                    .cardinality(isOne2One ? FeatureStoreRelation.CARDINALITY.ONE_2_ONE : FeatureStoreRelation.CARDINALITY.ONE_2_N)
                    .sourceContainer(sourceMatcher.group(SqlPathSyntax.MatcherGroups.TABLE))
                    .sourceField(sourceField)
                    .sourceSortKey(syntax.getOptions()
                            .getDefaultPrimaryKey())
                    .targetContainer(targetMatcher.group(SqlPathSyntax.MatcherGroups.TABLE))
                    .targetField(targetField)
                    .filter(filter)
                    .build();
        }

        throw new IllegalArgumentException(String.format("not a valid relation path: %s/%s", source, target));
    }

    private FeatureStoreRelation toRelation(String source, String link, String target) {
        Matcher sourceMatcher = syntax.getTablePattern()
                .matcher(source);
        Matcher junctionMatcher = syntax.getJoinedTablePattern()
                .matcher(link);
        Matcher targetMatcher = syntax.getJoinedTablePattern()
                .matcher(target);
        if (sourceMatcher.find() && junctionMatcher.find() && targetMatcher.find()) {
            return ImmutableFeatureStoreRelation.builder()
                    .cardinality(FeatureStoreRelation.CARDINALITY.M_2_N)
                    .sourceContainer(sourceMatcher.group(SqlPathSyntax.MatcherGroups.TABLE))
                    .sourceField(junctionMatcher.group(SqlPathSyntax.MatcherGroups.SOURCE_FIELD))
                    .sourceSortKey(syntax.getOptions()
                            .getDefaultPrimaryKey())
                    .junctionSource(junctionMatcher.group(SqlPathSyntax.MatcherGroups.TARGET_FIELD))
                    .junction(junctionMatcher.group(SqlPathSyntax.MatcherGroups.TABLE))
                    .junctionTarget(targetMatcher.group(SqlPathSyntax.MatcherGroups.SOURCE_FIELD))
                    .targetContainer(targetMatcher.group(SqlPathSyntax.MatcherGroups.TABLE))
                    .targetField(targetMatcher.group(SqlPathSyntax.MatcherGroups.TARGET_FIELD))
                    .build();
        }

        throw new IllegalArgumentException(String.format("not a valid relation path: %s/%s/%s", source, link, target));
    }

}
