package de.ii.xtraplatform.ogc.api.filter;

import de.ii.xtraplatform.ogc.api.FES;
import de.ii.xtraplatform.util.xml.XMLDocument;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import org.w3c.dom.Element;

/**
 *
 * @author fischer
 */
public abstract class OGCFilterExpression {
    
    protected XMLNamespaceNormalizer NSstore;

    public abstract void toXML(FES.VERSION version, Element e, XMLDocument doc);
}
