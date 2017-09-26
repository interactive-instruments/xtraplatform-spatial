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

import com.google.common.base.Joiner;
import de.ii.xtraplatform.ogc.api.CSW;
import de.ii.xtraplatform.ogc.api.filter.OGCFilter;
import de.ii.xtraplatform.util.xml.XMLDocument;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import org.w3c.dom.Element;

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
        Element query = doc.createElementNS(CSW.getNS(version), CSW.getPR(version), CSW.getWord(version, CSW.VOCABULARY.QUERY));
        Element constraint = doc.createElementNS(CSW.getNS(version), CSW.getPR(version), CSW.getWord(version, CSW.VOCABULARY.CONSTRAINT));
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

    Map<String, String> toKVP(Map<String, String> parameters, XMLNamespaceNormalizer nsStore, CSW.VERSION version){
        
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
