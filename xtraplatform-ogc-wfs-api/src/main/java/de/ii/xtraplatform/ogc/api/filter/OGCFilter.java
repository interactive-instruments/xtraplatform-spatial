/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
