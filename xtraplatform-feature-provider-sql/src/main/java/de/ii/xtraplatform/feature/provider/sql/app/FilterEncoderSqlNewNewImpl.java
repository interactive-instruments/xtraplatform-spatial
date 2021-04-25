/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Doubles;
import de.ii.xtraplatform.cql.domain.*;
import de.ii.xtraplatform.cql.domain.Geometry.Coordinate;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.feature.provider.sql.domain.FilterEncoderSqlNewNew;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlDialect;
import de.ii.xtraplatform.features.domain.FeatureStoreAttribute;
import de.ii.xtraplatform.features.domain.FeatureStoreAttributesContainer;
import de.ii.xtraplatform.features.domain.FeatureStoreInstanceContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import java.time.Instant;
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

public class FilterEncoderSqlNewNewImpl implements FilterEncoderSqlNewNew {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterEncoderSqlNewNewImpl.class);

    //TODO: move operator translation to SqlDialect
    private final static Map<Class<?>, String> TEMPORAL_OPERATORS = new ImmutableMap.Builder<Class<?>, String>()
            .put(ImmutableAfter.class, ">")
            .put(ImmutableBefore.class, "<")
            .put(ImmutableDuring.class, "BETWEEN")
            .put(ImmutableTEquals.class, "=")
            .put(ImmutableAnyInteracts.class, "OVERLAPS")
            .build();

    private final static Map<Class<?>, String> SPATIAL_OPERATORS = new ImmutableMap.Builder<Class<?>, String>()
            .put(ImmutableEquals.class, "ST_Equals")
            .put(ImmutableDisjoint.class, "ST_Disjoint")
            .put(ImmutableTouches.class, "ST_Touches")
            .put(ImmutableWithin.class, "ST_Within")
            .put(ImmutableOverlaps.class, "ST_Overlaps")
            .put(ImmutableCrosses.class, "ST_Crosses")
            .put(ImmutableIntersects.class, "ST_Intersects")
            .put(ImmutableContains.class, "ST_Contains")
            .build();

    private final static Map<Class<?>, String> ARRAY_OPERATORS = new ImmutableMap.Builder<Class<?>, String>()
            .put(ImmutableAContains.class, "@>")
            .put(ImmutableAEquals.class, "=")
            .put(ImmutableAOverlaps.class, "&&")
            .put(ImmutableContainedBy.class, "<@")
            .build();

    private final Function<FeatureStoreAttributesContainer, List<String>> aliasesGenerator;
    private final BiFunction<FeatureStoreAttributesContainer, List<String>, Function<Optional<CqlFilter>, String>> joinsGenerator;
    private final EpsgCrs nativeCrs;
    private final SqlDialect sqlDialect;
    private final CrsTransformerFactory crsTransformerFactory;
    java.util.function.BiFunction<List<Double>, Optional<EpsgCrs>, List<Double>> coordinatesTransformer;

    public FilterEncoderSqlNewNewImpl(
            Function<FeatureStoreAttributesContainer, List<String>> aliasesGenerator,
            BiFunction<FeatureStoreAttributesContainer, List<String>, Function<Optional<CqlFilter>, String>> joinsGenerator,
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
            Predicate<FeatureStoreAttribute> propertyMatches = attribute -> Objects.equals(property.getName(), attribute.getQueryable()) || (Objects.equals(property.getName(), ID_PLACEHOLDER) && attribute.isId());
            Optional<String> column = attributesContainer.getAttributes()
                                                         .stream()
                                                         .filter(propertyMatches)
                                                         .findFirst()
                                                         .map(FeatureStoreAttribute::getName);

            if (!column.isPresent()) {
                throw new IllegalArgumentException(String.format("Filter is invalid. Unknown property: %s", property.getName()));
            }

            List<String> aliases = isUserFilter ? aliasesGenerator.apply(attributesContainer)
                                                                  .stream()
                                                                  .map(s -> "A" + s)
                                                                  .collect(Collectors.toList()) : aliasesGenerator.apply(attributesContainer);

            String qualifiedColumn = String.format("%s.%s", aliases.get(aliases.size() - 1), column.get());

            return String.format("%%1$s%1$s%%2$s", qualifiedColumn);
        }
    }

    private class CqlToSql extends CqlToText {

        private final FeatureStoreInstanceContainer instanceContainer;

        private CqlToSql(FeatureStoreInstanceContainer instanceContainer) {
            super(coordinatesTransformer);
            this.instanceContainer = instanceContainer;
        }

        @Override
        public String visit(Property property, List<String> children) {
            //TODO: fast enough? maybe pass all typeInfos to constructor and create map?
            Predicate<FeatureStoreAttribute> propertyMatches = attribute -> Objects.equals(property.getName(), attribute.getQueryable()) || (Objects.equals(property.getName(), ID_PLACEHOLDER) && attribute.isId());
            Optional<FeatureStoreAttributesContainer> table = instanceContainer.getAllAttributesContainers()
                                                                               .stream()
                                                                               .filter(attributesContainer -> attributesContainer.getAttributes()
                                                                                                                                 .stream()
                                                                                                                                 .anyMatch(propertyMatches))
                                                                               .findFirst();

            Optional<String> column = table.flatMap(attributesContainer -> attributesContainer.getAttributes()
                                                                                              .stream()
                                                                                              .filter(propertyMatches)
                                                                                              .findFirst()
                                                                                              .map(attribute -> {
                                                                                                  if (attribute.isTemporal()) {
                                                                                                      return sqlDialect.applyToDatetime(attribute.getName());
                                                                                                  }
                                                                                                  return attribute.getName();
                                                                                              }));

            if (!table.isPresent() || !column.isPresent()) {
                throw new IllegalArgumentException(String.format("Filter is invalid. Unknown property: %s", property.getName()));
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("PROP {} {}", table.get()
                                                .getName(), column.get());
            }

            Optional<CqlFilter> userFilter;
            if (!property.getNestedFilters()
                         .isEmpty()) {
                Map<String, CqlFilter> userFilters = property.getNestedFilters();
                userFilter = userFilters.values()
                                        .stream()
                                        .findFirst();
            } else {
                userFilter = Optional.empty();
            }

            List<String> aliases = aliasesGenerator.apply(table.get())
                                                   .stream()
                                                   .map(s -> "A" + s)
                                                   .collect(Collectors.toList());
            String join = joinsGenerator.apply(table.get(), aliases)
                                        .apply(userFilter);
            String qualifiedColumn = String.format("%s.%s", aliases.get(aliases.size() - 1), column.get());

            return String.format("A.%3$s IN (SELECT %2$s.%3$s FROM %1$s %2$s %4$s WHERE %%1$s%5$s%%2$s)", instanceContainer.getName(), aliases.get(0), instanceContainer.getSortKey(), join, qualifiedColumn);
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

            Scalar op1 = (Scalar) scalarOperation.getOperand1().get();
            Scalar op2 = (Scalar) scalarOperation.getOperand2().get();
            List<String> expressions = processBinary(ImmutableList.of(op1, op2), children);

            String operation = String.format(" %s %s", operator, expressions.get(1));
            return String.format(expressions.get(0), "", operation);
        }

        @Override
        public String visit(Like like, List<String> children) {
            String operator = SCALAR_OPERATORS.get(like.getClass());

            Scalar op1 = like.getOperand1().get();
            Scalar op2 = like.getOperand2().get();
            List<String> expressions = processBinary(ImmutableList.of(op1, op2), children);

            // we may need to change the second expression
            String secondExpression = expressions.get(1);

            String functionStart = "";
            String functionEnd = "";
            // modifiers only work with a literal as the second value
            if (op2 instanceof ScalarLiteral) {
                if (like.getWildcard().isPresent() &&
                        !Objects.equals("%", like.getWildcard().get())) {
                    String wildCard = like.getWildcard().get();
                    secondExpression = secondExpression.replaceAll("%", "\\%")
                                                       .replaceAll(String.format("\\%s", wildCard), "%");
                }
                if (like.getSinglechar().isPresent() &&
                        !Objects.equals("_", like.getSinglechar().get())) {
                    String singlechar = like.getSinglechar().get();
                    secondExpression = secondExpression.replaceAll("_", "\\_")
                                                       .replaceAll(String.format("\\%s", singlechar), "_");
                }
                if (like.getEscapechar().isPresent() &&
                        !Objects.equals("\\", like.getEscapechar().get())) {
                    String escapechar = like.getEscapechar().get();
                    secondExpression = secondExpression.replaceAll("\\\\", "\\\\")
                                                       .replaceAll(String.format("\\%s", escapechar), "\\");
                }
            }
            if ((like.getNocase().isEmpty()) ||
                    (like.getNocase().isPresent() && Objects.equals(Boolean.TRUE, like.getNocase().get()))) {
                functionStart = "LOWER(";
                functionEnd = ")";
                if (op2 instanceof ScalarLiteral) {
                    secondExpression = secondExpression.toLowerCase();
                } else if (op2 instanceof Property) {
                    secondExpression = String.format("LOWER(%s::varchar)", secondExpression.toLowerCase());
                }
            }

            String operation = String.format("::varchar%s %s %s", functionEnd, operator, secondExpression);
            return String.format(expressions.get(0), functionStart, operation);
        }

        @Override
        public String visit(In in, List<String> children) {
            String operator = SCALAR_OPERATORS.get(in.getClass());

            String mainExpression = "";
            Scalar op1 = in.getOperand().get();
            if (op1 instanceof Property) {
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
            Scalar op1 = isNull.getOperand().get();
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

            Scalar op1 = between.getOperand().get();
            Scalar op2 = between.getLower().get();
            Scalar op3 = between.getUpper().get();
            List<String> expressions = processTernary(ImmutableList.of(op1, op2, op3), children);

            String operation = String.format(" %s %s AND %s", operator, expressions.get(1), expressions.get(2));
            return String.format(expressions.get(0), "", operation);
        }

        private Instant getStart(TemporalLiteral literal) {
            return (literal.getType() == Interval.class)
                    ? ((Interval) literal.getValue()).getStart()
                    : (Instant) literal.getValue();
        }

        private String getStartAsString(TemporalLiteral literal) {
            return String.format("TIMESTAMP '%s'", getStart(literal))
                         .replace("'0000-01-01T00:00:00Z'", "'-infinity'");
        }

        private Instant getEnd(TemporalLiteral literal) {
            return (literal.getType() == Interval.class)
                    ? ((Interval) literal.getValue()).getEnd()
                    : (Instant) literal.getValue();
        }

        private String getEndAsString(TemporalLiteral literal) {
            return String.format("TIMESTAMP '%s'", getEnd(literal))
                         .replace("'9999-12-31T23:59:59Z'", "'infinity'");
        }

        private Instant getEndExclusive(TemporalLiteral literal) {
            return (literal.getType() == Interval.class)
                    ? ((Interval) literal.getValue()).getEnd().plusSeconds(1)
                    : (Instant) literal.getValue();
        }

        private String getEndExclusiveAsString(TemporalLiteral literal) {
            return String.format("TIMESTAMP '%s'", getEndExclusive(literal))
                         .replace("'+10000-01-01T00:00:00Z'", "'infinity'");
        }

        @Override
        public String visit(TemporalOperation temporalOperation, List<String> children) {
            String operator = TEMPORAL_OPERATORS.get(temporalOperation.getClass());

            Temporal op1 = (Temporal) temporalOperation.getOperand1().get();
            Temporal op2 = (Temporal) temporalOperation.getOperand2().get();

            // TODO The behaviour of the temporal predicates should be improved by 
            //      distinguishing datetime properties of different granularity in
            //      the provider schema; at least second and day, but a more flexible 
            //      solution would be better.
            //      The public review of CQL resulted in a number of comments on the
            //      temporal predicates, partly also relates to this - wait for their 
            //      resolution.

            if (temporalOperation instanceof TEquals) {
                // Both side are a property or an instant, this was checked when the operation was built.
                if (op1 instanceof TemporalLiteral) {
                    children = ImmutableList.of(String.format("TIMESTAMP %s", children.get(0)), children.get(1));
                }
                if (op2 instanceof TemporalLiteral) {
                    children = ImmutableList.of(children.get(0), String.format("TIMESTAMP %s", children.get(1)));
                }
                List<String> expressions = processBinary(ImmutableList.of(op1, op2), children);
                return String.format(expressions.get(0), "", String.format(" %s %s", operator, expressions.get(1)));

            } else if (temporalOperation instanceof Before) {
                if (op1 instanceof TemporalLiteral) {
                    children = ImmutableList.of(getEndAsString((TemporalLiteral) op1), children.get(1));
                }
                if (op2 instanceof TemporalLiteral) {
                    children = ImmutableList.of(children.get(0), getStartAsString((TemporalLiteral) op2));
                }
                List<String> expressions = processBinary(ImmutableList.of(op1, op2), children);
                return String.format(expressions.get(0), "", String.format(" %s %s", operator, expressions.get(1)));

            } else if (temporalOperation instanceof After) {
                if (op1 instanceof TemporalLiteral) {
                    children = ImmutableList.of(getStartAsString((TemporalLiteral) op1), children.get(1));
                }
                if (op2 instanceof TemporalLiteral) {
                    children = ImmutableList.of(children.get(0), getEndAsString((TemporalLiteral) op2));
                }
                List<String> expressions = processBinary(ImmutableList.of(op1, op2), children);
                return String.format(expressions.get(0), "", String.format(" %s %s", operator, expressions.get(1)));

            } else if (temporalOperation instanceof During) {
                // The left hand side is a property or an instant, this was checked when the operation was built.
                // The right hand side is an interval, this was checked when the operation was built.
                Temporal op2a = op2;
                Temporal op2b = op2;
                if (op2 instanceof TemporalLiteral) {
                    op2a = TemporalLiteral.of(getStart((TemporalLiteral) op2));
                    op2b = TemporalLiteral.of(getEnd((TemporalLiteral) op2));
                    children = ImmutableList.of(children.get(0), getStartAsString((TemporalLiteral) op2a), getEndAsString((TemporalLiteral) op2b));
                } else if (op2 instanceof Property) {
                    children = ImmutableList.of(children.get(0), children.get(1), children.get(1));
                }
                List<String> expressions = processTernary(ImmutableList.of(op1, op2a, op2b), children);
                return String.format(expressions.get(0), "", String.format(" %s %s AND %s", operator, expressions.get(1), expressions.get(2)));

            } else if (temporalOperation instanceof AnyInteracts) {
                // ISO 8601 intervals include both the start and end instant
                // PostgreSQL intervals are exclusive of the end instant, so we add one second to each end instant
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
                    children = ImmutableList.of(children.get(0), String.format("(%s, %s)", getStartAsString((TemporalLiteral) op2), getEndExclusiveAsString((TemporalLiteral) op2)));
                }
                List<String> expressions = processBinary(ImmutableList.of(op1, op2), children);
                return String.format(expressions.get(0), "", String.format(" %s %s", operator, expressions.get(1)));
            }

            throw new IllegalArgumentException(String.format("unsupported temporal operator: %s", operator));
        }

        @Override
        public String visit(SpatialOperation spatialOperation, List<String> children) {
            String operator = SPATIAL_OPERATORS.get(spatialOperation.getClass());

            Spatial op1 = (Spatial) spatialOperation.getOperand1().get();
            Spatial op2 = (Spatial) spatialOperation.getOperand2().get();
            List<String> expressions = processBinary(ImmutableList.of(op1, op2), children);

            return String.format(expressions.get(0), String.format("%s(", operator), String.format(", %s)", expressions.get(1)));
        }

        @Override
        public String visit(TemporalLiteral temporalLiteral, List<String> children) {
            return String.format("'%s'", temporalLiteral.getValue());
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
            List<Coordinate> coordinates = ImmutableList.of(
                    Coordinate.of(c.get(0), c.get(1)),
                    Coordinate.of(c.get(2), c.get(1)),
                    Coordinate.of(c.get(2), c.get(3)),
                    Coordinate.of(c.get(0), c.get(3)),
                    Coordinate.of(c.get(0), c.get(1))
            );
            Geometry.Polygon polygon = new ImmutablePolygon.Builder().addCoordinates(coordinates)
                                                                     .crs(envelope.getCrs())
                                                                     .build();

            return visit(polygon, ImmutableList.of());
        }

        @Override
        public String visit(ArrayOperation arrayOperation, List<String> children) {
            String expression = children.get(0);
            String operator = ARRAY_OPERATORS.get(arrayOperation.getClass());
            String operation = String.format(" %s ARRAY[%s]", operator, children.get(1));
            return String.format(expression, "", operation);
        }

        @Override
        public String visit(LogicalOperation logicalOperation, List<String> children) {
            String operator = LOGICAL_OPERATORS.get(logicalOperation.getClass());

            if (Objects.equals(logicalOperation.getClass(), ImmutableNot.class)) {
                String operation = children.get(0);
                if (logicalOperation.getPredicates()
                        .get(0)
                        .getInOperator()
                        .isPresent()) {
                    // replace last IN with NOT IN
                    int pos = operation.lastIndexOf(" IN ");
                    int length = operation.length();
                    return String.format("%s %s %s", operation.substring(0, pos), operator, operation.substring(pos + 1, length));
                }
            }
            return super.visit(logicalOperation, children);
        }
    }

}
