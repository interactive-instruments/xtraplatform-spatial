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
import de.ii.xtraplatform.features.domain.FeatureProperty;
import de.ii.xtraplatform.features.domain.FeatureType;
import de.ii.xtraplatform.features.domain.ImmutableFeatureProperty;
import de.ii.xtraplatform.features.domain.ImmutableFeatureType;
import de.ii.xtraplatform.ogc.api.GML;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import de.ii.xtraplatform.util.xml.XMLPathTracker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

class WfsSchemaAnalyzer implements FeatureProviderSchemaConsumer {

    public static final String GML_NS_URI = GML.getNS(GML.VERSION._2_1_1);

    private final List<FeatureType> featureTypes;
    private final Set<String> mappedPaths;
    private final Map<String, String> crsMap;
    private final XMLNamespaceNormalizer namespaceNormalizer;

    private ImmutableFeatureType.Builder currentFeatureType;
    private String currentLocalName;
    private String currentQualifiedName;
    private XMLPathTracker currentPath;


    WfsSchemaAnalyzer(Map<String, String> crsMap, Map<String, String> namespaces) {
        this.featureTypes = new ArrayList<>();
        this.currentPath = new XMLPathTracker();
        this.mappedPaths = new HashSet<>();
        this.crsMap = crsMap;
        this.namespaceNormalizer = new XMLNamespaceNormalizer(namespaces);
    }

    public List<FeatureType> getFeatureTypes() {
        return featureTypes;
    }

    @Override
    public void analyzeFeatureType(String nsUri, String localName) {
        if (Objects.nonNull(currentFeatureType) && Objects.nonNull(currentLocalName)) {
            featureTypes.add(currentFeatureType.build());
        }
        currentLocalName = localName.toLowerCase();
        currentQualifiedName = namespaceNormalizer.getQualifiedName(nsUri, localName);
        mappedPaths.clear();
        currentPath.clear();

        currentFeatureType = new ImmutableFeatureType.Builder();
        currentFeatureType.name(currentLocalName);
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
                ImmutableFeatureProperty.Builder featureProperty = new ImmutableFeatureProperty.Builder()
                        .name("id")
                        .path(getFullPath(path))
                        .role(FeatureProperty.Role.ID)
                        .type(FeatureProperty.Type.STRING);
                currentFeatureType.putProperties(localName, featureProperty);
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
        ImmutableFeatureProperty.Builder featureProperty = new ImmutableFeatureProperty.Builder();
        if (currentFeatureType != null && !isPathMapped(path)) {
            featureProperty.additionalInfo(ImmutableMap.of("multiple", String.valueOf(isParentMultiple)));
            FeatureProperty.Type featurePropertyType;
            TargetMappingProviderFromGml.GML_TYPE dataType = TargetMappingProviderFromGml.GML_TYPE.fromString(type);

            if (dataType.isValid()) {
                featurePropertyType = getFeaturePropertyType(dataType);
                featureProperty.name(currentPath.toFieldNameGml())
                               .path(getFullPath(path))
                               .type(featurePropertyType);
                currentFeatureType.putProperties(localName, featureProperty);

            } else {
                TargetMappingProviderFromGml.GML_GEOMETRY_TYPE geoType = TargetMappingProviderFromGml.GML_GEOMETRY_TYPE.fromString(type);
                if (geoType.isValid()) {
                    featureProperty.name(currentPath.toFieldNameGml())
                                   .path(getFullPath(path))
                                   .type(FeatureProperty.Type.GEOMETRY);

                    if (crsMap.containsKey(currentLocalName)) {
                        featureProperty.additionalInfo(ImmutableMap.of(
                                "geometryType", geoType.toSimpleFeatureGeometry()
                                                       .toString(),
                                "crs", crsMap.get(currentLocalName),
                                "multiple", String.valueOf(isParentMultiple)));
                    }

                    currentFeatureType.putProperties(localName, featureProperty);
                }
            }
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
            featureTypes.add(currentFeatureType.build());
        }
    }

    private FeatureProperty.Type getFeaturePropertyType(TargetMappingProviderFromGml.GML_TYPE dataType) {

        switch (dataType) {
            case BOOLEAN:
                return FeatureProperty.Type.BOOLEAN;
            case STRING:
                return FeatureProperty.Type.STRING;
            case INT:
            case INTEGER:
            case SHORT:
            case LONG:
                return FeatureProperty.Type.INTEGER;
            case FLOAT:
            case DOUBLE:
            case DECIMAL:
                return FeatureProperty.Type.FLOAT;
            case DATE:
            case DATE_TIME:
                return FeatureProperty.Type.DATETIME;
            default:
                return FeatureProperty.Type.UNKNOWN;
        }
    }

    private String getFullPath(String path) {
        return String.format("/%s/%s", currentQualifiedName, namespaceNormalizer.getPrefixedPath(path));
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
