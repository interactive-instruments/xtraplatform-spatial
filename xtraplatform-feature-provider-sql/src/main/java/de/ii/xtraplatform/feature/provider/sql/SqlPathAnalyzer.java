package de.ii.xtraplatform.feature.provider.sql;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SqlPathAnalyzer {

    private final SqlPathSyntax syntax;

    public SqlPathAnalyzer(SqlPathSyntax syntax) {
        this.syntax = syntax;
    }

    /**
     * creates a tree of {@link SqlPathTable} with parent and child links
     *
     * @param sqlPaths sql paths
     * @return tree of {@link SqlPathTable}
     */
    public List<SqlPathTable> parse(List<SqlPath> sqlPaths) {
        List<SqlPath> sortedPaths = sqlPaths
                .stream()
                .sorted(this::sortByPriority)
                .collect(Collectors.toList());

        List<SqlPath> mergedPaths = syntax.merge(sortedPaths);

        List<SqlPathTable> roots = toTableTrees(mergedPaths);

        if (roots.isEmpty()) {
            throw new IllegalStateException("no root path found");
        }

        return roots;
    }

    private List<SqlPathTable> toTableTrees(List<SqlPath> pathsWithColumns) {

        Set<SqlPath> allPaths = fanOutObjectTables(pathsWithColumns);

        return allPaths.stream()
                       .filter(syntax::isRoot)
                       .map(rootPath -> toTableTree(rootPath, allPaths))
                       .collect(ImmutableList.toImmutableList());
    }

    private SqlPathTable toTableTree(SqlPath root, Set<SqlPath> all) {

        List<SqlPath> children = new ArrayList<>();

        for (SqlPath child: all) {
            if (!Objects.equals(child.getTablePath(), root.getTablePath())
                    && child.getTablePath()
                            .startsWith(root.getTablePath())) {

                children.add(child);
            }
        }



        return new ImmutableSqlPathTable.Builder().build();
    }

    private Set<SqlPath> fanOutObjectTables(List<SqlPath> pathsWithColumns) {

        Set<String> existingPaths = pathsWithColumns.stream()
                                                    .map(SqlPath::getTablePath)
                                                    .collect(Collectors.toSet());

        return pathsWithColumns.stream()
                               .flatMap(sqlPath -> {
                                   Stream<SqlPath> newPaths = syntax.getParentPaths(sqlPath)
                                                                    .stream()
                                                                    .filter(parentPath -> existingPaths.add(parentPath.getTablePath()));

                                   return Stream.concat(newPaths, Stream.of(sqlPath));
                               })
                               .collect(ImmutableSet.toImmutableSet());
    }

    //TODO: test with mappings without sortPriority, e.g. daraa
    private int sortByPriority(SqlPath path1, SqlPath path2) {
        return !path1.getSortPriority()
                     .isPresent() ? 1 : !path2.getSortPriority()
                                              .isPresent() ? -1 : path1.getSortPriority()
                                                                       .getAsInt() - path2.getSortPriority()
                                                                                     .getAsInt();
    }
}
