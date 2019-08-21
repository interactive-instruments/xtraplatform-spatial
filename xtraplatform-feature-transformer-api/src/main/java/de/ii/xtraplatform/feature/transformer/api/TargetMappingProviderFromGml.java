/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import de.ii.xtraplatform.feature.provider.api.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;

/**
 * @author zahnen
 */
public interface TargetMappingProviderFromGml {
    public enum GML_TYPE {
        ID("ID"),
        STRING("string"),
        DATE_TIME("dateTime"),
        DATE("date"),
        GEOMETRY("geometry"),
        DECIMAL("decimal"),
        DOUBLE("double"),
        FLOAT("float"),
        INT("int"),
        INTEGER("integer"),
        LONG("long"),
        SHORT("short"),
        BOOLEAN("boolean"),
        URI("anyURI"),
        NONE("");

        private String stringRepresentation;

        GML_TYPE(String stringRepresentation) {
            this.stringRepresentation = stringRepresentation;
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }

        public static GML_TYPE fromString(String type) {
            for (GML_TYPE v : GML_TYPE.values()) {
                if (v.toString().equals(type)) {
                    return v;
                }
            }

            return NONE;
        }

        public static boolean contains(String type) {
            for (GML_TYPE v : GML_TYPE.values()) {
                if (v.toString().equals(type)) {
                    return true;
                }
            }
            return false;
        }

        public boolean isValid() {
            return this != NONE;
        }
    }

    public enum GML_GEOMETRY_TYPE implements SimpleFeatureGeometryFrom {

        GEOMETRY("GeometryPropertyType"),
        ABSTRACT_GEOMETRY("GeometricPrimitivePropertyType"),
        POINT("PointPropertyType", "Point"),
        MULTI_POINT("MultiPointPropertyType", "MultiPoint"),
        LINE_STRING("LineStringPropertyType", "LineString"),
        MULTI_LINESTRING("MultiLineStringPropertyType", "MultiLineString"),
        CURVE("CurvePropertyType", "Curve"),
        MULTI_CURVE("MultiCurvePropertyType", "MultiCurve"),
        SURFACE("SurfacePropertyType", "Surface"),
        MULTI_SURFACE("MultiSurfacePropertyType", "MultiSurface"),
        POLYGON("PolygonPropertyType", "Polygon"),
        MULTI_POLYGON("MultiPolygonPropertyType", "MultiPolygon"),
        SOLID("SolidPropertyType"),
        NONE("");

        private String stringRepresentation;
        private String elementStringRepresentation;

        GML_GEOMETRY_TYPE(String stringRepresentation) {
            this.stringRepresentation = stringRepresentation;
        }
        GML_GEOMETRY_TYPE(String stringRepresentation, String elementStringRepresentation) {
            this(stringRepresentation);
            this.elementStringRepresentation = elementStringRepresentation;
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }

        public static GML_GEOMETRY_TYPE fromString(String type) {
            for (GML_GEOMETRY_TYPE v : GML_GEOMETRY_TYPE.values()) {
                if (v.toString().equals(type) || (v.elementStringRepresentation != null && v.elementStringRepresentation.equals(type))) {
                    return v;
                }
            }
            return NONE;
        }

        public static boolean contains(String type) {
            for (GML_GEOMETRY_TYPE v : GML_GEOMETRY_TYPE.values()) {
                if (v.toString().equals(type)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public SimpleFeatureGeometry toSimpleFeatureGeometry() {
            SimpleFeatureGeometry simpleFeatureGeometry = SimpleFeatureGeometry.NONE;

            switch (this) {

                case GEOMETRY:
                case ABSTRACT_GEOMETRY:
                    simpleFeatureGeometry = SimpleFeatureGeometry.ANY;
                    break;
                case POINT:
                    simpleFeatureGeometry = SimpleFeatureGeometry.POINT;
                    break;
                case MULTI_POINT:
                    simpleFeatureGeometry = SimpleFeatureGeometry.MULTI_POINT;
                    break;
                case LINE_STRING:
                    simpleFeatureGeometry = SimpleFeatureGeometry.LINE_STRING;
                    break;
                case MULTI_LINESTRING:
                    simpleFeatureGeometry = SimpleFeatureGeometry.MULTI_LINE_STRING;
                    break;
                case CURVE:
                    simpleFeatureGeometry = SimpleFeatureGeometry.LINE_STRING;
                    break;
                case MULTI_CURVE:
                    simpleFeatureGeometry = SimpleFeatureGeometry.MULTI_LINE_STRING;
                    break;
                case SURFACE:
                    simpleFeatureGeometry = SimpleFeatureGeometry.POLYGON;
                    break;
                case MULTI_SURFACE:
                    simpleFeatureGeometry = SimpleFeatureGeometry.MULTI_POLYGON;
                    break;
                case POLYGON:
                    simpleFeatureGeometry = SimpleFeatureGeometry.POLYGON;
                    break;
                case MULTI_POLYGON:
                    simpleFeatureGeometry = SimpleFeatureGeometry.MULTI_POLYGON;
                    break;
                case SOLID:
                    break;
            }

            return simpleFeatureGeometry;
        }

        @Override
        public boolean isValid() {
            return this != NONE;
        }
    }

    String getTargetType();

    TargetMapping getTargetMappingForFeatureType(String nsUri, String localName);

    TargetMapping getTargetMappingForAttribute(String path, String nsUri, String localName, GML_TYPE type);

    TargetMapping getTargetMappingForProperty(String path, String nsUri, String localName, GML_TYPE type,
                                              boolean isMultiple);

    TargetMapping getTargetMappingForGeometry(String path, String nsUri, String localName, GML_GEOMETRY_TYPE type);
}
