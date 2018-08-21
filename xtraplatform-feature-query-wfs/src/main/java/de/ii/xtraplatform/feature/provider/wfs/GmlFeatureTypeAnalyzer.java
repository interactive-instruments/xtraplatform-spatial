/**
 * Copyright 2018 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
/**
 * bla
 */
package de.ii.xtraplatform.feature.provider.wfs;

import de.ii.xtraplatform.feature.query.api.TargetMapping;
import de.ii.xtraplatform.feature.transformer.api.FeatureTypeConfigurationOld;
import de.ii.xtraplatform.feature.transformer.api.ImmutableSourcePathMapping;
import de.ii.xtraplatform.feature.transformer.api.ModifiableFeatureTypeMapping;
import de.ii.xtraplatform.feature.transformer.api.ModifiableSourcePathMapping;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml.GML_GEOMETRY_TYPE;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml.GML_TYPE;
import de.ii.xtraplatform.ogc.api.GML;
import de.ii.xtraplatform.util.xml.XMLPathTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author zahnen
 */
public class GmlFeatureTypeAnalyzer {



    public static final String GML_NS_URI = GML.getNS(GML.VERSION._2_1_1);
    private static final Logger LOGGER = LoggerFactory.getLogger(GmlFeatureTypeAnalyzer.class);

    private ModifiableFeatureProviderDataWfs providerDataWfs;
    // TODO: could it be more than one?
    //private FeatureTypeConfigurationOld currentFeatureType;
    private ModifiableFeatureTypeMapping currentFeatureTypeMapping;
    private XMLPathTracker currentPath;
    //private XMLPathTracker currentPathWithoutObjects;
    private Set<String> mappedPaths;
    //private boolean geometryMapped;
    private int geometryCounter;
    private final List<TargetMappingProviderFromGml> mappingProviders;

    public GmlFeatureTypeAnalyzer(ModifiableFeatureProviderDataWfs providerDataWfs, List<TargetMappingProviderFromGml> mappingProviders) {
        this.providerDataWfs = providerDataWfs;
        this.currentPath = new XMLPathTracker();
        //this.currentPathWithoutObjects = new XMLPathTracker();
        this.mappedPaths = new HashSet<>();
        //this.geometryMapped = false;
        this.geometryCounter = -1;
        this.mappingProviders = mappingProviders;
    }

    protected boolean analyzeNamespaceRewrite(String oldNamespace, String newNamespace, String featureTypeName) {
        boolean rewritten = false;

        /*TODO String prefix = providerDataWfs.getWfsAdapter().getNsStore().getNamespacePrefix(oldNamespace);
        if (prefix != null) {
            providerDataWfs.getWfsAdapter().getNsStore().addNamespace(prefix, newNamespace, true);

            String fullName = oldNamespace + ":" + featureTypeName;
            FeatureTypeConfigurationOld wfsProxyFeatureType = providerDataWfs.getFeatureTypes().get(fullName);
            if (wfsProxyFeatureType != null) {
                wfsProxyFeatureType.setNamespace(newNamespace);
                providerDataWfs.getFeatureTypes().remove(fullName);
                fullName = newNamespace + ":" + featureTypeName;
                providerDataWfs.getFeatureTypes().put(fullName, wfsProxyFeatureType);
                rewritten = true;
            }
        }*/

        return rewritten;
    }

    protected void analyzeFeatureType(String nsUri, String localName) {

        // finish former feature type
        // does the service exist yet or are we just building the data here?
        //featureProviderDataBuilder.putMappings(currentFeatureType.getName(), currentFeatureTypeMapping.build());


        if (nsUri.isEmpty()) {
            //LOGGER.error(FrameworkMessages.NSURI_IS_EMPTY);
        }

        String fullName = nsUri + ":" + localName;
        //currentFeatureType = providerDataWfs.getFeatureTypes().get(fullName);
        currentFeatureTypeMapping = ModifiableFeatureTypeMapping.create();
        providerDataWfs.putMappings(localName.toLowerCase(), currentFeatureTypeMapping);

        mappedPaths.clear();
        currentPath.clear();
        //currentPathWithoutObjects.clear();

        //geometryMapped = false;
        this.geometryCounter = -1;

        //providerDataWfs.getWfsAdapter().addNamespace(nsUri);

        ModifiableSourcePathMapping sourcePathMapping = ModifiableSourcePathMapping.create();

        for (TargetMappingProviderFromGml mappingProvider: mappingProviders) {

            TargetMapping targetMapping = mappingProvider.getTargetMappingForFeatureType(nsUri, localName);

            if (targetMapping != null) {
                sourcePathMapping.putMappings(mappingProvider.getTargetType(), targetMapping);

                //currentFeatureType.getMappings().addMapping(fullName, mappingProvider.getTargetType(), targetMapping);
            }
        }
        currentFeatureTypeMapping.putMappings(fullName, sourcePathMapping);
    }

    protected void analyzeAttribute(String nsUri, String localName, String type) {

        // only first level gml:ids
        if (!currentPath.isEmpty()) {
            return;
        }

        //providerDataWfs.getWfsAdapter().addNamespace(nsUri);

        currentPath.track(nsUri, "@" + localName);

        // only gml:id of the feature for now
        // TODO: version
        if ((localName.equals("id") && nsUri.startsWith(GML_NS_URI)) || localName.equals("fid")) {
            String path = currentPath.toString();

            if (currentFeatureTypeMapping != null && !isPathMapped(path)) {

                ModifiableSourcePathMapping sourcePathMapping = ModifiableSourcePathMapping.create();

                for (TargetMappingProviderFromGml mappingProvider: mappingProviders) {

                    TargetMapping targetMapping = mappingProvider.getTargetMappingForAttribute(currentPath.toFieldNameGml(), nsUri, localName, GML_TYPE.ID);

                    if (targetMapping != null) {
                        mappedPaths.add(path);

                        sourcePathMapping.putMappings(mappingProvider.getTargetType(), targetMapping);

                        //currentFeatureType.getMappings().addMapping(path, mappingProvider.getTargetType(), targetMapping);
                    }
                }
                currentFeatureTypeMapping.putMappings(path, sourcePathMapping);
            }
        }
    }

    protected void analyzeProperty(String nsUri, String localName, String type, int depth, boolean isObject) {

        //providerDataWfs.getWfsAdapter().addNamespace(nsUri);

        currentPath.track(nsUri, localName, depth);

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

            ModifiableSourcePathMapping sourcePathMapping = ModifiableSourcePathMapping.create();

            for (TargetMappingProviderFromGml mappingProvider: mappingProviders) {

                TargetMapping targetMapping = null;

                GML_TYPE dataType = GML_TYPE.fromString(type);

                if (dataType.isValid()) {

                    targetMapping = mappingProvider.getTargetMappingForProperty(currentPath.toFieldNameGml(), nsUri, localName, dataType);

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
            currentFeatureTypeMapping.putMappings(path, sourcePathMapping);
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
}
