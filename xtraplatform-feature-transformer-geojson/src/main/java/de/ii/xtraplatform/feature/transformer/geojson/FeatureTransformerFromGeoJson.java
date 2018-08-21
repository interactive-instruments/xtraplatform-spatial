/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.geojson;

import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.feature.query.api.FeatureConsumer;
import de.ii.xtraplatform.feature.query.api.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTransformer;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeMapping;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import static de.ii.xtraplatform.util.functional.LambdaWithException.consumerMayThrow;

/**
 * @author zahnen
 */
public class FeatureTransformerFromGeoJson implements FeatureConsumer {

    //TODO: from GeoJsonGeometryMapping
    public enum GEO_JSON_GEOMETRY_TYPE {

        POINT("Point", SimpleFeatureGeometry.POINT),
        MULTI_POINT("MultiPoint", SimpleFeatureGeometry.MULTI_POINT),
        LINE_STRING("LineString", SimpleFeatureGeometry.LINE_STRING),
        MULTI_LINE_STRING("MultiLineString", SimpleFeatureGeometry.MULTI_LINE_STRING),
        POLYGON("Polygon", SimpleFeatureGeometry.POLYGON),
        MULTI_POLYGON("MultiPolygon", SimpleFeatureGeometry.MULTI_POLYGON),
        GEOMETRY_COLLECTION("GeometryCollection", SimpleFeatureGeometry.NONE),
        GENERIC("", SimpleFeatureGeometry.NONE),
        NONE("", SimpleFeatureGeometry.NONE);

        private String stringRepresentation;
        private SimpleFeatureGeometry sfType;

        GEO_JSON_GEOMETRY_TYPE(String stringRepresentation, SimpleFeatureGeometry sfType) {
            this.stringRepresentation = stringRepresentation;
            this.sfType = sfType;
        }

        @Override
        public String toString() {
            return stringRepresentation;
        }

        public SimpleFeatureGeometry toSimpleFeatureGeometry() {
            return sfType;
        }

        public static GEO_JSON_GEOMETRY_TYPE forString(String type) {
            for (GEO_JSON_GEOMETRY_TYPE geoJsonType : GEO_JSON_GEOMETRY_TYPE.values()) {
                if (geoJsonType.toString()
                               .equals(type)) {
                    return geoJsonType;
                }
            }

            return NONE;
        }

        public boolean isValid() {
            return this != NONE;
        }
    }

    private final FeatureTypeMapping featureTypeMapping;
    protected final FeatureTransformer featureTransformer;
    private final String outputFormat;

    private boolean inProperty = false;
    private TargetMapping geometryMapping;
    private List<String> currentPath;
    private int nesting = 0;
    private StringBuilder stringBuilder = new StringBuilder();

    public FeatureTransformerFromGeoJson(FeatureTypeMapping featureTypeMapping, FeatureTransformer featureTransformer) {
        this.featureTypeMapping = featureTypeMapping;
        this.featureTransformer = featureTransformer;
        this.outputFormat = featureTransformer.getTargetFormat();
    }


    @Override
    public void onStart(OptionalLong numberReturned, OptionalLong numberMatched) throws Exception {
        featureTransformer.onStart(numberReturned, numberMatched);
    }

    @Override
    public void onEnd() throws Exception {
        featureTransformer.onEnd();
    }

    @Override
    public void onFeatureStart(List<String> path) throws Exception {
        final TargetMapping mapping = featureTypeMapping.findMappings(path, outputFormat)
                                                        .orElse(null);
        featureTransformer.onFeatureStart(mapping);
    }

    @Override
    public void onFeatureEnd(List<String> path) throws Exception {
        featureTransformer.onFeatureEnd();
        if (geometryMapping != null) {
            onGeometryEnd();
        }
    }

    @Override
    public void onPropertyStart(List<String> path, List<Integer> multiplicities) throws Exception {
        if (path.size() >= 2 && path.get(0)
                                    .equals("geometry")) {
            if (geometryMapping == null) {
                Optional<TargetMapping> mapping = featureTypeMapping.findMappings(ImmutableList.of("geometry"), outputFormat);
                if (mapping.isPresent()) {
                    geometryMapping = mapping.get();
                }
            }
            currentPath = path;

            return;
        }

        if (geometryMapping != null && (path.size() < 1 || !path.get(0)
                                                                .equals("geometry"))) {
            onGeometryEnd();
        }

        featureTypeMapping.findMappings(path, outputFormat)
                          .ifPresent(consumerMayThrow(mapping -> {
                              inProperty = true;
                              if (mapping.isSpatial()) {
                                  //geometryMapping = mapping;
                              } else {
                                  featureTransformer.onPropertyStart(mapping, multiplicities);
                              }
                          }));
    }

    private void onGeometryEnd() throws Exception {
        if (!(stringBuilder.length() == 0)) {
            featureTransformer.onGeometryCoordinates(stringBuilder.toString());
        }
        //TODO: check for all geo types
        for (int i = 1; i < nesting; i++) {
            featureTransformer.onGeometryNestedEnd();
        }
        featureTransformer.onGeometryEnd();
        geometryMapping = null;
        nesting = 0;
        currentPath = null;
        stringBuilder = new StringBuilder();
    }

    @Override
    public void onPropertyText(String text) throws Exception {
        if (geometryMapping != null) {
            if (currentPath.get(1)
                           .equals("type")) {
                featureTransformer.onGeometryStart(geometryMapping, GEO_JSON_GEOMETRY_TYPE.forString(text)
                                                                                          .toSimpleFeatureGeometry(), 2);
            } else if (currentPath.get(1)
                                  .equals("coordinates")) {
                if (nesting + 2 < currentPath.size() && !(stringBuilder.length() == 0)) {
                    featureTransformer.onGeometryCoordinates(stringBuilder.toString());
                    stringBuilder = new StringBuilder();
                }
                for (int i = nesting + 2; i < currentPath.size(); i++) {
                    //TODO: check for all geo types
                    if (nesting > 0) {
                        featureTransformer.onGeometryNestedStart();
                    }
                    nesting++;
                }
                stringBuilder.append(text)
                             .append(' ');
            }

        } else if (inProperty) {
            featureTransformer.onPropertyText(text);
        }
    }

    @Override
    public void onPropertyEnd(List<String> path) throws Exception {
        if (geometryMapping != null) {
            //geometryMapping = null;
            //featureTransformer.onGeometryEnd();
            if (path.size() < nesting + 2 && !(stringBuilder.length() == 0)) {
                featureTransformer.onGeometryCoordinates(stringBuilder.toString());
                stringBuilder = new StringBuilder();
            }
            for (int i = path.size(); i < nesting + 2; i++) {
            //for (int i = 0; i < nesting && i < 2; i++) {
                nesting--;
                if (nesting < 3)
                featureTransformer.onGeometryNestedEnd();
            }
        } else if (inProperty) {
            featureTransformer.onPropertyEnd();
            inProperty = false;
        }
    }
}
