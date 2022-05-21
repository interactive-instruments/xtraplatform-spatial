/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.threeten.extra.Interval;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.ii.xtraplatform.cql.domain.In.ID_PLACEHOLDER;

public class CqlToText implements CqlVisitor<String> {

    protected final static Set<String> KEYWORDS = new ImmutableSet.Builder<String>()
        .add("A_EQUALS", "A_CONTAINS", "A_CONTAINEDBY", "A_OVERLAPS", "ACCENTI", "AND", "BETWEEN", "CASEI", "DATE", "ENVELOPE", "FALSE", "GEOMETRYCOLLECTION", "IN", "LIKE", "LINESTRING", "MULTILINESTRING", "MULTIPOINT", "MULTIPOLYGON", "NOT", "NULL", "OR", "POINT", "POLYGON", "S_INTERSECTS", "S_EQUALS", "S_DISJOINT", "S_TOUCHES", "S_WITHIN", "S_OVERLAPS", "S_CROSSES", "S_CONTAINS", "T_AFTER", "T_BEFORE", "T_CONTAINS", "T_DISJOINT", "T_DURING", "T_EQUALS", "T_FINISHEDBY", "T_FINISHES", "T_INTERSECTS", "T_MEETS", "T_METBY", "T_OVERLAPPEDBY", "T_OVERLAPS", "T_STARTEDBY", "T_STARTS", "TIMESTAMP", "TRUE")
        .build();
    
    protected final static Map<Class<?>, String> LOGICAL_OPERATORS = new ImmutableMap.Builder<Class<?>, String>()
            .put(ImmutableAnd.class, "AND")
            .put(ImmutableOr.class, "OR")
            .put(ImmutableNot.class, "NOT")
            .build();

    protected final static Map<Class<?>, String> SCALAR_OPERATORS = new ImmutableMap.Builder<Class<?>, String>()
            .put(ImmutableEq.class, "=")
            .put(ImmutableNeq.class, "<>")
            .put(ImmutableGt.class, ">")
            .put(ImmutableGte.class, ">=")
            .put(ImmutableLt.class, "<")
            .put(ImmutableLte.class, "<=")
            .put(ImmutableLike.class, "LIKE")
            .put(ImmutableBetween.class, "BETWEEN")
            .put(ImmutableIn.class, "IN")
            .put(ImmutableIsNull.class, "IS NULL")
            .build();

    private final Optional<java.util.function.BiFunction<List<Double>, Optional<EpsgCrs>, List<Double>>> coordinatesTransformer;
    private final boolean isNestedFilter;

    public CqlToText() {
        this.coordinatesTransformer = Optional.empty();
        this.isNestedFilter = false;
    }

    public CqlToText(boolean isNestedFilter) {
        this.coordinatesTransformer = Optional.empty();
        this.isNestedFilter = isNestedFilter;
    }

    public CqlToText(java.util.function.BiFunction<List<Double>, Optional<EpsgCrs>, List<Double>> coordinatesTransformer) {
        this.coordinatesTransformer = Optional.ofNullable(coordinatesTransformer);
        this.isNestedFilter = false;
    }

    private java.util.function.Function<Geometry.Coordinate, Geometry.Coordinate> transformIfNecessary(Optional<EpsgCrs> sourceCrs) {
        return coordinate -> coordinatesTransformer.map(transformer -> transformer.apply(coordinate, sourceCrs))
                                     .map(list -> Geometry.Coordinate.of(list.get(0), list.get(1)))
                                     .orElse(coordinate);
    }

    private java.util.function.Function<List<Double>, List<Double>> transformIfNecessary2(Optional<EpsgCrs> sourceCrs) {
        return coordinates -> coordinatesTransformer.map(transformer -> transformer.apply(coordinates, sourceCrs))
                                     .orElse(coordinates);
    }

