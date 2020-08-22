/**
 * Copyright 2020 interactive instruments GmbH
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
public class OGCFilterPropertyIsLike extends OGCFilterExpression {

    private final OGCFilterLiteral left;
    private final OGCFilterLiteral right;
   
    private String wildCard;
    private String singleChar;
    private String escapeChar;
    
    public OGCFilterPropertyIsLike(OGCFilterLiteral left, OGCFilterLiteral right) {
        this.left = left;
        this.right = right;
        
        this.wildCard = "%";
        this.singleChar = "#";
        this.escapeChar = "\\";
    }

    public void setWildCard(String wildCard) {
        this.wildCard = wildCard;
    }

    public void setSingleChar(String singleChar) {
        this.singleChar = singleChar;
    }

    public void setEscapeChar(String escapeChar) {
        this.escapeChar = escapeChar;
    }
    
        
    @Override
    public void toXML(VERSION version, Element e, XMLDocument doc) {

        doc.addNamespace(FES.getNS(version), FES.getPR(version));
        Element ex = doc.createElementNS(FES.getNS(version), FES.getWord(version, FES.VOCABULARY.LIKE));
        
        ex.setAttribute( FES.getWord(version, FES.VOCABULARY.WILD_CARD) , wildCard);
        ex.setAttribute( FES.getWord(version, FES.VOCABULARY.SINGLE_CHAR) , singleChar);
        ex.setAttribute( FES.getWord(version, FES.VOCABULARY.ESCAPE_CHAR) , escapeChar);
                       
        e.appendChild(ex);
        
        left.toXML(version, ex, doc);
        right.toXML(version, ex, doc);
    }
}
