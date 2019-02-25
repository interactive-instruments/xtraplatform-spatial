/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs;

import akka.japi.Pair;
import de.ii.xtraplatform.feature.provider.api.FeatureQuery;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;
import de.ii.xtraplatform.ogc.api.wfs.GetFeature;
import de.ii.xtraplatform.ogc.api.wfs.GetFeatureBuilder;
import de.ii.xtraplatform.ogc.api.wfs.WfsQuery;
import de.ii.xtraplatform.ogc.api.wfs.WfsQueryBuilder;
import de.ii.xtraplatform.ogc.api.wfs.WfsRequestEncoder;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.spatial.BBOXImpl;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
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

import javax.xml.namespace.QName;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author zahnen
 */
public class FeatureQueryEncoderWfs {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureQueryEncoderWfs.class);

    private final Map<String, QName> featureTypes;
    private final Map<String, FeatureTypeMapping> featureTypeMappings;
    private final XMLNamespaceNormalizer namespaceNormalizer;
    private final WfsRequestEncoder wfsRequestEncoder;

    FeatureQueryEncoderWfs(Map<String, QName> featureTypes, Map<String, FeatureTypeMapping> featureTypeMappings, XMLNamespaceNormalizer namespaceNormalizer, WfsRequestEncoder wfsRequestEncoder) {
        this.featureTypes = featureTypes;
        this.featureTypeMappings = featureTypeMappings;
        this.namespaceNormalizer = namespaceNormalizer;
        this.wfsRequestEncoder = wfsRequestEncoder;
    }

    public boolean isValid(final FeatureQuery query) {
        return featureTypes.containsKey(query.getType());
    }

    public QName getFeatureTypeName(final FeatureQuery query) {
        return featureTypes.get(query.getType());
    }

    public String encodeFeatureQuery(FeatureQuery query, Map<String, String> additionalQueryParameters) {
        return wfsRequestEncoder.getAsUrl(encode(query, additionalQueryParameters));
    }

    public Pair<String, String> encodeFeatureQueryPost(FeatureQuery query, Map<String, String> additionalQueryParameters) {
        return wfsRequestEncoder.getAsUrlAndBody(encode(query, additionalQueryParameters));
    }

    GetFeature encode(final FeatureQuery query, Map<String, String> additionalQueryParameters) {
        try {
            final QName featureTypeName = getFeatureTypeName(query);

            return encode(query, featureTypeName, featureTypeMappings.get(query.getType()), additionalQueryParameters);
        } catch (CQLException e) {
            throw new IllegalArgumentException("Filter is invalid", e.getCause());
        }
    }

    public WfsRequestEncoder getWfsRequestEncoder() {
        return wfsRequestEncoder;
    }

    private GetFeature encode(FeatureQuery query, QName featureType, FeatureTypeMapping featureTypeMapping, Map<String, String> additionalQueryParameters) throws CQLException {
        final String featureTypeName = namespaceNormalizer.getQualifiedName(featureType.getNamespaceURI(), featureType.getLocalPart());

        final WfsQuery wfsQuery = new WfsQueryBuilder().typeName(featureTypeName)
                                                       //TODO .crs(query.getCrs())
                                                       .filter(encodeFilter(query.getFilter(), featureTypeMapping))
                                                       .build();
        final GetFeatureBuilder getFeature = new GetFeatureBuilder();

        getFeature.query(wfsQuery);

        if (query.getLimit() > 0) {
            getFeature.count(query.getLimit());
        }
        if (query.getOffset() > 0) {
            getFeature.startIndex(query.getOffset());
        }
        if (query.hitsOnly()) {
            getFeature.hitsOnly();
        }

        if (!additionalQueryParameters.isEmpty()) {
            getFeature.additionalOperationParameters(additionalQueryParameters);
        }

        return getFeature.build();
    }

    private Filter encodeFilter(final String filter, final FeatureTypeMapping featureTypeMapping) throws CQLException {
        if (Objects.isNull(filter) || Objects.isNull(featureTypeMapping)) {
            return null;
        }

        return (Filter) ECQL.toFilter(filter)
                            .accept(new ResolvePropertyNamesFilterVisitor(featureTypeMapping), null);
    }

    private class ResolvePropertyNamesFilterVisitor extends DuplicatingFilterVisitor {
        final FilterFactory2 filterFactory = new FilterFactoryImpl();
        final NamespaceSupport namespaceSupport;
        final FeatureTypeMapping featureTypeMapping;

        private ResolvePropertyNamesFilterVisitor(final FeatureTypeMapping featureTypeMapping) {
            namespaceSupport = new NamespaceSupport();
            namespaceNormalizer.getNamespaces()
                               .forEach(namespaceSupport::declarePrefix);
            this.featureTypeMapping = featureTypeMapping;
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

            Optional<String> property = getPrefixedPropertyName(filter.getExpression1()
                                                                      .toString());

            if (!property.isPresent() && filter.getExpression1()
                                               .toString()
                                               .equals("NOT_AVAILABLE")) {
                if (filter.getSRS() != null) {
                    return new BBOXImpl(null, filter.getBounds()
                                                    .getMinX(), filter.getBounds()
                                                                      .getMinY(), filter.getBounds()
                                                                                        .getMaxX(), filter.getBounds()
                                                                                                          .getMaxY(), filter.getSRS());
                }
                return filterFactory.bbox(null, filter.getBounds());
            }

            if (property.isPresent()) {
                LOGGER.debug("PROP {}", property.get());
                if (filter.getSRS() != null) {
                    return new BBOXImpl(filterFactory.property(property.get(), namespaceSupport), filter.getBounds()
                                                                                                        .getMinX(), filter.getBounds()
                                                                                                                          .getMinY(), filter.getBounds()
                                                                                                                                            .getMaxX(), filter.getBounds()
                                                                                                                                                              .getMaxY(), filter.getSRS());
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
            return featureTypeMapping
                    .findMappings(TargetMapping.BASE_TYPE)
                    .entrySet()
                    .stream()
                    .filter(targetMappings -> Objects.nonNull(targetMappings.getValue()
                                                                            .getName()) && Objects.equals(targetMappings.getValue()
                                                                                                                        .getName()
                                                                                                                        .toLowerCase(), property))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .map(namespaceNormalizer::getPrefixedPath);
        }

        protected Instant toInstant(Expression e) {
            return (Instant) e.evaluate(null, Instant.class);
        }

        protected Period toPeriod(Expression e) {
            return (Period) e.evaluate(null, Period.class);
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
