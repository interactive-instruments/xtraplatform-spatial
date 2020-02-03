/**
 * Copyright 2019 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import de.ii.xtraplatform.geometries.domain.EpsgCrs;
import de.ii.xtraplatform.feature.provider.sql.domain.FeatureStoreInstanceContainer;
import de.ii.xtraplatform.feature.provider.sql.domain.FilterEncoderSql;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.And;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsLike;
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
import org.xml.sax.helpers.NamespaceSupport;

import javax.ws.rs.BadRequestException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author zahnen
 */
public class FilterEncoderSqlImpl implements FilterEncoderSql {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterEncoderSqlImpl.class);

    //private final FeatureStoreInstanceContainer typeInfo;
    //private final Map<String, String> propertiesToPaths;

    //TODO: add property name to FeatureStoreAttributesContainer, remove need for mapping here
    //TODO: make typeInfo runtime parameter
    //TODO: get expressions from SqlDialect
    public FilterEncoderSqlImpl(FeatureStoreInstanceContainer typeInfo, FeatureTypeMapping featureTypeMapping) {
        //this.typeInfo = typeInfo;
        //LOGGER.debug("PATHS {}", queries.getPaths());
        /*this.propertiesToPaths = featureTypeMapping.findMappings(TargetMapping.BASE_TYPE)
                                                   .entrySet()
                                                   .stream()
                                                   .filter(entry -> Objects.nonNull(entry.getValue()
                                                                                         .getName()))
                                                   .map(entry -> new AbstractMap.SimpleEntry<>(entry.getValue()
                                                                                                    .getName(), entry.getKey()))
                                                   .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));*/
    }

    ///fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/name LIKE '*Mat*'
    //fundorttiere.id IN (SELECT artbeobachtung_2_erfasser.artbeobachtung_id FROM artbeobachtung_2_erfasser JOIN erfasser ON artbeobachtung_2_erfasser.erfasser_id=erfasser.id WHERE erfasser.name ILIKE '%Mat%' )
    public String encode(final String cqlFilter, FeatureStoreInstanceContainer typeInfo) {

        StringBuilder encoded = new StringBuilder();
        List<String> properties = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        List<String> conditionProperties = new ArrayList<>();

        try {
            ECQL.toFilter(cqlFilter)
                .accept(new DuplicatingFilterVisitor() {
                    final FilterFactory2 filterFactory = new FilterFactoryImpl();
                    final NamespaceSupport namespaceSupport = new NamespaceSupport();

                    @Override
                    public Object visit(And filter, Object extraData) {
                        LOGGER.debug("AND {} {}", filter.getChildren(), extraData);
                        //encoded.append(" AND ");
                        return super.visit(filter, extraData);
                    }

                    @Override
                    public Object visit(PropertyName expression, Object extraData) {

                        //TODO: fast enough? maybe pass all typeInfos to constructor and create map?
                        //TODO: remove propertyPathsToSelect, use queryGenerator instead
                        Optional<String> pathForProperty = typeInfo.getAllAttributesContainers()
                                                                   .stream()
                                                                   .flatMap(attributesContainer -> attributesContainer.getAttributes()
                                                                                                                  .stream())
                                                                   .filter(attribute -> attribute.getQueryable()
                                                                                                 .isPresent() && Objects.equals(expression.getPropertyName(), attribute.getQueryable()
                                                                                                                                                                       .get()))
                                                                   .findFirst()
                                                                   .map(attributesContainer -> propertyPathsToSelect(attributesContainer.getPath()));

                        //Optional<String> pathForProperty = Optional.ofNullable(propertiesToPaths.getOrDefault(expression.getPropertyName(), null))
                        //                                           .map(path -> propertyPathsToSelect(path));

                        if (!pathForProperty.isPresent()) {
                            throw new BadRequestException("Filter invalid");
                        }
                        /*Optional<String> path = queries.getPaths()
                                                       .stream()
                                                       .filter(p -> propertyPathsToShort(p).equals(expression.getPropertyName()))
                                                       .map(p -> propertyPathsToSelect(p))
                                                       .findFirst();*/
                        String path2 = pathForProperty.get()
                                                      .substring(0, pathForProperty.get()
                                                                                   .lastIndexOf(" ") + 1);
                        String cp = pathForProperty.get()
                                                   .substring(pathForProperty.get()
                                                                             .lastIndexOf(" ") + 1);

                        LOGGER.debug("PROP {} {}", expression.getPropertyName(), pathForProperty.get());
                        properties.add(path2);
                        conditionProperties.add(stripFunctions(cp));
                        //encoded.append(path.get());
                        return super.visit(filterFactory.property(pathForProperty.get(), namespaceSupport), extraData);

                        //return super.visit(expression, extraData);
                    }

                    //TODO: test if still works
                    @Override
                    public Object visit(BBOX filter, Object extraData) {
                        LOGGER.debug("BBOX {} | {}, {}, {}, {}", filter.getExpression1(), filter.getBounds().getMinX(), filter.getBounds().getMinY(), filter.getBounds().getMaxX(), filter.getBounds().getMaxY());

                        conditions.add(String.format(Locale.US, "ST_Intersects({{prop}}, ST_GeomFromText('POLYGON((%1$f %2$f,%3$f %2$f,%3$f %4$f,%1$f %4$f,%1$f %2$f))',%5$s)) = 'TRUE'", filter.getBounds().getMinX(), filter.getBounds().getMinY(), filter.getBounds().getMaxX(), filter.getBounds().getMaxY(), EpsgCrs.fromString(filter.getBounds()
                                                                                                                                                                                                                                                                                                                                           .getCoordinateReferenceSystem()
                                                                                                                                                                                                                                                                                                                                           .toString()).getCode()));
                        return super.visit(filter, extraData);
                    }

                    @Override
                    public Object visit(PropertyIsEqualTo filter, Object data) {
                        LOGGER.debug("EQUALS {}", filter);
                        //encoded.append(" = '").append(filter.getExpression2()).append("'");
                        conditions.add("{{prop}} = '" + filter.getExpression2() + "'");
                        return super.visit(filter, data);
                    }

                    @Override
                    public Object visit(PropertyIsLike filter, Object data) {
                        LOGGER.debug("LIKE {}", filter);
                        //encoded.append(filter.getExpression()).append(" LIKE '").append(filter.getLiteral()).append("'");
                        conditions.add("{{prop}} LIKE '" + filter.getLiteral()
                                                                 .replace('*', '%') + "'");
                        return super.visit(filter, data);
                    }

                    @Override
                    public Object visit(During during, Object extraData) {
                        LOGGER.debug("DURING {}", during);
                        //encoded.append(filter.getExpression()).append(" LIKE '").append(filter.getLiteral()).append("'");
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
                        LOGGER.debug("TEQUALS {}", equals);

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

                        String ids = Joiner.on(',')
                                           .join(filter.getIDs());

                        //properties.add(queries.getMainQuery().getDefaultSortKey());
                        //conditionProperties.add(queries.getMainQuery().getDefaultSortKey());
                        conditions.add(String.format("%s IN (%s)", typeInfo.getIdField(), ids));

                        return super.visit(filter, extraData);
                    }

                    protected Instant toInstant(Expression e) {
                        return (Instant) e.evaluate(null, Instant.class);
                    }

                    protected Period toPeriod(Expression e) {
                        return (Period) e.evaluate(null, Period.class);
                    }

                }, null);
        } catch (CQLException e) {
            throw new IllegalArgumentException("filter not valid: " + cqlFilter, e);
        }

        encoded.append("(");

        if (properties.isEmpty() && conditions.size() == 1) {
            encoded.append(conditions.get(0));
        } else {

            for (int i = 0; i < properties.size(); i++) {
                if (i > 0) encoded.append(" AND ");
                encoded.append("(");
                encoded.append(properties.get(i))
                       .append(conditions.get(i)
                                         .replace("{{prop}}", conditionProperties.get(i)));
                encoded.append(")");
                encoded.append(")");
            }
        }

        encoded.append(")");

        return encoded.toString();
    }

    private String propertyPathsToCql(String propertyPath) {
        return propertyPathsToShort(propertyPath).replace('/', '.');
    }

    private String propertyPathsToShort(String propertyPath) {

        return propertyPath.replaceAll("(?:(?:(^| |\\()/)|(/))(?:\\[\\w+=\\w+\\])?(?:\\w+\\()*(\\w+)(?:\\)(?:,| |\\)))*", "$1$2$3");
    }

    //TODO: PathParser + QueryGenerator
    private String propertyPathsToSelect(List<String> pathElements) {
        //List<String> pathElements = getPathElements(propertyPath);
        String parentTable = toTable(pathElements.get(0));
        String parentColumn = toCondition(pathElements.get(1))[0];
        String mainTable = toTable(pathElements.get(pathElements.size() - 3));
        String mainColumn = toCondition(pathElements.get(pathElements.size() - 3)).length > 1 ? toCondition(pathElements.get(pathElements.size() - 3))[1] : "id";
        String conditionTable = toTable(pathElements.get(pathElements.size() - 2));
        String conditionColumn = pathElements.get(pathElements.size() - 1);


        String join = pathElements.subList(pathElements.size() - 2, pathElements.size() - 1)
                                  .stream()
                                  .map(pathElement -> {
                                      String[] joinCondition = toCondition(pathElement);
                                      String table = toTable(pathElement);

                                      return String.format("JOIN %1$s ON %3$s.%2$s=%1$s.%4$s", table.equals(mainTable) ? table + " AS MAIN2" : table, joinCondition[0], mainTable, joinCondition[1]);
                                  })
                                  .collect(Collectors.joining(" "));

        return String.format("%6$s.%7$s IN (SELECT %1$s.%2$s FROM %1$s %3$s WHERE %4$s.%5$s", mainTable, mainColumn, join, conditionTable, conditionColumn, parentTable, parentColumn);
    }

    private String stripFunctions(String column) {
        if (column.contains(".")) {
            List<String> split = Splitter.on('.')
                                         .omitEmptyStrings()
                                         .splitToList(column);
            return split.get(0) + "." + (split.get(1)
                                              .contains("(") ? split.get(1)
                                                                    .substring(split.get(1)
                                                                                    .lastIndexOf("(") + 1, split.get(1)
                                                                                                                .indexOf(")")) : split.get(1));
        }
        return column.contains("(") ? column.substring(column.lastIndexOf("(") + 1, column.indexOf(")")) : column;
    }

    private String toTable(String pathElement) {
        return pathElement.substring(pathElement.indexOf("]") + 1);
    }

    private String[] toCondition(String pathElement) {
        return pathElement.contains("]") ? pathElement.substring(1, pathElement.indexOf("]"))
                                                      .split("=") : new String[]{pathElement};
    }

    private List<String> getPathElements(String path) {
        return Splitter.on('/')
                       .omitEmptyStrings()
                       .splitToList(path);
    }
}
