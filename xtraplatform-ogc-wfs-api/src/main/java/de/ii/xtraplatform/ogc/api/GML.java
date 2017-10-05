/**
 * Copyright 2017 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.ogc.api;

/**
 *
 * @author zahnen
 */
public class GML extends VersionedVocabulary {

    public enum VERSION {

        // IMPORTANT! keep the order of the items!
        _2_1_1("2.1.1"),
        _3_1_1("3.1.1"),     
        _3_2_1_OLD("3.2.1.OLD"),
        _3_2_1("3.2.1");
        
        private final String stringRepresentation;

        private VERSION(String stringRepresentation) {
            this.stringRepresentation = stringRepresentation;
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }

        public static VERSION fromString(String version) {
            for (VERSION v : VERSION.values()) {
                if (v.toString().equals(version)) {
                    return v;
                }
            }
            return null;
        }

        public static VERSION fromOutputFormatString(String value) {

            value = value.toLowerCase();
            
            if (value.contains("gml2")) {
                return VERSION._2_1_1;
            } else if (value.contains("text/xml") && value.contains("subtype=gml/3.1.1")) {
                return VERSION._3_1_1;
            } else if (value.contains("text/xml") && value.contains("subtype=gml/3.2.1")) {
                return VERSION._3_2_1_OLD;
            } else if (value.contains("application/gml+xml") && value.contains("version=3.2")) {
                return VERSION._3_2_1;
            }

            return null;
        }

        public boolean isGreaterOrEqual(GML.VERSION other) {
            return this.compareTo(other) >= 0;
        }

        public boolean isGreater(GML.VERSION other) {
            return this.compareTo(other) > 0;
        }
    }

    public enum VOCABULARY {

        ENVELOPE, SRSNAME, LOWER_CORNER, UPPER_CORNER, ABSTRACT_OBJECT, 
        OUTPUTFORMAT_VALUE, POLYGON, EXTERIOR, LINEAR_RING, POS_LIST, SRSDIMENSION,
        GMLID;
    }

    static {
        // IMPORTANT! make the order of the VERSIONS like in enum VERSION above!
        
        // Values in lower versions are automatically present in higher versions
        // if not overwritten.
        
        addWord(VERSION._2_1_1, NAMESPACE.PREFIX, "gml");
        addWord(VERSION._2_1_1, NAMESPACE.URI, "http://www.opengis.net/gml");

        addWord(VERSION._3_1_1, NAMESPACE.PREFIX, "gml");
        addWord(VERSION._3_1_1, NAMESPACE.URI, "http://www.opengis.net/gml");

        addWord(VERSION._3_2_1, NAMESPACE.PREFIX, "gml");
        addWord(VERSION._3_2_1, NAMESPACE.URI, "http://www.opengis.net/gml/3.2");

        addWord(VERSION._2_1_1, VOCABULARY.ABSTRACT_OBJECT, "_Object");
        addWord(VERSION._2_1_1, VOCABULARY.ENVELOPE, "Envelope");
        addWord(VERSION._2_1_1, VOCABULARY.SRSNAME, "srsName");
        addWord(VERSION._2_1_1, VOCABULARY.LOWER_CORNER, "lowerCorner");
        addWord(VERSION._2_1_1, VOCABULARY.UPPER_CORNER, "upperCorner");
        addWord(VERSION._2_1_1, VOCABULARY.OUTPUTFORMAT_VALUE, "GML2");//text/xml; subtype=gml/2.1.1");
        addWord(VERSION._2_1_1, VOCABULARY.POLYGON, "Polygon");
        addWord(VERSION._2_1_1, VOCABULARY.EXTERIOR, "exterior");
        addWord(VERSION._2_1_1, VOCABULARY.LINEAR_RING, "LinearRing");
        addWord(VERSION._2_1_1, VOCABULARY.POS_LIST, "posList");
        addWord(VERSION._2_1_1, VOCABULARY.SRSDIMENSION, "srsDimension");
        addWord(VERSION._2_1_1, VOCABULARY.GMLID, "gml:id");                        
        
        addWord(VERSION._3_1_1, VOCABULARY.OUTPUTFORMAT_VALUE, "text/xml; subtype=gml/3.1.1");
        
        addWord(VERSION._3_2_1_OLD, VOCABULARY.ABSTRACT_OBJECT, "AbstractObject");
        addWord(VERSION._3_2_1_OLD, VOCABULARY.OUTPUTFORMAT_VALUE, "text/xml; subtype=gml/3.2.1");
        
        addWord(VERSION._3_2_1, VOCABULARY.OUTPUTFORMAT_VALUE, "application/gml+xml; version=3.2");
    }
}
