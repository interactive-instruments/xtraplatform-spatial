/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableSqlPathDefaults;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlPathDefaults;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public interface SqlPathSyntax {

    default List<String> asList(String path) {
        return getPathSplitter().splitToList(path);
    }

    default boolean isJunctionTable(String pathElement) {
        return getOptions().getJunctionTablePattern()
            .map(Pattern::compile)
            .orElse(getJunctionTablePattern())
            .matcher(pathElement)
            .find();
    }

    default String asPath(List<String> pathElements) {
        StringBuilder path = new StringBuilder();
        if (!pathElements.isEmpty() && !pathElements.get(0)
                                                    .startsWith(getPathSeparator())) {
            path.append(getPathSeparator());
        }
        getPathJoiner()
                .appendTo(path, pathElements);

        return path.toString();
    }

    default String asPath(String... pathAndPathElements) {
        return asPath(Arrays.asList(pathAndPathElements));
    }

    default boolean getOidFlag(String path) {
        Matcher matcher = Pattern.compile(getOidFlagPattern())
                                 .matcher(path);

        return matcher.find();
    }

    default String setOidFlag(String path) {
        return String.format("%s{oid}", path);
    }

    default boolean getSpatialFlag(String path) {
        Matcher matcher = Pattern.compile(getSpatialFlagPattern())
                                 .matcher(path);

        return matcher.find();
    }

    default String setSpatialFlag(String path) {
        return String.format("%s{spatial}", path);
    }

    default boolean getTemporalFlag(String path) {
        Matcher matcher = Pattern.compile(getTemporalFlagPattern())
                                 .matcher(path);

        return matcher.find();
    }

    default String setTemporalFlag(String path) {
        return String.format("%s{temporal}", path);
    }

    default OptionalInt getPriorityFlag(String path) {
        Matcher matcher = Pattern.compile(getPriorityFlagPattern())
                                 .matcher(path);

        if (matcher.find()) {
            return OptionalInt.of(Integer.parseInt(matcher.group(MatcherGroups.PRIORITY)));
        }

        return OptionalInt.empty();
    }

    default String setPriorityFlag(String path, int priority) {
        return String.format("%s{priority=%d}", path, priority);
    }

    default Optional<String> getQueryableFlag(String flags) {
        Matcher matcher = Pattern.compile(getQueryableFlagPattern())
                                 .matcher(flags);

        if (matcher.find()) {
            return Optional.of(matcher.group(MatcherGroups.QUERYABLE));
        }

        return Optional.empty();
    }

    default Optional<String> getConstantFlag(String flags) {
        Matcher matcher = Pattern.compile(getConstantFlagPattern())
                                 .matcher(flags);

        if (matcher.find()) {
            return Optional.of(matcher.group(MatcherGroups.CONSTANT));
        }

        return Optional.empty();
    }

    default Optional<String> getSortKeyFlag(String flags) {
        Matcher matcher = Pattern.compile(getSortKeyFlagPattern())
                                 .matcher(flags);

        if (matcher.find()) {
            return Optional.of(matcher.group(MatcherGroups.SORT_KEY));
        }

        return Optional.empty();
    }

    default String setQueryableFlag(String path, String queryable) {
        return String.format("%s{queryable=%s}", path, queryable);
    }

    default Optional<String> getFilterFlag(String flags) {
        Matcher matcher = Pattern.compile(getFilterFlagPattern())
                                 .matcher(flags);

        if (matcher.find()) {
            return Optional.of(matcher.group(MatcherGroups.FILTER));
        }

        return Optional.empty();
    }


    default Optional<String> getFilterFlagExpression(String flags) {
        Matcher matcher = Pattern.compile(getFilterFlagPattern())
            .matcher(flags);

        if (matcher.find()) {
            return Optional.of(matcher.group());
        }

        return Optional.empty();
    }


    //TODO: start end separator for flags
    @Value.Derived
    default String getPriorityFlagPattern() {
        return "\\{priority=" + "(?<" + MatcherGroups.PRIORITY + ">[0-9]+)\\}";
    }

    @Value.Derived
    default String getOidFlagPattern() {
        return "\\{oid\\}";
    }

    @Value.Derived
    default String getSpatialFlagPattern() {
        return "\\{spatial\\}";
    }

    @Value.Derived
    default String getTemporalFlagPattern() {
        return "\\{temporal\\}";
    }

    @Value.Derived
    default String getQueryableFlagPattern() {
        return "\\{queryable=" + "(?<" + MatcherGroups.QUERYABLE + ">.+?)\\}";
    }

    @Value.Derived
    default String getConstantFlagPattern() {
        return "\\{constant=" + "(?<" + MatcherGroups.CONSTANT + ">.+?)\\}";
    }

    @Value.Derived
    default String getSortKeyFlagPattern() {
        return "\\{sortKey=" + "(?<" + MatcherGroups.SORT_KEY + ">.+?)\\}";
    }

    @Value.Derived
    default String getFilterFlagPattern() {
        return "\\{filter=" + "(?<" + MatcherGroups.FILTER + ">.+?)\\}";
    }

    @Value.Derived
    default String getFlagsPattern() {
        return "(?:\\{[a-z_]+.*?\\})*";
    }

    @Value.Immutable
    @Value.Style(builder = "new")
    @JsonDeserialize(builder = ImmutableOptions.Builder.class)
    interface Options {

        @Value.Default
        default String getDefaultPrimaryKey() {
            return "id";
        }

        @Value.Default
        default String getDefaultSortKey() {
            return getDefaultPrimaryKey();
        }

        @Value.Default
        default String getJunctionTablePattern() {
            return ".+_2_.+";
        }

        //TODO: optional syntax for flags after table name, allow in Syntax
        @Value.Default
        default String getJunctionTableFlag() {
            return "{junction}";
        }
    }

    interface MatcherGroups {
        String PATH = "path";
        String TABLE = "table";
        String COLUMNS = "columns";
        String SOURCE_FIELD = "sourceField";
        String TARGET_FIELD = "targetField";
        String PRIORITY = "priority";
        String PATH_FLAGS = "pathFlags";
        String TABLE_FLAGS = "tableFlags";
        String QUERYABLE = "queryable";
        String CONSTANT = "constant";
        String SORT_KEY = "sortKey";
        String FILTER = "filter";
    }


    @Value.Default
    default String getPathSeparator() {
        return "/";
    }

    @Value.Derived
    default Splitter getPathSplitter() {
        return Splitter.on(getPathSeparator())
                       .omitEmptyStrings();
    }

    @Value.Derived
    default Joiner getPathJoiner() {
        return Joiner.on(getPathSeparator())
                     .skipNulls();
    }

    @Value.Default
    default String getMultiColumnSeparator() {
        return ":";
    }

    @Value.Derived
    default Splitter getMultiColumnSplitter() {
        return Splitter.on(getMultiColumnSeparator())
                       .omitEmptyStrings();
    }

    @Value.Default
    default String getIdentifierPattern() {
        return "[a-zA-Z_]{1}[a-zA-Z0-9_]*";
    }

    @Value.Derived
    default String getColumnPattern() {
        return getIdentifierPattern() + "(?:" + getMultiColumnSeparator() + getIdentifierPattern() + ")*";
    }

    @Value.Default
    default String getJoinConditionStart() {
        return "[";
    }

    @Value.Default
    default String getJoinConditionSeparator() {
        return "=";
    }

    @Value.Default
    default String getJoinConditionEnd() {
        return "]";
    }

    @Value.Derived
    default String getJoinConditionPattern() {
        return Pattern.quote(getJoinConditionStart()) +
                "(?<" + MatcherGroups.SOURCE_FIELD + ">" + getIdentifierPattern() + ")" +
                Pattern.quote(getJoinConditionSeparator()) +
                "(?<" + MatcherGroups.TARGET_FIELD + ">" + getIdentifierPattern() + ")" +
                Pattern.quote(getJoinConditionEnd());
    }

    @Value.Derived
    default String getJoinConditionPlainPattern() {
        return Pattern.quote(getJoinConditionStart()) +
                "(?:" + getIdentifierPattern() + ")" +
                Pattern.quote(getJoinConditionSeparator()) +
                "(?:" + getIdentifierPattern() + ")" +
                Pattern.quote(getJoinConditionEnd());
    }

    @Value.Derived
    default String getTablePatternString() {
        return "(?:" + getJoinConditionPattern() + ")?" + "(?<" + MatcherGroups.TABLE + ">" + getIdentifierPattern() + ")" + "(?<" + MatcherGroups.TABLE_FLAGS + ">" + getFlagsPattern() + ")?";
    }

    @Value.Derived
    default String getTablePatternPlainString() {
        return "(?:" + getJoinConditionPlainPattern() + ")?" + "(?:" + getIdentifierPattern() + ")" + "(?:" + getFlagsPattern() + ")?";
    }

    @Value.Derived
    default Pattern getTablePattern() {
        return Pattern.compile(getTablePatternString());
    }

    @Value.Derived
    default Pattern getJoinedTablePattern() {
        return Pattern.compile(getJoinConditionPattern() + "(?<" + MatcherGroups.TABLE + ">" + getIdentifierPattern() + ")");
    }

    @Value.Derived
    default Pattern getColumnPathPattern() {
        return Pattern.compile("^(?<" + MatcherGroups.PATH + ">" + "(?:" + getPathSeparator() + getTablePatternString() + ")+)" + getPathSeparator() + "(?<" + MatcherGroups.COLUMNS + ">" + getColumnPattern() + ")?" + "(?<" + MatcherGroups.PATH_FLAGS + ">" + getFlagsPattern() + ")?$");
    }

    @Value.Derived
    default Pattern getPartialColumnPathPattern() {
        return Pattern.compile(getPathPatternString() + "(?:" + getPathSeparator() + "(?<" + MatcherGroups.COLUMNS + ">" + getColumnPattern() + "))" + "(?<" + MatcherGroups.PATH_FLAGS + ">" + getFlagsPattern() + ")?$");
    }

    @Value.Derived
    default Pattern getPathPattern() {
        return Pattern.compile(getPathPatternString());
    }

    @Value.Derived
    default String getPathPatternString() {
        return "(?<" + MatcherGroups.PATH + ">" + getPathSeparator() + "?" + getTablePatternPlainString() + "(?:" + getPathSeparator() + getTablePatternPlainString() + ")*)";
    }

    @Value.Default
    default SqlPathDefaults getOptions() {
        return new ImmutableSqlPathDefaults.Builder()
                               .build();
    }

    @Value.Derived
    default Pattern getJunctionTablePattern() {
        return Pattern.compile(".+_2_.+");
    }
}
