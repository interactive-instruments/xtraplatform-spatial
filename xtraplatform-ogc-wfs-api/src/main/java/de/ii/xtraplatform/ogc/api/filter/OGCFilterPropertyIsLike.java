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
import de.ii.xtraplatform.ogc.api.FES.VERSION;
import de.ii.xtraplatform.util.xml.XMLDocument;
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
    
        Element ex = doc.createElementNS(FES.getNS(version), FES.getPR(version), FES.getWord(version, FES.VOCABULARY.LIKE));
        
        ex.setAttribute( FES.getWord(version, FES.VOCABULARY.WILD_CARD) , wildCard);
        ex.setAttribute( FES.getWord(version, FES.VOCABULARY.SINGLE_CHAR) , singleChar);
        ex.setAttribute( FES.getWord(version, FES.VOCABULARY.ESCAPE_CHAR) , escapeChar);
                       
        e.appendChild(ex);
        
        left.toXML(version, ex, doc);
        right.toXML(version, ex, doc);
    }
}
