package de.ii.xtraplatform.ogc.api;

import java.util.Arrays;

/**
 *
 * @author fischer
 */
public class WFS extends VersionedVocabulary {

    public enum VERSION {

        _1_0_0("1.0.0", FES.VERSION._1_0_0, GML.VERSION._2_1_1),
        _1_1_0("1.1.0", FES.VERSION._1_1_0, GML.VERSION._3_1_1),
        _2_0_0("2.0.0", FES.VERSION._2_0_0, GML.VERSION._3_2_1);
        private final String stringRepresentation;
        private final FES.VERSION filterVersion;
        private final GML.VERSION gmlVersion;

        private VERSION(String stringRepresentation, FES.VERSION filterVersion, GML.VERSION gmlVersion) {
            this.stringRepresentation = stringRepresentation;
            this.filterVersion = filterVersion;
            this.gmlVersion = gmlVersion;
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }

        public FES.VERSION getFilterVersion() {
            return filterVersion;
        }

        public GML.VERSION getGmlVersion() {
            return gmlVersion;
        }

        public static VERSION fromString(String version) {
            for (VERSION v : VERSION.values()) {
                if (v.toString().equals(version)) {
                    return v;
                }
            }
            return null;
        }

        public boolean isGreaterOrEqual(VERSION other) {
            return this.compareTo(other) >= 0;
        }

        public boolean isGreater(VERSION other) {
            return this.compareTo(other) > 0;
        }
    }

    public static boolean hasKVPKey(String key) {

        String[] keys = {"REQUEST", "SERVICE", "VERSION", "NAMESPACES", "TYPENAMES", "COUNT", "NAMESPACE", "TYPENAME", "MAXFEATURES", "OUTPUTFORMAT", "VALUEREFERENCE"};

        return Arrays.asList(keys).contains(key);
    }

    public enum OPERATION {

        GET_CAPABILITES("GetCapabilities"),
        DESCRIBE_FEATURE_TYPE("DescribeFeatureType"),
        GET_FEATURE("GetFeature"),
        GET_PROPERTY_VALUE("GetPropertyValue");
        private final String stringRepresentation;

        private OPERATION(String stringRepresentation) {
            this.stringRepresentation = stringRepresentation;
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }

        public static OPERATION fromString(String type) {
            for (OPERATION v : OPERATION.values()) {
                if (v.toString().equals(type)) {
                    return v;
                }
            }
            return null;
        }
    }

    public enum METHOD {

        GET("GET"),
        POST("POST");
        private final String stringRepresentation;

        private METHOD(String stringRepresentation) {
            this.stringRepresentation = stringRepresentation;
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }

        public static METHOD fromString(String type) {
            for (METHOD v : METHOD.values()) {
                if (v.toString().equals(type)) {
                    return v;
                }
            }
            return null;
        }
    }

    public enum VOCABULARY {
        QUERY, TYPENAMES, SRSNAME, VERSION, COUNT, STARTINDEX, GET_FEATURE, GET_CAPABILITES, 
        DESCRIBE_FEATURE_TYPE, OUTPUTFORMAT, GET_PROPERTY_VALUE, VALUE_REFERENCE;
    }

    static {
        addWord(VERSION._1_0_0, NAMESPACE.PREFIX, "wfs");
        addWord(VERSION._1_0_0, NAMESPACE.URI, "http://www.opengis.net/wfs");

        addWord(VERSION._1_1_0, NAMESPACE.PREFIX, "wfs");
        addWord(VERSION._1_1_0, NAMESPACE.URI, "http://www.opengis.net/wfs");

        addWord(VERSION._2_0_0, NAMESPACE.PREFIX, "wfs");
        addWord(VERSION._2_0_0, NAMESPACE.URI, "http://www.opengis.net/wfs/2.0");

        addWord(VERSION._1_0_0, VOCABULARY.GET_FEATURE, "GetFeature");
        addWord(VERSION._1_0_0, VOCABULARY.GET_CAPABILITES, "GetCapabilities");
        addWord(VERSION._1_0_0, VOCABULARY.DESCRIBE_FEATURE_TYPE, "DescribeFeatureType");
        addWord(VERSION._1_0_0, VOCABULARY.GET_PROPERTY_VALUE, "GetPropertyValue");
        addWord(VERSION._1_0_0, VOCABULARY.VALUE_REFERENCE, "valueReference");
        addWord(VERSION._1_0_0, VOCABULARY.QUERY, "Query");
        addWord(VERSION._1_0_0, VOCABULARY.TYPENAMES, "typeName");
        addWord(VERSION._1_0_0, VOCABULARY.SRSNAME, "srsName");
        addWord(VERSION._1_0_0, VOCABULARY.VERSION, "version");
        addWord(VERSION._1_0_0, VOCABULARY.COUNT, "maxFeatures");
        addWord(VERSION._1_0_0, VOCABULARY.OUTPUTFORMAT, "outputFormat");

        addWord(VERSION._1_1_0, VOCABULARY.QUERY, "Query");
        addWord(VERSION._1_1_0, VOCABULARY.TYPENAMES, "typeName");
        addWord(VERSION._1_1_0, VOCABULARY.SRSNAME, "srsName");
        addWord(VERSION._1_1_0, VOCABULARY.VERSION, "version");
        addWord(VERSION._1_1_0, VOCABULARY.COUNT, "maxFeatures");

        addWord(VERSION._2_0_0, VOCABULARY.QUERY, "Query");
        addWord(VERSION._2_0_0, VOCABULARY.TYPENAMES, "typeNames");
        addWord(VERSION._2_0_0, VOCABULARY.SRSNAME, "srsName");
        addWord(VERSION._2_0_0, VOCABULARY.VERSION, "version");
        addWord(VERSION._2_0_0, VOCABULARY.COUNT, "count");
        addWord(VERSION._2_0_0, VOCABULARY.STARTINDEX, "startIndex");
    }
}
