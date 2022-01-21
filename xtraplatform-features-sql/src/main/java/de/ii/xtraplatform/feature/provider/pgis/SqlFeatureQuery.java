/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.pgis;

import akka.japi.Pair;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.features.domain.FeatureStoreInstanceContainer;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author zahnen
 */
@Value.Immutable
public abstract class SqlFeatureQuery implements FeatureStoreInstanceContainer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlFeatureQuery.class);

    @Override
    @Value.Lazy
    public String getName() {
        return getTableName();
    }

    @Override
    @Value.Lazy
    public String getSortKey() {
        return getSortColumn();
    }

    @Override
    @Value.Lazy
    public List<String> getPath() {
        return getPathElements(getPaths().get(0).substring(0, getPaths().get(0).lastIndexOf("/")));
    }

    @Override
    @Value.Lazy
    public String getInstanceContainerName() {
        return getPathElements(getPaths().get(0)).get(0);
    }

    protected abstract List<String> getPaths();

    protected abstract SqlPathTree getSqlPaths();

    @Value.Derived
    public String getTableName() {
        List<String> path = getPathElements(getPaths().get(0));
        return getTableAndJoinCondition(getLastTableElement(path)).first();
    }

    @Value.Derived
    public List<List<String>> getMatchPaths() {
        return getPaths().stream()
                         .map(this::getPathElements)
                         .filter(path -> path.size() > 1)
                         .flatMap(splitDoubleColumnPath())
                         .map(this::stripFunctionsPath)
                         .collect(Collectors.toList());
    }

    @Value.Derived
    public List<String> getColumnNames() {
        return getColumnStream()
                .map(this::stripFunctions)
                .collect(Collectors.toList());
    }

    @Value.Derived
    public String toSql(String whereClause, int limit, int offset) {
        String mainTable = getPathElements(getPaths().get(0)).get(0);
        List<String> sortFields = getSortFields();

        String columns = Stream.concat(sortFields.stream(), getColumnStream().map(this::getQualifiedColumn))
                               .collect(Collectors.joining(", "));


        String join = getJoinPathElements(getPaths().get(0)).stream()
                                                            .map(tableAndJoinCondition -> String.format("JOIN %s ON %s", tableAndJoinCondition.first().equals(mainTable) ? tableAndJoinCondition.first() + " AS MAIN2" : tableAndJoinCondition.first(), tableAndJoinCondition.first().equals(mainTable) ? tableAndJoinCondition.second().get().replace(mainTable, "MAIN2") : tableAndJoinCondition.second()
                                                                                                                                                                             .get()))
                                                            .collect(Collectors.joining(" "));

        String limit2 = limit > 0 ? " LIMIT " + limit : "";
        String offset2 = offset > 0 ? " OFFSET " + offset : "";
        String where = Strings.isNullOrEmpty(whereClause) ? "" : " WHERE " + whereClause;
        String orderBy = IntStream.rangeClosed(1, sortFields.size())
                                  .boxed()
                                  .map(String::valueOf)
                                  .collect(Collectors.joining(","));

        return String.format("SELECT %s FROM %s%s%s%s ORDER BY %s%s%s", columns, mainTable, join.isEmpty() ? "" : " ", join, where, orderBy, limit2, offset2);
    }

    @Value.Derived
    public String toSqlSimple() {
        String mainTable = getPathElements(getPaths().get(0)).get(0);

        String columns = getColumnStream().map(this::getQualifiedColumn)
                               .collect(Collectors.joining(", "));


        String join = getJoinPathElements(getPaths().get(0)).stream()
                                                            .map(tableAndJoinCondition -> String.format("JOIN %s ON %s", tableAndJoinCondition.first().equals(mainTable) ? tableAndJoinCondition.first() + " AS MAIN2" : tableAndJoinCondition.first(), tableAndJoinCondition.first().equals(mainTable) ? tableAndJoinCondition.second().get().replace(mainTable, "MAIN2") : tableAndJoinCondition.second()
                                                                                                                                                                                                                                                                                                                                                                                                  .get()))
                                                            .collect(Collectors.joining(" "));

        return String.format("SELECT %s FROM %s%s%s", columns, mainTable, join.isEmpty() ? "" : " ", join);
    }

    private Stream<String> getColumnStream() {
        return getPaths().stream()
                         .map(this::getPathElements)
                         .map(this::getColumnElement)
                         .filter(Optional::isPresent)
                         .map(Optional::get)
                         .flatMap(splitDoubleColumn());
    }

    private List<String> getPathElements(String path) {
        return Splitter.on('/')
                       .omitEmptyStrings()
                       .splitToList(path);
    }

    private List<Pair<String, Optional<String>>> getJoinPathElements(String path) {
        List<String> pathElements = getPathElements(path);

        if (pathElements.size() <= 2) {
            return new ArrayList<>();
        }

        String[] lastTable = {pathElements.get(0)};
        List<String> tables = new ArrayList<>();
        String mainTable = getTableName();

        return pathElements.subList(1, pathElements.size() - 1)
                           .stream()
                           .map(this::getTableAndJoinCondition)
                           .map(tableAndJoinCondition -> {
                               Pair<String, Optional<String>> tajc = tableAndJoinCondition;
                               if (tableAndJoinCondition.first()
                                                        .equals(mainTable) && tables.size() < pathElements.size() - 3) {
                                   //TODO: increment alias names
                                   tajc = new Pair<>(tableAndJoinCondition.first() + " AS A", tableAndJoinCondition.second());
                                   LOGGER.debug("ALIASING {} {} {}", mainTable, tables.size(), pathElements.size());
                               }
                               tables.add(tableAndJoinCondition.first());

                               Pair<String, Optional<String>> qualifiedTableAndJoinCondition = qualifyTableAndJoinCondition(tajc, lastTable[0]);
                               lastTable[0] = qualifiedTableAndJoinCondition.first();

                               return qualifiedTableAndJoinCondition;
                           })
                           .collect(Collectors.toList());
    }

    private String getLastTableElement(List<String> path) {
        return path.size() <= 2 ? path.get(0) : path.get(path.size() - 2);
    }

    private Optional<String> getColumnElement(List<String> path) {
        return path.size() <= 1 ? Optional.empty() : Optional.of(path.get(path.size() - 1));
    }

    private String stripFunctions(String column) {
        return column.contains("(") ? column.substring(column.lastIndexOf("(") + 1, column.indexOf(")")) : column;
    }

    private List<String> stripFunctionsPath(List<String> path) {
        Optional<String> column = getColumnElement(path);
        if (column.isPresent()) {
            return ImmutableList.<String>builder().addAll(path.subList(0, path.size() - 1))
                                                  .add(stripFunctions(column.get()))
                                                  .build();
        }
        return path;
    }

    private Pair<String, Optional<String>> getTableAndJoinCondition(String pathElement) {
        Optional<String> joinCondition = pathElement.contains("]") ? Optional.of(pathElement.substring(1, pathElement.indexOf("]"))) : Optional.empty();
        String table = joinCondition.isPresent() ? pathElement.substring(pathElement.indexOf("]") + 1) : pathElement;
        return new Pair<>(table, joinCondition);
    }

    private Pair<String, Optional<String>> qualifyTableAndJoinCondition(Pair<String, Optional<String>> tableAndJoinCondition, String sourceTable) {
        String sourceTable2 = sourceTable.contains(" AS ") ? sourceTable.substring(sourceTable.lastIndexOf(" ") + 1) : sourceTable;
        String targetTable = tableAndJoinCondition.first()
                                                  .contains(" AS ") ? tableAndJoinCondition.first()
                                                                                           .substring(tableAndJoinCondition.first()
                                                                                                                           .lastIndexOf(" ") + 1) : tableAndJoinCondition.first();
        return tableAndJoinCondition.second()
                                    .isPresent()
                ? new Pair<>(tableAndJoinCondition.first(), tableAndJoinCondition.second()
                                                                                 .map(joinCondition -> joinCondition.replaceAll("(\\w+)=(\\w+)", String.format("%s.$1=%s.$2", sourceTable2, targetTable))))
                : tableAndJoinCondition;
    }

    @Value.Derived
    public SqlPathTree getSqlPath() {
        return getSqlPaths().findChildEndsWith("/" + getMatchPaths().get(0)
                                                                    .get(getMatchPaths().get(0)
                                                                                        .size() - 2));
    }

    @Value.Derived
    public Optional<SqlPathTree> getSqlPathParent() {
        List<SqlPathTree> childTrailEndsWith = getSqlPaths().findChildTrailEndsWith("/" + getMatchPaths().get(0)
                                                                                                         .get(getMatchPaths().get(0)
                                                                                                                             .size() - 2));

        if (childTrailEndsWith.size() > 1) {
            return Optional.of(childTrailEndsWith.get(childTrailEndsWith.size()-2));
        }
        return Optional.empty();
    }

    public List<String> getSortFields() {
        String path = getSqlPath().getPath();
        return ImmutableList.<String>builder().add(getSortColumn() + " AS SKEY")
                                              .addAll(getSqlPaths().getAdditionalSortKeys(path))
                                              .build();
    }

    public String getSortColumn() {
        List<Pair<String, Optional<String>>> joinPathElements = getJoinPathElements(getPaths().get(0));
        //TODO: might be different fields for any join, so collect from subqueries


        //TODO: can this by anything else than id of first table?
//        if (joinPathElements.isEmpty()) {
            return getPathElements(getPaths().get(0)).get(0) + ".id";
//        }

/*        return joinPathElements.get(0)
                               .second()
                               .map(joinCondition -> joinCondition.replaceAll("(\\w+\\.\\w+)=\\w+\\.\\w+", "$1"))
                               .get();
                               */
    }

    private Function<String, Stream<String>> splitDoubleColumn() {
        return column -> Splitter.on(':')
                                 .omitEmptyStrings()
                                 .splitToList(column)
                                 .stream();
    }

    private Function<List<String>, Stream<List<String>>> splitDoubleColumnPath() {
        return path -> {
            Optional<String> column = getColumnElement(path);
            if (column.isPresent() && column.get()
                                            .contains(":")) {
                return splitDoubleColumn().apply(column.get())
                                          .map(col -> ImmutableList.<String>builder().addAll(path.subList(0, path.size() - 1))
                                                                                     .add(col)
                                                                                     .build());
            }
            return Stream.of(path);
        };
    }

    private String getQualifiedColumn(String column) {
        return column.contains("(")
                ? column.replaceAll("((?:\\w+\\()+)(\\w+)((?:\\))+)", "$1" + getTableName() + ".$2$3 AS $2")
                : String.format("%s.%s", getTableName(), column);
    }


    private String cqlToSql(String cql) {
        if (cql.startsWith("IN (")) {
            //TODO
            return getSortColumn() + " " + cql.replaceAll("'", "");
        }

        return cql;
    }
}
