package de.ii.xtraplatform.feature.source.wfs;

import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.feature.query.api.FeatureQuery;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.query.api.WfsProxyFeatureType;
import de.ii.xtraplatform.ogc.api.wfs.client.GetFeature;
import de.ii.xtraplatform.ogc.api.wfs.client.GetFeatureBuilder;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSQuery2;
import de.ii.xtraplatform.ogc.api.wfs.client.WFSQueryBuilder;
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

import java.util.Map;
import java.util.Optional;

import static de.ii.xtraplatform.util.functional.LambdaWithException.mayThrow;

/**
 * @author zahnen
 */
public class FeatureQueryEncoderWfs {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureQueryEncoderWfs.class);

    // TODO: resolve property and type names in query
    private final Map<String, WfsProxyFeatureType> featureTypes;
    private final XMLNamespaceNormalizer namespaceNormalizer;

    public FeatureQueryEncoderWfs(Map<String, WfsProxyFeatureType> featureTypes, XMLNamespaceNormalizer namespaceNormalizer) {
        this.featureTypes = featureTypes;
        this.namespaceNormalizer = namespaceNormalizer;
    }

    /*public String asXml(FeatureQuery query, XMLNamespaceNormalizer nsStore, Versions versions) throws CQLException, ParserConfigurationException, TransformerException, IOException, SAXException {
        final GetFeature getFeature = encode(query);
        final XMLDocumentFactory documentFactory = new XMLDocumentFactory(nsStore);
        final XMLDocument document = getFeature.asXml(documentFactory, versions);

        return document.toString(true);
    }

    public Map<String, String> asKvp(FeatureQuery query, XMLNamespaceNormalizer nsStore, Versions versions) throws CQLException, ParserConfigurationException, TransformerException, IOException, SAXException {
        final GetFeature getFeature = encode(query);
        final XMLDocumentFactory documentFactory = new XMLDocumentFactory(nsStore);

        return getFeature.asKvp(documentFactory, versions);
    }*/

    public Optional<GetFeature> encode(final FeatureQuery query) {
        return encode(query, false);
    }

    public Optional<GetFeature> encode(final FeatureQuery query, final boolean hitsOnly) {
        try {
            return featureTypes.values()
                               .stream()
                               .filter(ft -> ft.getName().equals(query.getType()))
                               .findFirst()
                               .map(mayThrow(ft -> encode(query, ft, hitsOnly)));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Filter is invalid", e.getCause());
        }
    }

    private GetFeature encode(FeatureQuery query, WfsProxyFeatureType featureType, final boolean hitsOnly) throws CQLException {
        final String featureTypeName = namespaceNormalizer.getQualifiedName(featureType.getNamespace(), featureType.getName());

        final WFSQuery2 wfsQuery = new WFSQueryBuilder().typeName(featureTypeName)
                                                        .crs(query.getCrs())
                                                        .filter(encodeFilter(query.getFilter(), featureType))
                                                        .build();
        final GetFeatureBuilder getFeature = new GetFeatureBuilder();

        getFeature.query(wfsQuery);

        if (query.getLimit() > 0) {
            getFeature.count(query.getLimit());
        }
        if (query.getOffset() > 0) {
            getFeature.startIndex(query.getOffset());
        }
        if (hitsOnly) {
            getFeature.hitsOnly();
        }

        return getFeature.build();
    }

    private Filter encodeFilter(final String filter, final WfsProxyFeatureType featureType) throws CQLException {
        return (Filter) ECQL.toFilter(filter)
                            .accept(new ResolvePropertyNamesFilterVisitor(featureType), null);
    }

    private class ResolvePropertyNamesFilterVisitor extends DuplicatingFilterVisitor {
        final FilterFactory2 filterFactory = new FilterFactoryImpl();
        final NamespaceSupport namespaceSupport;
        final WfsProxyFeatureType featureType;

        private ResolvePropertyNamesFilterVisitor(final WfsProxyFeatureType featureType) {
            namespaceSupport = new NamespaceSupport();
            namespaceNormalizer.getNamespaces()
                               .forEach(namespaceSupport::declarePrefix);
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
                              .map(namespaceNormalizer::getPrefixedPath);
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
