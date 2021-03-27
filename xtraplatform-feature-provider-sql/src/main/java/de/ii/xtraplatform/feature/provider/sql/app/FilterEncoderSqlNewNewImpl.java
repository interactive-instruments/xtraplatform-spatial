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
import de.ii.xtraplatform.cql.domain.CqlToText;
import de.ii.xtraplatform.cql.domain.Between;
import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.cql.domain.During;
import de.ii.xtraplatform.cql.domain.Geometry;
import de.ii.xtraplatform.cql.domain.Geometry.Coordinate;
import de.ii.xtraplatform.cql.domain.ImmutableAfter;
import de.ii.xtraplatform.cql.domain.ImmutableBefore;
import de.ii.xtraplatform.cql.domain.ImmutableContains;
import de.ii.xtraplatform.cql.domain.ImmutableCrosses;
import de.ii.xtraplatform.cql.domain.ImmutableDisjoint;
import de.ii.xtraplatform.cql.domain.ImmutableDuring;
import de.ii.xtraplatform.cql.domain.ImmutableEquals;
import de.ii.xtraplatform.cql.domain.ImmutableIntersects;
import de.ii.xtraplatform.cql.domain.ImmutableOverlaps;
import de.ii.xtraplatform.cql.domain.ImmutablePolygon;
import de.ii.xtraplatform.cql.domain.ImmutableTEquals;
import de.ii.xtraplatform.cql.domain.ImmutableTOverlaps;
import de.ii.xtraplatform.cql.domain.ImmutableTouches;
import de.ii.xtraplatform.cql.domain.ImmutableWithin;
import de.ii.xtraplatform.cql.domain.In;
import de.ii.xtraplatform.cql.domain.IsNull;
import de.ii.xtraplatform.cql.domain.Like;
import de.ii.xtraplatform.cql.domain.Property;
import de.ii.xtraplatform.cql.domain.ScalarOperation;
import de.ii.xtraplatform.cql.domain.SpatialOperation;
import de.ii.xtraplatform.cql.domain.TOverlaps;
import de.ii.xtraplatform.cql.domain.TemporalLiteral;
import de.ii.xtraplatform.cql.domain.TemporalOperation;
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
            .put(ImmutableTOverlaps.class, "OVERLAPS")
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

        //TODO: unary + nnary
        @Override
        public String visit(ScalarOperation scalarOperation, List<String> children) {
            String propertyExpression = children.get(0);
            String value = children.size() > 1 ? children.get(1) : "";
            String operator = CqlToText.SCALAR_OPERATORS.get(scalarOperation.getClass());
            String operation = String.format(" %s %s", operator, value);

            if (scalarOperation instanceof Between) {
                operation = String.format(" %s %s AND %s", operator, children.get(1), children.get(2));
            } else if (scalarOperation instanceof In) {
                operation = String.format(" %s (%s)", operator, String.join(", ", children.subList(1, children.size())));
            } else if (scalarOperation instanceof IsNull /*|| scalarOperation instanceof Exists*/) {
                //TODO: what is the difference between EXISTS and IS NULL? Postgres only knows the latter.
                //operator = CqlToText.SCALAR_OPERATORS.get(ImmutableIsNull.class);
                operation = String.format(" %s", operator);
            } else if (scalarOperation instanceof Like && !Objects.equals("%", ((Like) scalarOperation).getWildCard())) {
                String wildCard = ((Like) scalarOperation).getWildCard()
                                                          .replace("*", "\\*");
                value = value.replaceAll("%", "\\%")
                             .replaceAll(wildCard, "%");
                operation = String.format("::varchar %s %s", operator, value);
            }

            return String.format(propertyExpression, "", operation);
        }

        @Override
        public String visit(TemporalOperation temporalOperation, List<String> children) {
            String expression = children.get(0);
            String operator = TEMPORAL_OPERATORS.get(temporalOperation.getClass());

            // ISO 8601 intervals include both the start and end instant
            // PostgreSQL intervals are exclusive of the end instant, so we add one second to each end instant
            
            if (temporalOperation instanceof During) {
                Interval interval = (Interval) temporalOperation.getValue()
                                                                .get()
                                                                .getValue();
                if (interval.isUnboundedStart() && interval.isUnboundedEnd()) {
                    return "TRUE";
                } else if (interval.isUnboundedStart()) {
                    operator = TEMPORAL_OPERATORS.get(ImmutableBefore.class);
                    return String.format(expression, "", String.format(" %s TIMESTAMP '%s'", operator, interval.getEnd()
                                                                                                               .plusSeconds(1)
                                                                                                               .toString()));
                } else if (interval.isUnboundedEnd()) {
                    operator = TEMPORAL_OPERATORS.get(ImmutableAfter.class);
                    return String.format(expression, "", String.format(" %s TIMESTAMP '%s'", operator, interval.getStart()
                                                                                                               .toString()));
                }

                String[] interval2 = children.get(1)
                                             .split("/");
                return String.format(expression, "", String.format(" %s TIMESTAMP %s' AND TIMESTAMP '%s", operator, interval2[0], interval.getEnd()
                                                                                                                                          .plusSeconds(1)
                                                                                                                                          .toString()));
            }

            if (temporalOperation instanceof TOverlaps) {
                TemporalLiteral operand2 = (TemporalLiteral) temporalOperation.getOperands()
                                                                              .get(1);

                String literalInterval;

                if (operand2.getType() == Interval.class) {
                    Interval interval = (Interval) temporalOperation.getValue()
                                                                    .get()
                                                                    .getValue();
                    literalInterval = String.format("(TIMESTAMP '%s', TIMESTAMP '%s')", interval.getStart(), interval.getEnd()
                                                                                                                     .plusSeconds(1));
                    literalInterval = literalInterval.replaceAll("0000-01-01T00:00:00Z", "-infinity");
                } else {
                    Instant instant = (Instant) temporalOperation.getValue()
                                                                 .get()
                                                                 .getValue();
                    literalInterval = String.format("(TIMESTAMP '%s', TIMESTAMP '%s')", instant, instant.plusSeconds(1));
                }

                return String.format(expression, "", String.format(" %s %s", operator, literalInterval));
            }

            return String.format(expression, "", String.format(" %s TIMESTAMP %s", operator, children.get(1)));
        }

        @Override
        public String visit(SpatialOperation spatialOperation, List<String> children) {
            String expression = children.get(0);
            String operator = SPATIAL_OPERATORS.get(spatialOperation.getClass());

            return String.format(expression, String.format("%s(", operator), String.format(", %s)", children.get(1)));
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
    }

}
