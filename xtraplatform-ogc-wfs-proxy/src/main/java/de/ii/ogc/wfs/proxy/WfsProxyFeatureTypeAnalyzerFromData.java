/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogc.wfs.proxy;

import de.ii.xtraplatform.feature.transformer.api.GmlFeatureTypeAnalyzer;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformerService;
import de.ii.xtraplatform.ogc.api.exceptions.GMLAnalyzeFailed;
import de.ii.xtraplatform.ogc.api.gml.parser.GMLAnalyzer;
import org.codehaus.staxmate.in.SMInputCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.concurrent.Future;

/**
 *
 * @author zahnen
 */
public class WfsProxyFeatureTypeAnalyzerFromData extends GmlFeatureTypeAnalyzer implements GMLAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WfsProxyFeatureTypeAnalyzerFromData.class);

    public WfsProxyFeatureTypeAnalyzerFromData(FeatureTransformerService proxyService, List<TargetMappingProviderFromGml> mappingProviders) {
        super(proxyService, mappingProviders);
    }


    @Override
    public final void analyzeFailed(Exception ex) {
        LOGGER.error("AbstractFeatureWriter -> analyzeFailed", ex);
        throw new GMLAnalyzeFailed("AbstractFeatureWriter -> analyzeFailed");
    }

    @Override
    public boolean analyzeNamespaceRewrite(String oldNamespace, String newNamespace, String featureTypeName) {
        return super.analyzeNamespaceRewrite(oldNamespace, newNamespace, featureTypeName);
    }

    @Override
    public final void analyzeStart(Future<SMInputCursor> rootFuture) {

    }

    @Override
    public void analyzeFeatureStart(String id, String nsUri, String localName) {
        super.analyzeFeatureType(nsUri, localName);
    }

    @Override
    public final void analyzeAttribute(String nsUri, String localName, String value) {
        super.analyzeAttribute(nsUri, localName, "");
    }

    @Override
    public final void analyzePropertyStart(String nsUri, String localName, int depth, SMInputCursor feature, boolean nil) {
        // TODO: nsUri is GML
        GML_GEOMETRY_TYPE geoType = GML_GEOMETRY_TYPE.fromString(localName);

        if (geoType.isValid()) {
            super.analyzeProperty(nsUri, localName, localName, -1, (depth % 2 == 1));
        } else {
            super.analyzeProperty(nsUri, localName, "", depth, (depth % 2 == 1));
        }
    }

    @Override
    public void analyzePropertyText(String nsUri, String localName, int depth, String text) {
        GML_TYPE type = getTypeFromValue(text);

        super.analyzeProperty(nsUri, localName, type.toString(), depth, (depth % 2 == 1));
    }

    @Override
    public void analyzePropertyEnd(String nsUri, String localName, int depth) {
    }

    @Override
    public final void analyzeFeatureEnd() {
    }

    @Override
    public final void analyzeEnd() {
    }

    private GML_TYPE getTypeFromValue(String value) {

        GML_TYPE type = GML_TYPE.STRING;

        try {
            Long.parseLong(value);
            type = GML_TYPE.LONG;
        } catch (NumberFormatException e) {
            try {
                Double.parseDouble(value);
                type = GML_TYPE.DOUBLE;
            } catch (NumberFormatException e2) {
                try {
                    DateTimeFormatter parser = DateTimeFormatter.ofPattern("yyyy-MM-dd['T'HH:mm:ss][X]");
                    TemporalAccessor ta = parser.parseBest(value, OffsetDateTime::from, LocalDateTime::from, LocalDate::from);
                    type = GML_TYPE.DATE_TIME;
                } catch (Exception e3) {
                    if (value.toLowerCase().equals("true") || value.toLowerCase().equals("false")) {
                        type = GML_TYPE.BOOLEAN;
                    }
                }
            }
        }

        return  type;
    }
}
