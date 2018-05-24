/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.feature.query.api.TargetMapping;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static de.ii.xtraplatform.util.functional.LambdaWithException.consumerMayThrow;
import static de.ii.xtraplatform.util.functional.LambdaWithException.mayThrow;

/**
 * @author zahnen
 */
class FeatureTransformerFromGml implements GmlConsumer {

    static final List<String> GEOMETRY_PARTS = new ImmutableList.Builder<String>()
            .add("exterior")
            .add("interior")
            .add("outerBoundaryIs")
            .add("innerBoundaryIs")
            .add("LineString")
            .build();
    static final List<String> GEOMETRY_COORDINATES = new ImmutableList.Builder<String>()
            .add("posList")
            .add("pos")
            .add("coordinates")
            .build();

    private final FeatureTypeMapping featureTypeMapping;
    protected final FeatureTransformer featureTransformer;
    private final String outputFormat;
    private boolean inProperty;
    private List<String> inGeometry;
    private boolean geometrySent;
    private boolean inCoordinates;
    private TargetMapping transformGeometry;
    private GmlFeatureTypeAnalyzer.GML_GEOMETRY_TYPE transformGeometryType;
    private Integer transformGeometryDimension;
    private final Joiner joiner;
    private final StringBuilder stringBuilder;

    FeatureTransformerFromGml(FeatureTypeMapping featureTypeMapping, final FeatureTransformer featureTransformer) {
        this.featureTypeMapping = featureTypeMapping;
        this.featureTransformer = featureTransformer;
        this.outputFormat = featureTransformer.getTargetFormat();
        this.joiner = Joiner.on('/');
        this.stringBuilder = new StringBuilder();
    }

    @Override
    public void onGmlStart(OptionalInt numberReturned, OptionalInt numberMatched) throws Exception {
        featureTransformer.onStart(numberReturned, numberMatched);
    }

    @Override
    public void onGmlEnd() throws Exception {
        featureTransformer.onEnd();
    }

    @Override
    public void onGmlFeatureStart(String namespace, String localName, List<String> path) throws Exception {
        final TargetMapping mapping = featureTypeMapping.findMappings(getQualifiedName(namespace, localName), outputFormat)
                                                        .stream()
                                                        .filter(TargetMapping::isEnabled)
                                                        .findFirst()
                                                        .orElse(null);
        featureTransformer.onFeatureStart(mapping);
    }

    @Override
    public void onGmlFeatureEnd(String namespace, String localName, List<String> strings) throws Exception {
        featureTransformer.onFeatureEnd();
    }

    @Override
    public void onGmlAttribute(String namespace, String localName, List<String> path, String value) {
        if (transformGeometry != null) {
            if (transformGeometryDimension == null && localName.equals("srsDimension")) {
                try {
                    transformGeometryDimension = Integer.valueOf(value);
                } catch (NumberFormatException e) {
                    // ignore
                }

            }
            return;
        }

        featureTypeMapping.findMappings(getQualifiedName(namespace, "@" + localName), outputFormat)
                          .stream()
                          .filter(TargetMapping::isEnabled)
                          .forEach(consumerMayThrow(mapping -> featureTransformer.onAttribute(mapping, value)));
    }

    //TODO: on-the-fly mappings
    @Override
    public void onGmlPropertyStart(String namespace, String localName, List<String> path) throws Exception {
        boolean mapped = false;
        if (!inProperty) {
            mapped = featureTypeMapping.findMappings2(path, outputFormat)
                                       .stream()
                                       .filter(TargetMapping::isEnabled)
                                       .filter(targetMapping -> !targetMapping.isSpatial())
                                       .map(mayThrow(mapping -> {
                                           featureTransformer.onPropertyStart(mapping);
                                           return true;
                                       }))
                                       .reduce(false, (a, b) -> a || b);
        } else if (transformGeometry != null) {
            onGeometryPart(localName);
        }

        inProperty = inProperty || mapped;

        if (!inProperty && transformGeometry == null) {
            featureTypeMapping.findMappings2(path, outputFormat)
                              .stream()
                              .filter(TargetMapping::isEnabled)
                              .filter(TargetMapping::isSpatial)
                              .findFirst()
                              .ifPresent(mapping -> transformGeometry = mapping);

            if (transformGeometry != null) {
                inProperty = true;
                // has to be copy, as path is a reference to the list in pathTracker
                inGeometry = ImmutableList.copyOf(path);
            }
        }
    }

    private String join(List<String> elements) {
        stringBuilder.setLength(0);
        return joiner.appendTo(stringBuilder, elements)
                     .toString();
    }

    @Override
    public void onGmlPropertyText(String text) throws Exception {
        if (inProperty) {
            if (inCoordinates) {
                featureTransformer.onGeometryCoordinates(text);
            } else {
                featureTransformer.onPropertyText(text);
            }
        }
    }

    @Override
    public void onGmlPropertyEnd(String namespace, String localName, List<String> path) throws Exception {
        if (transformGeometry != null) {
            if (inGeometry != null && inGeometry.equals(path)) {
                inGeometry = null;
                transformGeometry = null;
                transformGeometryType = null;
                transformGeometryDimension = null;
                geometrySent = false;
                featureTransformer.onGeometryEnd();
                inProperty = false;
            } else {
                onGeometryPartEnd(localName);
            }
        } else if (inProperty) {
            featureTransformer.onPropertyEnd();
            inProperty = false;
        }
    }

    @Override
    public void onNamespaceRewrite(QName featureType, String namespace) {

    }

    private void onGeometryPart(final String localName) throws Exception {
        if (transformGeometry == null) return;

        if (transformGeometryType == null) {
            final GmlFeatureTypeAnalyzer.GML_GEOMETRY_TYPE geometryType = GmlFeatureTypeAnalyzer.GML_GEOMETRY_TYPE.fromString(localName);
            if (geometryType.isValid()) {
                transformGeometryType = geometryType;
            }
        }

        if (transformGeometryType != null) {
            if (GEOMETRY_PARTS.contains(localName)) {
                if (!geometrySent) {
                    featureTransformer.onGeometryStart(transformGeometry, transformGeometryType, transformGeometryDimension);
                    geometrySent = true;
                }
                featureTransformer.onGeometryNestedStart();
            } else if (GEOMETRY_COORDINATES.contains(localName)) {
                if (!geometrySent) {
                    featureTransformer.onGeometryStart(transformGeometry, transformGeometryType, transformGeometryDimension);
                    geometrySent = true;
                }
                inCoordinates = true;
            }
        }
    }

    private void onGeometryPartEnd(final String localName) throws Exception {
        if (transformGeometry == null) return;

        if (GEOMETRY_PARTS.contains(localName)) {
            featureTransformer.onGeometryNestedEnd();
        } else if (GEOMETRY_COORDINATES.contains(localName)) {
            inCoordinates = false;
        }
    }

    private String getQualifiedName(String namespaceUri, String localName) {
        return Optional.ofNullable(namespaceUri)
                       .map(ns -> ns + ":" + localName)
                       .orElse(localName);
    }
}
