/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.sql.app;


import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.transformer.api.SimpleFeatureGeometryFrom;

/**
 * @author zahnen
 */
public enum SimpleFeatureGeometryFromToWkt implements SimpleFeatureGeometryFrom {

    POINT,
    MULTIPOINT,
    LINESTRING,
    MULTILINESTRING,
    POLYGON,
    MULTIPOLYGON,
    GEOMETRYCOLLECTION,
    NONE;

    public static SimpleFeatureGeometryFromToWkt fromString(String type) {
        for (SimpleFeatureGeometryFromToWkt v : SimpleFeatureGeometryFromToWkt.values()) {
            if (v.toString().equals(type.toUpperCase())) {
                return v;
            }
        }
        return NONE;
    }

    public static SimpleFeatureGeometryFromToWkt fromSimpleFeatureGeometry(SimpleFeatureGeometry type) {
        switch (type) {
            case POINT:
                return POINT;
            case MULTI_POINT:
                return MULTIPOINT;
            case LINE_STRING:
                return LINESTRING;
            case MULTI_LINE_STRING:
                return MULTILINESTRING;
            case POLYGON:
                return POLYGON;
            case MULTI_POLYGON:
                return MULTIPOLYGON;
            case GEOMETRY_COLLECTION:
                return GEOMETRYCOLLECTION;
        }
        return NONE;
    }

    @Override
    public SimpleFeatureGeometry toSimpleFeatureGeometry() {
        SimpleFeatureGeometry simpleFeatureGeometry = SimpleFeatureGeometry.NONE;

        switch (this) {
            case POINT:
                simpleFeatureGeometry = SimpleFeatureGeometry.POINT;
                break;
            case MULTIPOINT:
                simpleFeatureGeometry = SimpleFeatureGeometry.MULTI_POINT;
                break;
            case LINESTRING:
                simpleFeatureGeometry = SimpleFeatureGeometry.LINE_STRING;
                break;
            case MULTILINESTRING:
                simpleFeatureGeometry = SimpleFeatureGeometry.MULTI_LINE_STRING;
                break;
            case POLYGON:
                simpleFeatureGeometry = SimpleFeatureGeometry.POLYGON;
                break;
            case MULTIPOLYGON:
                simpleFeatureGeometry = SimpleFeatureGeometry.MULTI_POLYGON;
                break;
            case GEOMETRYCOLLECTION:
                simpleFeatureGeometry = SimpleFeatureGeometry.GEOMETRY_COLLECTION;
                break;
        }

        return simpleFeatureGeometry;
    }

    @Override
    public boolean isValid() {
        return this != NONE;
    }
}
