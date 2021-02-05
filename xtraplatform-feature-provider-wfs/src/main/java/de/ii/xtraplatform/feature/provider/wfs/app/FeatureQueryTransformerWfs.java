/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.app;

import akka.japi.Pair;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.cql.domain.CqlToText;
import de.ii.xtraplatform.cql.domain.CqlFilter;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.feature.provider.wfs.FeatureProviderDataWfsFromMetadata;
import de.ii.xtraplatform.feature.provider.wfs.domain.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureQueryTransformer;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureStoreTypeInfo;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.features.domain.ImmutableFeatureType;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.wfs.FilterEncoder;
import de.ii.xtraplatform.ogc.api.wfs.GetFeature;
import de.ii.xtraplatform.ogc.api.wfs.GetFeatureBuilder;
import de.ii.xtraplatform.ogc.api.wfs.WfsQuery;
import de.ii.xtraplatform.ogc.api.wfs.WfsQueryBuilder;
import de.ii.xtraplatform.ogc.api.wfs.WfsRequestEncoder;
import de.ii.xtraplatform.xml.domain.XMLNamespaceNormalizer;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.spatial.BBOXImpl;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
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
import org.w3c.dom.Element;
import org.xml.sax.helpers.NamespaceSupport;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static de.ii.xtraplatform.cql.domain.In.ID_PLACEHOLDER;

