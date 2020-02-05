/**
 * Copyright 2019 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreAttribute;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreAttributesContainer;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.feature.provider.sql.domain.FilterEncoderSqlNew;
import de.ii.xtraplatform.feature.provider.sql.domain.ImmutableSqlCondition;
import de.ii.xtraplatform.feature.provider.sql.domain.SqlCondition;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.And;
import org.opengis.filter.Id;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.TEquals;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class FilterEncoderSqlNewImpl implements FilterEncoderSqlNew {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterEncoderSqlNewImpl.class);

    private final EpsgCrs nativeCrs;

    FilterEncoderSqlNewImpl(EpsgCrs nativeCrs) {
        this.nativeCrs = nativeCrs;
    }

    //TODO: get expressions from SqlDialect
    //TODO: implement own CQL parser
    // currently we hijack the transformation from cql to ogc filter to get a sql filter
    // that's pretty dirty and also not very fast
    public List<SqlCondition> encode(final String cqlFilter, FeatureStoreInstanceContainer typeInfo) {

        List<String> columns = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        Map<Integer, Boolean> nots = new HashMap<>();
        Map<Integer, Boolean> ors = new HashMap<>();
        List<FeatureStoreAttributesContainer> tables = new ArrayList<>();

        try {
            ECQL.toFilter(cqlFilter)
                .accept(new DuplicatingFilterVisitor() {

                    @Override
                    public Object visit(And filter, Object extraData) {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("AND {} {}", filter.getChildren(), extraData);
                        }

                        return super.visit(filter, extraData);
                    }

                    @Override
                    public Object visit(PropertyName expression, Object extraData) {

                        //TODO: fast enough? maybe pass all typeInfos to constructor and create map?
                        Predicate<FeatureStoreAttribute> propertyMatches = attribute -> attribute.getQueryable()
                                                                                                 .isPresent() && Objects.equals(expression.getPropertyName(), attribute.getQueryable()
                                                                                                                                                                       .get());
                        Optional<FeatureStoreAttributesContainer> table = typeInfo.getAllAttributesContainers()
                                                                                  .stream()
                                                                                  .filter(attributesContainer -> attributesContainer.getAttributes()
                                                                                                                                    .stream()
                                                                                                                                    .anyMatch(propertyMatches))
                                                                                  .findFirst();

                        Optional<String> column = table.flatMap(attributesContainer -> attributesContainer.getAttributes()
                                                                                                          .stream()
                                                                                                          .filter(propertyMatches)
                                                                                                          .findFirst()
                                                                                                          .map(FeatureStoreAttribute::getName));

                        if (!table.isPresent() || !column.isPresent()) {
                            throw new BadRequestException("Filter invalid");//TODO: field not allowed, valid fields are etc.
                        }

                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("PROP {} {}", table.get()
                                                            .getName(), column.get());
                        }

                        columns.add(column.get());
                        tables.add(table.get());

                        return super.visit(expression, extraData);
                    }

                    //TODO: test if still works
                    @Override
                    public Object visit(BBOX filter, Object extraData) {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("BBOX {} | {}, {}, {}, {}", filter.getExpression1(), filter.getBounds().getMinX(), filter.getBounds().getMinY(), filter.getBounds().getMaxX(), filter.getBounds().getMaxY());
                        }

                        conditions.add(String.format(Locale.US, "ST_Intersects({{prop}}, ST_GeomFromText('POLYGON((%1$f %2$f,%3$f %2$f,%3$f %4$f,%1$f %4$f,%1$f %2$f))',%5$s)) = 'TRUE'", filter.getBounds().getMinX(), filter.getBounds().getMinY(), filter.getBounds().getMaxX(), filter.getBounds().getMaxY(), nativeCrs.getCode()));

                        return super.visit(filter, extraData);
                    }

                    @Override
                    public Object visit(PropertyIsEqualTo filter, Object data) {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("EQUALS {}", filter);
                        }

                        conditions.add("{{prop}} = '" + filter.getExpression2() + "'");

                        return super.visit(filter, data);
                    }

                    @Override
                    public Object visit(PropertyIsLike filter, Object data) {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("LIKE {}", filter);
                        }

                        conditions.add("{{prop}} LIKE '" + filter.getLiteral()
                                                                 .replace('*', '%') + "'");

                        return super.visit(filter, data);
                    }

                    @Override
                    public Object visit(During during, Object extraData) {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("DURING {}", during);
                        }

                        DateTimeFormatter dateTimeFormatter = DateTimeFormatter
                                .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
                        Period period = toPeriod(during.getExpression2());
                        ZonedDateTime localDateTime = ZonedDateTime.parse(period.getBeginning()
                                                                                .getPosition()
                                                                                .getDateTime(), dateTimeFormatter);
                        ZonedDateTime localDateTime2 = ZonedDateTime.parse(period.getEnding()
                                                                                 .getPosition()
                                                                                 .getDateTime(), dateTimeFormatter);
                        conditions.add("{{prop}} BETWEEN '" + localDateTime.toInstant()
                                                                           .toString() + "' AND '" + localDateTime2.toInstant()
                                                                                                                   .toString() + "'");

                        return super.visit(during, extraData);
                    }

                    @Override
                    public Object visit(TEquals equals, Object extraData) {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("TEQUALS {}", equals);
                        }

                        DateTimeFormatter dateTimeFormatter = DateTimeFormatter
                                .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
                        ZonedDateTime localDateTime = ZonedDateTime.parse(toInstant(equals.getExpression2()).getPosition()
                                                                                                            .getDateTime(), dateTimeFormatter);
                        conditions.add("{{prop}} = '" + localDateTime.toInstant()
                                                                     .toString() + "'");

                        return super.visit(equals, extraData);
                    }

                    @Override
                    public Object visit(After after, Object extraData) {

                        DateTimeFormatter dateTimeFormatter = DateTimeFormatter
                                .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
                        ZonedDateTime localDateTime = ZonedDateTime.parse(toInstant(after.getExpression2()).getPosition()
                                                                                                           .getDateTime(), dateTimeFormatter);
                        conditions.add("{{prop}} > '" + localDateTime.toInstant()
                                                                     .toString() + "'");

                        return super.visit(after, extraData);
                    }

                    @Override
                    public Object visit(Before before, Object extraData) {

                        DateTimeFormatter dateTimeFormatter = DateTimeFormatter
                                .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
                        ZonedDateTime localDateTime = ZonedDateTime.parse(toInstant(before.getExpression2()).getPosition()
                                                                                                            .getDateTime(), dateTimeFormatter);
                        conditions.add("{{prop}} < '" + localDateTime.toInstant()
                                                                     .toString() + "'");

                        return super.visit(before, extraData);
                    }

                    @Override
                    public Object visit(Id filter, Object extraData) {

                        String ids = filter.getIDs()
                                           .stream()
                                           .map(id -> {
                                               try {
                                                   long longId = Long.parseLong((String) id);
                                               } catch (Throwable e) {
                                                   // not a number
                                                   return String.format("'%s'", id);
                                               }

                                               return (String) id;
                                           })
                                           .collect(Collectors.joining(","));

                        tables.add(typeInfo);
                        columns.add(typeInfo.getIdField());
                        conditions.add(String.format("{{prop}} IN (%s)", ids));

                        return super.visit(filter, extraData);
                    }

                    @Override
                    protected Expression visit(Expression expression, Object extraData) {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Expression {} {}", expression, extraData);
                        }

                        //conditions.add("{{prop}} = '" + filter.getExpression2() + "'");

                        return super.visit(expression, extraData);
                    }

                    @Override
                    public Object visit(Not filter, Object extraData) {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Not {}", filter);
                        }

                        nots.put(conditions.size(), true);
                        //conditions.add("{{prop}} = '" + filter.getExpression2() + "'");

                        return super.visit(filter, extraData);
                    }

                    @Override
                    public Object visit(Or filter, Object extraData) {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Or {} {} {}", filter, extraData, filter.getChildren().size());
                        }

                        ors.put(conditions.size(), true);
                        //conditions.add("{{prop}} = '" + filter.getExpression2() + "'");

                        return super.visit(filter, extraData);
                    }

                    @Override
                    public Object visit(PropertyIsNotEqualTo filter, Object extraData) {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("PropertyIsNotEqualTo {}", filter);
                        }

                        conditions.add("{{prop}} != '" + filter.getExpression2() + "'");

                        return super.visit(filter, extraData);
                    }

                    @Override
                    public Object visit(PropertyIsGreaterThan filter, Object extraData) {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("PropertyIsGreaterThan {}", filter);
                        }

                        conditions.add("{{prop}} > '" + filter.getExpression2() + "'");

                        return super.visit(filter, extraData);
                    }

                    @Override
                    public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object extraData) {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("PropertyIsGreaterThanOrEqualTo {}", filter);
                        }

                        conditions.add("{{prop}} >= '" + filter.getExpression2() + "'");

                        return super.visit(filter, extraData);
                    }

                    @Override
                    public Object visit(PropertyIsLessThan filter, Object extraData) {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("PropertyIsLessThan {}", filter);
                        }

                        conditions.add("{{prop}} < '" + filter.getExpression2() + "'");

                        return super.visit(filter, extraData);
                    }

                    @Override
                    public Object visit(PropertyIsLessThanOrEqualTo filter, Object extraData) {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("PropertyIsLessThanOrEqualTo {}", filter);
                        }

                        conditions.add("{{prop}} <= '" + filter.getExpression2() + "'");

                        return super.visit(filter, extraData);
                    }

                    @Override
                    public Object visit(PropertyIsNull filter, Object extraData) {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("PropertyIsNull {}", filter);
                        }

                        conditions.add("{{prop}} = NULL");

                        return super.visit(filter, extraData);
                    }

                    private Instant toInstant(Expression e) {
                        return e.evaluate(null, Instant.class);
                    }

                    private Period toPeriod(Expression e) {
                        return e.evaluate(null, Period.class);
                    }

                }, null);
        } catch (CQLException e) {
            throw new IllegalArgumentException("filter not valid: " + cqlFilter, e);
        }

        ImmutableList.Builder<SqlCondition> sqlConditions = ImmutableList.builder();

        for (int i = 0; i < columns.size(); i++) {
            String expression = conditions.get(i);
            if (nots.containsKey(i) && nots.get(i)) {
                expression = "NOT " + expression;
            }
            sqlConditions.add(ImmutableSqlCondition.builder()
                                                   .column(columns.get(i))
                                                   .table(tables.get(i))
                                                   .expression(expression)
                                                   .isOr(ors.containsKey(i) && ors.get(i))
                                                   .build());
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("SQL Filter {}", sqlConditions.build());
        }

        return sqlConditions.build();
    }
}