    @Override
    public String postProcess(CqlNode node, String text) {
        //remove outer brackets
        if (node instanceof And || node instanceof Or) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    @Override
    public String visit(CqlFilter cqlFilter, List<String> children) {
        CqlNode node = cqlFilter.getExpressions()
                                .get(0);
        String text = node.accept(this);

        //remove outer brackets
        if (node instanceof And || node instanceof Or) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    @Override
    public String visit(CqlPredicate cqlPredicate, List<String> children) {
        return cqlPredicate.getExpressions()
                           .get(0)
                           .accept(this);
    }

    @Override
    public String visit(LogicalOperation logicalOperation, List<String> children) {
        String operator = LOGICAL_OPERATORS.get(logicalOperation.getClass());

        return children.stream()
                       .collect(Collectors.joining(String.format(" %s ", operator), "(", ")"));
    }

    @Override
    public String visit(Not not, List<String> children) {
        String operator = LOGICAL_OPERATORS.get(not.getClass());

        String operation = children.get(0);

        if (not.getArgs().get(0) instanceof Like) {
            String like = Like.TYPE.toUpperCase();

            return operation.replace(like, String.format("%s %s", operator, like));
        } else if (not.getArgs().get(0) instanceof IsNull) {
            String isNull = IsNull.TEXT;

            return operation.replace(isNull, "IS NOT NULL");
        } /*TODO else if (not.getPredicate()
                      .get()
                      .getBetween()
                      .isPresent()) {
            String between = SCALAR_OPERATORS.get(ImmutableBetween.class);

            return operation.replace(between, String.format("%s %s", operator, between));
        } else if (not.getPredicate()
                      .get()
                      .getInOperator()
                      .isPresent()) {
            String in = SCALAR_OPERATORS.get(ImmutableIn.class);

            return operation.replace(in, String.format("%s %s", operator, in));
        }*/

        return String.format("NOT (%s)", operation);
    }

    @Override
    public String visit(BinaryScalarOperation scalarOperation, List<String> children) {
        String operator = SCALAR_OPERATORS.get(scalarOperation.getClass());
        return String.format("%s %s %s", children.get(0), operator, children.get(1));
    }

    @Override
    public String visit(Between between, List<String> children) {
        String operator = SCALAR_OPERATORS.get(between.getClass());
        return String.format("%s %s %s AND %s", children.get(0), operator, children.get(1), children.get(2));
    }

    @Override
    public String visit(Like like, List<String> children) {
        String operator = SCALAR_OPERATORS.get(like.getClass());
        return String.format("%s %s %s", children.get(0), operator, children.get(1))
                     .trim()
                     .replace("  ", " ");
    }

    @Override
    public String visit(In in, List<String> children) {
        String operator = SCALAR_OPERATORS.get(in.getClass());
        String property = Objects.equals(children.get(0), ID_PLACEHOLDER) ? "" : children.get(0);
        return String.format("%s %s (%s)", property, operator, String.join(", ", children.subList(1, children.size())));
    }

    @Override
    public String visit(IsNull isNull, List<String> children) {
        String operator = SCALAR_OPERATORS.get(isNull.getClass());
        return String.format("%s %s", children.get(0), operator);
    }

    @Override
    public String visit(BinaryTemporalOperation temporalOperation, List<String> children) {
        String operator = temporalOperation.getOp().toUpperCase();

        return String.format("%s(%s, %s)", operator, children.get(0), children.get(1));
    }

    @Override
    public String visit(SpatialOperation spatialOperation, List<String> children) {
        String operator = spatialOperation.getOperator().toString();

        return String.format("%s(%s, %s)", operator, children.get(0), children.get(1));
    }

    @Override
    public String visit(ArrayOperation arrayOperation, List<String> children) {
        String operator = arrayOperation.getOperator().toString();

        return String.format("%s(%s, %s)", operator, children.get(0), children.get(1));
    }

    @Override
    public String visit(Geometry.Coordinate coordinate, List<String> children) {
        return coordinate.stream()
                         .map(Object::toString)
                         .collect(Collectors.joining(" "));
    }

    @Override
    public String visit(Geometry.Point point, List<String> children) {
        return String.format("POINT(%s)", transformIfNecessary(point.getCrs()).apply(point.getCoordinates()
                                                                    .get(0))
                .accept(this));
    }

    @Override
    public String visit(Geometry.LineString lineString, List<String> children) {
        return String.format("LINESTRING%s", lineString.getCoordinates()
                                                       .stream()
                                                       .map(transformIfNecessary(lineString.getCrs()))
                                                       .map(coordinate -> coordinate.accept(this))
                                                       .collect(Collectors.joining(",", "(", ")")));
    }

    @Override
    public String visit(Geometry.Polygon polygon, List<String> children) {
        return String.format("POLYGON%s", polygon.getCoordinates()
                                                 .stream()
                                                 .flatMap(l -> Stream.of(l.stream()
                                                                          .map(transformIfNecessary(polygon.getCrs()))
                                                                          .flatMap(coordinate -> Stream.of(coordinate.accept(this)))
                                                                          .collect(Collectors.joining(",", "(", ")"))))
                                                 .collect(Collectors.joining(",", "(", ")")));
    }

    @Override
    public String visit(Geometry.MultiPoint multiPoint, List<String> children) {
        return String.format("MULTIPOINT%s", multiPoint.getCoordinates()
                                                       .stream()
                                                       .flatMap(point -> point.getCoordinates()
                                                                              .stream())
                                                       .map(transformIfNecessary(multiPoint.getCrs()))
                                                       .map(coordinate -> coordinate.accept(this))
                                                       .collect(Collectors.joining(",", "(", ")")));
    }

    @Override
    public String visit(Geometry.MultiLineString multiLineString, List<String> children) {
        return String.format("MULTILINESTRING%s", multiLineString.getCoordinates()
                                                                 .stream()
                                                                 .flatMap(ls -> Stream.of(ls.getCoordinates()
                                                                                            .stream()
                                                                                            .map(transformIfNecessary(multiLineString.getCrs()))
                                                                                            .map(coordinate -> coordinate.accept(this))
                                                                                            .collect(Collectors.joining(",", "(", ")"))))
                                                                 .collect(Collectors.joining(",", "(", ")")));
    }

    @Override
    public String visit(Geometry.MultiPolygon multiPolygon, List<String> children) {
        return String.format("MULTIPOLYGON%s", multiPolygon.getCoordinates()
                                                           .stream()
                                                           .flatMap(p -> Stream.of(p.getCoordinates()
                                                                                    .stream()
                                                                                    .flatMap(l -> Stream.of(l.stream()
                                                                                                             .map(transformIfNecessary(multiPolygon.getCrs()))
                                                                                                             .flatMap(coordinate -> Stream.of(coordinate.accept(this)))
                                                                                                             .collect(Collectors.joining(",", "(", ")"))))
                                                                                    .collect(Collectors.joining(",", "(", ")"))))
                                                           .collect(Collectors.joining(",", "(", ")")));
    }

    @Override
    public String visit(Geometry.Envelope envelope, List<String> children) {
        return String.format("ENVELOPE%s", transformIfNecessary2(envelope.getCrs()).apply(envelope.getCoordinates())
                                                   .stream()
                                                   .map(String::valueOf)
                                                   .collect(Collectors.joining(",", "(", ")")));
    }

    @Override
    public String visit(ScalarLiteral scalarLiteral, List<String> children) {
        if (scalarLiteral.getType() == String.class) {
            return String.format("'%s'", ((String) scalarLiteral.getValue()).replaceAll("'", "''"));
        }
        return scalarLiteral.getValue()
                            .toString();
    }

    @Override
    public String visit(TemporalLiteral temporalLiteral, List<String> children) {
        if (temporalLiteral.getType() == Interval.class) {
            String start;
            String end;
            Interval interval = (Interval) temporalLiteral.getValue();
            start = interval.getStart().equals(Instant.MIN)
                ? ".."
                : DateTimeFormatter.ISO_INSTANT.format(interval.getStart());
            end = interval.getEnd().equals(Instant.MAX)
                ? ".."
                : DateTimeFormatter.ISO_INSTANT.format(interval.getEnd());
            return String.format("INTERVAL('%s','%s')", start, end);
        } else if (temporalLiteral.getType() == Instant.class) {
            Instant instant = (Instant) temporalLiteral.getValue();
            if (instant == Instant.MIN ||  instant == Instant.MAX)
                return "'..'";
            return String.format("TIMESTAMP('%s')", DateTimeFormatter.ISO_INSTANT.format(instant));
        } else if (temporalLiteral.getType() == LocalDate.class) {
            return String.format("DATE('%s')", DateTimeFormatter.ISO_DATE.format((LocalDate) temporalLiteral.getValue()));
        } else if (temporalLiteral.getType() == Function.class) {
            Function function = (Function) temporalLiteral.getValue();
            return function.getArguments().stream()
                .map(arg -> arg.accept(this)
                    .replace("DATE(","")
                    .replace("TIMESTAMP(","")
                    .replace(")",""))
                .collect(Collectors.joining(",", function.getName()+"(", ")"));
        }

        throw new IllegalStateException("unsupported temporal literal type: " + temporalLiteral.getType().getSimpleName());
    }

    @Override
    public String visit(ArrayLiteral arrayLiteral, List<String> children) {
        if (arrayLiteral.getValue() instanceof String) {
            return (String) arrayLiteral.getValue();
        } else {
            List<String> elements = ((List<Scalar>) arrayLiteral.getValue()).stream()
                    .map(e -> e.accept(this))
                    .map(e -> String.format("%s", e))
                    .collect(Collectors.toList());
            return String.format("[%s]", String.join(",", elements));
        }
    }

    @Override
    public String visit(SpatialLiteral spatialLiteral, List<String> children) {
        return ((CqlNode) spatialLiteral.getValue()).accept(this);
    }

    @Override
    public String visit(Property property, List<String> children) {
        if (!property.getNestedFilters()
                     .isEmpty()) {
            Map<String, CqlFilter> nestedFilters = property.getNestedFilters();
            StringJoiner sj = new StringJoiner(".");
            for (String element : property.getPath()) {
                if (nestedFilters.containsKey(element)) {
                    sj.add(String.format("%s[%s]", addDoubleQuotes(element), nestedFilters.get(element)
                        .accept(new CqlToText(true))));
                } else {
                    sj.add(addDoubleQuotes(element));
                }
            }
            return sj.toString();
        } else if (isNestedFilter) {
            return addDoubleQuotes(property.getPath().get(property.getPath().size() - 1));
        } else {
            return property.getPath().stream().map(this::addDoubleQuotes).collect(Collectors.joining("."));
        }
    }

    private String addDoubleQuotes(String identifier) {
        if (KEYWORDS.contains(identifier.toUpperCase())) {
            return "\"" + identifier + "\"";
        }
        return identifier;
    }

    @Override
    public String visit(Function function, List<String> children) {
        if (function.isInterval())
            // DATE() and TIMESTAMP() are not used in interval
            return function.getName() +
                children.stream()
                    .map(argument -> argument.replace("DATE(","")
                             .replace("TIMESTAMP(","")
                             .replace(")",""))
                    .collect(Collectors.joining(",", "(", ")"));

        return function.getName() +
                children.stream()
                        .collect(Collectors.joining(",", "(", ")"));
    }

}
