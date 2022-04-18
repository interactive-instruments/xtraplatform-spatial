package de.ii.xtraplatform.features.gml.domain;

import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;

public enum GmlGeometryType {

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

  GmlGeometryType(String stringRepresentation) {
    this.stringRepresentation = stringRepresentation;
  }

  GmlGeometryType(String stringRepresentation, String elementStringRepresentation) {
    this(stringRepresentation);
    this.elementStringRepresentation = elementStringRepresentation;
  }

  @Override
  public String toString() {
    return stringRepresentation;
  }

  public static GmlGeometryType fromString(String type) {
    for (GmlGeometryType v : GmlGeometryType.values()) {
      if (v.toString().equals(type) || (v.elementStringRepresentation != null
          && v.elementStringRepresentation.equals(type))) {
        return v;
      }
    }
    return NONE;
  }

  public static boolean contains(String type) {
    for (GmlGeometryType v : GmlGeometryType.values()) {
      if (v.toString().equals(type)) {
        return true;
      }
    }
    return false;
  }

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

  public boolean isValid() {
    return this != NONE;
  }
}
