/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.ogc.api.filter;

import de.ii.xtraplatform.ogc.api.FES;
import de.ii.xtraplatform.ogc.api.FES.VERSION;
import de.ii.xtraplatform.xml.domain.XMLDocument;
import org.w3c.dom.Element;

/**
 *
 * @author fischer
 */
public class OGCFilterLiteral extends OGCFilterExpression {

    protected String value;
    
    public OGCFilterLiteral(String value){
        this.value = value;
    }
    
    @Override
    public void toXML(VERSION version, Element e, XMLDocument doc) {
        doc.addNamespace(FES.getNS(version), FES.getPR(version));
        Element ex = doc.createElementNS(FES.getNS(version), FES.getWord(version, FES.VOCABULARY.LITERAL));
        ex.setTextContent(value);
        e.appendChild(ex);
    }
    
}
