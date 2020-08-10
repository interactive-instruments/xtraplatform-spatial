/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.pgis;

import akka.japi.Pair;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author zahnen
 */
@Value.Immutable
public abstract class SqlPathTree {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlPathTree.class);

    enum TYPE {
        MAIN,
        MERGED,
        ID_1_1,
        ID_M_N,
        ID_1_N,
        REF,
        UNDECIDED
    }

    abstract TYPE getType();

    abstract List<String> getParentPaths();

    abstract List<SqlPathTree> getChildren();

    abstract String getPath();

    abstract List<String> getColumnPaths();

    @Value.Derived
    List<String> getTrail() {
        return new ImmutableList.Builder<String>()
                .addAll(getParentPaths())
                .add(getPath())
                .build();
    }

    @Value.Derived
    List<String> getColumns() {
        return getColumnPaths().stream()
                               .filter(col -> col.indexOf("/") < col.lastIndexOf("/"))
                               .map(col -> col.substring(col.lastIndexOf("/") + 1))
                               .collect(Collectors.toList());
    }

    @Value.Derived
    String getTableName() {
        List<Pair<String, Optional<List<String>>>> joinPathElements = getJoinPathElements();
        return joinPathElements.get(joinPathElements.size() - 1)
                               .first();
    }

    @Value.Derived
    List<Pair<String, Optional<List<String>>>> getJoinPathElements() {
        return Builder.getJoinPathElements(getPath());
    }


    //protected abstract List<String> getPaths();

    static class Builder extends ImmutableSqlPathTree.Builder {

        public Builder fromPaths(String... sqlPaths) {
            return Arrays.stream(sqlPaths)
                         .flatMap(splitDoubleColumnPath())
                         .collect(Collectors.collectingAndThen(groupByPath(), this::buildQueries));
        }

        public Builder fromPaths(List<String> sqlPaths) {
            return sqlPaths
                    .stream()
                    .flatMap(splitDoubleColumnPath())
                    .collect(Collectors.collectingAndThen(groupByPath(), this::buildQueries));
        }

        @Override
        public ImmutableSqlPathTree build() {


            // TODO: parent wrong, henne ei problem

            ImmutableSqlPathTree rootNode = super.build();

            List<SqlPathTree.Builder> rootChildrenBuilder = rootNode.getChildren()
                                                                    .stream()
                                                                    .map(c -> new Builder().path(c.getPath())
                                                                                           .columnPaths(c.getColumnPaths())
                                                                                           .type(c.getType())
                                                                                           .parentPaths(c.getParentPaths()))
                                                                    .collect(Collectors.toList());
            List<SqlPathTree.Builder> rootChildrenBuilder2 = Lists.newArrayList(rootChildrenBuilder);

            Builder newRoot = new Builder().path(rootNode.getPath())
                                           .columnPaths(rootNode.getColumnPaths())
                                           .type(rootNode.getType())
                    .parentPaths(rootNode.getParentPaths());

            for (int i = 0; i < rootNode.getChildren()
                                        .size(); i++) {
                SqlPathTree child = rootNode.getChildren()
                                            .get(i);

                for (int j = 0; j < rootNode.getChildren()
                                            .size(); j++) {
                    SqlPathTree child2 = rootNode.getChildren()
                                                 .get(j);
                    if (child.contains(child2)) {
                        rootChildrenBuilder.get(i)
                                           .addChildren(child2);
                        rootChildrenBuilder2.remove(rootChildrenBuilder.get(j));
                    }
                }

                //rootChildren.remove(child);
                //rootChildren.add(newChild.build());
            }

            List<ImmutableSqlPathTree> newChildren = rootChildrenBuilder2.stream()
                                                                         .map(childBuilder -> {


                                                                             return childBuilder.buildNested();
                                                                         })
                                                                         .map((ImmutableSqlPathTree node) -> shortenPath(node, rootNode.getPath(), rootNode.getParentPaths()))
                                                                         .map((ImmutableSqlPathTree node) -> determineType(node, rootNode.getType()))
                                                                         .collect(Collectors.toList());

            return newRoot.children(newChildren)
                          .buildNested();
        }

        private ImmutableSqlPathTree buildNested() {
            return super.build();
        }

        private ImmutableSqlPathTree shortenPath(ImmutableSqlPathTree node, String parentPath, List<String> parentPaths) {
            return new Builder().from(node)
                                .path(node.getPath()
                                          .substring(parentPath.length() + Joiner.on("").join(parentPaths).length()))
.parentPaths(new ImmutableList.Builder<String>().addAll(parentPaths).add(parentPath).build())
                                .build();
        }

        private ImmutableSqlPathTree determineType(ImmutableSqlPathTree node, TYPE parentType) {
            if (node.getType() != TYPE.UNDECIDED) return node;

            TYPE type = null;

            if ((parentType == TYPE.MAIN || parentType == TYPE.MERGED) && node.getPath()
                                                                              .startsWith("/[id=id]") && !node.getPath().contains("_2_")) {
                type = TYPE.MERGED;
            } else {
                List<Pair<String, Optional<List<String>>>> joinPathElements = getJoinPathElements(node.getPath());

                if (joinPathElements.stream().anyMatch(stringOptionalPair -> stringOptionalPair.first().contains("_2_"))) {
                    if (joinPathElements.size() == 1) {
                        type = TYPE.ID_1_N;
                    } else {
                        type = TYPE.ID_M_N;
                    }
                } else if (joinPathElements.get(0)
                                           .second()
                                           .isPresent() && joinPathElements.get(0)
                                                                           .second()
                                                                           .get()
                                                                           .get(0)
                                                                           .equals("id")) {
                    type = TYPE.ID_1_N;
                } else {
                    type = TYPE.ID_1_1;
                }
            }

            Objects.requireNonNull(type, "Type of SQL sub path could not be determined");

            return new Builder().from(node)
                                .type(type)
                                .buildNested();
        }

        private Function<String, Stream<String>> splitDoubleColumn() {
            return column -> Splitter.on(':')
                                     .omitEmptyStrings()
                                     .splitToList(column)
                                     .stream();
        }

        private Function<String, Stream<String>> splitDoubleColumnPath() {
            return path -> {
                Optional<String> column = getColumnElement(path);
                if (column.isPresent() && column.get()
                                                .contains(":")) {
                    return splitDoubleColumn().apply(column.get())
                                              .map(col -> path.substring(0, path.lastIndexOf("/") + 1) + col);
                }
                return Stream.of(path);
            };
        }

        private Optional<String> getColumnElement(String path) {
            return path.lastIndexOf("/") < 1 ? Optional.empty() : Optional.of(path.substring(path.lastIndexOf("/") + 1));
        }

        private Collector<String, ?, LinkedHashMap<String, List<String>>> groupByPath() {
            return Collectors.groupingBy(matchPath(), LinkedHashMap::new, Collectors.toList());
        }

        private Function<String, String> matchPath() {
            return path -> path.lastIndexOf("/") > 0 ? path.substring(0, path.lastIndexOf("/")) : path;
        }

        private Builder buildQueries(Map<String, List<String>> queryGroups) {
            Map<String, String> collect2 = new LinkedHashMap<>();
            List<String> collect3 = new ArrayList<>();

            queryGroups.keySet()
                       .stream()
                       //TODO
                       .sorted(Comparator.comparingInt(s -> Splitter.on('/')
                                                                    .omitEmptyStrings()
                                                                    .splitToList(s)
                                                                    .size()))
                       .forEach(path -> {
                           boolean shortened = false;
                           int i = collect3.size() - 1;
                           for (; i >= 0; i--) {
                               String prevPath = collect3.get(i);
                               if (path.startsWith(prevPath)) {
                                   collect2.put(path, path.substring(prevPath.length()));

                                   shortened = true;
                                   break;
                               }
                           }
                           //split up paths with at least 2 m:n relations where the object table in the middle has no column mappings
                           if (shortened && path.split("/").length > 4 && collect2.get(path).split("_2_").length > 2) {
                               if (LOGGER.isTraceEnabled()) {
                                   LOGGER.trace("SPLIT PATH {}", path);
                               }

                               int splitAt = path.indexOf("_2_");

                               while (splitAt > -1) {
                                   splitAt = path.indexOf("/", splitAt);
                                   splitAt = path.indexOf("/", splitAt+1);
                                   if (splitAt > -1) {
                                       String parentPath = path.substring(0, splitAt);
                                       if (LOGGER.isTraceEnabled()) {
                                           LOGGER.trace("PARTIAL {}", parentPath);
                                       }

                                       collect2.put(parentPath, parentPath);
                                       collect2.put(path, parentPath);
                                       collect3.add(parentPath);

                                       splitAt = path.indexOf("_2_", splitAt);
                                   }
                               }
                           }
                           if (!shortened)
                               collect2.put(path, path);
                           collect3.add(path);
                       });

            List<String> sortedPaths = ImmutableList.copyOf(collect2.keySet());

            SqlPathTree.Builder root = new SqlPathTree.Builder()
                    .path(sortedPaths.get(0))
                    .columnPaths(queryGroups.get(sortedPaths.get(0)));

            boolean isMain = true;
            for (int i = 1; i < sortedPaths.size(); i++) {
                List<String> columnsPaths = Optional.ofNullable(queryGroups.get(sortedPaths.get(i))).orElse(ImmutableList.of());
                Builder node = new Builder()
                        .path(sortedPaths.get(i))
                        .columnPaths(columnsPaths);

                if (sortedPaths.get(i)
                               .startsWith(sortedPaths.get(0) + "/[id=id]") && columnsPaths.stream()
                                                                                           .anyMatch(col -> col.endsWith("/id"))) {
                    isMain = false;
                    node.type(TYPE.MAIN);
                } else {
                    node.type(TYPE.UNDECIDED);
                }

                root.addChildren(node.buildNested());
            }

            root.type(isMain ? TYPE.MAIN : TYPE.MERGED);

            return root;
        }

        private static List<String> getPathElements(String path) {
            return Splitter.on('/')
                           .omitEmptyStrings()
                           .splitToList(path);
        }

        private static List<Pair<String, Optional<List<String>>>> getJoinPathElements(String path) {
            List<String> pathElements = getPathElements(path);

            return pathElements.stream()
                               .map(Builder::getTableAndJoinCondition)
                               .collect(Collectors.toList());
        }

        private static Pair<String, Optional<List<String>>> getTableAndJoinCondition(String pathElement) {
            Optional<List<String>> joinCondition = pathElement.contains("]") ? Optional.of(Splitter.on('=')
                                                                                                   .omitEmptyStrings()
                                                                                                   .splitToList(pathElement.substring(1, pathElement.indexOf("]")))) : Optional.empty();
            String table = joinCondition.isPresent() ? pathElement.substring(pathElement.indexOf("]") + 1) : pathElement;
            return new Pair<>(table, joinCondition);
        }
    }

    boolean contains(SqlPathTree node) {
        return node.getPath()
                   .startsWith(getPath()) && !Objects.equals(node.getPath(), getPath());
    }

    public List<SqlPathTree> findChild(String path) {
        return findChild(path::equals);
    }

    public SqlPathTree findChildEndsWith(String path) {
        List<SqlPathTree> trail = findChild(nextPath -> nextPath.endsWith(path));

        return trail.get(trail.size()-1);
    }

    public List<SqlPathTree> findChildTrailEndsWith(String path) {
        return findChild(nextPath -> nextPath.endsWith(path));
    }

    public List<SqlPathTree> findChild(Predicate<String> matcher) {
        List<SqlPathTree> trail = new ArrayList<>();

        SqlPathTree last = null;
        //int branch = 0;
        Map<String,Pair<AtomicInteger,AtomicInteger>> branchs = new LinkedHashMap<>();
        for (SqlPathTree next: depthFirst(this)) {
            if (matcher.test(next.getPath())) {
                trail.add(next);
                return trail;
            }
            //trail.add(next);


            // branch
            if (next.getChildren().size() > 0) {
                trail.add(next);
                branchs.put(next.getPath(), new Pair<>(new AtomicInteger(next.getChildren().size()), new AtomicInteger(0)));
                //branch++;
            } /*else if (!next.getChildren().isEmpty()) {
                trail.add(next);
                branchs.get(Lists.reverse(Lists.newArrayList(branchs.keySet())).iterator().next()).second().incrementAndGet();
            }*/
            // dead end, rewind
            else {
                Pair<AtomicInteger, AtomicInteger> branch = branchs.get(trail.get(trail.size() - 1)
                                                                                                     .getPath());

                while (branch != null) {
                    int width = branch.first()
                                      .decrementAndGet();
                    int depth = branch.second()
                                      .get();

                    if (width == 0) {
                        branchs.remove(trail.get(trail.size() - 1)
                                            .getPath());
                        trail.remove(trail.size() - 1);

                        branch = branchs.get(trail.get(trail.size() - 1)
                                                  .getPath());
                    } else {
                        branch = null;
                    }
                }

                //if (depth > 0)
                //    branchs.remove(branchs.size()-1);
                /*int size = trail.size();
                for (int i = size-1; i > size-1-depth; i--) {
                    trail.remove(i);
                }*/
                //branch = 0;
            }

            last = next;
        }

        return trail;
    }

    public List<String> getAdditionalSortKeys(String path) {
        List<String> keyList = new ArrayList<>();
        boolean hasMultiParent = false;
        int skeys = 1;
        SqlPathTree last = null;
        List<SqlPathTree> trail = findChild(path);
        for (SqlPathTree next: trail) {
            if (next.getType() == SqlPathTree.TYPE.ID_M_N || next.getType() == SqlPathTree.TYPE.ID_1_N) {
                if (!hasMultiParent) {
                    hasMultiParent = true;
                } else {
                    keyList.add(String.format("%s.%s AS SKEY_%s",last.getTableName(), next.getJoinPathElements().get(0).second().get().get(0), skeys++));
                }
            }
            last = next;
        }
        if (last != null) {
         if (!last.getTableName().contains("_2_")) {
             keyList.add(String.format("%s.%s AS SKEY_%s", last.getTableName(), "id", skeys));
         } else if (!last.getColumns().isEmpty()) {
             keyList.add(String.format("%s.%s AS SKEY_%s", last.getTableName(), last.getColumns().get(last.getColumns().size()-1), skeys));
         }
        }
        return keyList;
    }

    public static Iterable<SqlPathTree> breadthFirst(SqlPathTree node) {
        return () -> new Iterator<SqlPathTree>() {
            Deque<SqlPathTree> queue = new ArrayDeque<>(Arrays.asList(node));

            @Override
            public boolean hasNext() {
                return !queue.isEmpty();
            }

            @Override
            public SqlPathTree next() {
                if (queue.isEmpty()) {
                    throw new NoSuchElementException();
                }
                SqlPathTree next = queue.removeFirst();
                next.getChildren().forEach(queue::addLast);
                return next;
            }
        };
    }

    public static Iterable<SqlPathTree> depthFirst(SqlPathTree node) {
        return () -> new Iterator<SqlPathTree>() {
            List<SqlPathTree> trail = new ArrayList<>();
            Deque<SqlPathTree> queue = new ArrayDeque<>(Arrays.asList(node));

            @Override
            public boolean hasNext() {
                return !queue.isEmpty();
            }

            @Override
            public SqlPathTree next() {
                if (queue.isEmpty()) {
                    throw new NoSuchElementException();
                }
                SqlPathTree next = queue.removeLast();
                List<SqlPathTree> children = next.getChildren();
                ListIterator<SqlPathTree> iterator = children.listIterator(children.size());
                while (iterator.hasPrevious()) {
                    queue.addLast(iterator.previous());
                }


                return next;
            }
        };
    }
}
