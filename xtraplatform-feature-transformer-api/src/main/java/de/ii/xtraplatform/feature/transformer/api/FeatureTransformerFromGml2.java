/**
 * Copyright 2020 interactive instruments GmbH
 * <p>
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.transformer.api;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import de.ii.xtraplatform.features.domain.legacy.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml.GML_GEOMETRY_TYPE;
import de.ii.xtraplatform.features.domain.FeatureConsumer;
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.FeatureTransformer2;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Predicate;

import static de.ii.xtraplatform.dropwizard.domain.LambdaWithException.biConsumerMayThrow;
import static de.ii.xtraplatform.dropwizard.domain.LambdaWithException.consumerMayThrow;
import static de.ii.xtraplatform.dropwizard.domain.LambdaWithException.mayThrow;

/**
 * @author zahnen
 */
class FeatureTransformerFromGml2 implements FeatureConsumer {

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

    private final FeatureType featureType;
    protected final FeatureTransformer2 featureTransformer;
    private final Map<String, List<String>> resolvableTypes;
    private final String outputFormat;
    private boolean inProperty;
    private List<String> inGeometry;
    private boolean geometrySent;
    private boolean inCoordinates;
    private FeatureProperty transformGeometry;
    private SimpleFeatureGeometry transformGeometryType;
    private Integer transformGeometryDimension;
    private final Joiner joiner;
    private final StringBuilder stringBuilder;
    private final List<String> fields;
    private final boolean skipGeometry;
    private final boolean onTheFly;
    private OnTheFlyMapping onTheFlyMapping;
    private List<Integer> currentMultiplicities;
    private List<String> currentType;

    FeatureTransformerFromGml2(FeatureType featureType, final FeatureTransformer2 featureTransformer,
                               List<String> fields, boolean skipGeometry, Map<QName, List<String>> resolvableTypes) {
        this.featureType = featureType;
        this.featureTransformer = featureTransformer;
        this.resolvableTypes = resolvableTypes.entrySet()
                                              .stream()
                                              .collect(ImmutableMap.toImmutableMap(entry -> entry.getKey()
                                                                                                 .getNamespaceURI() + ":" + entry.getKey()
                                                                                                                                 .getLocalPart(), Map.Entry::getValue));
        this.outputFormat = featureTransformer.getTargetFormat();
        this.fields = fields;
        this.skipGeometry = skipGeometry;
        this.joiner = Joiner.on('/');
        this.stringBuilder = new StringBuilder();

        if (featureType == null) {
            this.onTheFly = true;
            try {
                OnTheFly onTheFly = (OnTheFly) featureTransformer;
                this.onTheFlyMapping = onTheFly.getOnTheFlyMapping();
            } catch (ClassCastException e) {
                this.onTheFlyMapping = null;
            }
        } else {
            this.onTheFlyMapping = null;
            this.onTheFly = false;
        }
    }

    /*private Optional<FeatureProperty> getMapping(String pathElement) {
        return getMapping(pathElement, outputFormat);
    }

    private Optional<FeatureProperty> getMapping(String pathElement, String format) {
        if (onTheFly) {
            return Optional.ofNullable(onTheFlyMapping.getTargetMappingForFeatureType(pathElement));
        } else {
            if (!resolvableTypes.isEmpty() && resolvableTypes.containsKey(pathElement)) {
                return getMapping(resolvableTypes.get(pathElement), null, format);
            }

            return featureTypeMapping.findMappings(pathElement, format);
        }
    }*/

    private Optional<FeatureProperty> getMapping(List<String> path) {
        if (onTheFly) {
            return Optional.empty();//.ofNullable(onTheFlyMapping.getTargetMappingForGeometry(path));
        } else {
            return getMapping(path, outputFormat);
        }
    }

    private Optional<FeatureProperty> getMappingForFormat(List<String> path, String format) {
        return getMapping(path, null, format);
    }

