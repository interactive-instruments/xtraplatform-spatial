package de.ii.xtraplatform.feature.query.api;

/**
 * @author zahnen
 */
public enum SimpleFeatureGeometry {
    POINT,
    MULTI_POINT,
    LINE_STRING,
    MULTI_LINE_STRING,
    POLYGON,
    MULTI_POLYGON,
    GEOMETRY_COLLECTION,
    NONE;

    public boolean isValid() {
        return this != NONE;
    }
}
