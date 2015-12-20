/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
public class OGCFilterLiteral extends OGCFilterExpression {

    protected String value;
    
    public OGCFilterLiteral(String value){
        this.value = value;
    }
    
    @Override
    public void toXML(VERSION version, Element e, XMLDocument doc) {       
        Element ex = doc.createElementNS(FES.getNS(version), FES.getPR(version), FES.getWord(version, FES.VOCABULARY.LITERAL));
        ex.setTextContent(value);
        e.appendChild(ex);
    }
    
}
