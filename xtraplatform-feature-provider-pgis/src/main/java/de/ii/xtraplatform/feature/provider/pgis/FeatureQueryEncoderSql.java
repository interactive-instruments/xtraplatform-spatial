package de.ii.xtraplatform.feature.provider.pgis;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfigurationOld;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.spatial.BBOXImpl;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.spatial.BBOX;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.AnyInteracts;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.Begins;
import org.opengis.filter.temporal.BegunBy;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.EndedBy;
import org.opengis.filter.temporal.Ends;
import org.opengis.filter.temporal.Meets;
import org.opengis.filter.temporal.MetBy;
import org.opengis.filter.temporal.OverlappedBy;
import org.opengis.filter.temporal.TContains;
import org.opengis.filter.temporal.TEquals;
import org.opengis.filter.temporal.TOverlaps;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.helpers.NamespaceSupport;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author zahnen
 */
public class FeatureQueryEncoderSql {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureQueryEncoderSql.class);

    private final SqlFeatureQueries queries;

    public FeatureQueryEncoderSql(SqlFeatureQueries queries) {
        this.queries = queries;
        //LOGGER.debug("PATHS {}", queries.getPaths());
    }

    ///fundorttiere/[id=id]artbeobachtung/[id=artbeobachtung_id]artbeobachtung_2_erfasser/[erfasser_id=id]erfasser/name LIKE '*Mat*'
    //fundorttiere.id IN (SELECT artbeobachtung_2_erfasser.artbeobachtung_id FROM artbeobachtung_2_erfasser JOIN erfasser ON artbeobachtung_2_erfasser.erfasser_id=erfasser.id WHERE erfasser.name ILIKE '%Mat%' )
    public String encodeFilter(final String filter) throws CQLException {

        StringBuilder encoded = new StringBuilder();
        List<String> properties = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        List<String> conditionProperties = new ArrayList<>();

        ECQL.toFilter(filter)
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
                    Optional<String> path = queries.getPaths().stream()
                                                   .filter(p -> propertyPathsToShort(p).equals(expression.getPropertyName()))
                                                   .map(p -> propertyPathsToSelect(p))
                                                   .findFirst();
                    String path2 = path.get().substring(0, path.get().lastIndexOf(" ")+1);
                    String cp = path.get().substring(path.get().lastIndexOf(" ")+1);

                    LOGGER.debug("PROP {} {}", expression.getPropertyName(), path.get());
                    properties.add(path2);
                    conditionProperties.add(stripFunctions(cp));
                    //encoded.append(path.get());
                    return  super.visit(filterFactory.property(path.get(), namespaceSupport), extraData);

                    //return super.visit(expression, extraData);
                }

                @Override
                public Object visit(BBOX filter, Object extraData) {
                    LOGGER.debug("BBOX {} | {}, {}, {}, {}", filter.getPropertyName(), filter.getMinX(), filter.getMinY(), filter.getMaxX(), filter.getMaxY());

                    //conditions.add(String.format("ST_Intersects({{prop}}, ST_GeomFromText('POLYGON((%2$s %1$s,%2$s %3$s,%4$s %3$s,%4$s %1$s,%2$s %1$s))',4326)) = 'TRUE'", filter.getMinX(), filter.getMinY(), filter.getMaxX(), filter.getMaxY()));
                    //TODO: crs from config
                    conditions.add(String.format(Locale.US, "ST_Intersects({{prop}}, ST_GeomFromText('POLYGON((%1$.3f %2$.3f,%3$.3f %2$.3f,%3$.3f %4$.3f,%1$.3f %4$.3f,%1$.3f %2$.3f))',25832)) = 'TRUE'", filter.getMinX(), filter.getMinY(), filter.getMaxX(), filter.getMaxY()));
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
                    conditions.add("{{prop}} LIKE '" + filter.getLiteral().replace('*', '%') + "'");
                    return super.visit(filter, data);
                }

                @Override
                public Object visit(During during, Object extraData) {
                    LOGGER.debug("DURING {}", during);
                    //encoded.append(filter.getExpression()).append(" LIKE '").append(filter.getLiteral()).append("'");
                    DateTimeFormatter dateTimeFormatter = DateTimeFormatter
                            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
                    Period period = toPeriod(during.getExpression2());
                    ZonedDateTime localDateTime = ZonedDateTime.parse(period.getBeginning().getPosition().getDateTime(), dateTimeFormatter);
                    ZonedDateTime localDateTime2 = ZonedDateTime.parse(period.getEnding().getPosition().getDateTime(), dateTimeFormatter);
                    conditions.add("{{prop}} BETWEEN '" + localDateTime.toInstant().toString() + "' AND '" + localDateTime2.toInstant().toString() + "'");
                    return super.visit(during, extraData);
                }

                @Override
                public Object visit(TEquals equals, Object extraData) {
                    LOGGER.debug("TEQUALS {}", equals);

                    DateTimeFormatter dateTimeFormatter = DateTimeFormatter
                            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
                    ZonedDateTime localDateTime = ZonedDateTime.parse(toInstant(equals.getExpression2()).getPosition().getDateTime(), dateTimeFormatter);
                    conditions.add("{{prop}} = '" + localDateTime.toInstant().toString() + "'");
                    return super.visit(equals, extraData);
                }

                protected Instant toInstant(Expression e) {
                    return (Instant)e.evaluate(null, Instant.class);
                }

                protected Period toPeriod(Expression e) {
                    return (Period)e.evaluate(null, Period.class);
                }

            }, null);

        encoded.append("(");
        for (int i = 0; i < properties.size(); i++) {
            if (i > 0) encoded.append(" AND ");
            encoded.append("(");
            encoded.append(properties.get(i)).append(conditions.get(i).replace("{{prop}}", conditionProperties.get(i)));
            encoded.append(")");
            encoded.append(")");
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

    //TODO: to SqlFeatureQuery?
    private String propertyPathsToSelect(String propertyPath) {
        List<String> pathElements = getPathElements(propertyPath);
        String parentTable = toTable(pathElements.get(0));
        String parentColumn = toCondition(pathElements.get(1))[0];
        String mainTable = toTable(pathElements.get(pathElements.size()-3));
        String mainColumn = toCondition(pathElements.get(pathElements.size()-3)).length > 1 ? toCondition(pathElements.get(pathElements.size()-3))[1] : "id";
        String conditionTable = toTable(pathElements.get(pathElements.size()-2));
        String conditionColumn = pathElements.get(pathElements.size()-1);


        String join = pathElements.subList(pathElements.size()-2, pathElements.size()-1).stream()
                                                            .map(pathElement -> {
                                                                String[] joinCondition = toCondition(pathElement);
                                                                String table = toTable(pathElement);

                                                                return String.format("JOIN %1$s ON %3$s.%2$s=%1$s.%4$s", table, joinCondition[0], mainTable, joinCondition[1]);
                                                            })
                                                            .collect(Collectors.joining(" "));

        return String.format("%6$s.%7$s IN (SELECT %1$s.%2$s FROM %1$s %3$s WHERE %4$s.%5$s", mainTable, mainColumn, join, conditionTable, conditionColumn, parentTable, parentColumn);
    }

    private String stripFunctions(String column) {
        if (column.contains(".")) {
            List<String> split = Splitter.on('.').omitEmptyStrings().splitToList(column);
            return split.get(0) + "." + (split.get(1).contains("(") ? split.get(1).substring(split.get(1).lastIndexOf("(") + 1, split.get(1).indexOf(")")) : split.get(1));
        }
        return column.contains("(") ? column.substring(column.lastIndexOf("(") + 1, column.indexOf(")")) : column;
    }

    private String toTable(String pathElement) {
        return pathElement.substring(pathElement.indexOf("]") + 1);
    }

    private String[] toCondition(String pathElement) {
        return pathElement.contains("]") ? pathElement.substring(1, pathElement.indexOf("]")).split("=") : new String[]{pathElement};
    }

    private List<String> getPathElements(String path) {
        return Splitter.on('/')
                       .omitEmptyStrings()
                       .splitToList(path);
    }

    private Filter encodeFilter(final String filter, final FeatureTypeConfigurationOld featureType) throws CQLException {
        if (Objects.isNull(filter) || Objects.isNull(featureType)) {
            return null;
        }

        return (Filter) ECQL.toFilter(filter)
                            .accept(new ResolvePropertyNamesFilterVisitor(featureType), null);
    }

    private class ResolvePropertyNamesFilterVisitor extends DuplicatingFilterVisitor {
        final FilterFactory2 filterFactory = new FilterFactoryImpl();
        final NamespaceSupport namespaceSupport;
        final FeatureTypeConfigurationOld featureType;

        private ResolvePropertyNamesFilterVisitor(final FeatureTypeConfigurationOld featureType) {
            namespaceSupport = new NamespaceSupport();
            //namespaceNormalizer.getNamespaces().forEach(namespaceSupport::declarePrefix);
            this.featureType = featureType;
        }

        @Override
        public Object visit(PropertyName expression, Object extraData) {
            LOGGER.debug("PROP {} {}", expression.getPropertyName(), extraData);

            Optional<String> property = getPrefixedPropertyName(expression.getPropertyName());

            if (property.isPresent()) {
                LOGGER.debug("PROP {}", property.get());
                return filterFactory.property(property.get(), namespaceSupport);
            }

            return super.visit(expression, extraData);
        }

        @Override
        public Object visit(BBOX filter, Object extraData) {
            LOGGER.debug("BBOX {} | {} | {}", filter.getExpression1(), filter.getSRS(), extraData);

            Optional<String> property = getPrefixedPropertyName(filter.getExpression1().toString());

            if (property.isPresent()) {
                LOGGER.debug("PROP {}", property.get());
                if (filter.getSRS() != null) {
                    return new BBOXImpl(filterFactory.property(property.get(), namespaceSupport), filter.getBounds().getMinX(), filter.getBounds().getMinY(), filter.getBounds().getMaxX(), filter.getBounds().getMaxY(), new EpsgCrs(filter.getSRS()).getAsUri());
                }
                return filterFactory.bbox(filterFactory.property(property.get(), namespaceSupport), filter.getBounds());
            }

            return super.visit(filter, extraData);
        }

        @Override
        public Object visit(TEquals equals, Object extraData) {
            Expression t1 = toTemporal(equals.getExpression1());
            Expression t2 = toTemporal(equals.getExpression2());

            return super.visit(filterFactory.tequals(t1, t2), extraData);
        }

        @Override
        public Object visit(After after, Object extraData) {
            Expression t1 = toTemporal(after.getExpression1());
            Expression t2 = toTemporal(after.getExpression2());

            return super.visit(filterFactory.after(t1, t2), extraData);
        }

        @Override
        public Object visit(AnyInteracts anyInteracts, Object extraData) {
            Expression t1 = toTemporal(anyInteracts.getExpression1());
            Expression t2 = toTemporal(anyInteracts.getExpression2());

            return super.visit(filterFactory.anyInteracts(t1, t2), extraData);
        }

        @Override
        public Object visit(Before before, Object extraData) {
            Expression t1 = toTemporal(before.getExpression1());
            Expression t2 = toTemporal(before.getExpression2());

            return super.visit(filterFactory.before(t1, t2), extraData);
        }

        @Override
        public Object visit(Begins begins, Object extraData) {
            Expression t1 = toTemporal(begins.getExpression1());
            Expression t2 = toTemporal(begins.getExpression2());

            return super.visit(filterFactory.begins(t1, t2), extraData);
        }

        @Override
        public Object visit(BegunBy begunBy, Object extraData) {
            Expression t1 = toTemporal(begunBy.getExpression1());
            Expression t2 = toTemporal(begunBy.getExpression2());

            return super.visit(filterFactory.begunBy(t1, t2), extraData);
        }

        @Override
        public Object visit(During during, Object extraData) {
            Expression t1 = toTemporal(during.getExpression1());
            Expression t2 = toTemporal(during.getExpression2());

            return super.visit(filterFactory.during(t1, t2), extraData);
        }

        @Override
        public Object visit(EndedBy endedBy, Object extraData) {
            Expression t1 = toTemporal(endedBy.getExpression1());
            Expression t2 = toTemporal(endedBy.getExpression2());

            return super.visit(filterFactory.endedBy(t1, t2), extraData);
        }

        @Override
        public Object visit(Ends ends, Object extraData) {
            Expression t1 = toTemporal(ends.getExpression1());
            Expression t2 = toTemporal(ends.getExpression2());

            return super.visit(filterFactory.ends(t1, t2), extraData);
        }

        @Override
        public Object visit(Meets meets, Object extraData) {
            Expression t1 = toTemporal(meets.getExpression1());
            Expression t2 = toTemporal(meets.getExpression2());

            return super.visit(filterFactory.meets(t1, t2), extraData);
        }

        @Override
        public Object visit(MetBy metBy, Object extraData) {
            Expression t1 = toTemporal(metBy.getExpression1());
            Expression t2 = toTemporal(metBy.getExpression2());

            return super.visit(filterFactory.metBy(t1, t2), extraData);
        }

        @Override
        public Object visit(OverlappedBy overlappedBy, Object extraData) {
            Expression t1 = toTemporal(overlappedBy.getExpression1());
            Expression t2 = toTemporal(overlappedBy.getExpression2());

            return super.visit(filterFactory.overlappedBy(t1, t2), extraData);
        }

        @Override
        public Object visit(TContains contains, Object extraData) {
            Expression t1 = toTemporal(contains.getExpression1());
            Expression t2 = toTemporal(contains.getExpression2());

            return super.visit(filterFactory.tcontains(t1, t2), extraData);
        }

        @Override
        public Object visit(TOverlaps contains, Object extraData) {
            Expression t1 = toTemporal(contains.getExpression1());
            Expression t2 = toTemporal(contains.getExpression2());

            return super.visit(filterFactory.toverlaps(t1, t2), extraData);
        }

        private Optional<String> getPrefixedPropertyName(String property) {
            return featureType.getMappings()
                              .findMappings(TargetMapping.BASE_TYPE)
                              .entrySet()
                              .stream()
                              .filter(targetMappings -> targetMappings.getKey()
                                                                      .endsWith(":"+property))
                              .map(Map.Entry::getKey)
                              .findFirst()
                              ;//.map(namespaceNormalizer::getPrefixedPath);
        }

        protected Instant toInstant(Expression e) {
            return (Instant)e.evaluate(null, Instant.class);
        }

        protected Period toPeriod(Expression e) {
            return (Period)e.evaluate(null, Period.class);
        }

        protected Expression toTemporal(Expression e) {
            TemporalPrimitive p = this.toPeriod(e);
            if (p != null) {
                return filterFactory.literal(p);
            } else {
                TemporalPrimitive p2 = this.toInstant(e);
                return p2 != null ? filterFactory.literal(p2) : e;
            }
        }
    }
}
