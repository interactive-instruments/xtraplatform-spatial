/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.feature.provider.wfs;

import de.ii.xtraplatform.feature.provider.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureProviderDataTransformer;
import de.ii.xtraplatform.feature.transformer.api.ImmutableFeatureProviderDataTransformer;
import de.ii.xtraplatform.feature.transformer.api.ImmutableFeatureTypeMapping;
import de.ii.xtraplatform.feature.transformer.api.ImmutableSourcePathMapping;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml.GML_GEOMETRY_TYPE;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml.GML_TYPE;
import de.ii.xtraplatform.ogc.api.GML;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import de.ii.xtraplatform.util.xml.XMLPathTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author zahnen
 */
public class GmlFeatureTypeAnalyzer {



    public static final String GML_NS_URI = GML.getNS(GML.VERSION._2_1_1);
    private static final Logger LOGGER = LoggerFactory.getLogger(GmlFeatureTypeAnalyzer.class);

    protected final FeatureProviderDataTransformer providerDataWfs;
    protected final ImmutableFeatureProviderDataTransformer.Builder dataBuilder;
    // TODO: could it be more than one?
    //private FeatureTypeConfigurationOld currentFeatureType;
    private ImmutableFeatureTypeMapping.Builder currentFeatureTypeMapping;
    private XMLPathTracker currentPath;
    private String currentLocalName;
    //private XMLPathTracker currentPathWithoutObjects;
    private Set<String> mappedPaths;
    //private boolean geometryMapped;
    private int geometryCounter;
    private final List<TargetMappingProviderFromGml> mappingProviders;
    private final XMLNamespaceNormalizer namespaceNormalizer;

    public GmlFeatureTypeAnalyzer(FeatureProviderDataTransformer data,
                                  ImmutableFeatureProviderDataTransformer.Builder dataBuilder,
                                  List<TargetMappingProviderFromGml> mappingProviders) {
        this.providerDataWfs = data;
        this.dataBuilder = dataBuilder;
        this.currentPath = new XMLPathTracker();
        //this.currentPathWithoutObjects = new XMLPathTracker();
        this.mappedPaths = new HashSet<>();
        //this.geometryMapped = false;
        this.geometryCounter = -1;
        this.mappingProviders = mappingProviders;
        this.namespaceNormalizer = new XMLNamespaceNormalizer(((ConnectionInfoWfsHttp)providerDataWfs.getConnectionInfo()).getNamespaces());
    }

    protected boolean analyzeNamespaceRewrite(String oldNamespace, String newNamespace, String featureTypeName) {
        boolean rewritten = false;

        String prefix = namespaceNormalizer.getNamespacePrefix(oldNamespace);
        if (prefix != null) {
            namespaceNormalizer.addNamespace(prefix, newNamespace, true);

            QName newQualifiedName = new QName(newNamespace, featureTypeName);
            if (providerDataWfs.getLocalFeatureTypeNames().containsKey(featureTypeName)) {
                providerDataWfs.getLocalFeatureTypeNames().put(featureTypeName, newQualifiedName);
                rewritten = true;
            }
        }

        return rewritten;
    }

    protected void analyzeSuccess() {
        // finish last feature type
        if (Objects.nonNull(currentFeatureTypeMapping) && Objects.nonNull(currentLocalName)) {
            dataBuilder.putMappings(currentLocalName, currentFeatureTypeMapping.build());
        }
    }

    protected void analyzeFeatureType(String nsUri, String localName) {

        // finish former feature type
        // does the service exist yet or are we just building the data here?
        //featureProviderDataBuilder.putMappings(currentFeatureType.getName(), currentFeatureTypeMapping.build());

        if (Objects.nonNull(currentFeatureTypeMapping) && Objects.nonNull(currentLocalName)) {
            dataBuilder.putMappings(currentLocalName, currentFeatureTypeMapping.build());
        }


        if (nsUri.isEmpty()) {
            //LOGGER.error(FrameworkMessages.NSURI_IS_EMPTY);
        }

        String fullName = nsUri + ":" + localName;
        //currentFeatureType = providerDataWfs.getFeatureTypes().get(fullName);
        currentFeatureTypeMapping = new ImmutableFeatureTypeMapping.Builder();
        currentLocalName = localName.toLowerCase();

        mappedPaths.clear();
        currentPath.clear();
        //currentPathWithoutObjects.clear();

        //geometryMapped = false;
        this.geometryCounter = -1;

        namespaceNormalizer.addNamespace(nsUri);

        ImmutableSourcePathMapping.Builder sourcePathMapping = new ImmutableSourcePathMapping.Builder();

        for (TargetMappingProviderFromGml mappingProvider: mappingProviders) {

            TargetMapping targetMapping = mappingProvider.getTargetMappingForFeatureType(nsUri, localName);

            if (targetMapping != null) {
                sourcePathMapping.putMappings(mappingProvider.getTargetType(), targetMapping);

                //currentFeatureType.getMappings().addMapping(fullName, mappingProvider.getTargetType(), targetMapping);
            }
        }
        currentFeatureTypeMapping.putMappings(fullName, sourcePathMapping.build());

    }

