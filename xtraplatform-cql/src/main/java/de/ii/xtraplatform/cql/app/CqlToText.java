package de.ii.xtraplatform.cql.app;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.cql.domain.*;
import org.threeten.extra.Interval;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CqlToText implements CqlVisitor<String> {

    private final static Map<Class<?>, String> LOGICAL_OPERATORS = new ImmutableMap.Builder<Class<?>, String>()
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
            .put(ImmutableExists.class, "EXISTS")
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

        if (Objects.equals(logicalOperation.getClass(), ImmutableNot.class)) {
            String operation = children.get(0);

            if (logicalOperation.getPredicates().get(0).getLike().isPresent()) {
                String like = SCALAR_OPERATORS.get(ImmutableLike.class);

                return operation.replace(like, String.format("%s %s", operator, like));
            } else if (logicalOperation.getPredicates().get(0).getExists().isPresent()) {
                String exists = SCALAR_OPERATORS.get(ImmutableExists.class);

                return operation.replace(exists, "DOES-NOT-EXIST");
            } else if (logicalOperation.getPredicates().get(0).getIsNull().isPresent()) {
                String isNull = SCALAR_OPERATORS.get(ImmutableIsNull.class);

                return operation.replace(isNull, "IS NOT NULL");
            }

            return String.format("NOT (%s)", operation);
        }

        return children.stream()
                       .collect(Collectors.joining(String.format(" %s ", operator), "(", ")"));
    }

    @Override
    public String visit(ScalarOperation scalarOperation, List<String> children) {
        String operator = SCALAR_OPERATORS.get(scalarOperation.getClass());

        if (scalarOperation instanceof Between) {
            return String.format("%s %s %s AND %s", children.get(0), operator, children.get(1), children.get(2));
        } else if (scalarOperation instanceof In) {
            return String.format("%s %s (%s)", children.get(0), operator, String.join(", ", children.subList(1, children.size())));
        } else if (scalarOperation instanceof IsNull || scalarOperation instanceof Exists) {
            return String.format("%s %s", children.get(0), operator);
        }

        return String.format("%s %s %s", children.get(0), operator, children.get(1));
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
    public String visit(Geometry.Coordinate coordinate, List<String> children) {
        return coordinate.stream()
                         .map(Object::toString)
                         .collect(Collectors.joining(" "));
    }

    @Override
    public String visit(Geometry.Point point, List<String> children) {
        return String.format("POINT(%s)", point.getCoordinates()
                                               .get(0)
                                               .accept(this));
    }

    @Override
    public String visit(Geometry.LineString lineString, List<String> children) {
        return String.format("LINESTRING%s", lineString.getCoordinates()
                                                       .stream()
                                                       .map(coordinate -> coordinate.accept(this))
                                                       .collect(Collectors.joining(",", "(", ")")));
    }

    @Override
    public String visit(Geometry.Polygon polygon, List<String> children) {
        return String.format("POLYGON%s", polygon.getCoordinates()
                                                 .stream()
                                                 .flatMap(l -> Stream.of(l.stream()
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
                                                       .map(coordinate -> coordinate.accept(this))
                                                       .collect(Collectors.joining(",", "(", ")")));
    }

    @Override
    public String visit(Geometry.MultiLineString multiLineString, List<String> children) {
        return String.format("MULTILINESTRING%s", multiLineString.getCoordinates()
                                                                 .stream()
                                                                 .flatMap(ls -> Stream.of(ls.getCoordinates()
                                                                                            .stream()
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
                                                                                                             .flatMap(coordinate -> Stream.of(coordinate.accept(this)))
                                                                                                             .collect(Collectors.joining(",", "(", ")"))))
                                                                                    .collect(Collectors.joining(",", "(", ")"))))
                                                           .collect(Collectors.joining(",", "(", ")")));
    }

    @Override
    public String visit(Geometry.Envelope envelope, List<String> children) {
        return String.format("ENVELOPE%s", envelope.getCoordinates()
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
            if (interval.getStart().equals(TemporalLiteral.MIN_DATE)) {
                return String.join("/", "..", interval.getEnd().toString());
            }
            if (interval.getEnd().equals(TemporalLiteral.MAX_DATE)) {
                return String.join("/", interval.getStart().toString(), "..");
            }
        }
        return temporalLiteral.getValue().toString();
    }

    @Override
    public String visit(SpatialLiteral spatialLiteral, List<String> children) {
        return ((CqlNode) spatialLiteral.getValue()).accept(this);
    }

    @Override
    public String visit(Property property, List<String> children) {
        if (!property.getNestedFilters().isEmpty()) {
            Map<String, CqlFilter> nestedFilters = property.getNestedFilters();
            StringJoiner sj = new StringJoiner(".");
            for (String element : property.getPath()) {
                if (nestedFilters.containsKey(element)) {
                    sj.add(String.format("%s[%s]", element, nestedFilters.get(element).accept(this)));
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
