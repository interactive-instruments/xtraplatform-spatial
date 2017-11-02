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
package de.ii.xtraplatform.ogc.api.wfs.client;

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
    private List<String> typeNames = null;
    private EpsgCrs srs = null;

    public WFSQuery() {
        this.filter = new ArrayList<>();
    }

    public void addFilter(OGCFilter filter) {
        this.filter.add(filter);
    }
        
    public void addTypename(String typename) {
        if (typeNames == null) {
            typeNames = new ArrayList<String>();
        }

        typeNames.add(typename);
    }
    
    public String getTypeNames() {
        
        String tns = "";
            for (int i = 0; i < typeNames.size(); i++) {
                if (i != 0) {
                    tns += ",";
                }
                tns += typeNames.get(i);
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

        if (typeNames != null) {
            query.setAttribute(WFS.getWord(version, WFS.VOCABULARY.TYPENAMES), getTypeNames());
        }

        if (this.srs != null) {
           query.setAttribute(WFS.getWord(version, WFS.VOCABULARY.SRSNAME), getSrs(version));
        }

    }
    
    public void toKVP(WFS.VERSION version, Map<String, String> params, XMLNamespaceNormalizer nsStore){
        
        if (typeNames != null) {
            params.put(WFS.getWord(version, WFS.VOCABULARY.TYPENAMES).toUpperCase(), getTypeNames());
        }

        if (this.srs != null) {
           params.put(WFS.getWord(version, WFS.VOCABULARY.SRSNAME).toUpperCase(), getSrs(version));
        }

        for (OGCFilter fil : filter) {
            fil.toKVP(version.getFilterVersion(), params, nsStore);
        } 
    }
}
