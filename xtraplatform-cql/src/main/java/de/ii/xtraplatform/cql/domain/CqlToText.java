/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.cql.domain;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.threeten.extra.Interval;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.ii.xtraplatform.cql.domain.In.ID_PLACEHOLDER;

public class CqlToText implements CqlVisitor<String> {

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

    private final static Map<Class<?>, String> TEMPORAL_OPERATORS = new ImmutableMap.Builder<Class<?>, String>()
            .put(ImmutableAfter.class, "AFTER")
            .put(ImmutableBefore.class, "BEFORE")
            .put(ImmutableBegins.class, "BEGINS")
            .put(ImmutableBegunBy.class, "BEGUNBY")
            .put(ImmutableTContains.class, "TCONTAINS")
            .put(ImmutableDuring.class, "DURING")
            .put(ImmutableEndedBy.class, "ENDEDBY")
            .put(ImmutableEnds.class, "ENDS")
            .put(ImmutableTEquals.class, "TEQUALS")
            .put(ImmutableMeets.class, "MEETS")
            .put(ImmutableMetBy.class, "METBY")
            .put(ImmutableTOverlaps.class, "TOVERLAPS")
            .put(ImmutableOverlappedBy.class, "OVERLAPPEDBY")
            .put(ImmutableAnyInteracts.class, "ANYINTERACTS")
            .build();

    private final static Map<Class<?>, String> SPATIAL_OPERATORS = new ImmutableMap.Builder<Class<?>, String>()
            .put(ImmutableEquals.class, "EQUALS")
            .put(ImmutableDisjoint.class, "DISJOINT")
            .put(ImmutableTouches.class, "TOUCHES")
            .put(ImmutableWithin.class, "WITHIN")
            .put(ImmutableOverlaps.class, "OVERLAPS")
            .put(ImmutableCrosses.class, "CROSSES")
            .put(ImmutableIntersects.class, "INTERSECTS")
            .put(ImmutableContains.class, "CONTAINS")
            .build();

    private final static Map<Class<?>, String> ARRAY_OPERATORS = new ImmutableMap.Builder<Class<?>, String>()
            .put(ImmutableAContains.class, "ACONTAINS")
            .put(ImmutableAEquals.class, "AEQUALS")
            .put(ImmutableAOverlaps.class, "AOVERLAPS")
            .put(ImmutableContainedBy.class, "CONTAINED BY")
            .build();

    private final Optional<java.util.function.BiFunction<List<Double>, Optional<EpsgCrs>, List<Double>>> coordinatesTransformer;

    public CqlToText() {
        this.coordinatesTransformer = Optional.empty();
    }

    public CqlToText(java.util.function.BiFunction<List<Double>, Optional<EpsgCrs>, List<Double>> coordinatesTransformer) {
        this.coordinatesTransformer = Optional.ofNullable(coordinatesTransformer);
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

        if (not.getPredicate()
               .get()
               .getLike()
               .isPresent()) {
            String like = SCALAR_OPERATORS.get(ImmutableLike.class);

            return operation.replace(like, String.format("%s %s", operator, like));
        } else if (not.getPredicate()
                      .get()
                      .getIsNull()
                      .isPresent()) {
            String isNull = SCALAR_OPERATORS.get(ImmutableIsNull.class);

            return operation.replace(isNull, "IS NOT NULL");
        } else if (not.getPredicate()
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
        }

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
        String wildcard = like.getWildcard().isPresent() ? String.format("WILDCARD '%s'", like.getWildcard().get()) : "";
        String singlechar = like.getSingleChar().isPresent() ? String.format(" SINGLECHAR '%s'", like.getSingleChar().get()) : "";
        String escapechar = like.getEscapeChar().isPresent() ? String.format(" ESCAPECHAR '%s'", like.getEscapeChar().get()) : "";
        String nocase = like.getNocase().isPresent() ? String.format(" NOCASE %s", like.getNocase().get()) : "";
        return String.format("%s %s %s %s%s%s%s", children.get(0), operator, children.get(1), wildcard, singlechar, escapechar, nocase)
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
    public String visit(TemporalOperation temporalOperation, List<String> children) {
        String operator = TEMPORAL_OPERATORS.get(temporalOperation.getClass());

        return String.format("%s %s %s", children.get(0), operator, children.get(1));
    }

    @Override
    public String visit(SpatialOperation spatialOperation, List<String> children) {
        String operator = SPATIAL_OPERATORS.get(spatialOperation.getClass());

        return String.format("%s(%s, %s)", operator, children.get(0), children.get(1));
    }

    @Override
    public String visit(ArrayOperation arrayOperation, List<String> children) {
        String operator = ARRAY_OPERATORS.get(arrayOperation.getClass());
        return String.format("%s %s %s", children.get(0), operator, children.get(1));
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
        if (Objects.equals(temporalLiteral.getType(), Interval.class)) {
            Interval interval = (Interval) temporalLiteral.getValue();
            if (interval.equals(Interval.of(TemporalLiteral.MIN_DATE, TemporalLiteral.MAX_DATE))) {
                return "../..";
            }
            if (interval.getStart()
                        .equals(TemporalLiteral.MIN_DATE)) {
                return String.join("/", "..", interval.getEnd()
                                                      .toString());
            }
            if (interval.getEnd()
                        .equals(TemporalLiteral.MAX_DATE)) {
                return String.join("/", interval.getStart()
                                                .toString(), "..");
            }
        }
        return temporalLiteral.getValue()
                              .toString();
    }

    @Override
    public String visit(ArrayLiteral arrayLiteral, List<String> children) {
        return (String) arrayLiteral.getValue();
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
                    sj.add(String.format("%s[%s]", element, nestedFilters.get(element)
                                                                         .accept(this)));
                } else {
                    sj.add(element);
                }
            }
            return sj.toString();
        } else {
            return property.getName();
        }
    }

    @Override
    public String visit(Function function, List<String> children) {
        return function.getName() +
                function.getArguments()
                        .stream()
                        .map(argument -> argument.accept(this))
                        .collect(Collectors.joining(",", "(", ")"));
    }

}
