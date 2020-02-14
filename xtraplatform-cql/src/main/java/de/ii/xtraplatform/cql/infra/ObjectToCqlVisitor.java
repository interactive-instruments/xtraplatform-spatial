package de.ii.xtraplatform.cql.infra;

import de.ii.xtraplatform.cql.domain.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ObjectToCqlVisitor implements ObjectVisitor<String> {

    @Override
    public String visit(CqlPredicate cqlPredicate) {
        return cqlPredicate.getExpressions()
                .get(0)
                .accept(this);
    }

    @Override
    public String visitTopLevel(CqlPredicate cqlPredicate) {
        return cqlPredicate.getExpressions()
                .get(0)
                .acceptTopLevel(this);
    }

    @Override
    public String visit(And and, List<String> children) {
        return children.stream()
                .collect(Collectors.joining(" AND ", "(", ")"));
    }

    @Override
    public String visitTopLevel(And and, List<String> children) {
        return String.join(" AND ", children);
    }

    @Override
    public String visit(Or or, List<String> children) {
        return children.stream()
                .collect(Collectors.joining(" OR ", "(", ")"));
    }

    @Override
    public String visitTopLevel(Or or, List<String> children) {
        return String.join(" OR ", children);
    }

    @Override
    public String visit(Not not) {
        CqlNode operation = not.getPredicates()
                .get(0)
                .getExpressions()
                .get(0);

        if (operation instanceof Like) {
            return String.format("%s NOT LIKE %s", ((Like) operation).getOperands().get(0).accept(this),
                    ((Like) operation).getOperands().get(1).accept(this));
        } else if (operation instanceof Exists) {
            return String.format("%s DOES-NOT-EXIST", ((Exists) operation).getOperands().get(0).accept(this));
        } else if (operation instanceof IsNull) {
            return String.format("%s IS NOT NULL", ((IsNull) operation).getOperands().get(0).accept(this));
        }
        return String.format("NOT (%s)", operation.accept(this));
    }

    @Override
    public String visit(Eq eq) {
        return getScalarOperationCql(eq, "=");
    }

    @Override
    public String visit(Neq neq) {
        return getScalarOperationCql(neq, "<>");
    }

    @Override
    public String visit(Gt gt) {
        return getScalarOperationCql(gt, ">");
    }

    @Override
    public String visit(Gte gte) {
        return getScalarOperationCql(gte, ">=");
    }

    @Override
    public String visit(Lt lt) {
        return getScalarOperationCql(lt, "<");
    }

    @Override
    public String visit(Lte lte) {
        return getScalarOperationCql(lte, "<=");
    }

    @Override
    public String visit(Like like) {
        return getScalarOperationCql(like, "LIKE");
    }

    @Override
    public String visit(Between between) {
        return String.format("%s BETWEEN %s AND %s", between.getProperty().get().accept(this),
                between.getLower().get().accept(this), between.getUpper().get().accept(this));
    }

    @Override
    public String visit(In in) {
        return String.format("%s IN (%s)", in.getProperty().get().accept(this),
                in.getValues()
                        .stream()
                        .map(literal -> literal.accept(this))
                        .collect(Collectors.joining(", ")));
    }

    @Override
    public String visit(IsNull isNull) {
        return String.format("%s IS NULL", isNull.getOperands().get(0).accept(this));
    }

    @Override
    public String visit(Exists exists) {
        return String.format("%s EXISTS", exists.getOperands().get(0).accept(this));
    }

    @Override
    public String visit(After after) {
        return getTemporalOperationCql(after, "AFTER");
    }

    @Override
    public String visit(Before before) {
        return getTemporalOperationCql(before, "BEFORE");
    }

    @Override
    public String visit(Begins begins) {
        return getTemporalOperationCql(begins, "BEGINS");
    }

    @Override
    public String visit(BegunBy begunBy) {
        return getTemporalOperationCql(begunBy, "BEGUNBY");
    }

    @Override
    public String visit(TContains tContains) {
        return getTemporalOperationCql(tContains, "TCONTAINS");
    }

    @Override
    public String visit(During during) {
        return getTemporalOperationCql(during, "DURING");
    }

    @Override
    public String visit(EndedBy endedBy) {
        return getTemporalOperationCql(endedBy, "ENDEDBY");
    }

    @Override
    public String visit(Ends ends) {
        return getTemporalOperationCql(ends, "ENDS");
    }

    @Override
    public String visit(TEquals tEquals) {
        return getTemporalOperationCql(tEquals, "TEQUALS");
    }

    @Override
    public String visit(Meets meets) {
        return getTemporalOperationCql(meets, "MEETS");

    }

    @Override
    public String visit(MetBy metBy) {
        return getTemporalOperationCql(metBy, "METBY");
    }

    @Override
    public String visit(TOverlaps tOverlaps) {
        return getTemporalOperationCql(tOverlaps, "TOVERLAPS");
    }

    @Override
    public String visit(OverlappedBy overlappedBy) {
        return getTemporalOperationCql(overlappedBy, "OVERLAPPEDBY");
    }

    @Override
    public String visit(Equals equals) {
        return getSpatialOperationCql(equals, "EQUALS");
    }

    @Override
    public String visit(Disjoint disjoint) {
        return getSpatialOperationCql(disjoint, "DISJOINT");
    }

    @Override
    public String visit(Touches touches) {
        return getSpatialOperationCql(touches, "TOUCHES");
    }

    @Override
    public String visit(Within within) {
        return getSpatialOperationCql(within, "WITHIN");
    }

    @Override
    public String visit(Overlaps overlaps) {
        return getSpatialOperationCql(overlaps, "OVERLAPS");
    }

    @Override
    public String visit(Crosses crosses) {
        return getSpatialOperationCql(crosses, "CROSSES");
    }

    @Override
    public String visit(Intersects intersects) {
        return getSpatialOperationCql(intersects, "INTERSECTS");
    }

    @Override
    public String visit(Contains contains) {
        return getSpatialOperationCql(contains, "CONTAINS");
    }

    @Override
    public String visit(Geometry.Coordinate coordinate) {
        return coordinate.stream()
                .map(Object::toString)
                .collect(Collectors.joining(" "));
    }

    @Override
    public String visit(Geometry.Point point) {
        return String.format("POINT(%s)", point.getCoordinates().get(0).accept(this));
    }

    @Override
    public String visit(Geometry.LineString lineString) {
        return String.format("LINESTRING%s", lineString.getCoordinates()
                .stream()
                .map(coordinate -> coordinate.accept(this))
                .collect(Collectors.joining(",", "(", ")")));
    }

    @Override
    public String visit(Geometry.Polygon polygon) {
        return String.format("POLYGON%s", polygon.getCoordinates()
                .stream()
                .flatMap(l -> Stream.of(l.stream()
                        .flatMap(coordinate -> Stream.of(coordinate.accept(this)))
                        .collect(Collectors.joining(",", "(", ")"))))
                .collect(Collectors.joining(",", "(", ")")));
    }

    @Override
    public String visit(Geometry.MultiPoint multiPoint) {
        return String.format("MULTIPOINT%s", multiPoint.getCoordinates()
                .stream()
                .flatMap(point -> point.getCoordinates().stream())
                .map(coordinate -> coordinate.accept(this))
                .collect(Collectors.joining(",", "(", ")")));
    }

    @Override
    public String visit(Geometry.MultiLineString multiLineString) {
        return String.format("MULTILINESTRING%s", multiLineString.getCoordinates()
                .stream()
                .flatMap(ls -> Stream.of(ls.getCoordinates()
                        .stream()
                        .map(coordinate -> coordinate.accept(this))
                        .collect(Collectors.joining(",", "(", ")"))))
                .collect(Collectors.joining(",", "(", ")")));
    }

    @Override
    public String visit(Geometry.MultiPolygon multiPolygon) {
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
    public String visit(Geometry.Envelope envelope) {
        return String.format("ENVELOPE%s", envelope.getCoordinates()
                .stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "(", ")")));
    }

    @Override
    public String visit(Literal literal) {
        if (literal.getType() == String.class) {
            return String.format("'%s'", ((String) literal.getValue()).replaceAll("'", "''"));
        }
        return literal.getValue().toString();
    }

    @Override
    public String visit(SpatialLiteral spatialLiteral) {
        return ((CqlNode) spatialLiteral.getValue()).accept(this);
    }

    @Override
    public String visit(CqlNode cqlNode) {
        return null;
    }

    @Override
    public String visit(Operand operand) {
        return operand.toString();
    }

    @Override
    public String visit(Property property) {
        return property.getName();
    }

    private String getScalarOperationCql(ScalarOperation operation, String operator) {
        return String.format("%s %s %s", operation.getOperands().get(0).accept(this), operator,
                operation.getOperands().get(1).accept(this));
    }

    private String getTemporalOperationCql(TemporalOperation operation, String operator) {
        return String.format("%s %s %s", operation.getOperands().get(0).accept(this), operator,
                operation.getOperands().get(1).accept(this));
    }

    private String getSpatialOperationCql(SpatialOperation operation, String operator) {
        return String.format("%s(%s, %s)", operator, operation.getOperands().get(0).accept(this),
                operation.getOperands().get(1).accept(this));
    }

}
