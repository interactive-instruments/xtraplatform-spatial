/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.infra;

import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.feature.provider.api.FeatureProviderSchemaConsumer;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.ogc.api.GML;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import de.ii.xtraplatform.util.xml.XMLPathTracker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

class WfsSchemaAnalyzer implements FeatureProviderSchemaConsumer {

    public static final String GML_NS_URI = GML.getNS(GML.VERSION._2_1_1);

    private final List<FeatureSchema> featureTypes;
    private final Set<String> mappedPaths;
    private final Map<String, String> crsMap;
    private final XMLNamespaceNormalizer namespaceNormalizer;

    private ImmutableFeatureSchema.Builder currentFeatureType;
    private String currentLocalName;
    private String currentQualifiedName;
    private XMLPathTracker currentPath;
    private String currentParentProperty;
    private Map<String, ImmutableFeatureSchema> properties;

    WfsSchemaAnalyzer(Map<String, String> crsMap, Map<String, String> namespaces) {
        this.featureTypes = new ArrayList<>();
        this.currentPath = new XMLPathTracker();
        this.mappedPaths = new HashSet<>();
        this.crsMap = crsMap;
        this.namespaceNormalizer = new XMLNamespaceNormalizer(namespaces);
        this.properties = new LinkedHashMap<>();
    }

    public List<FeatureSchema> getFeatureTypes() {
        return featureTypes;
    }

    @Override
    public void analyzeFeatureType(String nsUri, String localName) {
        if (Objects.nonNull(currentFeatureType) && Objects.nonNull(currentLocalName)) {
            addLastProperty();
            featureTypes.add(currentFeatureType.build());
        }
        properties = new LinkedHashMap<>();
        currentParentProperty = null;
        currentLocalName = localName.toLowerCase();
        currentQualifiedName = namespaceNormalizer.getQualifiedName(nsUri, localName);
        mappedPaths.clear();
        currentPath.clear();

        currentFeatureType = new ImmutableFeatureSchema.Builder();
        currentFeatureType.name(currentLocalName)
                .type(SchemaBase.Type.OBJECT)
                .sourcePath("/" + localName.toLowerCase());
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
                        .sourcePath(currentPath.toFieldNameGml())
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
        if (currentFeatureType != null && !isPathMapped(path)) {

            ImmutableFeatureSchema.Builder property = new ImmutableFeatureSchema.Builder();
            property.additionalInfo(ImmutableMap.of("multiple", String.valueOf(isParentMultiple)));

            Optional<FeatureSchema.Type> propertyType = getPropertyType(type, isParentMultiple, isComplex, isObject);
            if (propertyType.isPresent()) {
                String fieldNameGml = currentPath.toFieldNameGml();
                property.name(getShortPropertyName(fieldNameGml))
                        .sourcePath(getSourcePath(fieldNameGml))
                        .type(propertyType.get());
                if (propertyType.get() == FeatureSchema.Type.GEOMETRY) {
                    property.geometryType(TargetMappingProviderFromGml.GML_GEOMETRY_TYPE.fromString(type).toSimpleFeatureGeometry());
                    if (crsMap.containsKey(currentLocalName)) {
                        String crs = crsMap.get(currentLocalName).split("EPSG::")[1];
                        property.additionalInfo(ImmutableMap.of(
                                "crs", crs,
                                "multiple", String.valueOf(isParentMultiple)));
                    }
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

    private String getSourcePath(String path) {
        return path.replace("[]", "").replace(".", "/");
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
