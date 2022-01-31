/**
 * Copyright 2022 interactive instruments GmbH
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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author zahnen
 */
abstract class NestedSql<T extends NestedSql> {
    enum TYPE {
        MAIN,
        MERGED,
        ID_1_1,
        ID_M_N,
        ID_1_N,
        REF
    }

    final String path;
    final List<T> nestedPaths;
    List<String> columns;
    T parent;
    T mainPath;
    public TYPE type;

    // build helpers
    final List<T> nestedPaths2;
    final List<String> mergeTables;

    public NestedSql(String path, List<String> columnPaths) {
        this(path, columnPaths, null);
    }

    public NestedSql(String path, List<String> columnPaths, T parent) {
        this.path = path;
        nestedPaths = new ArrayList<>();
        columns = new ArrayList<>();
        nestedPaths2 = new ArrayList<>();
        mergeTables = new ArrayList<>();
        this.parent = parent;

        //TODO
        if (path.endsWith("[id=id]osirisobjekt")) {
            //columns.add("id");
        }
        columns.addAll(columnPaths.stream()
                                  .map(column -> column.startsWith(path + "/") ? column.substring(path.length() + 1) : column.startsWith(path) ? column.substring(path.length()) : column)
                                  .filter(col -> !col.isEmpty())
                                  .collect(Collectors.toList()));
    }

    protected List<T> getNestedSqls() {
        return nestedPaths;
    }

    protected T getParent() {
        return parent;
    }

    public List<T> getPaths() {
        List<T> parents = new ArrayList<>();
        T parent = (T) this;
        while (parent != null) {
            parents.add(0, parent);
            parent = (T) parent.getParent();
        }
        return parents;
    }

    //TODO
    boolean ignoreId = true;

    String getColumnValues(List<String> columnPaths, List<String> columnNames, Map<String, String> values, Map<String, String> ids) {
        List<String> columnValues = columnPaths.stream()
                                               .map(col -> col.endsWith(".id") ? ids.get(col) : values.get(col))
                                               .collect(Collectors.toList());

        for (int j = 0; j < columnNames.size(); j++) {
            if (columnNames.get(j)
                           .startsWith("ST_AsText(ST_ForcePolygonCCW(")) {
                columnValues.set(j, "ST_ForcePolygonCW(ST_GeomFromText(" + columnValues.get(j) + ",25832))"); //TODO srid from config
                break;
            }
        }

        return columnValues.stream()
                           .collect(Collectors.joining(","));
    }
    // TODO: FeatureTransformerFromGeoJson gets above paths and multiplicity related to json path e.g (1,1) or (2.1)
    // as only the first json path element is referenced, transform multiplicities to e.g. (1) or (2)

    // TODO: rows = counts per parent index (1,2)
    // TODO: parentRows = current index path zero based for NestedRow (0,0,0,0,0), (0,0,0,1,0), (0,0,0,1,1)

    T findMainPath() {
        Optional<T> mainPath = Optional.empty();/*TODO Stream.concat(Stream.of(this), nestedPaths.stream())
                                             .filter(NestedSql::isMain)
                                             .findFirst();*/
        if (!mainPath.isPresent()) {
            throw new IllegalStateException("No id mapping found");
        }

        mainPath.get()
                .setType(TYPE.MAIN);

        return mainPath.get();
    }

    List<T> findMergedPaths(T mainPath) {
        List<T> mergedPaths = Lists.newArrayList();
        // root is main
        if (mainPath.equals(this)) {

        }
        // root child is main
        else if (nestedPaths.contains(mainPath)) {
            //TODO mergedPaths.add(this);
            mergedPaths.addAll(nestedPaths.stream()
                                          .filter(nestedPath -> !nestedPath.equals(mainPath) && nestedPath.path.startsWith("/[id=id]") && !nestedPath.path.contains("_2_"))
                                          .collect(Collectors.toList()));
        }
        mergedPaths.forEach(mergedPath -> mergedPath.setType(TYPE.MERGED));

        return mergedPaths;
    }

    void findIdTable() {
    }

    void findMultiTable() {
    }

