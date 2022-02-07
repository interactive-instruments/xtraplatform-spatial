/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Doubles;
import de.ii.xtraplatform.cql.domain.*;
import de.ii.xtraplatform.cql.domain.Geometry.Coordinate;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.feature.provider.sql.domain.FilterEncoderSqlNewNew;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialect;
import de.ii.xtraplatform.features.domain.FeatureStoreAttribute;
import de.ii.xtraplatform.features.domain.FeatureStoreAttributesContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.features.domain.SchemaBase;
import org.opengis.filter.temporal.OverlappedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static de.ii.xtraplatform.cql.domain.In.ID_PLACEHOLDER;

// TODO: This is now only used for the SQL queries for spatial/temporal extents, and only,
//       if the properties are only accessible via joins - which is typically not the case.
//       Delete after updating the ExtentReaderSql logic.

public class FilterEncoderSqlNewNewImpl implements FilterEncoderSqlNewNew {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterEncoderSqlNewNewImpl.class);

    final static Splitter ARRAY_SPLITTER = Splitter.on(",").trimResults().omitEmptyStrings();

    private final static Map<Class<?>, List<String>> TEMPORAL_OPERATORS = new ImmutableMap.Builder<Class<?>, List<String>>()
            .put(ImmutableTIntersects.class, ImmutableList.of("OVERLAPS"))
            .build();

    private final static Map<Class<?>, String> SPATIAL_OPERATORS = new ImmutableMap.Builder<Class<?>, String>()
            .put(ImmutableSEquals.class, "ST_Equals")
            .put(ImmutableSDisjoint.class, "ST_Disjoint")
            .put(ImmutableSTouches.class, "ST_Touches")
            .put(ImmutableSWithin.class, "ST_Within")
            .put(ImmutableSOverlaps.class, "ST_Overlaps")
            .put(ImmutableSCrosses.class, "ST_Crosses")
            .put(ImmutableSIntersects.class, "ST_Intersects")
            .put(ImmutableSContains.class, "ST_Contains")
            .build();

    private final Function<FeatureStoreAttributesContainer, List<String>> aliasesGenerator;
    private final BiFunction<FeatureStoreAttributesContainer, List<String>, BiFunction<FeatureStoreAttributesContainer, Optional<CqlFilter>, Function<Optional<String>, String>>> joinsGenerator;
    private final EpsgCrs nativeCrs;
    private final SqlDialect sqlDialect;
    private final CrsTransformerFactory crsTransformerFactory;
    java.util.function.BiFunction<List<Double>, Optional<EpsgCrs>, List<Double>> coordinatesTransformer;

    public FilterEncoderSqlNewNewImpl(
            Function<FeatureStoreAttributesContainer, List<String>> aliasesGenerator,
            BiFunction<FeatureStoreAttributesContainer, List<String>, BiFunction<FeatureStoreAttributesContainer, Optional<CqlFilter>, Function<Optional<String>, String>>> joinsGenerator,
            EpsgCrs nativeCrs, SqlDialect sqlDialect,
            CrsTransformerFactory crsTransformerFactory) {
        this.aliasesGenerator = aliasesGenerator;
        this.joinsGenerator = joinsGenerator;
        this.nativeCrs = nativeCrs;
        this.sqlDialect = sqlDialect;
        this.crsTransformerFactory = crsTransformerFactory;
        this.coordinatesTransformer = this::transformCoordinatesIfNecessary;
    }

    private List<Double> transformCoordinatesIfNecessary(List<Double> coordinates, Optional<EpsgCrs> sourceCrs) {

        if (sourceCrs.isPresent() && !Objects.equals(sourceCrs.get(), nativeCrs)) {
            Optional<CrsTransformer> transformer = crsTransformerFactory.getTransformer(sourceCrs.get(), nativeCrs);
            if (transformer.isPresent()) {
                double[] transformed = transformer.get()
                                                  .transform(Doubles.toArray(coordinates), coordinates.size() / 2, false);
                return Doubles.asList(transformed);
            }
        }
        return coordinates;
    }

    @Override
    public String encode(CqlFilter cqlFilter, FeatureStoreInstanceContainer typeInfo) {
        return cqlFilter.accept(new CqlToSql(typeInfo));
    }

    @Override
    public String encodeNested(CqlFilter cqlFilter, FeatureStoreAttributesContainer typeInfo, boolean isUserFilter) {
        return cqlFilter.accept(new CqlToSqlJoin(typeInfo, isUserFilter));
    }

    private class CqlToSqlJoin extends CqlToSql {

        private final FeatureStoreAttributesContainer attributesContainer;
        private final boolean isUserFilter;

        private CqlToSqlJoin(FeatureStoreAttributesContainer attributesContainer, boolean isUserFilter) {
            super(null);
            this.attributesContainer = attributesContainer;
            this.isUserFilter = isUserFilter;
        }

        @Override
        public String visit(Property property, List<String> children) {
            // strip double quotes from the property name
            String propertyName = property.getName().replaceAll("^\"|\"$", "");
            Predicate<FeatureStoreAttribute> propertyMatches = attribute -> Objects.equals(propertyName, attribute.getQueryable()) || (Objects.equals(propertyName, ID_PLACEHOLDER) && attribute.isId());
            String column = getColumn(attributesContainer, propertyMatches, propertyName);
            List<String> aliases = isUserFilter ? getAliases(attributesContainer) : aliasesGenerator.apply(attributesContainer);
            String qualifiedColumn = String.format("%s.%s", aliases.get(aliases.size() - 1), column);

            return String.format("%%1$s%1$s%%2$s", qualifiedColumn);
        }
    }

    private class CqlToSql extends CqlToText {

        private final FeatureStoreInstanceContainer instanceContainer;

        private CqlToSql(FeatureStoreInstanceContainer instanceContainer) {
            super(coordinatesTransformer);
            this.instanceContainer = instanceContainer;
        }

        protected List<String> getAliases(FeatureStoreAttributesContainer container) {
            return aliasesGenerator.apply(container)
                                   .stream()
                                   .map(s -> "A" + s)
                                   .collect(Collectors.toList());
        }

        protected FeatureStoreAttributesContainer getTable(Predicate<FeatureStoreAttribute> propertyMatches, String propertyName) {
            return instanceContainer.getAllAttributesContainers()
                                    .stream()
                                    .filter(attributesContainer -> attributesContainer.getAttributes()
                                                                                      .stream()
                                                                                      .anyMatch(propertyMatches))
                                    .findFirst()
                                    .orElseThrow(() -> new IllegalArgumentException(String.format("Filter is invalid. Unknown property: %s", propertyName)));
        }

        protected String getColumn(FeatureStoreAttributesContainer table,
                                   Predicate<FeatureStoreAttribute> propertyMatches,
                                   String propertyName) {
            return table.getAttributes()
                        .stream()
                        .filter(propertyMatches)
                        .findFirst()
                        .map(attribute -> {
                            if (attribute.isTemporal()) {
                                return sqlDialect.applyToDatetime(attribute.getName());
                            }
                            return attribute.getName();
                        })
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Filter is invalid. Unknown property: %s", propertyName)));
        }

        @Override
        public String visit(Property property, List<String> children) {
            // strip double quotes from the property name
            String propertyName = property.getName().replaceAll("^\"|\"$", "");
            Predicate<FeatureStoreAttribute> propertyMatches = attribute -> Objects.equals(propertyName, attribute.getQueryable()) || (Objects.equals(propertyName, ID_PLACEHOLDER) && attribute.isId());
            FeatureStoreAttributesContainer table = getTable(propertyMatches, propertyName);
            String column = getColumn(table, propertyMatches, propertyName);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("PROP {} {}", table.getName(), column);
            }

            Optional<CqlFilter> userFilter;
            FeatureStoreAttributesContainer userFilterTable = null;
            Optional<String> instanceFilter = Optional.empty();
            if (!property.getNestedFilters()
                .isEmpty()) {
                userFilter = property.getNestedFilters()
                    .values()
                    .stream()
                    .findFirst();
                String userFilterPropertyName = getUserFilterPropertyName(userFilter.get());
                if (userFilterPropertyName.contains("row_number")) {
                    userFilterTable = table;
                    instanceFilter = instanceContainer.getFilter().map(cql -> encode(cql ,instanceContainer));
                } else {
                    Predicate<FeatureStoreAttribute> userFilterPropertyMatches = attribute -> Objects.equals(userFilterPropertyName, attribute.getQueryable()) || (Objects.equals(userFilterPropertyName, ID_PLACEHOLDER) && attribute.isId());
                    userFilterTable = getTable(userFilterPropertyMatches, userFilterPropertyName);
                }
            } else {
                userFilter = Optional.empty();
            }

            List<String> aliases = getAliases(table);
            String qualifiedColumn = String.format("%s.%s", aliases.get(aliases.size() - 1), column);

            String join = joinsGenerator.apply(table, aliases)
                                        .apply(userFilterTable, userFilter)
                                        .apply(instanceFilter);

            return String.format("A.%3$s IN (SELECT %2$s.%3$s FROM %1$s %2$s %4$s WHERE %%1$s%5$s%%2$s)", instanceContainer.getName(), aliases.get(0), instanceContainer.getSortKey(), join, qualifiedColumn);
        }

        private String getUserFilterPropertyName(CqlFilter userFilter) {
            CqlNode nestedFilter = userFilter.getExpressions().get(0);
            Operand operand = null;
            if (nestedFilter instanceof BinaryScalarOperation) {
                operand = ((BinaryScalarOperation) nestedFilter).getOperands().get(0);
            } else if (nestedFilter instanceof TemporalOperation) {
                operand = ((TemporalOperation) nestedFilter).getOperands().get(0);
            } else if (nestedFilter instanceof SpatialOperation) {
                operand = ((SpatialOperation) nestedFilter).getOperands().get(0);
            } else if (nestedFilter instanceof Like) {
                operand = ((Like) nestedFilter).getOperands().get(0);
            } else if (nestedFilter instanceof In) {
                operand = ((In) nestedFilter).getValue().get();
            } else if (nestedFilter instanceof Between) {
                operand = ((Between) nestedFilter).getValue().get();
            }
            if (operand instanceof Property) {
                return ((Property) operand).getName();
            } else if (operand instanceof de.ii.xtraplatform.cql.domain.Function) {
                return operand.accept(this);
            }
            throw new IllegalArgumentException("unsupported nested filter");
        }

        @Override
        public String visit(de.ii.xtraplatform.cql.domain.Function function, List<String> children) {
            if (Objects.equals(function.getName(), "interval")) {
                String start = children.get(0);
                String end = children.get(1);
                String endColumn = end.substring(end.indexOf("%1$s") + 4, end.indexOf("%2$s"));

                return String.format(start, "%1$s(", ", " + endColumn + ")%2$s");
            } else if (Objects.equals(function.getName(), "position")) {
                return "%1$srow_number%2$s";
            } else if (Objects.equals(function.getName().toUpperCase(), "CASEI")) {
                if (function.getArguments().get(0) instanceof ScalarLiteral) {
                    return children.get(0).toLowerCase();
                } else if (function.getArguments().get(0) instanceof Property) {
                    return String.format(children.get(0), "%1$sLOWER(", ")%2$s");
                }
            } else if (Objects.equals(function.getName().toUpperCase(), "ACCENTI")) {
                if (function.getArguments().get(0) instanceof ScalarLiteral) {
                    return String.format("%s %s", children.get(0), "COLLATE \"de-DE-x-icu\"");
                } else if (function.getArguments().get(0) instanceof Property) {
                    return children.get(0).replace("%2$s", " COLLATE \"de-DE-x-icu\"%2$s");
                }
            }

            return super.visit(function, children);
        }

        private String reduceSelectToColumn(String expression) {
            return String.format(expression.substring(expression.indexOf(" WHERE ") + 7, expression.length() - 1), "", "");
        }

        private String replaceColumnWithLiteral(String expression, String column, String literal) {
            return expression.replace(String.format("%%1$s%1$s%%2$s",column),String.format("%%1$s%1$s%%2$s",literal));
        }

        private String replaceColumnWithInterval(String expression, String column) {
            return expression.replace(String.format("%%1$s%1$s%%2$s",column),String.format("%%1$s(%1$s,%1$s)%%2$s",column));
        }

        private boolean operandIsOfType(Operand operand, Class... classes) {
            return Arrays.stream(classes).anyMatch(clazz -> clazz.isInstance(operand));
        }

        private List<String> processBinary(List<Operand> operands, List<String> children) {
            // The two operands may be either a property reference or a literal.
            // If there is at least one property reference, that fragment will
            // be used as the basis (mainExpression). If the other operand is
            // a property reference, too, it is in the same table and the second
            // fragment will be reduced to qualified column name (second expression).
            String mainExpression = children.get(0);
            String secondExpression = children.get(1);
            boolean op1hasSelect = operandIsOfType(operands.get(0), Property.class, de.ii.xtraplatform.cql.domain.Function.class);
            boolean op2hasSelect = operandIsOfType(operands.get(1), Property.class, de.ii.xtraplatform.cql.domain.Function.class);
            if (op1hasSelect) {
                if (op2hasSelect) {
                    secondExpression = reduceSelectToColumn(children.get(1));
                }
            } else {
                // the unusual case that a literal is on the left side
                if (op2hasSelect) {
                    secondExpression = reduceSelectToColumn(children.get(1));
                    mainExpression = replaceColumnWithLiteral(children.get(1), secondExpression, children.get(0));
                } else {
                    mainExpression = String.format("%%1$s%1$s%%2$s", children.get(0));
                }
            }

            return ImmutableList.of(mainExpression, secondExpression);
        }

        private List<String> processTernary(List<Operand> operands, List<String> children) {
            // The three operands may be either a property reference or a literal.
            // If there is at least one property reference, that fragment will
            // be used as the basis (mainExpression). If another operand is
            // a property reference, too, it is in the same table and the
            // fragment will be reduced to qualified column name (second or third
            // expression).
            String mainExpression = children.get(0);
            String secondExpression = children.get(1);
            String thirdExpression = children.get(2);
            boolean op1hasSelect = operandIsOfType(operands.get(0), Property.class, de.ii.xtraplatform.cql.domain.Function.class);
            boolean op2hasSelect = operandIsOfType(operands.get(1), Property.class, de.ii.xtraplatform.cql.domain.Function.class);
            boolean op3hasSelect = operandIsOfType(operands.get(2), Property.class, de.ii.xtraplatform.cql.domain.Function.class);
            if (op1hasSelect) {
                if (op2hasSelect)
                    secondExpression = reduceSelectToColumn(children.get(1));
                if (op3hasSelect)
                    thirdExpression = reduceSelectToColumn(children.get(2));
            } else {
                // the unusual case that a literal is on the left side
                if (op2hasSelect && !op3hasSelect) {
                    secondExpression = reduceSelectToColumn(children.get(1));
                    mainExpression = replaceColumnWithLiteral(children.get(1), secondExpression, children.get(0));
                } else if (!op2hasSelect && op3hasSelect) {
                    thirdExpression = reduceSelectToColumn(children.get(2));
                    mainExpression = replaceColumnWithLiteral(children.get(2), thirdExpression, children.get(0));
                } else if (op2hasSelect && op3hasSelect) {
                    secondExpression = reduceSelectToColumn(children.get(1));
                    mainExpression = replaceColumnWithLiteral(children.get(1), secondExpression, children.get(0));
                    thirdExpression = reduceSelectToColumn(children.get(2));
                } else if (!op2hasSelect && !op3hasSelect) {
                    // special case of three literals, we need to build the SQL expression
                    mainExpression = String.format("%%1$s%1$s%%2$s", children.get(0));
                }
            }

            return ImmutableList.of(mainExpression, secondExpression, thirdExpression);
        }

        @Override
        public String visit(BinaryScalarOperation scalarOperation, List<String> children) {
            String operator = SCALAR_OPERATORS.get(scalarOperation.getClass());

            List<String> expressions = processBinary(scalarOperation.getOperands(), children);

            String operation = String.format(" %s %s", operator, expressions.get(1));
            return String.format(expressions.get(0), "", operation);
        }

        @Override
        public String visit(Like like, List<String> children) {
            String operator = SCALAR_OPERATORS.get(like.getClass());

            List<String> expressions = processBinary(like.getOperands(), children);

            // we may need to change the second expression
            String secondExpression = expressions.get(1);

            String functionStart = "";
            String functionEnd = "";

            String operation = String.format("::varchar%s %s %s", functionEnd, operator, secondExpression);
            return String.format(expressions.get(0), functionStart, operation);
        }

        @Override
        public String visit(In in, List<String> children) {
            String operator = SCALAR_OPERATORS.get(in.getClass());

            String mainExpression = "";
            Scalar op1 = in.getValue().get();
            if (op1 instanceof Property) {
                mainExpression = children.get(0);
            } else if (op1 instanceof de.ii.xtraplatform.cql.domain.Function) {
                mainExpression = children.get(0);
            } else if (op1 instanceof ScalarLiteral) {
                // special case of a literal, we need to build the SQL expression
                mainExpression = String.format("%%1$s%1$s%%2$s", children.get(0));
            } else {
                throw new IllegalArgumentException(String.format("In: Cannot process operand of type %s with value %s.", op1.getClass().getSimpleName(), mainExpression));
            }

            // mainExpression is either a literal value or a SELECT expression
            String operation = String.format(" %s (%s)", operator, String.join(", ", children.subList(1, children.size())));
            return String.format(mainExpression, "", operation);
        }

        @Override
        public String visit(IsNull isNull, List<String> children) {
            String operator = SCALAR_OPERATORS.get(isNull.getClass());

            String mainExpression = "";
            Operand op1 = isNull.getOperand().get();
            if (op1 instanceof Property) {
                mainExpression = children.get(0);
            } else if (op1 instanceof ScalarLiteral) {
                // special case of a literal (which will never be NULL), we need to build the SQL expression
                mainExpression = String.format("%%1$s%1$s%%2$s", children.get(0));
            } else {
                throw new IllegalArgumentException(String.format("IsNull: Cannot process operand of type %s with value %s.", op1.getClass().getSimpleName(), mainExpression));
            }

            // mainExpression is either a literal value or a SELECT expression
            String operation = String.format(" %s", operator);
            return String.format(mainExpression, "", operation);
        }

        @Override
        public String visit(Between between, List<String> children) {
            String operator = SCALAR_OPERATORS.get(between.getClass());

            Scalar op1 = between.getValue().get();
            Scalar op2 = between.getLower().get();
            Scalar op3 = between.getUpper().get();
            List<String> expressions = processTernary(ImmutableList.of(op1, op2, op3), children);

            String operation = String.format(" %s %s AND %s", operator, expressions.get(1), expressions.get(2));
            return String.format(expressions.get(0), "", operation);
        }

        private Instant getStart(TemporalLiteral literal) {
            if (literal.getType() == Interval.class) {
                return ((Interval) literal.getValue()).getStart();
            } else if (literal.getType() == Instant.class) {
                return ((Instant) literal.getValue());
            } else {
                return ((LocalDate) literal.getValue()).atStartOfDay(ZoneOffset.UTC).toInstant();
            }
        }

        private String getStartAsString(TemporalLiteral literal) {
            return String.format("TIMESTAMP '%s'", getStart(literal))
                         .replace("'0000-01-01T00:00:00Z'", "'-infinity'");
        }

        private Instant getEnd(TemporalLiteral literal) {
            if (literal.getType() == Interval.class) {
                return ((Interval) literal.getValue()).getEnd().minusSeconds(1);
            } else if (literal.getType() == Instant.class) {
                return ((Instant) literal.getValue());
            } else {
                return ((LocalDate) literal.getValue()).atTime(23,59,59).atZone(ZoneOffset.UTC).toInstant();
            }
        }

        private String getEndAsString(TemporalLiteral literal) {
            return String.format("TIMESTAMP '%s'", getEnd(literal))
                         .replace("'9999-12-31T23:59:59Z'", "'infinity'");
        }

        private Instant getEndExclusive(TemporalLiteral literal) {
            if (literal.getType() == Interval.class) {
                return ((Interval) literal.getValue()).getEnd();
            } else if (literal.getType() == Instant.class) {
                return ((Instant) literal.getValue());
            } else {
                return ((LocalDate) literal.getValue()).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            }
        }

        private String getEndExclusiveAsString(TemporalLiteral literal) {
            return String.format("TIMESTAMP '%s'", getEndExclusive(literal))
                         .replace("'+10000-01-01T00:00:00Z'", "'infinity'");
        }

        @Override
        public String visit(TemporalOperation temporalOperation, List<String> children) {
            List<String> operator = TEMPORAL_OPERATORS.get(temporalOperation.getClass());

            Temporal op1 = (Temporal) temporalOperation.getOperands().get(0);
            Temporal op2 = (Temporal) temporalOperation.getOperands().get(1);

            if (temporalOperation instanceof TIntersects) {
                if (op1 instanceof Property) {
                    // need to change "column" to "(column,column)"
                    children = ImmutableList.of(replaceColumnWithInterval(children.get(0), reduceSelectToColumn(children.get(0))), children.get(1));
                } else if (op1 instanceof TemporalLiteral) {
                    // need to construct "(start, end)" where start and end are identical for an instant and end is exclusive otherwise
                    children = ImmutableList.of(String.format("(%s, %s)", getStartAsString((TemporalLiteral) op1), getEndExclusiveAsString((TemporalLiteral) op1)), children.get(1));
                }

                if (op2 instanceof Property) {
                    // need to change "column" to "(column,column)"
                    children = ImmutableList.of(children.get(0),replaceColumnWithInterval(children.get(1), reduceSelectToColumn(children.get(1))));
                } else if (op2 instanceof TemporalLiteral) {
                    // need to construct "(start, end)" where start and end are identical for an instant and end is exclusive otherwise
                    children = ImmutableList.of(children.get(0), getInterval(op2));
                }
                List<String> expressions = processBinary(ImmutableList.of(op1, op2), children);
                return String.format(expressions.get(0), "", String.format(" %s %s", operator.get(0), expressions.get(1)));
            }

            throw new IllegalArgumentException(String.format("unsupported temporal operator: %s", operator));
        }

        /**
         * ISO 8601 intervals include both the start and end instant
         * PostgreSQL intervals are exclusive of the end instant, so we add one second to each end instant
         * @param literal temporal literal
         * @return PostgreSQL interval
         */
        private String getInterval(Temporal literal) {
            return String.format("(%s, %s)", getStartAsString((TemporalLiteral) literal), getEndExclusiveAsString((TemporalLiteral) literal));
        }

        @Override
        public String visit(SpatialOperation spatialOperation, List<String> children) {
            String operator = SPATIAL_OPERATORS.get(spatialOperation.getClass());

            List<String> expressions = processBinary(spatialOperation.getOperands(), children);

            return String.format(expressions.get(0), String.format("%s(", operator), String.format(", %s)", expressions.get(1)));
        }

        @Override
        public String visit(TemporalLiteral temporalLiteral, List<String> children) {
            if (temporalLiteral.getType() == Instant.class) {
                return String.format("'%s'", ((Instant) temporalLiteral.getValue()).toString());
            } else if (temporalLiteral.getType() == Interval.class) {
                return String.format("'%s'", ((Interval) temporalLiteral.getValue()).toString());
            } else if (temporalLiteral.getType() == LocalDate.class) {
                return String.format("'%s'", ((LocalDate) temporalLiteral.getValue()).toString());
            }
            throw new IllegalArgumentException("unsupported temporal literal");
        }

        @Override
        public String visit(Geometry.Point point, List<String> children) {
            return String.format("ST_GeomFromText('%s',%s)", super.visit(point, children), nativeCrs.getCode());
        }

        @Override
        public String visit(Geometry.LineString lineString, List<String> children) {
            return String.format("ST_GeomFromText('%s',%s)", super.visit(lineString, children), nativeCrs.getCode());
        }

        @Override
        public String visit(Geometry.Polygon polygon, List<String> children) {
            return String.format("ST_GeomFromText('%s',%s)", super.visit(polygon, children), nativeCrs.getCode());
        }

        @Override
        public String visit(Geometry.MultiPoint multiPoint, List<String> children) {
            return String.format("ST_GeomFromText('%s',%s)", super.visit(multiPoint, children), nativeCrs.getCode());
        }

        @Override
        public String visit(Geometry.MultiLineString multiLineString, List<String> children) {
            return String.format("ST_GeomFromText('%s',%s)", super.visit(multiLineString, children), nativeCrs.getCode());
        }

        @Override
        public String visit(Geometry.MultiPolygon multiPolygon, List<String> children) {
            return String.format("ST_GeomFromText('%s',%s)", super.visit(multiPolygon, children), nativeCrs.getCode());
        }

        @Override
        public String visit(Geometry.Envelope envelope, List<String> children) {
            List<Double> c = envelope.getCoordinates();

            EpsgCrs crs = envelope.getCrs().orElse(OgcCrs.CRS84);
            int epsgCode = crs.getCode();
            boolean hasDiscontinuityAt180DegreeLongitude = ImmutableList.of(4326, 4979, 4259, 4269).contains(epsgCode);

            if (c.get(0)>c.get(2) && hasDiscontinuityAt180DegreeLongitude) {
                // special case, the bbox crosses the antimeridian, we create convert this to a MultiPolygon
                List<Coordinate> coordinates1 = ImmutableList.of(
                        Coordinate.of(c.get(0), c.get(1)),
                        Coordinate.of(180.0, c.get(1)),
                        Coordinate.of(180.0, c.get(3)),
                        Coordinate.of(c.get(0), c.get(3)),
                        Coordinate.of(c.get(0), c.get(1))
                );
                List<Coordinate> coordinates2 = ImmutableList.of(
                        Coordinate.of(-180, c.get(1)),
                        Coordinate.of(c.get(2), c.get(1)),
                        Coordinate.of(c.get(2), c.get(3)),
                        Coordinate.of(-180, c.get(3)),
                        Coordinate.of(-180, c.get(1))
                );
                Geometry.Polygon polygon1 = new ImmutablePolygon.Builder().addCoordinates(coordinates1)
                                                                          .crs(crs)
                                                                          .build();
                Geometry.Polygon polygon2 = new ImmutablePolygon.Builder().addCoordinates(coordinates2)
                                                                          .crs(crs)
                                                                          .build();
                Geometry.MultiPolygon twoEnvelopes = new ImmutableMultiPolygon.Builder().addCoordinates(polygon1, polygon2)
                                                                                        .crs(crs)
                                                                                        .build();
                return visit(twoEnvelopes, ImmutableList.of());
            }

            // standard case
            List<Coordinate> coordinates = ImmutableList.of(
                    Coordinate.of(c.get(0), c.get(1)),
                    Coordinate.of(c.get(2), c.get(1)),
                    Coordinate.of(c.get(2), c.get(3)),
                    Coordinate.of(c.get(0), c.get(3)),
                    Coordinate.of(c.get(0), c.get(1))
            );
            Geometry.Polygon polygon = new ImmutablePolygon.Builder().addCoordinates(coordinates)
                                                                     .crs(crs)
                                                                     .build();

            return visit(polygon, ImmutableList.of());
        }

        @Override
        public String visit(ArrayOperation arrayOperation, List<String> children) {
            // The two operands may be either a property reference or a literal.
            // If there is at least one property reference, that fragment will
            // be used as the basis (mainExpression). If the other operand is
            // a property reference, too, it is in the same table and the second
            // fragment will be reduced to qualified column name (second expression).
            String mainExpression = children.get(0);
            String secondExpression = children.get(1);
            boolean op1hasSelect = operandIsOfType(arrayOperation.getOperands().get(0), Property.class, de.ii.xtraplatform.cql.domain.Function.class);
            boolean op2hasSelect = operandIsOfType(arrayOperation.getOperands().get(1), Property.class, de.ii.xtraplatform.cql.domain.Function.class);
            boolean notInverse = true;
            if (op1hasSelect) {
                if (op2hasSelect) {
                    throw new IllegalArgumentException("Array predicates with property references on both sides are not supported.");
                    // secondExpression = reduceSelectToColumn(children.get(1));
                }
            } else {
                // the unusual case that a literal is on the left side
                if (op2hasSelect) {
                    mainExpression = children.get(1);
                    secondExpression = children.get(0);
                    notInverse = false;
                    op1hasSelect = true;
                    op2hasSelect = false;
                } else {
                    // literal op literal, we can decide here
                    List<String> firstOp = ARRAY_SPLITTER.splitToList(mainExpression.replaceAll("\\[|\\]", ""));
                    List<String> secondOp = ARRAY_SPLITTER.splitToList(secondExpression.replaceAll("\\[|\\]", ""));
                    if (arrayOperation instanceof AContains) {
                        // each item of the second array must be in the first array
                        return secondOp.stream().allMatch(item -> firstOp.stream().anyMatch(item2 -> item.equals(item2))) ? "1=1" : "1=0";
                    } else if (arrayOperation instanceof AEquals) {
                        // items must be identical
                        if (firstOp.size()!=secondOp.size())
                            return "1=0";
                        return secondOp.stream().allMatch(item -> firstOp.stream().anyMatch(item2 -> item.equals(item2))) ? "1=1" : "1=0";
                    } else if (arrayOperation instanceof AOverlaps) {
                        // at least one common element
                        return secondOp.stream().anyMatch(item -> firstOp.stream().anyMatch(item2 -> item.equals(item2))) ? "1=1" : "1=0";
                    } else if (arrayOperation instanceof AContainedBy) {
                        // each item of the first array must be in the second array
                        return firstOp.stream().allMatch(item -> secondOp.stream().anyMatch(item2 -> item.equals(item2))) ? "1=1" : "1=0";
                    }
                    throw new IllegalArgumentException("unsupported array operator");
                }
            }

            if (op1hasSelect && op2hasSelect) {
                // property op property

            }

            // property op literal

            int elementCount = secondExpression.split(",").length;

            String propertyName = ((Property) arrayOperation.getOperands().get(notInverse ? 0 : 1)).getName();
            Predicate<FeatureStoreAttribute> propertyMatches = attribute -> Objects.equals(propertyName, attribute.getQueryable()) || (Objects.equals(propertyName, ID_PLACEHOLDER) && attribute.isId());
            FeatureStoreAttributesContainer table = getTable(propertyMatches, propertyName);
            String column = getColumn(table, propertyMatches, propertyName);
            List<String> aliases = getAliases(table);
            String qualifiedColumn = String.format("%s.%s", aliases.get(aliases.size() - 1), column);
            List<Map<String, List<String>>> x = ImmutableList.of();
            boolean xx = x.stream().map(theme -> theme.get("concept")).flatMap(List::stream).filter(concept -> concept.equals("DLKM")).distinct().count() == 1;

            if (notInverse ? arrayOperation instanceof AContains : arrayOperation instanceof AContainedBy) {
                String arrayQuery = String.format(" IN %1$s GROUP BY %2$s.%3$s HAVING count(distinct %4$s) = %5$s", secondExpression, aliases.get(0), instanceContainer.getSortKey(), qualifiedColumn, elementCount);
                return String.format(mainExpression, "", arrayQuery);
            } else if (arrayOperation instanceof AEquals) {
                String arrayQuery = String.format(" IS NOT NULL GROUP BY %2$s.%3$s HAVING count(distinct %4$s) = %5$s AND count(case when %4$s not in %1$s then %4$s else null end) = 0",
                                                  secondExpression, aliases.get(0), instanceContainer.getSortKey(), qualifiedColumn, elementCount);
                return String.format(mainExpression, "", arrayQuery);
            } else if (arrayOperation instanceof AOverlaps) {
                String arrayQuery = String.format(" IN %1$s GROUP BY %2$s.%3$s", secondExpression, aliases.get(0), instanceContainer.getSortKey());
                return String.format(mainExpression, "", arrayQuery);
            } else if (notInverse ? arrayOperation instanceof AContainedBy : arrayOperation instanceof AContains) {
                String arrayQuery = String.format(" IS NOT NULL GROUP BY %2$s.%3$s HAVING count(case when %4$s not in %1$s then %4$s else null end) = 0",
                                                  secondExpression, aliases.get(0), instanceContainer.getSortKey(), qualifiedColumn);
                return String.format(mainExpression, "", arrayQuery);
            }
            throw new IllegalArgumentException("unsupported array operator");
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
                return String.format("(%s)", String.join(",", elements));
            }
        }

        @Override
        public String visit(LogicalOperation logicalOperation, List<String> children) {
            String operator = LOGICAL_OPERATORS.get(logicalOperation.getClass());

            return super.visit(logicalOperation, children);
        }

        @Override
        public String visit(Not not, List<String> children) {
            String operator = LOGICAL_OPERATORS.get(not.getClass());

            String operation = children.get(0);
            if (not.getPredicate()
                                .get()
                                .getInOperator()
                                .isPresent()) {
                // replace last IN with NOT IN
                int pos = operation.lastIndexOf(" IN ");
                int length = operation.length();
                return String.format("%s %s %s", operation.substring(0, pos), operator, operation.substring(pos + 1, length));
            }

            return super.visit(not, children);
        }
    }

}
