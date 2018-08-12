/**
 * Copyright 2017 European Union, interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.ogc.wfs.proxy;

import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfigurationOld;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformerService;
import de.ii.xtraplatform.ogc.api.GML;
import de.ii.xtraplatform.ogc.api.OWS;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.exceptions.ParseError;
import de.ii.xtraplatform.ogc.api.wfs.parser.AbstractWfsCapabilitiesAnalyzer;
import de.ii.xtraplatform.ogc.api.wfs.parser.WFSCapabilitiesAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author zahnen
 */
public class WfsProxyCapabilitiesAnalyzer extends AbstractWfsCapabilitiesAnalyzer implements WFSCapabilitiesAnalyzer  {

    private static final Logger LOGGER = LoggerFactory.getLogger(WfsProxyCapabilitiesAnalyzer.class);
    private FeatureTransformerService wfsProxy;
    private Map<WFS.OPERATION, GML.VERSION> versions;
    private  String url;

    public WfsProxyCapabilitiesAnalyzer(FeatureTransformerService wfsProxy, String url) {
        this.wfsProxy = wfsProxy;
        this.versions = new HashMap<>();
        this.url = url;
    }

    @Override
    public void analyzeVersion(String version) {
        wfsProxy.getWfsAdapter().setVersion(version);
    }

    @Override
    public void analyzeTitle(String title) {
        wfsProxy.setName(title);
    }

    @Override
    public void analyzeAbstract(String abstrct) {
        wfsProxy.setDescription(abstrct);
    }

    @Override
    public void analyzeNamespace(String prefix, String uri) {
        wfsProxy.getWfsAdapter().addNamespace(prefix, uri);
    }

    @Override
    public void analyzeFeatureType(String featureTypeName) {

        if (!featureTypeName.equals("gml:AbstractFeature")) {
            String uri = wfsProxy.getWfsAdapter().getNsStore().getNamespaceURI(wfsProxy.getWfsAdapter().getNsStore().extractPrefix(featureTypeName));
            String localName = wfsProxy.getWfsAdapter().getNsStore().getLocalName(featureTypeName);

            LOGGER.debug("WFS FeatureType '{}'", featureTypeName);

            String displayName = wfsProxy.getWfsAdapter().getNsStore().getShortNamespacePrefix(uri); //getNamespacePrefix(uri);
            if (displayName.length() > 0) {
                displayName += ":" + localName;
            } else {
                displayName = localName;
            }

            String fullName = uri + ":" + localName;

            wfsProxy.getFeatureTypes().put(fullName, new FeatureTypeConfigurationOld(localName, uri, displayName));
        }
    }

    @Override
    public void analyzeFeatureTypeBoundingBox(String featureTypeName, String xmin, String ymin, String xmax, String ymax) {

        double dxmin = Double.parseDouble(xmin);
        double dymin = Double.parseDouble(ymin);
        double dxmax = Double.parseDouble(xmax);
        double dymax = Double.parseDouble(ymax);

        analyzeBoundingBox(dxmin, dymin, dxmax, dymax);
    }

    public void analyzeBoundingBox(Double xmin, Double ymin, Double xmax, Double ymax) {

        /*Envelope envelope;

        SpatialReference srIn = new SpatialReference(); // 4326 by default
        SpatialReference srOut = new SpatialReference(3857);

        Double defaultXmin = -24132518.00;
        Double defaultYmin = -19783705.00;
        Double defaultXmax = 15864227.00;
        Double defaultYmax = 20682670.00;

        // TODO: we have to fix this in Transformations ....
        if (xmin <= -180 || ymin <= -90 || xmax >= 180 || ymax >= 90) {
            envelope = new Envelope(defaultXmin, defaultYmin, defaultXmax, defaultYmax, srOut);
        } else {

            if (gsfs.getSrsTransformations().isAvailable()) {
                try {
                    envelope = gsfs.getSrsTransformations().transformOutput(new Envelope(xmin, ymin, xmax, ymax, srIn), srOut);
                } catch (CrsTransformationException ex) {
                    LOGGER.warn(FrameworkMessages.TRANSFORMATION_OF_LATLONBOUNDINGBOX_FAILED_USING_WORLD_EXTENT, xmin, ymin, xmax, ymax);
                    envelope = new Envelope(defaultXmin, defaultYmin, defaultXmax, defaultYmax, srOut);
                }
            } else {
                envelope = new Envelope(xmin, ymin, xmax, ymax, srIn);
            }
        }

        if (layer != null) {
            layer.setExtent(envelope);

            //LOGGER.debug("WFS FeatureType.BoundingBox: {} {} {} {}", envelope.getXmin(), envelope.getXmax(), envelope.getYmin(), envelope.getYmax());
        }*/
    }

