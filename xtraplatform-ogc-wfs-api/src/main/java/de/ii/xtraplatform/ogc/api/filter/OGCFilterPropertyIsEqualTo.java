package de.ii.xtraplatform.ogc.api.filter;

import de.ii.xtraplatform.ogc.api.FES;
import de.ii.xtraplatform.util.xml.XMLDocument;
import org.w3c.dom.Element;

/**
 *
 * @author fischer
 */
public class OGCFilterPropertyIsEqualTo extends OGCFilterExpression {

    private final OGCFilterLiteral left;
    private final OGCFilterLiteral right;

    
    public OGCFilterPropertyIsEqualTo(OGCFilterLiteral left, OGCFilterLiteral right) {
        this.left = left;
        this.right = right;
    }   
        
    @Override
    public void toXML(FES.VERSION version, Element e, XMLDocument doc) {
        Element ex = doc.createElementNS(FES.getNS(version), FES.getPR(version), FES.getWord(version, FES.VOCABULARY.EQUAL));     
        e.appendChild(ex);
        left.toXML(version, ex, doc);
        right.toXML(version, ex, doc);
    }
}

