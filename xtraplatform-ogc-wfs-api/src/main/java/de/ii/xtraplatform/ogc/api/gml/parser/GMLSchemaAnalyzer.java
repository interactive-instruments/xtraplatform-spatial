package de.ii.xtraplatform.ogc.api.gml.parser;

/**
 *
 * @author zahnen
 */
public interface GMLSchemaAnalyzer {
    public void analyzeFeatureType(String nsuri, String localName);
    public void analyzeAttribute(String nsuri, String localName, String type, boolean required);
    public void analyzeProperty(String nsuri, String localName, String type, long minOccurs, long maxOccurs, 
            int depth, boolean isParentMultible, boolean isComplex, boolean isObject);
}