    @Override
    public void analyzeFeatureTypeDefaultCrs(String featureTypeName, String crs) {

        //LOGGER.info("analyzing default SRS: {}", name);
        // TODO: workaround
        if (crs.equals("urn:ogc:def:crs:OGC:1.3:CRS84")) {
            crs = "EPSG:4326";
        }

        EpsgCrs sr = new EpsgCrs(crs);
        //if (!gsfs.getSrsTransformations().isAvailable() || gsfs.getSrsTransformations().isSrsSupported(sr)) {
            wfsProxy.getWfsAdapter().setDefaultCrs(sr);
        /*} else {
            LOGGER.warn(FrameworkMessages.THE_SRS_NAME_IS_NOT_SUPPORTED_BY_THIS_SERVICE, name);
            //throw new RessourceNotFound("The SRS '"+name+"' is not supported by this service!");
        }*/
    }

    @Override
    public void analyzeFeatureTypeOtherCrs(String featureTypeName, String crs) {
        EpsgCrs sr = new EpsgCrs(crs);
        //if (!gsfs.getSrsTransformations().isAvailable() || gsfs.getSrsTransformations().isSrsSupported(sr)) {
            wfsProxy.getWfsAdapter().addOtherCrs(sr);
        //}
    }

    @Override
    public void analyzeOperationPostUrl(OWS.OPERATION op, String url) {
        WFS.OPERATION op2 = WFS.OPERATION.fromString(op.toString());
        try {
            wfsProxy.getWfsAdapter().addUrl(new URI(url.trim()), op2, WFS.METHOD.POST);
        } catch (URISyntaxException ex) {
            // TODO
        }
    }

    @Override
    public void analyzeOperationGetUrl(OWS.OPERATION op, String url) {
        WFS.OPERATION op2 = WFS.OPERATION.fromString(op.toString());
        try {
            wfsProxy.getWfsAdapter().addUrl(new URI(url.trim()), op2, WFS.METHOD.GET);
        } catch (URISyntaxException ex) {
            // TODO
        }
    }

    @Override
    public void analyzeOperationParameter(OWS.OPERATION operation, OWS.VOCABULARY parameterName, String value) {
        WFS.OPERATION op2 = WFS.OPERATION.fromString(operation.toString());
        if (parameterName == OWS.VOCABULARY.OUTPUT_FORMAT) {
            LOGGER.debug("CAP VERSION {} {}", op2, value);
            GML.VERSION v1 = versions.get(op2);
            GML.VERSION v2 = GML.VERSION.fromOutputFormatString(value);
            if (v2 != null) {
                if (v1 != null) {
                   if (v2.isGreater(v1)) {
                   versions.put(op2, v2);
                   }
                }
                else {
                   versions.put(op2, v2);
                }
            }
            GML.VERSION v3 = versions.get(WFS.OPERATION.DESCRIBE_FEATURE_TYPE);
            GML.VERSION v4 = versions.get(WFS.OPERATION.GET_FEATURE);
            GML.VERSION v5 = (v3 == null ? v4 : (v4 == null ? v3 : (v3.isGreater(v4) ? v4 : v3)));
            LOGGER.debug("CAP VERSION {}", v5);
            if (v5 != null) {
               wfsProxy.getWfsAdapter().setGmlVersion(v5.toString());
            }
            //wfsProxy.getWfsAdapter().setGmlVersionFromOutputFormat(value);
        }
    }

    @Override
    public void analyzeFailed(String exceptionCode, String exceptionText) {
        ParseError pe = new ParseError("GetCapabilities request returned ExceptionReport: " + url);
        pe.addDetail("ExceptionCode: " + exceptionCode);
        pe.addDetail("ExceptionText: " + exceptionText);
        throw pe;
    }
}
