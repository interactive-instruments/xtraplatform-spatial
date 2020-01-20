package de.ii.xtraplatform.feature.provider.sql;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.immutables.value.Value;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public interface SqlPathSyntax {

    default List<String> asList(String path) {
        return getPathSplitter().splitToList(path);
    }

    default boolean isJunctionTable(String pathElement) {
        return getJunctionTablePattern().matcher(pathElement)
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

    default String setQueryableFlag(String path, String queryable) {
        return String.format("%s{queryable=%s}", path, queryable);
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
    default String getQueryableFlagPattern() {
        return "\\{queryable=" + "(?<" + MatcherGroups.QUERYABLE + ">.+?)\\}";
    }

    @Value.Derived
    default String getFlagsPattern() {
        return "(?:\\{[a-z_]+.*\\})*";
    }

    @Value.Immutable
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
        String FLAGS = "flags";
        String QUERYABLE = "queryable";
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
    default String getTablePatternString() {
        return "(?:" + getJoinConditionPattern() + ")?" + "(?<" + MatcherGroups.TABLE + ">" + getIdentifierPattern() + ")";
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
        return Pattern.compile("(?<" + MatcherGroups.PATH + ">" + "(?:" + getPathSeparator() + getTablePatternString() + ")+)" + getPathSeparator() + "(?<" + MatcherGroups.COLUMNS + ">" + getColumnPattern() + ")" + "(?<" + MatcherGroups.FLAGS + ">" + getFlagsPattern() + ")?");
    }

    @Value.Default
    default Options getOptions() {
        return ImmutableOptions.builder()
                               .build();
    }

    @Value.Derived
    default Pattern getJunctionTablePattern() {
        return Pattern.compile(getOptions().getJunctionTablePattern());
    }
}