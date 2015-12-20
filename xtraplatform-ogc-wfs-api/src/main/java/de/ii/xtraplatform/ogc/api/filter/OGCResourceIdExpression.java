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