    private Optional<FeatureProperty> getMapping(List<String> path, String value) {
        return getMapping(path, value, outputFormat);
    }

    private Optional<FeatureProperty> getMapping(List<String> path, String value, String format) {
        if (onTheFly) {
            return Optional.empty();//.ofNullable(onTheFlyMapping.getTargetMappingForProperty(path, value));
        } else {
            if (!resolvableTypes.isEmpty() && resolvableTypes.containsKey(currentType)) {
                return featureType.findPropertiesForPath(ImmutableList.<String>builder().addAll(resolvableTypes.get(currentType))
                                                                                        .addAll(path)
                                                                                        .build())
                                  .stream()
                                  .findFirst();
            }

            //TODO
            List<String> fullPath = new ImmutableList.Builder<String>().add(featureType.getAdditionalInfo()
                                                                                       .get("featureTypePath"))
                                                                       .addAll(path)
                                                                       .build();

            return featureType.findPropertiesForPath(fullPath)
                              .stream()
                              .findFirst();
        }
    }

    @Override
    public void onStart(OptionalLong numberReturned, OptionalLong numberMatched,
                        Map<String, String> additionalInfos) throws Exception {
        featureTransformer.onStart(numberReturned, numberMatched);

        additionalInfos.forEach(biConsumerMayThrow((key, value) -> onGmlAttribute(key, value, ImmutableList.of(), ImmutableList.of())));
    }

    @Override
    public void onEnd() throws Exception {
        featureTransformer.onEnd();
    }

    class GmlFeatureProperty {
        private List<String> path;
        List<Integer> multiplicities;
        String value;
        final boolean isFeature;

        GmlFeatureProperty(List<String> path, List<Integer> multiplicities, String value) {
            this.path = path;
            this.multiplicities = multiplicities;
            this.value = value;
            this.isFeature = false;
        }

        GmlFeatureProperty(List<String> path) {
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
    Map<String, List<GmlFeatureProperty>> buffer = new HashMap<>();
    boolean doBuffer = false;
    List<GmlFeatureProperty> currentBuffer;
    GmlFeatureProperty currentProperty;

    @Override
    public void onFeatureStart(List<String> path, Map<String, String> additionalInfos) throws Exception {
        //TODO maybe flag resolve and mainType is enough?
        if (resolvableTypes.containsKey(path.get(0))) {
            doBuffer = true;
            currentType = path;
        } else {
            featureTransformer.onFeatureStart(featureType);
        }

        additionalInfos.forEach(biConsumerMayThrow((key, value) -> onGmlAttribute(key, value, ImmutableList.of(), ImmutableList.of())));
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

    private void onGmlAttribute(String name, String value, List<String> path,
                                List<Integer> multiplicities) throws Exception {
        onGmlAttribute(getNamespaceUri(name), getLocalName(name), path, value, multiplicities);
    }

    //@Override
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
                currentBuffer.add(new GmlFeatureProperty(currentType));
                currentProperty = null;
            }
            /*if (Objects.nonNull(currentProperty) && Objects.isNull(currentProperty.value) && fullPath.subList(0,currentProperty.getPath().size()).equals(currentProperty.getPath())) {
                currentProperty.setPath(fullPath);
                currentProperty.multiplicities = ImmutableList.of();
                currentProperty.value = value;
                currentProperty = null;
            } else {
                currentProperty = new FeatureProperty(fullPath, ImmutableList.of(), value);*/
            currentBuffer.add(new GmlFeatureProperty(fullPath, multiplicities, value));
            //  currentProperty = null;
            //}
        } else {
            getMapping(fullPath, value)
                    .ifPresent(consumerMayThrow(featureProperty -> {
                        writeProperty(featureProperty, multiplicities, path, value);
                    }));
        }
    }

