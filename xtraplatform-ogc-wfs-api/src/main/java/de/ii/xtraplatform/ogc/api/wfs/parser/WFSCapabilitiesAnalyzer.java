package de.ii.xtraplatform.ogc.api.wfs.parser;

import de.ii.xtraplatform.ogc.api.WFS;
import javax.xml.stream.XMLStreamReader;

/**
 *
 * @author zahnen
 */
public interface WFSCapabilitiesAnalyzer {
    public void analyzeNamespaces(XMLStreamReader xml);
    public void analyzeTitle(String title);
    public void analyzeCopyright(String copyright);
    public void analyzeVersion(String version);
    public void analyzeFeatureType(String name);
    public void analyzeBoundingBox(String bblower, String bbupper);
    public void analyzeBoundingBox(String xmin, String ymin, String xmax, String ymax);
    public void analyzeDefaultSRS(String name);
    public void analyzeOtherSRS(String name);
    public void analyzeDCPPOST( WFS.OPERATION op, String url);
    public void analyzeDCPGET( WFS.OPERATION op, String url);
    public void analyzeGMLOutputFormat(String outputformat);
}
