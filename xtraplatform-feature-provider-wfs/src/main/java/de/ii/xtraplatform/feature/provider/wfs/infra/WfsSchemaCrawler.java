/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.feature.provider.wfs.infra;

import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.feature.provider.api.AbstractFeatureProviderMetadataConsumer;
import de.ii.xtraplatform.feature.provider.api.FeatureProviderMetadataConsumer;
import de.ii.xtraplatform.feature.provider.api.FeatureProviderSchemaConsumer;
import de.ii.xtraplatform.feature.provider.wfs.FeatureProviderDataWfsFromMetadata;
import de.ii.xtraplatform.feature.provider.wfs.GMLSchemaParser;
import de.ii.xtraplatform.feature.provider.wfs.WFSCapabilitiesParser;
import de.ii.xtraplatform.feature.provider.wfs.domain.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.feature.transformer.api.TargetMappingProviderFromGml;
import de.ii.xtraplatform.features.domain.FeaturePropertyV2;
import de.ii.xtraplatform.features.domain.FeatureTypeV2;
import de.ii.xtraplatform.features.domain.ImmutableFeaturePropertyV2;
import de.ii.xtraplatform.features.domain.ImmutableFeatureTypeV2;
import de.ii.xtraplatform.ogc.api.GML;
import de.ii.xtraplatform.ogc.api.WFS;
import de.ii.xtraplatform.ogc.api.wfs.DescribeFeatureType;
import de.ii.xtraplatform.ogc.api.wfs.GetCapabilities;
import de.ii.xtraplatform.ogc.api.wfs.WfsRequestEncoder;
import de.ii.xtraplatform.scheduler.api.TaskProgress;
import de.ii.xtraplatform.util.xml.XMLNamespaceNormalizer;
import de.ii.xtraplatform.util.xml.XMLPathTracker;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.staxmate.SMInputFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class WfsSchemaCrawler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WfsSchemaCrawler.class);
    private static SMInputFactory staxFactory = new SMInputFactory(new InputFactoryImpl());
    private final ConnectionInfoWfsHttp connectionInfo;
    private final WfsRequestEncoder wfsRequestEncoder;

    public WfsSchemaCrawler(ConnectionInfoWfsHttp connectionInfo) {
        this.connectionInfo = connectionInfo;

        this.wfsRequestEncoder = new WfsRequestEncoder();
        wfsRequestEncoder.setVersion(connectionInfo.getVersion());
        wfsRequestEncoder.setGmlVersion(connectionInfo.getGmlVersion());
        wfsRequestEncoder.setUrls(ImmutableMap.of("default", ImmutableMap.of(WFS.METHOD.GET, FeatureProviderDataWfsFromMetadata.parseAndCleanWfsUrl(connectionInfo.getUri()), WFS.METHOD.POST, FeatureProviderDataWfsFromMetadata.parseAndCleanWfsUrl(connectionInfo.getUri()))));
        wfsRequestEncoder.setNsStore(new XMLNamespaceNormalizer(connectionInfo.getNamespaces()));
    }

    public List<FeatureTypeV2> parseSchema() {

        MetadataConsumer metadataConsumer = new MetadataConsumer();
        analyzeFeatureTypesWithDescribeGetCapabilities(metadataConsumer);
        Map<String, String> crsMap = metadataConsumer.getCrsMap();
        Map<String, QName> featureTypes = metadataConsumer.getFeatureTypes();
        SchemaConsumer schemaConsumer = new SchemaConsumer(crsMap);
        analyzeFeatureTypes(schemaConsumer, featureTypes, new TaskProgressNoop());

        return schemaConsumer.getFeatureTypes();
    }

    private void analyzeFeatureTypes(FeatureProviderSchemaConsumer schemaConsumer, Map<String, QName> featureTypes,
                                     TaskProgress taskProgress) {
        Map<String, List<String>> featureTypesByNamespace = getSupportedFeatureTypesPerNamespace(featureTypes);

        if (!featureTypesByNamespace.isEmpty()) {
            analyzeFeatureTypesWithDescribeFeatureType(schemaConsumer, featureTypesByNamespace, taskProgress);
        }
    }

    private void analyzeFeatureTypesWithDescribeFeatureType(FeatureProviderSchemaConsumer schemaConsumer,
                                                            Map<String, List<String>> featureTypesByNamespace,
                                                            TaskProgress taskProgress) {

        HttpClient httpClient = new DefaultHttpClient();
        URI baseUri = connectionInfo.getUri();
        String requestUrl = wfsRequestEncoder.getAsUrl(new DescribeFeatureType());

        try {
            HttpResponse response = httpClient.execute(new HttpGet(requestUrl));
            InputStream inputStream = response.getEntity()
                                              .getContent();

            GMLSchemaParser gmlSchemaParser = new GMLSchemaParser(ImmutableList.of(schemaConsumer), baseUri);
            gmlSchemaParser.parse(inputStream, featureTypesByNamespace, taskProgress);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void analyzeFeatureTypesWithDescribeGetCapabilities(FeatureProviderMetadataConsumer metadataConsumer) {

        HttpClient httpClient = new DefaultHttpClient();
        String requestUrl = wfsRequestEncoder.getAsUrl(new GetCapabilities());

        try {
            HttpResponse response = httpClient.execute(new HttpGet(requestUrl));
            InputStream inputStream = response.getEntity()
                    .getContent();

            WFSCapabilitiesParser gmlSchemaParser = new WFSCapabilitiesParser(metadataConsumer, staxFactory);
            gmlSchemaParser.parse(inputStream);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, List<String>> getSupportedFeatureTypesPerNamespace(Map<String, QName> featureTypes) {
        Map<String, List<String>> featureTypesPerNamespace = new HashMap<>();

        for (QName featureType : featureTypes.values()) {
            if (!featureTypesPerNamespace.containsKey(featureType.getNamespaceURI())) {
                featureTypesPerNamespace.put(featureType.getNamespaceURI(), new ArrayList<>());
            }
            featureTypesPerNamespace.get(featureType.getNamespaceURI())
                                    .add(featureType.getLocalPart());
        }

        return featureTypesPerNamespace;
    }

    static class SchemaConsumer implements FeatureProviderSchemaConsumer {

        public static final String GML_NS_URI = GML.getNS(GML.VERSION._2_1_1);

        private final List<FeatureTypeV2> featureTypes;
        private ImmutableFeatureTypeV2.Builder currentFeatureType;
        private String currentLocalName;
        private XMLPathTracker currentPath;
        private Set<String> mappedPaths;
        private Map<String, String> crsMap;


        SchemaConsumer(Map<String, String> crsMap) {
            this.featureTypes = new ArrayList<>();
            this.currentPath = new XMLPathTracker();
            this.mappedPaths = new HashSet<>();
            this.crsMap = crsMap;
        }

        public List<FeatureTypeV2> getFeatureTypes() {
            return featureTypes;
        }

        @Override
        public void analyzeFeatureType(String nsUri, String localName) {
            if (Objects.nonNull(currentFeatureType) && Objects.nonNull(currentLocalName)) {
                featureTypes.add(currentFeatureType.build());
            }
            currentLocalName = localName.toLowerCase();
            mappedPaths.clear();
            currentPath.clear();

            currentFeatureType = new ImmutableFeatureTypeV2.Builder();
            currentFeatureType.name(localName);
            currentFeatureType.path("/" + localName.toLowerCase());
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
                    ImmutableFeaturePropertyV2.Builder featureProperty = new ImmutableFeaturePropertyV2.Builder()
                            .name(localName)
                            .path(currentPath.toFieldNameGml())
                            .role(FeaturePropertyV2.Role.ID)
                            .type(FeaturePropertyV2.Type.STRING);
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
            ImmutableFeaturePropertyV2.Builder featureProperty = new ImmutableFeaturePropertyV2.Builder();
            if (currentFeatureType != null && !isPathMapped(path)) {
                featureProperty.additionalInfo(ImmutableMap.of("multiple", String.valueOf(isParentMultiple)));
                FeaturePropertyV2.Type featurePropertyType;
                TargetMappingProviderFromGml.GML_TYPE dataType = TargetMappingProviderFromGml.GML_TYPE.fromString(type);
                if (dataType.isValid()) {
                    featurePropertyType = getFeaturePropertyType(dataType);
                    featureProperty.name(localName)
                            .path(localName)
                            .type(featurePropertyType);
                    currentFeatureType.putProperties(localName, featureProperty);

                } else {
                    TargetMappingProviderFromGml.GML_GEOMETRY_TYPE geoType = TargetMappingProviderFromGml.GML_GEOMETRY_TYPE.fromString(type);
                    if (geoType.isValid()) {
                        featureProperty.name(localName)
                                .path(localName)
                                .type(FeaturePropertyV2.Type.GEOMETRY);
                        if (crsMap.containsKey(currentLocalName)) {
                            featureProperty.additionalInfo(ImmutableMap.of(
                                    "geometryType", geoType.toSimpleFeatureGeometry().toString(),
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

        private FeaturePropertyV2.Type getFeaturePropertyType(TargetMappingProviderFromGml.GML_TYPE dataType) {

            switch (dataType) {
                case BOOLEAN:
                    return FeaturePropertyV2.Type.BOOLEAN;
                case STRING:
                    return FeaturePropertyV2.Type.STRING;
                case INT:
                case INTEGER:
                case SHORT:
                case LONG:
                    return FeaturePropertyV2.Type.INTEGER;
                case FLOAT:
                case DOUBLE:
                case DECIMAL:
                    return FeaturePropertyV2.Type.FLOAT;
                case DATE:
                case DATE_TIME:
                    return FeaturePropertyV2.Type.DATETIME;
                default:
                    return FeaturePropertyV2.Type.UNKNOWN;
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

    static class MetadataConsumer extends AbstractFeatureProviderMetadataConsumer {

        private final XMLNamespaceNormalizer namespaceNormalizer;
        private final Map<String, String> crsMap;
        private final Map<String, QName> featureTypes;
        private String currentFeatureTypeName;
        private String currentCrs;

        MetadataConsumer() {
            this.crsMap = new HashMap<>();
            this.featureTypes = new HashMap<>();
            this.namespaceNormalizer = new XMLNamespaceNormalizer();
        }

        public Map<String, String> getCrsMap() {
            return crsMap;
        }

        public Map<String, QName> getFeatureTypes() {
            return featureTypes;
        }

        @Override
        public void analyzeNamespace(String prefix, String uri) {
            if (!namespaceNormalizer.getNamespaces().containsKey(prefix)) {
                namespaceNormalizer.addNamespace(prefix, uri);
            }
        }

        @Override
        public void analyzeFeatureType(String featureTypeName) {
            if (featureTypeName.contains(":")) {
                String[] name = featureTypeName.split(":");
                String namespace = namespaceNormalizer.getNamespaceURI(name[0]);
                currentFeatureTypeName = name[1];
                featureTypes.put(currentFeatureTypeName.toLowerCase(), new QName(namespace, currentFeatureTypeName, name[0]));
            }
        }

        @Override
        public void analyzeFeatureTypeDefaultCrs(String featureTypeName, String crs) {
            if (Objects.nonNull(currentFeatureTypeName)) {
                currentCrs = namespaceNormalizer.getLocalName(crs);
            }
            crsMap.put(currentFeatureTypeName.toLowerCase(), currentCrs);
        }

    }

    static class TaskProgressNoop implements TaskProgress {

        @Override
        public void setStatusMessage(String statusMessage) {

        }

        @Override
        public void setCompleteness(double completeness) {

        }
    }
}