    private void writeProperty(FeatureProperty featureProperty, List<Integer> multiplicities, List<String> path,
                               String value) throws Exception {
        if (!buffer.isEmpty() && featureProperty.isReferenceEmbed()) {
            if (buffer.containsKey(value.substring(1))) {
                List<GmlFeatureProperty> feature = buffer.get(value.substring(1));
                List<String> pathPrefix = ImmutableList.<String>builder().addAll(path)
                                                                         .addAll(feature.get(0)
                                                                                        .getPath())
                                                                         .build();

                for (int i = 1; i < feature.size(); i++) {
                    GmlFeatureProperty property = feature.get(i);
                    if (Objects.isNull(property.value)) {
                        continue;
                    }
                    List<String> propertyPath = ImmutableList.<String>builder().addAll(pathPrefix)
                                                                               .addAll(property.getPath())
                                                                               .build();
                    onPropertyStart(propertyPath, ImmutableList.<Integer>builder().addAll(multiplicities)
                                                                                  .addAll(property.multiplicities)
                                                                                  .build(), ImmutableMap.of());
                    onPropertyText(property.value);
                    onPropertyEnd(propertyPath);
                }
            }

        } else {
            featureTransformer.onPropertyStart(featureProperty, multiplicities);
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
    public void onPropertyStart(List<String> path, List<Integer> multiplicities,
                                Map<String, String> additionalInfos) throws Exception {
        boolean mapped = false;

        //TODO
        if (doBuffer) {
            if (Objects.nonNull(currentProperty) && Objects.isNull(currentProperty.value) && path.subList(0, currentProperty.getPath()
                                                                                                                            .size())
                                                                                                 .equals(currentProperty.getPath())) {
                currentProperty.setPath(ImmutableList.copyOf(path));
                currentProperty.multiplicities = multiplicities;
            } else {
                currentProperty = new GmlFeatureProperty(ImmutableList.copyOf(path), multiplicities, null);
                currentBuffer.add(currentProperty);
            }
            return;
        }

        if (!inProperty) {
            if (!onTheFly) {
                if (shouldIgnoreProperty(path)) {
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
                    .ifPresent(consumerMayThrow(featureProperty -> {
                        transformGeometry = featureProperty;
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

        additionalInfos.forEach(biConsumerMayThrow((key, value) -> onGmlAttribute(key, value, path, multiplicities)));
    }

    private Predicate<FeatureProperty> isSpatial() {
        return FeatureProperty::isSpatial;
    }

    private Predicate<FeatureProperty> isNotSpatial() {
        return featureProperty -> !featureProperty.isSpatial();
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
            if (shouldIgnoreProperty(path)) {
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

    private boolean shouldIgnoreProperty(List<String> path) {
        return !inProperty
                && ((!fields.contains("*") && !getMapping(path).filter(this::isPropertyInWhitelist)
                                                               .isPresent())
                || (skipGeometry && getMapping(path).filter(FeatureProperty::isSpatial)
                                                    .isPresent()));
    }

    private boolean isPropertyInWhitelist(FeatureProperty featureProperty) {
        if (featureProperty.isSpatial()) {
            return !skipGeometry;
        }
        return featureProperty.isId()
                || fields.contains(featureProperty.getName())
                || fields.stream()
                         .anyMatch(field -> {
                             String regex = field + "(?:\\[\\w*\\])?\\..*";
                             return featureProperty.getName()
                                                   .matches(regex);
                         });
    }

    /*@Override
    public void onNamespaceRewrite(QName featureType, String namespace) {

    }*/

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
        return path.isEmpty() ? null : getLocalName(path.get(path.size() - 1));
    }

    private String getLocalName(String name) {
        return name.substring(name.lastIndexOf(":") + 1);
    }

    private String getNamespaceUri(List<String> path) {
        return path.isEmpty() ? null : getNamespaceUri(path.get(path.size() - 1));
    }

    private String getNamespaceUri(String name) {
        return name.substring(0, name.lastIndexOf(":"));
    }
}