    protected void analyzeAttribute(String nsUri, String localName, String type) {

        // only first level gml:ids
        if (!currentPath.isEmpty()) {
            return;
        }

        namespaceNormalizer.addNamespace(nsUri);

        currentPath.track(nsUri, "@" + localName, false);

        // only gml:id of the feature for now
        // TODO: version
        if ((localName.equals("id") && nsUri.startsWith(GML_NS_URI)) || localName.equals("fid")) {
            String path = currentPath.toString();

            if (currentFeatureTypeMapping != null && !isPathMapped(path)) {

                ImmutableSourcePathMapping.Builder sourcePathMapping = new ImmutableSourcePathMapping.Builder();

                for (TargetMappingProviderFromGml mappingProvider: mappingProviders) {

                    TargetMapping targetMapping = mappingProvider.getTargetMappingForAttribute(currentPath.toFieldNameGml(), nsUri, localName, GML_TYPE.ID);

                    if (targetMapping != null) {
                        mappedPaths.add(path);

                        sourcePathMapping.putMappings(mappingProvider.getTargetType(), targetMapping);

                        //currentFeatureType.getMappings().addMapping(path, mappingProvider.getTargetType(), targetMapping);
                    }
                }
                currentFeatureTypeMapping.putMappings(path, sourcePathMapping.build());
            }
        }
    }

    protected void analyzeProperty(String nsUri, String localName, String type, int depth, boolean isObject,
                                   boolean isMultiple) {

        namespaceNormalizer.addNamespace(nsUri);

        currentPath.track(nsUri, localName, depth, isMultiple);

        /*if (!isObject) {
            currentPathWithoutObjects.track(nsUri, localName, depth);
        } else {
            currentPathWithoutObjects.track(null, null, depth);
        }*/

        String path = currentPath.toString();

        // TODO: version
        // skip first level gml properties
        if (path.startsWith(GML_NS_URI)) {
            return;
        }

        if (currentFeatureTypeMapping != null && !isPathMapped(path)) {

            ImmutableSourcePathMapping.Builder sourcePathMapping = new ImmutableSourcePathMapping.Builder();

            for (TargetMappingProviderFromGml mappingProvider: mappingProviders) {

                TargetMapping targetMapping = null;

                GML_TYPE dataType = GML_TYPE.fromString(type);

                if (dataType.isValid()) {

                    targetMapping = mappingProvider.getTargetMappingForProperty(currentPath.toFieldNameGml(), nsUri, localName, dataType, isMultiple);

                } else {

                    GML_GEOMETRY_TYPE geoType = GML_GEOMETRY_TYPE.fromString(type);

                    if (geoType.isValid()) {

                        targetMapping = mappingProvider.getTargetMappingForGeometry(currentPath.toFieldNameGml(), nsUri, localName, geoType);
                    } else {
                        LOGGER.debug("NOT MAPPED {} {}", currentPath.toFieldNameGml(), type);
                    }
                }

                if (targetMapping != null) {
                    mappedPaths.add(path);

                    sourcePathMapping.putMappings(mappingProvider.getTargetType(), targetMapping);
                    //currentFeatureType.getMappings().addMapping(path, mappingProvider.getTargetType(), targetMapping);
                }
            }
            if (!sourcePathMapping.build().getMappings().isEmpty()) {
                currentFeatureTypeMapping.putMappings(path, sourcePathMapping.build());
            }
        }
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

    protected Map<String,String> getNamespaces() {
        return namespaceNormalizer.getNamespaces();
    }
}
