package de.ii.ogc.wfs.proxy;

import de.ii.xtraplatform.util.xml.XMLPathTracker;

/**
 * @author zahnen
 */
public interface WfsProxyOnTheFlyMapping {
    TargetMapping getTargetMappingForFeatureType(XMLPathTracker path, String nsuri, String localName);

    TargetMapping getTargetMappingForAttribute(XMLPathTracker path, String nsuri, String localName, String value);

    TargetMapping getTargetMappingForProperty(XMLPathTracker path, String nsuri, String localName, String value);

    TargetMapping getTargetMappingForGeometry(XMLPathTracker path, String nsuri, String localName);
}