    void nest() {
        nest(true);
    }

    void determineTypes() {
        T mainPath = findMainPath();
        List<T> mergedPaths = findMergedPaths(mainPath);

        determineType(mainPath, mergedPaths);
    }

    void nest(boolean determineTypes) {
        nestedPaths2.forEach(nestedPath -> {
            nestedPaths2.forEach(nestedPath2 -> {
                if (nestedPath.contains(nestedPath2)) {
                    nestedPath.add(nestedPath2);
                    nestedPaths.remove(nestedPath2);
                }
            });
            nestedPath.nest(false);
        });

        if (determineTypes) {
            determineTypes();
        }
    }

    void add(T nestedPath) {
        if (contains(nestedPath)) {
            T nestedPath1 = null;//TODO new T(nestedPath.path.substring(path.length()), nestedPath.columns, this);
            nestedPaths.add(nestedPath1);
            nestedPaths2.add(nestedPath1);
            /*if (nestedPath1.determineType() == TYPE.MAIN) {
                mainTable = nestedPath1.getPathElements()
                                       .get(getPathElements().size() - 1);
                nestedPath1.setType(TYPE.MAIN);
                //mergeTables.addAll(nestedPaths.stream().filter(nestedPath2 -> ));
            }*/
        }
    }

    boolean contains(T nestedPath) {
        return nestedPath.path.startsWith(path) && !Objects.equals(nestedPath.path, path);
    }

    //TODO
    boolean isMain() {
        return columns.contains("id");
    }

    void determineType(T mainPath, List<T> mergedPaths) {
        this.mainPath = mainPath;

        List<Pair<String, Optional<List<String>>>> joinPathElements = getJoinPathElements();

        if (mainPath.equals(this) && columns.contains("id")) {
            this.type = TYPE.MAIN;
        } else if (mergedPaths.contains(this)) {
            this.type = TYPE.MERGED;
        } else if (joinPathElements.get(0)
                                   .first()
                                   .contains("_2_")) {
            if (joinPathElements.size() == 1) {
                this.type = TYPE.ID_1_N;
            } else {
                this.type = TYPE.ID_M_N;
            }
        } else if (joinPathElements.get(0)
                                   .second()
                                   .isPresent() && joinPathElements.get(0)
                                                                   .second()
                                                                   .get()
                                                                   .get(0)
                                                                   .equals("id")) {
            this.type = TYPE.ID_1_N;
        } else {
            this.type = TYPE.ID_1_1;
        }

        nestedPaths.forEach(nestedPath -> nestedPath.determineType(mainPath, mergedPaths));
    }

    void setType(TYPE type) {
        this.type = type;
    }

    List<String> getPathElements() {
        return Splitter.on('/')
                       .omitEmptyStrings()
                       .splitToList(path);
    }

    List<Pair<String, Optional<List<String>>>> getJoinPathElements() {
        List<String> pathElements = getPathElements();

        String[] lastTable = {pathElements.get(0)};
        List<String> tables = new ArrayList<>();
        String mainTable = getTableAndJoinCondition(getPathElements().get(getPathElements().size() - 1)).first();

        return pathElements.stream()
                           .map(this::getTableAndJoinCondition)
                           .collect(Collectors.toList());
    }

    Pair<String, Optional<List<String>>> getTableAndJoinCondition(String pathElement) {
        Optional<List<String>> joinCondition = pathElement.contains("]") ? Optional.of(Splitter.on('=')
                                                                                               .omitEmptyStrings()
                                                                                               .splitToList(pathElement.substring(1, pathElement.indexOf("]")))) : Optional.empty();
        String table = joinCondition.isPresent() ? pathElement.substring(pathElement.indexOf("]") + 1) : pathElement;
        return new Pair<>(table, joinCondition);
    }

    String getTableName() {
        return getJoinPathElements().get(getJoinPathElements().size() - 1)
                                    .first();
    }

    //TODO parentPath or fullPath
    List<String> getColumnPaths() {
        return columns.stream()
                      .map(col -> path + "/" + col)
                      .collect(Collectors.toList());
    }
}
