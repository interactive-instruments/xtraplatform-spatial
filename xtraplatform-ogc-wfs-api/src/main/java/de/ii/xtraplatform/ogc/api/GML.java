/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
