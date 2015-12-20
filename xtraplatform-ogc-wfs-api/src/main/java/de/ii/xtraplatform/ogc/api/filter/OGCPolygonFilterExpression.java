package de.ii.xtraplatform.ogc.api.filter;

import de.ii.xsf.logging.XSFLogger;
import de.ii.xtraplatform.crs.api.Polygon;
import de.ii.xtraplatform.ogc.api.FES;
import de.ii.xtraplatform.ogc.api.GML;
import de.ii.xtraplatform.util.xml.XMLDocument;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Map;

/**
 *
 * @author fischer
 */
public class OGCPolygonFilterExpression extends OGCFilterExpression {

    private static final LocalizedLogger LOGGER = XSFLogger.getLogger(OGCFilterExpression.class);
    private Polygon poly;
    private String geometryPath;

    public OGCPolygonFilterExpression(Polygon env, String geometryPath) {
        this.geometryPath = geometryPath;
        this.poly = env;
    }

    @Override
    public void toXML(FES.VERSION version, Element e, XMLDocument doc) {

        Element intersects = doc.createElementNS(FES.getNS(version), FES.getPR(version), FES.getWord(version, FES.VOCABULARY.INTERSECTS));
        e.appendChild(intersects);

        Element valRef = doc.createElementNS(FES.getNS(version), FES.getPR(version), FES.getWord(version, FES.VOCABULARY.VALUE_REFERENCE));
        intersects.appendChild(valRef);
        valRef.setTextContent(geometryPath);

        //Element literal = doc.createElementNS(FES.getNS(version), FES.getPR(version), FES.getWord(version, FES.VOCABULARY.LITERAL));
        //intersects.appendChild(literal);

        GML.VERSION gmlversion = version.getGmlVersion();
        Element polygon = doc.createElementNS(GML.getNS(gmlversion), GML.getPR(gmlversion), GML.getWord(gmlversion, GML.VOCABULARY.POLYGON));
        intersects.appendChild(polygon);

        if (version.isGreaterOrEqual(FES.VERSION._2_0_0)) {
            polygon.setAttribute(GML.getWord(version.getGmlVersion(), GML.VOCABULARY.SRSNAME), poly.getSpatialReference().getAsUrn());
        } else {
            polygon.setAttribute(GML.getWord(version.getGmlVersion(), GML.VOCABULARY.SRSNAME), poly.getSpatialReference().getAsSimple());
        }
        polygon.setAttribute(GML.getWord(version.getGmlVersion(), GML.VOCABULARY.SRSDIMENSION), "2");

        polygon.setAttribute(GML.getWord(version.getGmlVersion(), GML.VOCABULARY.GMLID), "Id1");
        
        for (List<List<Double>> rings : poly.getRings()) {

            // TODO: interior ...
            Element exterior = doc.createElementNS(GML.getNS(gmlversion), GML.getPR(gmlversion), GML.getWord(gmlversion, GML.VOCABULARY.EXTERIOR));
            polygon.appendChild(exterior);

            Element linearring = doc.createElementNS(GML.getNS(gmlversion), GML.getPR(gmlversion), GML.getWord(gmlversion, GML.VOCABULARY.LINEAR_RING));
            exterior.appendChild(linearring);

            StringBuilder c = new StringBuilder();
            for (List<Double> ring : rings) {
                for (Double d : ring) {
                    c.append(d);
                    c.append(" ");
                }
            }

            Element poslist = doc.createElementNS(GML.getNS(gmlversion), GML.getPR(gmlversion), GML.getWord(gmlversion, GML.VOCABULARY.POS_LIST));
            linearring.appendChild(poslist);
            poslist.setTextContent(c.toString());
        }
    }

    public void toKVP(FES.VERSION version, Map<String, String> params) {

        /*
         String min = env.getXmin() + "," + env.getYmin();
         String max = env.getXmax() + "," + env.getYmax();
         String bbox = min+","+max;
        
         if( !env.getSpatialReference().getWkidString().equals("4326")) {
         bbox += ","+env.getSpatialReference().getEPSGCode();
         }
         params.put(FES.getWord(version, FES.VOCABULARY.BBOX).toUpperCase(), bbox);     
         */
    }
}
