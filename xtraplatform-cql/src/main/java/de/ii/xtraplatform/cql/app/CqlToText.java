package de.ii.xtraplatform.cql.app;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.cql.domain.And;
import de.ii.xtraplatform.cql.domain.Between;
import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.cql.domain.CqlNode;
import de.ii.xtraplatform.cql.domain.CqlPredicate;
import de.ii.xtraplatform.cql.domain.CqlVisitor;
import de.ii.xtraplatform.cql.domain.Exists;
import de.ii.xtraplatform.cql.domain.Function;
import de.ii.xtraplatform.cql.domain.Geometry;
import de.ii.xtraplatform.cql.domain.ImmutableAfter;
import de.ii.xtraplatform.cql.domain.ImmutableAnd;
import de.ii.xtraplatform.cql.domain.ImmutableBefore;
import de.ii.xtraplatform.cql.domain.ImmutableBegins;
import de.ii.xtraplatform.cql.domain.ImmutableBegunBy;
import de.ii.xtraplatform.cql.domain.ImmutableBetween;
import de.ii.xtraplatform.cql.domain.ImmutableContains;
import de.ii.xtraplatform.cql.domain.ImmutableCrosses;
import de.ii.xtraplatform.cql.domain.ImmutableDisjoint;
import de.ii.xtraplatform.cql.domain.ImmutableDuring;
import de.ii.xtraplatform.cql.domain.ImmutableEndedBy;
import de.ii.xtraplatform.cql.domain.ImmutableEnds;
import de.ii.xtraplatform.cql.domain.ImmutableEq;
import de.ii.xtraplatform.cql.domain.ImmutableEquals;
import de.ii.xtraplatform.cql.domain.ImmutableExists;
import de.ii.xtraplatform.cql.domain.ImmutableGt;
import de.ii.xtraplatform.cql.domain.ImmutableGte;
import de.ii.xtraplatform.cql.domain.ImmutableIn;
import de.ii.xtraplatform.cql.domain.ImmutableIntersects;
import de.ii.xtraplatform.cql.domain.ImmutableIsNull;
import de.ii.xtraplatform.cql.domain.ImmutableLike;
import de.ii.xtraplatform.cql.domain.ImmutableLt;
import de.ii.xtraplatform.cql.domain.ImmutableLte;
import de.ii.xtraplatform.cql.domain.ImmutableMeets;
import de.ii.xtraplatform.cql.domain.ImmutableMetBy;
import de.ii.xtraplatform.cql.domain.ImmutableNeq;
import de.ii.xtraplatform.cql.domain.ImmutableNot;
import de.ii.xtraplatform.cql.domain.ImmutableOr;
import de.ii.xtraplatform.cql.domain.ImmutableOverlappedBy;
import de.ii.xtraplatform.cql.domain.ImmutableOverlaps;
import de.ii.xtraplatform.cql.domain.ImmutableTContains;
import de.ii.xtraplatform.cql.domain.ImmutableTEquals;
import de.ii.xtraplatform.cql.domain.ImmutableTOverlaps;
import de.ii.xtraplatform.cql.domain.ImmutableTouches;
import de.ii.xtraplatform.cql.domain.ImmutableWithin;
import de.ii.xtraplatform.cql.domain.In;
import de.ii.xtraplatform.cql.domain.IsNull;
import de.ii.xtraplatform.cql.domain.LogicalOperation;
import de.ii.xtraplatform.cql.domain.Or;
import de.ii.xtraplatform.cql.domain.Property;
import de.ii.xtraplatform.cql.domain.ScalarLiteral;
import de.ii.xtraplatform.cql.domain.ScalarOperation;
import de.ii.xtraplatform.cql.domain.SpatialLiteral;
import de.ii.xtraplatform.cql.domain.SpatialOperation;
import de.ii.xtraplatform.cql.domain.TemporalLiteral;
import de.ii.xtraplatform.cql.domain.TemporalOperation;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import org.threeten.extra.Interval;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

        if (Objects.equals(logicalOperation.getClass(), ImmutableNot.class)) {
            String operation = children.get(0);

            if (logicalOperation.getPredicates()
                                .get(0)
                                .getLike()
                                .isPresent()) {
                String like = SCALAR_OPERATORS.get(ImmutableLike.class);

                return operation.replace(like, String.format("%s %s", operator, like));
            } else if (logicalOperation.getPredicates()
                                       .get(0)
                                       .getExists()
                                       .isPresent()) {
                String exists = SCALAR_OPERATORS.get(ImmutableExists.class);

                return operation.replace(exists, "DOES-NOT-EXIST");
            } else if (logicalOperation.getPredicates()
                                       .get(0)
                                       .getIsNull()
                                       .isPresent()) {
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
