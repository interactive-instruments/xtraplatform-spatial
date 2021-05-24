/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.infra;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureProviderSchemaConsumer;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.ogc.api.GML;
import de.ii.xtraplatform.xml.domain.XMLNamespaceNormalizer;
import de.ii.xtraplatform.xml.domain.XMLPathTracker;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WfsSchemaAnalyzer implements FeatureProviderSchemaConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WfsSchemaAnalyzer.class);
    public static final String GML_NS_URI = GML.getNS(GML.VERSION._2_1_1);

    private final List<FeatureSchema> featureTypes;
    private final Set<String> mappedPaths;
    private final Map<QName, String> crsMap;
    private final XMLNamespaceNormalizer namespaceNormalizer;
    private final Map<QName, String> typeIds;

    private ImmutableFeatureSchema.Builder currentFeatureType;
    private String currentLocalName;
    private String currentPrefixedName;
    private QName currentQualifiedName;
    private XMLPathTracker currentPath;
    private String currentParentProperty;
    private Map<String, ImmutableFeatureSchema> properties;

    WfsSchemaAnalyzer(List<QName> featureTypes,
        Map<QName, String> crsMap, Map<String, String> namespaces) {
        this.featureTypes = new ArrayList<>();
        this.currentPath = new XMLPathTracker();
        this.mappedPaths = new HashSet<>();
        this.crsMap = crsMap;
        this.namespaceNormalizer = new XMLNamespaceNormalizer(namespaces);
        this.properties = new LinkedHashMap<>();
        this.typeIds = getTypeIds(featureTypes);
    }

    private Map<QName, String> getTypeIds(List<QName> featureTypes) {
        return featureTypes.stream().map(qName -> {
            boolean hasConflict = featureTypes.stream().anyMatch(
                qName1 -> Objects.equals(qName.getLocalPart(), qName1.getLocalPart()) && !Objects.equals(qName, qName1));

            String id = hasConflict
                ? WfsCapabilitiesAnalyzer.getLongFeatureTypeId(namespaceNormalizer.getPrefixedName(qName), namespaceNormalizer)
                : WfsCapabilitiesAnalyzer.getShortFeatureTypeId(namespaceNormalizer.getPrefixedName(qName), namespaceNormalizer);

            return new SimpleImmutableEntry<>(qName, id);
        }).collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public List<FeatureSchema> getFeatureTypes() {
        return featureTypes;
    }

    @Override
    public void analyzeNamespace(String uri) {
        namespaceNormalizer.addNamespace(uri);
    }

    @Override
    public void analyzeFeatureType(String nsUri, String localName) {
        if (Objects.nonNull(currentFeatureType) && Objects.nonNull(currentLocalName)) {
            addLastProperty();
            featureTypes.add(currentFeatureType.build());
        }
        properties = new LinkedHashMap<>();
        currentParentProperty = null;
        currentPrefixedName = namespaceNormalizer.getQualifiedName(nsUri, localName);
        currentQualifiedName = namespaceNormalizer.getQName(nsUri, localName);
        currentLocalName = typeIds.get(currentQualifiedName);
        mappedPaths.clear();
        currentPath.clear();

        currentFeatureType = new ImmutableFeatureSchema.Builder();
        currentFeatureType.name(currentLocalName)
                .type(SchemaBase.Type.OBJECT)
                .sourcePath("/" + currentPrefixedName);
    }

    @Override
    public void analyzeAttribute(String nsUri, String localName, String type, boolean required) {
        // only first level gml:ids
        if (!currentPath.isEmpty()) {
            return;
        }
        currentPath.track(nsUri, "@" + localName, false);

        if ((localName.equals("id") && nsUri.startsWith(GML_NS_URI)) || localName.equals("fid")) {
            String path = currentPath.toString();
            if (currentFeatureType != null && !isPathMapped(path)) {
                ImmutableFeatureSchema.Builder featureProperty = new ImmutableFeatureSchema.Builder()
                        .name("id")
                        .sourcePath(getSourcePath(currentPath.asList()))
                        .role(FeatureSchema.Role.ID)
                        .type(FeatureSchema.Type.STRING);
                currentFeatureType.putPropertyMap(localName, featureProperty);
            }
        }

    }

    @Override
    public void analyzeProperty(String nsUri, String localName, String type, long minOccurs, long maxOccurs,
                                int depth, boolean isParentMultiple, boolean isComplex, boolean isObject) {

        currentPath.track(nsUri, localName, depth, isParentMultiple);

        String path = currentPath.toString();
        // skip first level gml properties
        if (path.startsWith(GML_NS_URI)) {
            return;
        }

        if (depth == 1) {
            currentParentProperty = null;
        }

        if (currentFeatureType != null && !isPathMapped(path)) {

            ImmutableFeatureSchema.Builder property = new ImmutableFeatureSchema.Builder();
            property.additionalInfo(ImmutableMap.of("multiple", String.valueOf(isParentMultiple)));

            Optional<FeatureSchema.Type> propertyType = getPropertyType(type, isParentMultiple, isComplex, isObject);
            if (propertyType.isPresent()) {
                String fieldNameGml = currentPath.toFieldNameGml();
                if (fieldNameGml.equals("id"))
                    fieldNameGml = "_id_";
                property.name(getShortPropertyName(fieldNameGml))
                        .sourcePath(getSourcePath(currentPath.asList()))
                        .type(propertyType.get());
                if (propertyType.get() == FeatureSchema.Type.GEOMETRY) {
                    property.geometryType(TargetMappingProviderFromGml.GML_GEOMETRY_TYPE.fromString(type).toSimpleFeatureGeometry());
                    if (crsMap.containsKey(currentQualifiedName)) {
                        String crs = String
                            .valueOf(EpsgCrs.fromString(crsMap.get(currentQualifiedName)).getCode());
                        property.additionalInfo(ImmutableMap.of(
                                "crs", crs,
                                "multiple", String.valueOf(isParentMultiple)));
                    }
                    mappedPaths.add(path);
                }
                if (propertyType.get() == SchemaBase.Type.VALUE_ARRAY) {
                    property.valueType(getFeatureSchemaType(TargetMappingProviderFromGml.GML_TYPE.fromString(type)));
                }
                if (isComplexType(propertyType.get())) {
                    if (!fieldNameGml.equals(currentParentProperty)) {
                        if (Objects.nonNull(currentParentProperty) &&
                                !properties.get(currentParentProperty).getProperties().isEmpty()) {
                            currentFeatureType.putPropertyMap(getShortParentName(currentParentProperty), properties.get(currentParentProperty));
                        }
                        currentParentProperty = fieldNameGml;
                    }
                    properties.put(currentParentProperty, property.build());
                    return;
                }
                if (depth == 1) {
                    currentFeatureType.putPropertyMap(fieldNameGml, property.build());
                } else {
                    addToParentProperty(property.build(), getFullParentName(fieldNameGml));
                }
            }
        }
    }

    private void addToParentProperty(ImmutableFeatureSchema childProperty, String parentName) {
        if (properties.containsKey(parentName)) {
            ImmutableFeatureSchema parentProperty = new ImmutableFeatureSchema.Builder()
                    .from(properties.get(parentName))
                    .putPropertyMap(childProperty.getName(), childProperty)
                    .build();
            properties.put(parentName, parentProperty);
        }
    }

    private Optional<FeatureSchema.Type> getPropertyType(String type, boolean isParentMultiple, boolean isComplex, boolean isObject) {
        if (isParentMultiple && isComplex && isObject) {
            return Optional.of(FeatureSchema.Type.OBJECT_ARRAY);
        }
        if (isParentMultiple && TargetMappingProviderFromGml.GML_TYPE.fromString(type).isValid()) {
            return Optional.of(FeatureSchema.Type.VALUE_ARRAY);
        }
        if (isComplex && isObject) {
            return Optional.of(FeatureSchema.Type.OBJECT);
        }
        if (TargetMappingProviderFromGml.GML_TYPE.fromString(type).isValid()) {
            return Optional.of(getFeatureSchemaType(TargetMappingProviderFromGml.GML_TYPE.fromString(type)));
        }
        if (TargetMappingProviderFromGml.GML_GEOMETRY_TYPE.fromString(type).isValid()) {
            return Optional.of(FeatureSchema.Type.GEOMETRY);
        }
        return Optional.empty();
    }

    private String getFullParentName(String fullName) {
        int lastDot = fullName.lastIndexOf(".");
        return fullName.substring(0, lastDot);
    }

    private String getShortParentName(String fullName) {
        String[] nameTokens = fullName.replace("[]", "").split("\\.");
        return nameTokens[nameTokens.length - 1];
    }

    private String getShortPropertyName(String fullName) {
        String[] nameTokens = fullName.replace("[]", "").split("\\.");
        return nameTokens[nameTokens.length - 1];
    }

    private boolean isComplexType(FeatureSchema.Type type) {
        return type == FeatureSchema.Type.OBJECT_ARRAY || type == FeatureSchema.Type.OBJECT;
    }

    private void addLastProperty() {
        if (Objects.nonNull(currentParentProperty) &&
                !properties.get(currentParentProperty).getProperties().isEmpty()) {
            currentFeatureType.putPropertyMap(getShortParentName(currentParentProperty), properties.get(currentParentProperty));
        }
    }


    @Override
    public boolean analyzeNamespaceRewrite(String oldNamespace, String newNamespace, String featureTypeName) {
        return false;
    }

    @Override
    public void analyzeFailure(Throwable e) {
    }

    @Override
    public void analyzeSuccess() {
        // finish last feature type
        if (Objects.nonNull(currentFeatureType) && Objects.nonNull(currentLocalName)) {
            addLastProperty();
            featureTypes.add(currentFeatureType.build());
        }
    }

    private FeatureSchema.Type getFeatureSchemaType(TargetMappingProviderFromGml.GML_TYPE dataType) {

        switch (dataType) {
            case BOOLEAN:
                return FeatureSchema.Type.BOOLEAN;
            case STRING:
                return FeatureSchema.Type.STRING;
            case INT:
            case INTEGER:
            case SHORT:
            case LONG:
                return FeatureSchema.Type.INTEGER;
            case FLOAT:
            case DOUBLE:
            case DECIMAL:
                return FeatureSchema.Type.FLOAT;
            case DATE:
            case DATE_TIME:
                return FeatureSchema.Type.DATETIME;
            default:
                return FeatureSchema.Type.UNKNOWN;
        }
    }

    private String getSourcePath(List<String> path) {
        String sourcePath = namespaceNormalizer.getPrefixedPath(Joiner.on('/').join(path));
        if (Objects.nonNull(currentParentProperty) &&
            properties.get(currentParentProperty).getSourcePath().isPresent()) {
            //LOGGER.info("SP: {} {}", sourcePath, properties.get(currentParentProperty).getSourcePath().get());
            sourcePath = sourcePath.substring(properties.get(currentParentProperty).getSourcePath().get().length() +1);
        }

        return sourcePath;
    }

    // this prevents that we descend further on a mapped path
    private boolean isPathMapped(String path) {
        for (String mappedPath : mappedPaths) {
            if (path.startsWith(mappedPath + "/")) {
                return true;
            }
        }
        return false;
    }
}
