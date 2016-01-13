/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
