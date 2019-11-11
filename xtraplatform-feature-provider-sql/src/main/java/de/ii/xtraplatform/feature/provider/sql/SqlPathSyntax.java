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

    //SqlPath parse(String path);
    default Optional<SqlPath> toSqlPath(String path) {
        Matcher matcher = getColumnPathPattern().matcher(path);

        if (matcher.find()) {
            String tablePath = matcher.group(MatcherGroups.PATH);
            List<String> columns = getMultiColumnSplitter().splitToList(matcher.group(MatcherGroups.COLUMNS));
            String flags = matcher.group(MatcherGroups.FLAGS);
            OptionalInt priority = getPriorityFlag(flags);
            boolean hasOid = getOidFlag(flags);
            List<String> tablePathAsList = asList(tablePath);
            boolean isRoot = tablePathAsList.size() == 1;
            boolean isJunction = isJunctionTable(tablePathAsList.get(tablePathAsList.size()-1));

            return Optional.of(ImmutableSqlPath.builder()
                                               .tablePath(tablePath)
                                               .columns(columns)
                                               .hasOid(hasOid)
                                               .sortPriority(priority)
                                               .isRoot(isRoot)
                                               .isJunction(isJunction)
                                               .build());
        }

        return Optional.empty();
        //throw new IllegalArgumentException("not a valid sql path: " + path);
    }

    default List<SqlPathTable> parse(List<String> paths) {

        Map<String, SqlPath> sqlPaths = new LinkedHashMap<>();

        List<SqlPath> sortedPaths = paths
                .stream()
                .map(this::toSqlPath)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(this::sortByPriority)
                .collect(Collectors.toList());

        List<SqlPath> mergedPaths = merge(sortedPaths);

        Set<SqlPath> allPaths = fanOutObjectTables(mergedPaths);


        return toTableTree(ImmutableList.copyOf(allPaths));
    }

    //TODO: by using parentBuilder, we end up with different parent instances for every child
    //TODO: only children links, parent link not needed, only parent path
    //TODO: map to SqlPathTable here?
    default Set<SqlPath> fanOutObjectTables(List<SqlPath> pathsWithColumns) {

        Map<String, ImmutableSqlPath.Builder> existingPaths = pathsWithColumns.stream()
                                                             .collect(Collectors.toMap(SqlPath::getTablePath, sqlPath -> ImmutableSqlPath.builder()
                                                                                                                                         .from(sqlPath), (sqlPath, sqlPath2) -> {
                                                                 throw new IllegalStateException();
                                                             }, LinkedHashMap::new));

        pathsWithColumns.forEach(sqlPath -> {
            ImmutableSqlPath.Builder parent = null;

            for (SqlPath child : getParentPaths(sqlPath)) {
                ImmutableSqlPath.Builder current;
                if (existingPaths.containsKey(child.getTablePath())) {
                    current = existingPaths.get(child.getTablePath());
                } else {
                    current = ImmutableSqlPath.builder()
                                              .from(child);
                    existingPaths.put(child.getTablePath(), current);
                }

                if (Objects.nonNull(parent)) {
                    current.parentBuilder(parent);
                }

                parent = current;
            }
        });

        return existingPaths.values()
                            .stream()
                            .map(ImmutableSqlPath.Builder::build)
                            .collect(ImmutableSet.toImmutableSet());
    }

    //TODO: test with mappings without sortPriority, e.g. daraa
    default int sortByPriority(SqlPath path1, SqlPath path2) {
        return !path1.getSortPriority()
                     .isPresent() ? 1 : !path2.getSortPriority()
                                              .isPresent() ? -1 : path1.getSortPriority()
                                                                       .getAsInt() - path2.getSortPriority()
                                                                                          .getAsInt();
    }

    default List<SqlPath> merge(List<SqlPath> sqlPaths) {
        LinkedHashMap<String, List<SqlPath>> groupedPaths = sqlPaths.stream()
                                                                    .collect(Collectors.groupingBy(SqlPath::getTablePath, LinkedHashMap::new, Collectors.toList()));

        return groupedPaths.entrySet()
                           .stream()
                           //TODO: is this really needed?
                           .sorted(Comparator.comparingInt(entry -> asList(entry.getKey()).size()))
                           .map(entry -> {
                               String tablePath = entry.getKey();
                               List<SqlPath> columnPaths = entry.getValue();
                               List<String> columns = columnPaths.stream()
                                                                 .flatMap(sqlPath -> sqlPath.getColumns()
                                                                                            .stream())
                                                                 .collect(Collectors.toList());
                               boolean hasOid = columnPaths.stream()
                                                           .anyMatch(SqlPath::hasOid);
                               OptionalInt priority = columnPaths.stream()
                                                                 .flatMapToInt(columnPath -> {
                                                                     OptionalInt sortPriority = columnPath.getSortPriority();
                                                                     return sortPriority.isPresent() ? IntStream.of(sortPriority.getAsInt()) : IntStream.empty();
                                                                 })
                                                                 .findFirst();
                               boolean isRoot = columnPaths.stream()
                                                           .anyMatch(SqlPath::isRoot);

                               return ImmutableSqlPath.builder()
                                                      .tablePath(tablePath)
                                                      .columns(columns)
                                                      .hasOid(hasOid)
                                                      .sortPriority(priority)
                                                      .isRoot(isRoot)
                                                      .build();
                           })
                           .collect(Collectors.toList());
    }

    default List<SqlPathTable> toTableTree(final List<SqlPath> tableInfos) {
        return tableInfos.stream()
                         .filter(tableInfo -> tableInfo.isRoot())
                         .map(tableInfo1 -> toSqlPathTableBuilder(tableInfo1, tableInfos))
                         .map(SqlPathTable.Builder::build)
                .collect(Collectors.toList());
    }

    default SqlPathTable.Builder toSqlPathTableBuilder(SqlPath rootSqlPath,
                                                       List<SqlPath> allSqlPaths) {
        return toSqlPathTableBuilder(rootSqlPath, allSqlPaths, SqlPathTable.TYPE.UNDECIDED);
    }

    default SqlPathTable.Builder toSqlPathTableBuilder(SqlPath rootSqlPath,
                                                       List<SqlPath> allSqlPaths,
                                                       SqlPathTable.TYPE parentType) {
        String path = rootSqlPath.getTablePath();
        List<String> columnPaths = rootSqlPath.getColumns()
                                            .stream()
                                            .map(column -> asPath(rootSqlPath.getTablePath(),  column))
                                            .collect(Collectors.toList());
        //TODO: absolutePath + relativePath?
        if (!rootSqlPath.isRoot()) {
            path = path.substring(rootSqlPath.getParent()
                                           .getTablePath()
                                           .length());
            //TODO: do we really need full paths here
            /*columnPaths = columnPaths.stream()
                                     .map(col -> col.substring(node.parent.path.length()))
                                     .collect(Collectors.toList());*/
        }

        List<SqlPathColumn> columns = columnPaths.stream()
                                                 .map(columnPath -> ImmutableSqlPathColumn.builder()
                                                                                          .path(columnPath)
                                                                                          .build())
                                                 .collect(Collectors.toList());
        List<SqlPathJoin> joins = asJoins(path);

        boolean hasId = rootSqlPath.hasOid();

        SqlPathTable.TYPE type = determineType(path, joins, hasId, parentType);

        SqlPathTable.Builder builder = new SqlPathTable.Builder()
                .path(path)
                .columnsNew(columns)
                .joins(joins)
                .type(type);

        List<SqlPathTable> children = allSqlPaths.stream()
                                                 .filter(child -> child.getParent() == rootSqlPath)
                                               .map(childTableInfo -> toSqlPathTableBuilder(childTableInfo, allSqlPaths, type))
                                               .map(childBuilder -> childBuilder.parentBuilder(builder)
                                                                                .build())
                                               .collect(Collectors.toList());

        return builder.addAllChildren(children);
    }
    default Optional<SqlPathJoin> asJoin(String pathElement) {
        Matcher matcher = getJoinedTablePattern()
                                .matcher(pathElement);
        if (matcher.find()) {
            return Optional.of(ImmutableSqlPathJoin.builder()
                                                   .targetTable(matcher.group(SqlPathParser.Syntax.TABLE_GROUP))
                                                   .sourceColumn(matcher.group(SqlPathParser.Syntax.SOURCE_FIELD_GROUP))
                                                   .targetColumn(matcher.group(SqlPathParser.Syntax.TARGET_FIELD_GROUP))
                                                   .build());
        }

        return Optional.empty();
    }

    default List<SqlPathJoin> asJoins(String path) {
        return asList(path).stream()
                           .map(this::asJoin)
                           .filter(Optional::isPresent)
                           .map(Optional::get)
                           .collect(Collectors.toList());
    }

    //TODO: extract patterns to config
    default SqlPathTable.TYPE determineType(String path, List<SqlPathJoin> joins, boolean hasId,
                                            SqlPathTable.TYPE parentType) {

        // segment with id
        if (hasId) {
            return SqlPathTable.TYPE.MAIN;
        }

        // root segment without id
        if (parentType == SqlPathTable.TYPE.UNDECIDED) {
            return SqlPathTable.TYPE.MERGED;
        }

        //TODO: only 3 cases (artbeobachtung), where is the difference to 1_1?
        // MERGE only used in SqlFeatureInserts, diff output when deactivating this
        // merged segment
        if ((parentType == SqlPathTable.TYPE.MAIN || parentType == SqlPathTable.TYPE.MERGED) && path.startsWith("/[id=id]") && !isJunctionTable(path)) {
            return SqlPathTable.TYPE.MERGED;
        }

        // segment with connection table
        if (joins.stream()
                 .anyMatch(join -> isJunctionTable(join.getTargetTable()))) {
            return joins.size() == 1 ? SqlPathTable.TYPE.ID_1_N : SqlPathTable.TYPE.ID_M_N;
        }

        if (!joins.isEmpty() && joins.get(0)
                                     .getSourceColumn()
                                     .equals("id")) {
            return SqlPathTable.TYPE.ID_1_N;
        }

        return SqlPathTable.TYPE.ID_1_1;
    }



    default List<SqlPath> getParentPaths(SqlPath sqlPath) {
        List<String> tables = asList(sqlPath.getTablePath());

        return IntStream.range(0, tables.size())
                        .mapToObj(i -> ImmutableSqlPath.builder()
                                                       .tablePath(asPath(tables.subList(0, i + 1)))
                                                       .isRoot(i == 0)
                                                       .isJunction(isJunctionTable(tables.get(i)))
                                                       .build())
                        .collect(ImmutableList.toImmutableList());
    }

    default boolean isRoot(SqlPath sqlPath) {
        return asList(sqlPath.getTablePath()).size() == 1;
    }

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
