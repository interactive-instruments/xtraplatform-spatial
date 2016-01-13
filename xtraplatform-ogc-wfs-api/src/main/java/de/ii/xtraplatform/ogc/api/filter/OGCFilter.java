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
import de.ii.xtraplatform.ogc.api.FES;
import de.ii.xtraplatform.util.xml.XMLDocument;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.w3c.dom.Element;

/**
 *
 * @author fischer
 */
public class OGCFilter {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(OGCFilter.class);
    private final List<OGCFilterExpression> expressions;

    public OGCFilter() {
        expressions = new ArrayList();
    }

    public void addExpression(OGCFilterExpression expression) {
        this.expressions.add(expression);
    }

    public void toXML(FES.VERSION version, Element e, XMLDocument doc) {

        if (expressions.isEmpty()) {
            return;
        }

        Element ex = doc.createElementNS(FES.getNS(version), FES.getPR(version), FES.getWord(version, FES.VOCABULARY.FILTER));

        for (OGCFilterExpression expr : expressions) {
            expr.toXML(version, ex, doc);
        }

        if (ex.getChildNodes().getLength() > 0) {
            e.appendChild(ex);
        }

    }

    public void toKVP(FES.VERSION version, Map<String, String> params, XMLNamespaceNormalizer nsStore) {
        if (expressions.isEmpty()) {
            return;
        }

        // check if the first lexel expression is BBOX
        try {
            OGCFilterAnd and = (OGCFilterAnd) expressions.get(0);
            if (and != null && !and.operands.isEmpty()) {
                OGCBBOXFilterExpression bbox = (OGCBBOXFilterExpression) and.operands.get(0);
                if (bbox != null) {

                    bbox.toKVP(version, params);
                    return;
                }
            }
        } catch (ClassCastException ex) {
        }

        // check if the first lexel expression is ResourceId
        try {
            OGCResourceIdExpression resid = (OGCResourceIdExpression) expressions.get(0);
            if (resid != null) {

                resid.toKVP(version, params);
                return;
            }
        } catch (ClassCastException ex) {
        }

        if (expressions.get(0) instanceof OGCFilterExpression) {

            XMLDocument doc = new XMLDocument(nsStore);
            Element e = doc.createElementNS(FES.getNS(version), FES.getPR(version), FES.getWord(version, FES.VOCABULARY.FILTER));
            doc.appendChild(e);

            // is not null because we won't be here in that case (null instanceof SomeClass == false)
            expressions.get(0).toXML(version, e, doc);
            
            if (e.hasChildNodes()) {

                // attach the namespace(s) for the PropertyName value
                for (String uri : nsStore.xgetNamespaceUris()) {
                    e.setAttribute("xmlns:" + uri + "", nsStore.getNamespaceURI(uri));
                }

                String filter = doc.toString(false);
                filter = filter.replace("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>", "");
                filter = "(" + filter + ")";
                
                params.put("FILTER", filter);
            }
        }
    }
}
