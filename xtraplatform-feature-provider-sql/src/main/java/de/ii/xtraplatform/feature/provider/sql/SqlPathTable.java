/**
 * Copyright 2019 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql;

import akka.japi.Pair;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
@Value.Immutable
@Value.Style(deepImmutablesDetection = true, attributeBuilderDetection = true)
public abstract class SqlPathTable {

    public enum TYPE {
        MAIN,
        MERGED,
        ID_1_1,
        ID_M_N,
        ID_1_N,
        REF,
        UNDECIDED
    }

    /**
     * TODO: maybe split into role and cardinality
     *
     * @return mix of join cardinality and (schema) role of the segment
     */
    public abstract TYPE getType();

    /**
     * @return the substring of the path this segment represents
     */
    public abstract String getPath();

    //TODO: replace getColumnPaths and getColumns with List<SqlColumn> getColumns
    public abstract List<SqlPathColumn> getColumnsNew();

    @Value.Derived
    public List<String> getColumnPaths() {
        return getColumnsNew().stream()
                              .map(SqlPathColumn::getPath)
                              .collect(Collectors.toList());
    }

    //TODO: replace with single link to parent segment? only used in SqlFeatureInserts, save derived path list there?
    @Value.Derived
    public List<String> getParentPaths() {
        Optional<SqlPathTable> parent = Optional.ofNullable(getParent());
        List<String> parentPaths = new ArrayList<>();

        while (parent.isPresent()) {
            parentPaths.add(0, parent.get()
                                     .getPath());

            parent = Optional.ofNullable(parent.get()
                                               .getParent());
        }

        return ImmutableList.copyOf(parentPaths);
    }

    @Nullable
    public abstract SqlPathTable getParent();


    abstract List<SqlPathTable> getChildren();

    //TODO: only used in SqlFeatureInserts, save derived path list there?
    @Value.Derived
    List<String> getTrail() {
        return new ImmutableList.Builder<String>()
                .addAll(getParentPaths())
                .add(getPath())
                .build();
    }

    //TODO: replace getColumnPaths and getColumns with List<SqlColumn> getColumns
    @Value.Derived
    public List<String> getColumns() {
        return getColumnPaths().stream()
                               .filter(col -> col.indexOf("/") < col.lastIndexOf("/"))
                               .map(col -> col.substring(col.lastIndexOf("/") + 1))
                               .collect(Collectors.toList());
    }

    /**
     * @return the target table name
     */
    @Value.Derived
    public String getTableName() {
        List<Pair<String, Optional<List<String>>>> joinPathElements = getJoinPathElements();
        return joinPathElements.get(joinPathElements.size() - 1)
                               .first();
    }

    //TODO: replace with List<SqlJoin>
    @Value.Derived
    public List<Pair<String, Optional<List<String>>>> getJoinPathElements() {
        return Builder.getJoinPathElements(getPath());
    }
    public abstract List<SqlPathJoin> getJoins();


    //protected abstract List<String> getPaths();

    public static class Builder extends ImmutableSqlPathTable.Builder {

        /*@Override
        public ImmutableSqlPathSegment build() {


            // TODO: parent wrong, henne ei problem

            ImmutableSqlPathSegment rootNode = super.build();

            List<SqlPathSegment.Builder> rootChildrenBuilder = rootNode.getChildren()
                                                                       .stream()
                                                                       .map(c -> new Builder().path(c.getPath())
                                                                                           .columnPaths(c.getColumnPaths())
                                                                                           .type(c.getType())
                                                                                           .parentPaths(c.getParentPaths()))
                                                                       .collect(Collectors.toList());
            List<SqlPathSegment.Builder> rootChildrenBuilder2 = Lists.newArrayList(rootChildrenBuilder);

            Builder newRoot = new Builder().path(rootNode.getPath())
                                           .columnPaths(rootNode.getColumnPaths())
                                           .type(rootNode.getType())
                    .parentPaths(rootNode.getParentPaths());

            for (int i = 0; i < rootNode.getChildren()
                                        .size(); i++) {
                SqlPathSegment child = rootNode.getChildren()
                                               .get(i);

                for (int j = 0; j < rootNode.getChildren()
                                            .size(); j++) {
                    SqlPathSegment child2 = rootNode.getChildren()
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

            List<ImmutableSqlPathSegment> newChildren = rootChildrenBuilder2.stream()
                                                                            .map(childBuilder -> {


                                                                             return childBuilder.buildNested();
                                                                         })
                                                                            .map((ImmutableSqlPathSegment node) -> shortenPath(node, rootNode.getPath(), rootNode.getParentPaths()))
                                                                            .map((ImmutableSqlPathSegment node) -> determineType(node, rootNode.getType()))
                                                                            .collect(Collectors.toList());

            return newRoot.children(newChildren)
                          .buildNested();
        }*/

        private ImmutableSqlPathTable buildNested() {
            return super.build();
        }

        /*private ImmutableSqlPathSegment shortenPath(ImmutableSqlPathSegment node, String parentPath, List<String> parentPaths) {
            return new Builder().from(node)
                                .path(node.getPath()
                                          .substring(parentPath.length() + Joiner.on("").join(parentPaths).length()))
.parentPaths(new ImmutableList.Builder<String>().addAll(parentPaths).add(parentPath).build())
                                .build();
        }*/

        private ImmutableSqlPathTable determineType(ImmutableSqlPathTable node, TYPE parentType) {
            if (node.getType() != TYPE.UNDECIDED) return node;

            TYPE type = null;

            if ((parentType == TYPE.MAIN || parentType == TYPE.MERGED) && node.getPath()
                                                                              .startsWith("/[id=id]") && !node.getPath()
                                                                                                              .contains("_2_")) {
                type = TYPE.MERGED;
            } else {
                List<Pair<String, Optional<List<String>>>> joinPathElements = getJoinPathElements(node.getPath());

                if (joinPathElements.stream()
                                    .anyMatch(stringOptionalPair -> stringOptionalPair.first()
                                                                                      .contains("_2_"))) {
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

    boolean contains(SqlPathTable node) {
        return node.getPath()
                   .startsWith(getPath()) && !Objects.equals(node.getPath(), getPath());
    }

    public List<SqlPathTable> findChild(String path) {
        return findChild(path::equals);
    }

    public SqlPathTable findChildEndsWith(String path) {
        List<SqlPathTable> trail = findChild(nextPath -> nextPath.endsWith(path));

        return trail.get(trail.size() - 1);
    }

    public List<SqlPathTable> findChildTrailEndsWith(String path) {
        return findChild(nextPath -> nextPath.endsWith(path));
    }

    public List<SqlPathTable> findChild(Predicate<String> matcher) {
        List<SqlPathTable> trail = new ArrayList<>();

        SqlPathTable last = null;
        //int branch = 0;
        Map<String, Pair<AtomicInteger, AtomicInteger>> branchs = new LinkedHashMap<>();
        for (SqlPathTable next : depthFirst(this)) {
            if (matcher.test(next.getPath())) {
                trail.add(next);
                return trail;
            }
            //trail.add(next);


            // branch
            if (next.getChildren()
                    .size() > 0) {
                trail.add(next);
                branchs.put(next.getPath(), new Pair<>(new AtomicInteger(next.getChildren()
                                                                             .size()), new AtomicInteger(0)));
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

    //TODO: can't this be a member? Value.Derived
    public List<String> getAdditionalSortKeys(String path) {
        List<String> keyList = new ArrayList<>();
        boolean hasMultiParent = false;
        int skeys = 1;
        SqlPathTable last = null;
        List<SqlPathTable> trail = findChild(path);
        for (SqlPathTable next : trail) {
            if (next.getType() == SqlPathTable.TYPE.ID_M_N || next.getType() == SqlPathTable.TYPE.ID_1_N) {
                if (!hasMultiParent) {
                    hasMultiParent = true;
                } else {
                    keyList.add(String.format("%s.%s AS SKEY_%s", last.getTableName(), next.getJoinPathElements()
                                                                                           .get(0)
                                                                                           .second()
                                                                                           .get()
                                                                                           .get(0), skeys++));
                }
            }
            last = next;
        }
        if (last != null) {
            if (!last.getTableName()
                     .contains("_2_")) {
                keyList.add(String.format("%s.%s AS SKEY_%s", last.getTableName(), "id", skeys));
            } else if (!last.getColumns()
                            .isEmpty()) {
                keyList.add(String.format("%s.%s AS SKEY_%s", last.getTableName(), last.getColumns()
                                                                                       .get(last.getColumns()
                                                                                                .size() - 1), skeys));
            }
        }
        return keyList;
    }

    public static Iterable<SqlPathTable> breadthFirst(SqlPathTable node) {
        return () -> new Iterator<SqlPathTable>() {
            Deque<SqlPathTable> queue = new ArrayDeque<>(Arrays.asList(node));

            @Override
            public boolean hasNext() {
                return !queue.isEmpty();
            }

            @Override
            public SqlPathTable next() {
                if (queue.isEmpty()) {
                    throw new NoSuchElementException();
                }
                SqlPathTable next = queue.removeFirst();
                next.getChildren()
                    .forEach(queue::addLast);
                return next;
            }
        };
    }

    public static Iterable<SqlPathTable> depthFirst(SqlPathTable node) {
        return () -> new Iterator<SqlPathTable>() {
            List<SqlPathTable> trail = new ArrayList<>();
            Deque<SqlPathTable> queue = new ArrayDeque<>(Arrays.asList(node));

            @Override
            public boolean hasNext() {
                return !queue.isEmpty();
            }

            @Override
            public SqlPathTable next() {
                if (queue.isEmpty()) {
                    throw new NoSuchElementException();
                }
                SqlPathTable next = queue.removeLast();
                List<SqlPathTable> children = next.getChildren();
                ListIterator<SqlPathTable> iterator = children.listIterator(children.size());
                while (iterator.hasPrevious()) {
                    queue.addLast(iterator.previous());
                }


                return next;
            }
        };
    }
}
