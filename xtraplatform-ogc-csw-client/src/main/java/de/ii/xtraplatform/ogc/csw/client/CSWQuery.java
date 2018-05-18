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
package de.ii.xtraplatform.ogc.csw.client;

import com.google.common.base.Joiner;
import de.ii.xtraplatform.ogc.api.CSW;
import de.ii.xtraplatform.ogc.api.filter.OGCFilter;
import de.ii.xtraplatform.util.xml.XMLDocument;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author zahnen
 */
public class CSWQuery {

    private List<OGCFilter> filter = null;
    private List<String> typeNames = null;

    public CSWQuery() {
        this.filter = new ArrayList<>();
        this.typeNames = new ArrayList<>();
    }

    public void addFilter(OGCFilter filter) {
        this.filter.add(filter);
    }
        
    public void addTypename(String typeName) {
        this.typeNames.add(typeName);
    }
    
    private String getTypeNames() {
        return Joiner.on(",").join(typeNames);
    }

    Element toXML(XMLDocument doc, CSW.VERSION version) {
        Element query = doc.createElementNS(CSW.getNS(version), CSW.getWord(version, CSW.VOCABULARY.QUERY));
        Element constraint = doc.createElementNS(CSW.getNS(version), CSW.getWord(version, CSW.VOCABULARY.CONSTRAINT));
        constraint.setAttribute(CSW.getWord(version, CSW.VOCABULARY.VERSION), version.getFilterVersion().toString());
        query.appendChild(constraint);

        for (OGCFilter fil : filter) {
            fil.toXML(version.getFilterVersion(), constraint, doc);
        }

        if (typeNames != null) {
            query.setAttribute(CSW.getWord(version, CSW.VOCABULARY.TYPENAMES), getTypeNames());
        }
         return query;
    }

    Map<String, String> toKVP(Map<String, String> parameters, XMLNamespaceNormalizer nsStore, CSW.VERSION version) throws ParserConfigurationException {
        
        if (!typeNames.isEmpty()) {
            parameters.put(CSW.getWord(version, CSW.VOCABULARY.TYPENAMES).toUpperCase(), getTypeNames());
        }

        for (OGCFilter fil : filter) {
            fil.toKVP(version.getFilterVersion(), parameters, nsStore);

            // only 1 in KVP ???
            break;
        }

        if (parameters.containsKey("FILTER")) {
            parameters.put("CONSTRAINT", parameters.get("FILTER").substring(1, parameters.get("FILTER").length()-1));
            parameters.remove("FILTER");
        }

        return parameters;
    }
}
