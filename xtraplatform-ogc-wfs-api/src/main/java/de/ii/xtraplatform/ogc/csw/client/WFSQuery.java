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
package de.ii.xtraplatform.ogc.csw.client;

import de.ii.xtraplatform.crs.api.EpsgCrs;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.filter.OGCFilter;
import de.ii.xtraplatform.util.xml.XMLDocument;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author fischer
 */
public class WFSQuery {

    private List<OGCFilter> filter = null;
    private List<String> typenames = null;
    private EpsgCrs srs = null;
    private boolean hits;

    public WFSQuery() {
        filter = new ArrayList();
        this.hits = true;
    }

    public void addFilter(OGCFilter filter) {
        this.filter.add(filter);
    }
        
    public void addTypename(String typename) {
        if (typenames == null) {
            typenames = new ArrayList<String>();
        }

        typenames.add(typename);
    }
    
    public String getTypenames() {
        
        String tns = "";
            for (int i = 0; i < typenames.size(); i++) {
                if (i != 0) {
                    tns += ",";
                }
                tns += typenames.get(i);
            }
        
        return tns;
    }

    public void setSrs(EpsgCrs srs) {
        this.srs = srs;
    }
    
    public String getSrs(WFS.VERSION version) {
        if (this.srs != null) {
            /*if (version.isGreaterOrEqual(WFS.VERSION._2_0_0)) {
                return srs.getOgcEPSGUrn();
            } else {*/
                return srs.getAsSimple();
            //}
        }
        return null;
    }

    public void toXML(WFS.VERSION version, Element e, XMLDocument doc) {
        Element query = doc.createElementNS(WFS.getNS(version), WFS.getPR(version), WFS.getWord(version, WFS.VOCABULARY.QUERY));
        e.appendChild(query);

        for (OGCFilter fil : filter) {
            fil.toXML(version.getFilterVersion(), query, doc);
        }

        if (typenames != null) {
            query.setAttribute(WFS.getWord(version, WFS.VOCABULARY.TYPENAMES), getTypenames());
        }

        if (this.srs != null) {
           query.setAttribute(WFS.getWord(version, WFS.VOCABULARY.SRSNAME), getSrs(version));
        }

    }
    
    public void toKVP(WFS.VERSION version, Map<String, String> params, XMLNamespaceNormalizer nsStore){
        
        if (typenames != null) {
            params.put(WFS.getWord(version, WFS.VOCABULARY.TYPENAMES).toUpperCase(), getTypenames());
        }
        
        if (this.srs != null) {
           params.put(WFS.getWord(version, WFS.VOCABULARY.SRSNAME).toUpperCase(), getSrs(version));
        }

        for (OGCFilter fil : filter) {
            fil.toKVP(version.getFilterVersion(), params, nsStore);
        } 
    }
}