public class FeatureQueryTransformerWfs implements FeatureQueryTransformer<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureQueryTransformerWfs.class);

    static {
        LOGGER.debug("warming up GeoTools Filter ...");

        try {
            Filter filter = (Filter) ECQL.toFilter("'foo' = 'bar' AND BBOX(geometry, 366703.806, 5807220.953, 367087.571, 5807603.808, 'EPSG:25833')")
                                         .accept(new FeatureQueryTransformerWfs.ResolvePropertyNamesFilterVisitor(new ImmutableFeatureType.Builder().name("test").build(), new XMLNamespaceNormalizer()), null);

            Element encode = new FilterEncoder(WFS.VERSION._2_0_0).encode(filter);
            LOGGER.trace("{}", encode);
        } catch (Throwable ex) {
            //ignore
        }

        LOGGER.debug("done");
    }

    private final Map<String, FeatureStoreTypeInfo> typeInfos;
    private final Map<String, FeatureType> featureTypes;
    private final Map<String, FeatureSchema> featureSchemas;
    private final XMLNamespaceNormalizer namespaceNormalizer;
    private final WfsRequestEncoder wfsRequestEncoder;
    private final EpsgCrs nativeCrs;

    public FeatureQueryTransformerWfs(Map<String, FeatureStoreTypeInfo> typeInfos, Map<String, FeatureType> types,
                                      Map<String, FeatureSchema> featureSchemas, ConnectionInfoWfsHttp connectionInfo,
                                      EpsgCrs nativeCrs) {
        this.typeInfos = typeInfos;
        this.featureTypes = types;
        this.featureSchemas = featureSchemas;
        this.namespaceNormalizer = new XMLNamespaceNormalizer(connectionInfo.getNamespaces());

        Map<String, Map<WFS.METHOD, URI>> urls = ImmutableMap.of("default", ImmutableMap.of(WFS.METHOD.GET, FeatureProviderDataWfsFromMetadata.parseAndCleanWfsUrl(connectionInfo.getUri()), WFS.METHOD.POST, FeatureProviderDataWfsFromMetadata.parseAndCleanWfsUrl(connectionInfo.getUri())));

        this.wfsRequestEncoder = new WfsRequestEncoder(connectionInfo.getVersion(), connectionInfo.getGmlVersion(), connectionInfo.getNamespaces(), urls);
        this.nativeCrs = nativeCrs;
    }

    //TODO
    @Override
    public String transformQuery(FeatureQuery featureQuery, Map<String, String> additionalQueryParameters) {

        return encodeFeatureQuery(featureQuery, additionalQueryParameters);
    }

    public boolean isValid(final FeatureQuery query) {
        return typeInfos.containsKey(query.getType());
    }

    public QName getFeatureTypeName(final FeatureQuery query) {
        FeatureSchema featureSchema = featureSchemas.get(query.getType());
        String name = featureSchema.getSourcePath().map(sourcePath -> sourcePath.substring(1)).orElse(null);

        return new QName(namespaceNormalizer.getNamespaceURI(namespaceNormalizer.extractURI(name)), namespaceNormalizer.getLocalName(name));
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

            return encode(query, featureTypeName, featureTypes.get(query.getType()), additionalQueryParameters);
        } catch (CQLException e) {
            throw new IllegalArgumentException("Filter is invalid", e.getCause());
        }
    }

    public WfsRequestEncoder getWfsRequestEncoder() {
        return wfsRequestEncoder;
    }

    private GetFeature encode(FeatureQuery query, QName featureTypeName, FeatureType featureType, Map<String, String> additionalQueryParameters) throws CQLException {
        final String featureTypeNameFull = namespaceNormalizer.getQualifiedName(featureTypeName.getNamespaceURI(), featureTypeName.getLocalPart());

        final WfsQuery wfsQuery = new WfsQueryBuilder().typeName(featureTypeNameFull)
                                                       .crs(nativeCrs)
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
        if (query.hitsOnly()) {
            getFeature.hitsOnly();
        }

        if (!additionalQueryParameters.isEmpty()) {
            getFeature.additionalOperationParameters(additionalQueryParameters);
        }

        return getFeature.build();
    }

    private Filter encodeFilter(final Optional<CqlFilter> filter, final FeatureType featureType) throws CQLException {
        if (!filter.isPresent() || Objects.isNull(featureType)) {
            return null;
        }

        return (Filter) ECQL.toFilter(filter.get().accept(new CqlToText()))
                            .accept(new FeatureQueryTransformerWfs.ResolvePropertyNamesFilterVisitor(featureType, namespaceNormalizer), null);
    }

    private static class ResolvePropertyNamesFilterVisitor extends DuplicatingFilterVisitor {
        final FilterFactory2 filterFactory = new FilterFactoryImpl();
        final NamespaceSupport namespaceSupport;
        final FeatureType featureType;
        final XMLNamespaceNormalizer namespaceNormalizer;

        private ResolvePropertyNamesFilterVisitor(final FeatureType featureType, final XMLNamespaceNormalizer namespaceNormalizer) {
            namespaceSupport = new NamespaceSupport();
            namespaceNormalizer.getNamespaces()
                               .forEach(namespaceSupport::declarePrefix);
            this.featureType = featureType;
            this.namespaceNormalizer = namespaceNormalizer;
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

        //TODO: test if still works
        @Override
        public Object visit(BBOX filter, Object extraData) {
            LOGGER.debug("BBOX {} | {} | {}", filter.getExpression1(), filter.getSRS(), extraData);

            Optional<String> property = getPrefixedPropertyName(filter.getExpression1()
                                                                      .toString());

            if (!property.isPresent() && filter.getExpression1()
                                               .toString()
                                               .equals(FeatureQueryTransformer.PROPERTY_NOT_AVAILABLE)) {
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
        public Object visit(Function expression, Object extraData) {
            if (expression.getName().toUpperCase().equals("STRTOLOWERCASE") && !expression.getParameters().isEmpty() && expression.getParameters().get(0) instanceof PropertyName) {
                Optional<String> prefixedPropertyName = getPrefixedPropertyName(((PropertyName) expression.getParameters()
                                                                                                          .get(0)).getPropertyName());
                if (prefixedPropertyName.isPresent()) {
                    return filterFactory.property(prefixedPropertyName.get(), namespaceSupport);
                }
            }

            return super.visit(expression, extraData);
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
            return featureType.getProperties()
                    .values()
                    .stream()
                              //TODO: why lower case???
                    .filter(featureProperty -> Objects.nonNull(featureProperty.getName()) && Objects.equals(featureProperty.getName()
                                                                                                                        .toLowerCase(), property.replaceAll(ID_PLACEHOLDER, "id").toLowerCase()))
                    .map(FeatureProperty::getPath)
                              .map(path -> path.substring(path.indexOf("/", 1)+1))
                    .findFirst()
                    //.map(namespaceNormalizer::getPrefixedPath)
                    .map(prefixedPath -> {
                        if (prefixedPath.contains("@")) {
                            return "@" + prefixedPath.replaceAll("@", "");
                        }
                        return prefixedPath;
                    });
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
