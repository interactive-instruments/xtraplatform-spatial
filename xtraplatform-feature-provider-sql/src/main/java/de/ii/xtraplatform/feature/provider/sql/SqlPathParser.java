package de.ii.xtraplatform.feature.provider.sql;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.SourcePathMapping;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SqlPathParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlPathParser.class);

    private final Syntax syntax;
    private final PathUtil pathUtil;

    public SqlPathParser(Syntax syntax) {
        this.syntax = syntax;
        this.pathUtil = new PathUtil(syntax);
    }

    public SqlPathParser() {
        this.syntax = ImmutableSyntax.builder()
                                     .build();
        this.pathUtil = new PathUtil(syntax);
    }

    /**
     * normalizes input, delegates to {@link SqlPathParser#parse(List)}
     *
     * @param mappings sql paths with mapping instructions
     * @return tree of {@link SqlPathTable}
     */
    public SqlPathTable parse(Map<String, SourcePathMapping> mappings) {
        List<ColumnInfo> sortedPaths = mappings.entrySet()
                                               .stream()
                                               .sorted(this::sortByPriority)
                                               .flatMap(this::toColumnInfo)
                                               //.sorted(this::sortByPriority)
                                               .collect(Collectors.toList());

        return parse(sortedPaths);
    }

    /**
     * creates a tree of {@link SqlPathTable} with parent and child links
     *
     * @param sqlPaths sql paths
     * @return tree of {@link SqlPathTable}
     */
    public SqlPathTable parse(List<ColumnInfo> sqlPaths) {
        List<TableInfo> tableInfos = toTableInfo(sqlPaths);

        Optional<SqlPathTable> root = toTableTree(tableInfos);

        if (!root.isPresent()) {
            throw new IllegalStateException("no root path found");
        }

        return root.get();
    }

    //TODO: sort on SourcePathMapping, simplify ColumnInfo
    //TODO: test with mappings without sortPriority, e.g. daraa
    private int sortByPriority(ColumnInfo column1, ColumnInfo column2) {
        return column1.noPriority() ? 1 : column2.noPriority() ? -1 : column1.getPriority() - column2.getPriority();
    }

    private int sortByPriority(Map.Entry<String, SourcePathMapping> mapping1, Map.Entry<String, SourcePathMapping> mapping2) {
        Optional<Integer> sortPriority1 = getSortPriority(mapping1.getValue());
        Optional<Integer> sortPriority2 = getSortPriority(mapping2.getValue());

        return !sortPriority1.isPresent() ? 1 : !sortPriority2.isPresent() ? -1 : sortPriority1.get() - sortPriority2.get();
    }

    private Optional<Integer> getSortPriority(SourcePathMapping sourcePathMapping) {
        return Optional.ofNullable(sourcePathMapping.getMappingForType(TargetMapping.BASE_TYPE)).map(TargetMapping::getSortPriority);
    }

    /**
     * normalize sql path and mapping instructions to {@link ColumnInfo}
     *
     * @param mapping sql paths with mapping instructions
     * @return stream of {@link ColumnInfo}
     */
    private Stream<ColumnInfo> toColumnInfo(Map.Entry<String, SourcePathMapping> mapping) {
        List<ColumnInfo> columnInfos = pathUtil.asColumnInfos(mapping.getKey());

        if (!columnInfos.isEmpty()) {
            TargetMapping general = mapping.getValue()
                                           .getMappingForType(TargetMapping.BASE_TYPE);

            Optional<Integer> sortPriority = Optional.ofNullable(general.getSortPriority());
            boolean isId = Optional.ofNullable(general.getType())
                                   .map(enumValue -> enumValue.toString()
                                                              .equals("ID"))
                                   .orElse(false);

            return columnInfos
                    .stream()
                    .map(columnInfo -> new ColumnInfo(columnInfo.path, columnInfo.name, sortPriority, isId));
        }

        return Stream.empty();
    }

    /**
     * @param sqlPaths list of ColumnInfo
     * @return list of TableInfo
     */
    private List<TableInfo> toTableInfo(List<ColumnInfo> sqlPaths) {
        LinkedHashMap<String, List<ColumnInfo>> groupedPaths = sqlPaths.stream()
                                                                       .collect(Collectors.groupingBy(ColumnInfo::getPath, LinkedHashMap::new, Collectors.toList()));

        Set<String> existingPaths = new HashSet<>(groupedPaths.keySet());

        List<TableInfo> tableInfos = groupedPaths.entrySet()
                                                 .stream()
                                                 //TODO: is this really needed?
                                                 .sorted(Comparator.comparingInt(entry -> pathUtil.asList(entry.getKey())
                                                                                                  .size()))
                                                 .flatMap(pathWithColumns -> {
                                                     String path = pathWithColumns.getKey();
                                                     List<ColumnInfo> columnInfos = pathWithColumns.getValue();
                                                     List<String> elements = pathUtil.asList(path);
                                                     List<TableInfo> newPaths = new ArrayList<>();

                                                     for (int i = 0; i < elements.size(); i++) {
                                                         //TODO: test if massnahmen still works when activating change
                                                         if (!pathUtil.isJunctionTable(elements.get(i)) && !elements.get(i)
                                                                                                                    .contains("zustandangaben")) {
                                                             String newPath = pathUtil.asPath(elements.subList(0, i + 1));

                                                             if (!existingPaths.contains(newPath)) {
                                                                 existingPaths.add(newPath);
                                                                 newPaths.add(new TableInfo(newPath));
                                                             }
                                                         }
                                                     }

                                                     return Stream.concat(newPaths.stream(), Stream.of(new TableInfo(path, columnInfos)));
                                                 })
                                                 .collect(Collectors.toList());

        return applyParentChildLinks(tableInfos);
    }

    private List<TableInfo> applyParentChildLinks(final List<TableInfo> tableInfos) {
        tableInfos.forEach(parent -> {
            tableInfos.forEach(child -> {
                if (!Objects.equals(child.getPath(), parent.getPath())
                        && child.getPath()
                                .startsWith(parent.getPath())
                        && (!child.hasParent() || parent.getPath()
                                                        .length() > child.getParent()
                                                                         .getPath()
                                                                         .length())) {

                    child.setParent(parent);
                }
            });
        });

        return tableInfos;
    }

    private Optional<SqlPathTable> toTableTree(final List<TableInfo> tableInfos) {
        return tableInfos.stream()
                         .filter(tableInfo -> !tableInfo.hasParent())
                         .findFirst()
                         .map(this::toSqlPathTableBuilder)
                         .map(SqlPathTable.Builder::build);
    }

    private SqlPathTable.Builder toSqlPathTableBuilder(TableInfo tableInfo) {
        return toSqlPathTableBuilder(tableInfo, SqlPathTable.TYPE.UNDECIDED);
    }

    private SqlPathTable.Builder toSqlPathTableBuilder(TableInfo tableInfo,
                                                       SqlPathTable.TYPE parentType) {
        String path = tableInfo.getPath();
        List<String> columnPaths = tableInfo.getColumns()
                                            .stream()
                                            .map(ColumnInfo::getFullPath)
                                            .collect(Collectors.toList());
        //TODO: absolutePath + relativePath?
        if (tableInfo.hasParent()) {
            path = path.substring(tableInfo.getParent()
                                           .getPath()
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
        List<SqlPathJoin> joins = pathUtil.asJoins(path);

        boolean hasId = tableInfo.getColumns()
                                 .stream()
                                 .anyMatch(column -> column.isId);

        SqlPathTable.TYPE type = determineType(path, joins, hasId, parentType);

        SqlPathTable.Builder builder = new SqlPathTable.Builder()
                .path(path)
                .columnsNew(columns)
                .joins(joins)
                .type(type);

        List<SqlPathTable> children = tableInfo.getChildren()
                                               .stream()
                                               .map(childTableInfo -> toSqlPathTableBuilder(childTableInfo, type))
                                               .map(childBuilder -> childBuilder.parentBuilder(builder)
                                                                                .build())
                                               .collect(Collectors.toList());

        return builder.addAllChildren(children);
    }

    //TODO: extract patterns to config
    private SqlPathTable.TYPE determineType(String path, List<SqlPathJoin> joins, boolean hasId,
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
        if ((parentType == SqlPathTable.TYPE.MAIN || parentType == SqlPathTable.TYPE.MERGED) && path.startsWith("/[id=id]") && !pathUtil.isJunctionTable(path)) {
            return SqlPathTable.TYPE.MERGED;
        }

        // segment with connection table
        if (joins.stream()
                 .anyMatch(join -> pathUtil.isJunctionTable(join.getTargetTable()))) {
            return joins.size() == 1 ? SqlPathTable.TYPE.ID_1_N : SqlPathTable.TYPE.ID_M_N;
        }

        if (!joins.isEmpty() && joins.get(0)
                                     .getSourceColumn()
                                     .equals("id")) {
            return SqlPathTable.TYPE.ID_1_N;
        }

        return SqlPathTable.TYPE.ID_1_1;
    }

    @Value.Immutable
    interface Syntax {

        String PATH_GROUP = "path";
        String TABLE_GROUP = "table";
        String COLUMNS_GROUP = "columns";
        String SOURCE_FIELD_GROUP = "sourceField";
        String TARGET_FIELD_GROUP = "targetField";


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
                    "(?<" + SOURCE_FIELD_GROUP + ">" + getIdentifierPattern() + ")" +
                    Pattern.quote(getJoinConditionSeparator()) +
                    "(?<" + TARGET_FIELD_GROUP + ">" + getIdentifierPattern() + ")" +
                    Pattern.quote(getJoinConditionEnd());
        }

        @Value.Derived
        default String getTablePattern() {
            return "(?:" + getJoinConditionPattern() + ")?" + "(?<" + TABLE_GROUP + ">" + getIdentifierPattern() + ")";
        }

        @Value.Derived
        default Pattern getJoinedTablePattern() {
            return Pattern.compile(getJoinConditionPattern() + "(?<" + TABLE_GROUP + ">" + getIdentifierPattern() + ")");
        }

        @Value.Derived
        default Pattern getColumnPathPattern() {
            return Pattern.compile("(?<" + PATH_GROUP + ">" + "(?:" + getPathSeparator() + getTablePattern() + ")+)" + getPathSeparator() + "(?<" + COLUMNS_GROUP + ">" + getColumnPattern() + ")");
        }

        @Value.Default
        default Options2 getOptions() {
            return ImmutableOptions2.builder()
                                   .build();
        }

        @Value.Derived
        default Pattern getJunctionTablePattern() {
            return Pattern.compile(getOptions().getJunctionTablePattern());
        }
    }

    @Value.Immutable
    interface Options2 {

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

    private class PathUtil {
        private final Syntax syntax;

        private PathUtil(Syntax syntax) {
            this.syntax = syntax;
        }

        List<String> asList(String path) {
            return syntax.getPathSplitter()
                         .splitToList(path);
        }

        boolean isJunctionTable(String pathElement) {
            return syntax.getJunctionTablePattern()
                         .matcher(pathElement)
                         .find();
        }

        private Optional<SqlPathJoin> asJoin(String pathElement) {
            Matcher matcher = syntax.getJoinedTablePattern()
                                    .matcher(pathElement);
            if (matcher.find()) {
                return Optional.of(ImmutableSqlPathJoin.builder()
                                                       .targetTable(matcher.group(Syntax.TABLE_GROUP))
                                                       .sourceColumn(matcher.group(Syntax.SOURCE_FIELD_GROUP))
                                                       .targetColumn(matcher.group(Syntax.TARGET_FIELD_GROUP))
                                                       .build());
            }

            return Optional.empty();
        }

        List<SqlPathJoin> asJoins(String path) {
            return asList(path).stream()
                               .map(this::asJoin)
                               .filter(Optional::isPresent)
                               .map(Optional::get)
                               .collect(Collectors.toList());
        }

        List<ColumnInfo> asColumnInfos(String path) {
            Matcher matcher = syntax.getColumnPathPattern()
                                    .matcher(path);

            if (matcher.find()) {
                return syntax.getMultiColumnSplitter()
                             .splitToList(matcher.group(Syntax.COLUMNS_GROUP))
                             .stream()
                             .map(column -> new ColumnInfo(matcher.group(Syntax.PATH_GROUP), column))
                             .collect(Collectors.toList());
            }

            return ImmutableList.of();
        }

        String asPath(List<String> pathElements) {
            StringBuilder path = new StringBuilder();
            if (!pathElements.isEmpty() && !pathElements.get(0)
                                                        .startsWith(syntax.getPathSeparator())) {
                path.append(syntax.getPathSeparator());
            }
            syntax.getPathJoiner()
                  .appendTo(path, pathElements);

            return path.toString();
        }

        String asPath(String... pathAndPathElements) {
            return asPath(Arrays.asList(pathAndPathElements));
        }
    }

    private class ColumnInfo {
        private final String path;
        private final String name;
        private final Optional<Integer> priority;
        private final boolean isId;

        ColumnInfo(String path, String name, Optional<Integer> priority, boolean isId) {
            this.path = path;
            this.name = name;
            this.priority = priority;
            this.isId = isId;
        }

        ColumnInfo(String path, String name) {
            this(path, name, Optional.empty(), false);
        }

        String getPath() {
            return path;
        }

        String getFullPath() {
            return pathUtil.asPath(path, name);
        }

        boolean noPriority() {
            return !priority.isPresent();
        }

        int getPriority() {
            return priority.get();
        }
    }

    private static class TableInfo {
        private final String path;
        private final List<ColumnInfo> columns;
        private TableInfo parent;
        private final List<TableInfo> children;

        TableInfo(String path, List<ColumnInfo> columns) {
            this.path = path;
            this.columns = ImmutableList.copyOf(columns);
            this.children = new ArrayList<>();
        }

        TableInfo(String path) {
            this(path, ImmutableList.of());
        }

        String getPath() {
            return path;
        }

        List<ColumnInfo> getColumns() {
            return columns;
        }

        boolean hasParent() {
            return Objects.nonNull(parent);
        }

        TableInfo getParent() {
            return parent;
        }

        void setParent(TableInfo newParent) {
            if (hasParent()) {
                parent.removeChild(this);
            }
            newParent.addChild(this);
            this.parent = newParent;
        }

        ImmutableList<TableInfo> getChildren() {
            return ImmutableList.copyOf(children);
        }

        void addChild(TableInfo child) {
            children.add(child);
        }

        void removeChild(TableInfo child) {
            children.remove(child);
        }
    }
}
