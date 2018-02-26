/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.ogc.api.filter;

import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.crs.api.BoundingBox;
import de.ii.xtraplatform.ogc.api.FES;
import de.ii.xtraplatform.ogc.api.GML;
import de.ii.xtraplatform.util.xml.XMLDocument;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.w3c.dom.Element;

import java.util.Map;

/**
 *
 * @author fischer
 */
public class OGCBBOXFilterExpression extends OGCFilterExpression {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(OGCFilterExpression.class);
    private BoundingBox env;
    private String geometryPath;

    public OGCBBOXFilterExpression(BoundingBox env, String geometryPath) {
        this.geometryPath = geometryPath;
        this.env = env;
    }

    @Override
    public void toXML(FES.VERSION version, Element e, XMLDocument doc) {

        //LOGGER.debug("BBOX {} {}", FES.getNS(version), FES.getQN(version, FES.VOCABULARY.BBOX));      
        
        Element bbox = doc.createElementNS(FES.getNS(version), FES.getPR(version), FES.getWord(version, FES.VOCABULARY.BBOX));
        e.appendChild(bbox);

        Element valRef = doc.createElementNS(FES.getNS(version), FES.getPR(version), FES.getWord(version, FES.VOCABULARY.VALUE_REFERENCE));
        bbox.appendChild(valRef);
        valRef.setTextContent(geometryPath);

        String min = env.getXmin() + " " + env.getYmin();
        String max = env.getXmax() + " " + env.getYmax();

        // only swap if WFS 1.0.0 ...
        if (!version.isGreaterOrEqual(FES.VERSION._2_0_0)) {
            min = env.getYmin() + " " + env.getXmin();
            max = env.getYmax() + " " + env.getXmax();
        }

        Element envelope = doc.createElementNS(GML.getNS(version.getGmlVersion()), GML.getPR(version), GML.getWord(version.getGmlVersion(), GML.VOCABULARY.ENVELOPE));

        if (version.isGreaterOrEqual(FES.VERSION._2_0_0)) {
            envelope.setAttribute(GML.getWord(version.getGmlVersion(), GML.VOCABULARY.SRSNAME), env.getEpsgCrs().getAsUrn());
        } else {
            envelope.setAttribute(GML.getWord(version.getGmlVersion(), GML.VOCABULARY.SRSNAME), env.getEpsgCrs().getAsSimple());
        }

        bbox.appendChild(envelope);

        Element lower = doc.createElementNS(GML.getNS(version.getGmlVersion()), GML.getPR(version), GML.getWord(version.getGmlVersion(), GML.VOCABULARY.LOWER_CORNER));
        lower.setTextContent(min);
        envelope.appendChild(lower);

        Element upper = doc.createElementNS(GML.getNS(version.getGmlVersion()), GML.getPR(version), GML.getWord(version.getGmlVersion(), GML.VOCABULARY.UPPER_CORNER));
        upper.setTextContent(max);
        envelope.appendChild(upper);
    }
    
    public void toKVP(FES.VERSION version, Map<String, String> params) {
              
        String min = env.getXmin() + "," + env.getYmin();
        String max = env.getXmax() + "," + env.getYmax();
        String bbox = min+","+max;
        
        if( env.getEpsgCrs().getCode() != 4326) {
            bbox += ","+env.getEpsgCrs().getAsSimple();
        }
        params.put(FES.getWord(version, FES.VOCABULARY.BBOX).toUpperCase(), bbox);     
    }
}
