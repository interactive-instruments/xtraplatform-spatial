/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.ogc.api.filter;


import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.ogc.api.FES;
import de.ii.xtraplatform.xml.domain.XMLDocument;

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
        doc.addNamespace(FES.getNS(version), FES.getPR(version));
        Element ex = doc.createElementNS(FES.getNS(version), FES.getWord(version, FES.VOCABULARY.RESOURCEID));
        if (version.isEqual(FES.VERSION._1_1_0)) {
            ex.setAttribute("gml:" + FES.getWord(version, FES.VOCABULARY.RESOURCEID_ATTR), id);
        } else {
            ex.setAttribute(FES.getWord(version, FES.VOCABULARY.RESOURCEID_ATTR), id);
        }
        e.appendChild(ex);
    }

    @Override
    public Map<String, String> toKVP(FES.VERSION version) {
        return ImmutableMap.of(FES.getWord(version, FES.VOCABULARY.RESOURCEID_KVP).toUpperCase(), id);
    }

    public void toKVP(FES.VERSION version, Map<String, String> params) {

        params.put(FES.getWord(version, FES.VOCABULARY.RESOURCEID_KVP).toUpperCase(), id);

    }
}
