/**
 * Copyright 2019 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.feature.provider.api.SimpleFeatureGeometry;
import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml.GML_GEOMETRY_TYPE;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Predicate;

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
            .add("pointMember")
            .build();
    static final List<String> GEOMETRY_COORDINATES = new ImmutableList.Builder<String>()
            .add("posList")
            .add("pos")
            .add("coordinates")
            .build();

    private final FeatureTypeMapping featureTypeMapping;
    protected final FeatureTransformer featureTransformer;
    private final Map<String, List<String>> resolvableTypes;
    private final String outputFormat;
    private boolean inProperty;
    private List<String> inGeometry;
    private boolean geometrySent;
    private boolean inCoordinates;
    private TargetMapping transformGeometry;
    private SimpleFeatureGeometry transformGeometryType;
    private Integer transformGeometryDimension;
    private final Joiner joiner;
    private final StringBuilder stringBuilder;
    private final List<String> fields;
    private final boolean onTheFly;
    private OnTheFlyMapping onTheFlyMapping;
    private List<Integer> currentMultiplicities;
    private List<String> currentType;

    FeatureTransformerFromGml(FeatureTypeMapping featureTypeMapping, final FeatureTransformer featureTransformer,
                              List<String> fields, Map<QName, List<String>> resolvableTypes) {
        this.featureTypeMapping = featureTypeMapping;
        this.featureTransformer = featureTransformer;
        this.resolvableTypes = resolvableTypes.entrySet()
                                              .stream()
                                              .collect(ImmutableMap.toImmutableMap(entry -> entry.getKey()
                                                                                                 .getNamespaceURI() + ":" + entry.getKey()
                                                                                                                                 .getLocalPart(), Map.Entry::getValue));
        this.outputFormat = featureTransformer.getTargetFormat();
        this.fields = fields;
        this.joiner = Joiner.on('/');
        this.stringBuilder = new StringBuilder();

        if (featureTypeMapping == null) {
            this.onTheFly = true;
            try {
                FeatureTransformer.OnTheFly onTheFly = (FeatureTransformer.OnTheFly) featureTransformer;
                this.onTheFlyMapping = onTheFly.getOnTheFlyMapping();
            } catch (ClassCastException e) {
                this.onTheFlyMapping = null;
            }
        } else {
            this.onTheFlyMapping = null;
            this.onTheFly = false;
        }
    }

    private Optional<TargetMapping> getMapping(String pathElement) {
        return getMapping(pathElement, outputFormat);
    }

    private Optional<TargetMapping> getMapping(String pathElement, String format) {
        if (onTheFly) {
            return Optional.ofNullable(onTheFlyMapping.getTargetMappingForFeatureType(pathElement));
        } else {
            if (!resolvableTypes.isEmpty() && resolvableTypes.containsKey(pathElement)) {
                return getMapping(resolvableTypes.get(pathElement), null, format);
            }

            return featureTypeMapping.findMappings(pathElement, format);
        }
    }

    /*private Optional<TargetMapping> getMapping(String pathElement, String value) {
        if (onTheFly) {
            return Optional.ofNullable(onTheFlyMapping.getTargetMappingForAttribute(ImmutableList.of(pathElement), value));
        } else {
            return featureTypeMapping.findMappings(pathElement, outputFormat);
        }
    }*/

    private Optional<TargetMapping> getMapping(List<String> path) {
        if (onTheFly) {
            return Optional.ofNullable(onTheFlyMapping.getTargetMappingForGeometry(path));
        } else {
            return getMapping(path, outputFormat);
        }
    }

    private Optional<TargetMapping> getMappingForFormat(List<String> path, String format) {
        return getMapping(path, null, format);
    }

    private Optional<TargetMapping> getMapping(List<String> path, String value) {
        return getMapping(path, value, outputFormat);
    }

    private Optional<TargetMapping> getMapping(List<String> path, String value, String format) {
        if (onTheFly) {
            return Optional.ofNullable(onTheFlyMapping.getTargetMappingForProperty(path, value));
        } else {
            if (!resolvableTypes.isEmpty() && resolvableTypes.containsKey(currentType)) {
                return featureTypeMapping.findMappings(ImmutableList.<String>builder().addAll(resolvableTypes.get(currentType))
                                                                                      .addAll(path)
                                                                                      .build(), format);
            }

            return featureTypeMapping.findMappings(path, format);
        }
    }

    @Override
    public void onStart(OptionalLong numberReturned, OptionalLong numberMatched) throws Exception {
        featureTransformer.onStart(numberReturned, numberMatched);
    }

    @Override
    public void onEnd() throws Exception {
        featureTransformer.onEnd();
    }

    class FeatureProperty {
        private List<String> path;
        List<Integer> multiplicities;
        String value;
        final boolean isFeature;

        FeatureProperty(List<String> path, List<Integer> multiplicities, String value) {
            this.path = path;
            this.multiplicities = multiplicities;
            this.value = value;
            this.isFeature = false;
        }

        FeatureProperty(List<String> path) {
            this.path = path;
            this.multiplicities = null;
            this.value = null;
            this.isFeature = true;
        }

        public List<String> getPath() {
            return path;
        }

        public void setPath(List<String> path) {
            if (!path.isEmpty() && path.get(path.size() - 1)
                                       .endsWith(":component")) {
                boolean stop = true;
            }
            this.path = path;
        }
    }

    //TODO ignore geometries for now, move nestingLevelChange logic here
    Map<String, List<FeatureProperty>> buffer = new HashMap<>();
    boolean doBuffer = false;
    List<FeatureProperty> currentBuffer;
    FeatureProperty currentProperty;

    @Override
    public void onFeatureStart(List<String> path) throws Exception {
        //TODO maybe flag resolve and mainType is enough?
        if (resolvableTypes.containsKey(path.get(0))) {
            doBuffer = true;
            currentType = path;
        } else {
            final TargetMapping mapping = getMapping(path.get(0)).orElse(getMapping(path.get(0), TargetMapping.BASE_TYPE).orElse(null));
            featureTransformer.onFeatureStart(mapping);
        }
    }

    @Override
    public void onFeatureEnd(List<String> path) throws Exception {
        if (doBuffer) {
            doBuffer = false;
            currentType = null;
            currentBuffer = null;
            currentProperty = null;
        } else {
            featureTransformer.onFeatureEnd();
        }
    }

    @Override
    public void onGmlAttribute(String namespace, String localName, List<String> path, String value,
                               List<Integer> multiplicities) {
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

        List<String> fullPath = ImmutableList.<String>builder().addAll(path)
                                                               .add(getQualifiedName(namespace, "@" + localName))
                                                               .build();

        //TODO
        if (doBuffer) {
            if (path.isEmpty() && localName.equals("id")) {
                buffer.putIfAbsent(value, new ArrayList<>());
                currentBuffer = buffer.get(value);
                currentBuffer.add(new FeatureProperty(currentType));
                currentProperty = null;
            }
            /*if (Objects.nonNull(currentProperty) && Objects.isNull(currentProperty.value) && fullPath.subList(0,currentProperty.getPath().size()).equals(currentProperty.getPath())) {
                currentProperty.setPath(fullPath);
                currentProperty.multiplicities = ImmutableList.of();
                currentProperty.value = value;
                currentProperty = null;
            } else {
                currentProperty = new FeatureProperty(fullPath, ImmutableList.of(), value);*/
            currentBuffer.add(new FeatureProperty(fullPath, multiplicities, value));
            //  currentProperty = null;
            //}
        } else {
            getMapping(fullPath, value)
                    .ifPresent(consumerMayThrow(mapping -> {
                        writeProperty(mapping, multiplicities, path, value);
                    }));
        }
    }

    private void writeProperty(TargetMapping mapping, List<Integer> multiplicities, List<String> path,
                               String value) throws Exception {
        if (!buffer.isEmpty() && Objects.nonNull(mapping.getBaseMapping()) && mapping.getBaseMapping()
                                                                                     .isReferenceEmbed()) {
            if (buffer.containsKey(value.substring(1))) {
                List<FeatureProperty> feature = buffer.get(value.substring(1));
                List<String> pathPrefix = ImmutableList.<String>builder().addAll(path)
                                                                         .addAll(feature.get(0)
                                                                                        .getPath())
                                                                         .build();

                for (int i = 1; i < feature.size(); i++) {
                    FeatureProperty property = feature.get(i);
                    if (Objects.isNull(property.value)) {
                        continue;
                    }
                    List<String> propertyPath = ImmutableList.<String>builder().addAll(pathPrefix)
                                                                               .addAll(property.getPath())
                                                                               .build();
                    onPropertyStart(propertyPath, ImmutableList.<Integer>builder().addAll(multiplicities)
                                                                                  .addAll(property.multiplicities)
                                                                                  .build());
                    onPropertyText(property.value);
                    onPropertyEnd(propertyPath);
                }
            }

        } else {
            featureTransformer.onPropertyStart(mapping, multiplicities);
            featureTransformer.onPropertyText(value);
            featureTransformer.onPropertyEnd();
        }
    }

    private void writePropertyText(String value) throws Exception {
        if (doBuffer) {
            currentProperty.value = value;
            currentProperty = null;
        } else {
            featureTransformer.onPropertyText(value);
        }
    }

    //TODO: on-the-fly mappings
    @Override
    public void onPropertyStart(List<String> path, List<Integer> multiplicities) throws Exception {
        boolean mapped = false;

        //TODO
        if (doBuffer) {
            if (Objects.nonNull(currentProperty) && Objects.isNull(currentProperty.value) && path.subList(0, currentProperty.getPath()
                                                                                                                            .size())
                                                                                                 .equals(currentProperty.getPath())) {
                currentProperty.setPath(ImmutableList.copyOf(path));
                currentProperty.multiplicities = multiplicities;
            } else {
                currentProperty = new FeatureProperty(ImmutableList.copyOf(path), multiplicities, null);
                currentBuffer.add(currentProperty);
            }
            return;
        }

        if (!inProperty) {
            if (!onTheFly) {
                boolean ignore = !fields.contains("*") && !getMappingForFormat(path, TargetMapping.BASE_TYPE)
                        .filter(targetMapping -> fields.contains(targetMapping.getName()))
                        .isPresent();
                if (ignore) {
                    return;
                }

                mapped = getMapping(path)
                        .filter(isNotSpatial())
                        .map(mayThrow(mapping -> {
                            featureTransformer.onPropertyStart(mapping, multiplicities);
                            return mapping;
                        }))
                        .isPresent();
            } else {
                this.currentMultiplicities = multiplicities;
            }
        } else if (transformGeometry != null) {
            onGeometryPart(getLocalName(path));
        }

        inProperty = inProperty || mapped;

        if (!inProperty && transformGeometry == null) {
            getMapping(path)
                    .filter(isSpatial())
                    .ifPresent(consumerMayThrow(mapping -> {
                        transformGeometry = mapping;
                        if (onTheFly) {
                            onGeometryPart(getLocalName(path));
                        }
                    }));

            if (transformGeometry != null) {
                inProperty = true;
                // has to be copy, as path is a reference to the list in pathTracker
                inGeometry = ImmutableList.copyOf(path);
            }
        }
    }

    private Predicate<TargetMapping> isSpatial() {
        return TargetMapping::isSpatial;
    }

    private Predicate<TargetMapping> isNotSpatial() {
        return mapping -> !mapping.isSpatial();
    }

    private String join(List<String> elements) {
        stringBuilder.setLength(0);
        return joiner.appendTo(stringBuilder, elements)
                     .toString();
    }

    @Override
    public void onPropertyText(String text) throws Exception {
        if (inProperty || onTheFly || doBuffer) {
            stringBuilder.append(text);
            /*if (inCoordinates) {
                featureTransformer.onGeometryCoordinates(text);
            } else {
                featureTransformer.onPropertyText(text);
            }*/
        }
    }

    @Override
    public void onPropertyEnd(List<String> path) throws Exception {
        if (onTheFly && inGeometry == null) {
            boolean ignore = !fields.contains("*") && !getMapping(path, stringBuilder.toString())
                    .filter(targetMapping -> fields.contains(targetMapping.getName()))
                    .isPresent();
            if (ignore) {
                return;
            }

            this.inProperty = true;

            getMapping(path, stringBuilder.toString())
                    .filter(isNotSpatial())
                    .map(mayThrow(mapping -> {
                        featureTransformer.onPropertyStart(mapping, currentMultiplicities);
                        return mapping;
                    }));
        }

        if (stringBuilder.length() > 0) {
            if (inCoordinates) {
                featureTransformer.onGeometryCoordinates(stringBuilder.toString());
            } else {
                writePropertyText(stringBuilder.toString());
            }
            stringBuilder.setLength(0);
        }

        if (transformGeometry != null) {
            if (inGeometry != null && inGeometry.equals(path)) {
                if (transformGeometryType == SimpleFeatureGeometry.MULTI_POLYGON) {
                    featureTransformer.onGeometryNestedEnd();
                }
                if (onTheFly) {
                    onGeometryPartEnd(getLocalName(path));
                }
                inGeometry = null;
                transformGeometry = null;
                transformGeometryType = null;
                transformGeometryDimension = null;
                geometrySent = false;
                featureTransformer.onGeometryEnd();
                inProperty = false;
            } else {
                onGeometryPartEnd(getLocalName(path));
            }
        } else if (inProperty) {
            if (!doBuffer) {
                featureTransformer.onPropertyEnd();
            }
            inProperty = false;
        }
    }

    @Override
    public void onNamespaceRewrite(QName featureType, String namespace) {

    }

    private void onGeometryPart(final String localName) throws Exception {
        if (transformGeometry == null) return;

        if (transformGeometryType == null) {
            final SimpleFeatureGeometry geometryType = GML_GEOMETRY_TYPE.fromString(localName)
                                                                        .toSimpleFeatureGeometry();
            if (geometryType.isValid()) {
                transformGeometryType = geometryType;
            }
        }

        if (transformGeometryType != null) {
            if (GEOMETRY_PARTS.contains(localName)) {
                if (!geometrySent) {
                    featureTransformer.onGeometryStart(transformGeometry, transformGeometryType, transformGeometryDimension);
                    if (transformGeometryType == SimpleFeatureGeometry.MULTI_POLYGON) {
                        featureTransformer.onGeometryNestedStart();
                    }
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

    private String getLocalName(List<String> path) {
        return path.isEmpty() ? null : path.get(path.size() - 1)
                                           .substring(path.get(path.size() - 1)
                                                          .lastIndexOf(":") + 1);
    }
}
