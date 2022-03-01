/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.gml.infra;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.features.domain.FeatureProviderSchemaConsumer;
import de.ii.xtraplatform.features.gml.GMLSchemaParser;
import de.ii.xtraplatform.features.gml.domain.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.features.gml.domain.ImmutableConnectionInfoWfsHttp;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.Metadata;
import de.ii.xtraplatform.features.gml.app.request.DescribeFeatureType;
import de.ii.xtraplatform.services.domain.TaskProgress;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WfsSchemaCrawler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WfsSchemaCrawler.class);

    private WfsConnectorHttp connector;
    private final ConnectionInfoWfsHttp connectionInfo;

    public WfsSchemaCrawler(WfsConnectorHttp connector, ConnectionInfoWfsHttp connectionInfo) {
        this.connector = connector;
        this.connectionInfo = connectionInfo;
    }

    public ConnectionInfoWfsHttp completeConnectionInfo() {
        Optional<Metadata> metadata = connector.getMetadata();

        if (metadata.isPresent()) {
            String version = metadata.flatMap(Metadata::getVersion)
                                     .orElse(connectionInfo.getVersion());
            Map<String,String> namespaces = metadata.map(Metadata::getNamespaces).orElse(ImmutableMap.of());

            return new ImmutableConnectionInfoWfsHttp.Builder()
                    .from(connectionInfo)
                    .version(version)
                    .namespaces(namespaces)
                    .build();
    }

        return connectionInfo;
    }

    public List<FeatureSchema> parseSchema() {
        Optional<Metadata> metadata = connector.getMetadata();

        List<QName> featureTypes = metadata.map(Metadata::getFeatureTypes).orElse(ImmutableList.of());
        Map<QName, String> crsMap = metadata.map(Metadata::getFeatureTypesCrs).orElse(ImmutableMap.of());
        Map<String, String> namespaces = metadata.map(Metadata::getNamespaces).orElse(ImmutableMap.of());

        WfsSchemaAnalyzer schemaConsumer = new WfsSchemaAnalyzer(featureTypes, crsMap, namespaces);
        analyzeFeatureTypes(schemaConsumer, featureTypes, new TaskProgressNoop());

        return schemaConsumer.getFeatureTypes();
    }

    private void analyzeFeatureTypes(FeatureProviderSchemaConsumer schemaConsumer, List<QName> featureTypes,
                                     TaskProgress taskProgress) {
        Map<String, List<String>> featureTypesByNamespace = getSupportedFeatureTypesPerNamespace(featureTypes);

        if (!featureTypesByNamespace.isEmpty()) {
            analyzeFeatureTypesWithDescribeFeatureType(schemaConsumer, featureTypesByNamespace, taskProgress);
        }
    }

    private void analyzeFeatureTypesWithDescribeFeatureType(FeatureProviderSchemaConsumer schemaConsumer,
                                                            Map<String, List<String>> featureTypesByNamespace,
                                                            TaskProgress taskProgress) {

        URI baseUri = connectionInfo.getUri();
        InputStream inputStream = connector.runWfsOperation(new DescribeFeatureType());

        GMLSchemaParser gmlSchemaParser = new GMLSchemaParser(ImmutableList.of(schemaConsumer), baseUri);
        gmlSchemaParser.parse(inputStream, featureTypesByNamespace, taskProgress);
    }

    private Map<String, List<String>> getSupportedFeatureTypesPerNamespace(List<QName> featureTypes) {
        Map<String, List<String>> featureTypesPerNamespace = new LinkedHashMap<>();

        for (QName featureType : featureTypes) {
            if (!featureTypesPerNamespace.containsKey(featureType.getNamespaceURI())) {
                featureTypesPerNamespace.put(featureType.getNamespaceURI(), new ArrayList<>());
            }
            featureTypesPerNamespace.get(featureType.getNamespaceURI())
                                    .add(featureType.getLocalPart());
        }

        return featureTypesPerNamespace;
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
