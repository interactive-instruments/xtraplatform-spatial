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

import de.ii.xtraplatform.ogc.api.FES;
import de.ii.xtraplatform.util.xml.XMLDocument;

import java.util.Map;
import org.w3c.dom.Element;

/**
 *
 * @author fischer
 */
public class OGCResourceIdExpression extends OGCFilterExpression {

    private String id;

    public OGCResourceIdExpression(String id) {
        this.id = id;
    }

    @Override
    public void toXML(FES.VERSION version, Element e, XMLDocument doc) {
        Element ex = doc.createElementNS(FES.getNS(version), FES.getPR(version), FES.getWord(version, FES.VOCABULARY.RESOURCEID));
        if (version.isEqual(FES.VERSION._1_1_0)) {
            ex.setAttribute("gml:" + FES.getWord(version, FES.VOCABULARY.RESOURCEID_ATTR), id);
        } else {
            ex.setAttribute(FES.getWord(version, FES.VOCABULARY.RESOURCEID_ATTR), id);
        }
        e.appendChild(ex);
    }

    public void toKVP(FES.VERSION version, Map<String, String> params) {

        params.put(FES.getWord(version, FES.VOCABULARY.RESOURCEID_KVP).toUpperCase(), id);

    }
}
