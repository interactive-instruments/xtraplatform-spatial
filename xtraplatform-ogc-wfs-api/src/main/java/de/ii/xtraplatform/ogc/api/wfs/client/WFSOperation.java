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

import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.ogc.api.Versions;
import de.ii.xtraplatform.ogc.api.GML;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.util.xml.XMLDocument;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.Header;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.w3c.dom.Element;

/**
 *
 * @author fischer
 */
public abstract class WFSOperation {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(WFSOperation.class);
    private List<WFSQuery> query;
    private Integer count;
    private Integer startIndex;
    private XMLNamespaceNormalizer localNS;
    private boolean hits;
    private String valueReference;
    private boolean inialized;

    public WFSOperation() {
        this.query = new ArrayList<WFSQuery>();
    }

    protected abstract void initialize(XMLNamespaceNormalizer nsStore);

    public abstract WFS.OPERATION getOperation();

    protected abstract String getOperationName(WFS.VERSION version);
    
    public Map<String, String> getRequestHeaders(){
        return new HashMap();
    }
    
    public void setResponseHeaders(Header[] headers){
        // nothing to do
    }
    
    public void addQuery(WFSQuery query) {
        this.query.add(query);
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public void setValueReference(String valueReference) {
        this.valueReference = valueReference;
    }

    public void addNamespace(String prefix, String uri) {
        if (localNS == null) {
            localNS = new XMLNamespaceNormalizer();
        }

        localNS.addNamespace(prefix, uri);
    }

    public void setHits() {
        this.hits = true;
    }

    // returns the XML POST Request as String
    public String getPOSTXML(XMLNamespaceNormalizer nsStore, Versions vs) {
        if (!inialized) {
            this.initialize(nsStore);
            this.inialized = true;
        }
                
        XMLDocument doc = new XMLDocument(nsStore);
        
        Element oper = doc.createElementNS(WFS.getNS(vs.getWfsVersion()), WFS.getPR(vs.getWfsVersion()), getOperationName(vs.getWfsVersion()));
        doc.appendChild(oper);

        if (this.count != null) {
            oper.setAttribute(WFS.getWord(vs.getWfsVersion(), WFS.VOCABULARY.COUNT), String.valueOf(count));
        }
        
        if (this.startIndex != null && vs.getWfsVersion().isGreaterOrEqual(WFS.VERSION._2_0_0)) {
            oper.setAttribute(WFS.getWord(vs.getWfsVersion(), WFS.VOCABULARY.STARTINDEX), String.valueOf(startIndex));
        }
        
        if (vs.getGmlVersion() != null && vs.getWfsVersion() != null) {
            oper.setAttribute(GML.getWord(vs.getWfsVersion(), WFS.VOCABULARY.OUTPUT_FORMAT), GML.getWord(vs.getGmlVersion(), GML.VOCABULARY.OUTPUTFORMAT_VALUE));
            oper.setAttribute(WFS.getWord(vs.getWfsVersion(), WFS.VOCABULARY.VERSION), vs.getWfsVersion().toString());
        }

        if (getOperationName(vs.getWfsVersion()).equals(GML.getWord(vs.getWfsVersion(), WFS.VOCABULARY.GET_PROPERTY_VALUE))) {
            if (valueReference != null) {
                oper.setAttribute(WFS.getWord(vs.getWfsVersion(), WFS.VOCABULARY.VALUE_REFERENCE), valueReference);
            } else {
                oper.setAttribute(WFS.getWord(vs.getWfsVersion(), WFS.VOCABULARY.VALUE_REFERENCE), "@gml:id");
            }
        }

        for (WFSQuery q : query) {
            q.toXML(vs.getWfsVersion(), oper, doc);
        }

        if (localNS != null) {
            for (String uri : localNS.xgetNamespaceUris()) {
                oper.setAttribute("xmlns:" + uri + "", localNS.getNamespaceURI(uri));
            }
        }
       
        for (String uri : nsStore.xgetNamespaceUris()) {
            oper.setAttribute("xmlns:" + uri + "", nsStore.getNamespaceURI(uri));
        }
        
        if (this.hits) {
            oper.setAttribute(WFS.getWord(vs.getWfsVersion(), WFS.VOCABULARY.RESULT_TYPE), WFS.getWord(vs.getWfsVersion(), WFS.VOCABULARY.HITS));
        }

        oper.setAttribute("service", "WFS");

        String out = doc.toString(true);
        
        LOGGER.getLogger().debug(out);

        return out;
    }

    // Returns the GET parameters for a GET request as Map<String, String>
    public Map<String, String> getGETParameters(XMLNamespaceNormalizer nsStore, Versions vs) {
        if (!inialized) {
            this.initialize(nsStore);
            this.inialized = true;
        }

        Map<String, String> params = new HashMap<>();

        params.put("REQUEST", this.getOperation().toString());
        params.put("SERVICE", "WFS");

        if (vs != null) {
            params.put(GML.getWord(vs.getWfsVersion(), WFS.VOCABULARY.OUTPUT_FORMAT).toUpperCase(), GML.getWord(vs.getGmlVersion(), GML.VOCABULARY.OUTPUTFORMAT_VALUE));
            params.put(WFS.getWord(vs.getWfsVersion(), WFS.VOCABULARY.VERSION).toUpperCase(), vs.getWfsVersion().toString());
        }   
        if (this.count != null) {
            params.put(WFS.getWord(vs.getWfsVersion(), WFS.VOCABULARY.COUNT).toUpperCase(), String.valueOf(count));
        }

        if (this.startIndex != null && vs.getWfsVersion().isGreaterOrEqual(WFS.VERSION._2_0_0)) {
            params.put(WFS.getWord(vs.getWfsVersion(), WFS.VOCABULARY.STARTINDEX).toUpperCase(), String.valueOf(startIndex));
        }

        if (getOperationName(vs.getWfsVersion()).equals(GML.getWord(vs.getWfsVersion(), WFS.VOCABULARY.GET_PROPERTY_VALUE))) {
            if (valueReference != null) {
                params.put(WFS.getWord(vs.getWfsVersion(), WFS.VOCABULARY.VALUE_REFERENCE).toUpperCase(), valueReference);
            } else {
                params.put(WFS.getWord(vs.getWfsVersion(), WFS.VOCABULARY.VALUE_REFERENCE).toUpperCase(), "@gml:id");
            }
        }
        
        if (this.hits) {
            params.put(WFS.getWord(vs.getWfsVersion(), WFS.VOCABULARY.RESULT_TYPE).toUpperCase(), WFS.getWord(vs.getWfsVersion(), WFS.VOCABULARY.HITS));
        }

        if (localNS != null) {
            String namespaces = "";
            boolean first = true;
            for (String uri : localNS.xgetNamespaceUris()) {
                if (!first) {
                    namespaces += ",";
                    first = false;
                }
                namespaces += "xmlns(" + uri + "," + localNS.getNamespaceURI(uri) + ")";
            }
            params.put("NAMESPACES", namespaces);
        }

        for (WFSQuery q : query) {
            q.toKVP(vs.getWfsVersion(), params, nsStore);
        }

        return params;
    }
}
