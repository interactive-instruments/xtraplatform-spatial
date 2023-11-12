/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.gml.infra;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureProviderSchemaConsumer;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.Metadata;
import de.ii.xtraplatform.features.gml.domain.ConnectionInfoWfsHttp;
import de.ii.xtraplatform.features.gml.domain.ImmutableConnectionInfoWfsHttp;
import de.ii.xtraplatform.features.gml.domain.WfsClientBasic;
import de.ii.xtraplatform.features.gml.infra.req.DescribeFeatureType;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WfsSchemaCrawler {
  private static final Logger LOGGER = LoggerFactory.getLogger(WfsSchemaCrawler.class);

  private WfsClientBasic wfsClient;
  private final ConnectionInfoWfsHttp connectionInfo;

  public WfsSchemaCrawler(WfsClientBasic wfsClient, ConnectionInfoWfsHttp connectionInfo) {
    this.wfsClient = wfsClient;
    this.connectionInfo = connectionInfo;
  }

  public ConnectionInfoWfsHttp completeConnectionInfo() {
    Optional<Metadata> metadata = wfsClient.getMetadata();

    if (metadata.isPresent()) {
      String version = metadata.flatMap(Metadata::getVersion).orElse(connectionInfo.getVersion());
      Map<String, String> namespaces =
          metadata.map(Metadata::getNamespaces).orElse(ImmutableMap.of());

      return new ImmutableConnectionInfoWfsHttp.Builder()
          .from(connectionInfo)
          .version(version)
          .namespaces(namespaces)
          .build();
    }

    return connectionInfo;
  }

  public Optional<EpsgCrs> getNativeCrs() {
    Optional<Metadata> metadata = wfsClient.getMetadata();

    if (metadata.isPresent()) {
      return metadata.get().getFeatureTypesCrs().values().stream()
          .findFirst()
          .map(EpsgCrs::fromString);
    }

    return Optional.empty();
  }

  public List<FeatureSchema> parseSchema(
      Map<String, List<String>> types, Consumer<Map<String, List<String>>> tracker) {
    Optional<Metadata> metadata = wfsClient.getMetadata();

    List<QName> featureTypes =
        metadata.map(Metadata::getFeatureTypes).orElse(ImmutableList.of()).stream()
            .filter(
                qn -> {
                  String ns = Objects.requireNonNullElse(qn.getPrefix(), qn.getNamespaceURI());

                  return types.containsKey(ns) && types.get(ns).contains(qn.getLocalPart());
                })
            .collect(Collectors.toList());

    Map<String, String> namespaces =
        metadata.map(Metadata::getNamespaces).orElse(ImmutableMap.of());

    WfsSchemaAnalyzer schemaConsumer = new WfsSchemaAnalyzer(featureTypes, namespaces);
    analyzeFeatureTypes(schemaConsumer, featureTypes, namespaces, tracker);

    return schemaConsumer.getFeatureTypes();
  }

  private void analyzeFeatureTypes(
      FeatureProviderSchemaConsumer schemaConsumer,
      List<QName> featureTypes,
      Map<String, String> namespaces,
      Consumer<Map<String, List<String>>> tracker) {
    Map<String, List<String>> featureTypesByNamespace =
        getSupportedFeatureTypesPerNamespace(featureTypes);

    if (!featureTypesByNamespace.isEmpty()) {
      analyzeFeatureTypesWithDescribeFeatureType(
          schemaConsumer, featureTypesByNamespace, namespaces, tracker);
    }
  }

  private void analyzeFeatureTypesWithDescribeFeatureType(
      FeatureProviderSchemaConsumer schemaConsumer,
      Map<String, List<String>> featureTypesByNamespace,
      Map<String, String> namespaces,
      Consumer<Map<String, List<String>>> tracker) {

    URI baseUri = connectionInfo.getUri();
    InputStream inputStream = wfsClient.runWfsOperation(new DescribeFeatureType());

    GMLSchemaParser gmlSchemaParser =
        new GMLSchemaParser(ImmutableList.of(schemaConsumer), baseUri, namespaces);
    gmlSchemaParser.parse(inputStream, featureTypesByNamespace, tracker);
  }

  private Map<String, List<String>> getSupportedFeatureTypesPerNamespace(List<QName> featureTypes) {
    Map<String, List<String>> featureTypesPerNamespace = new LinkedHashMap<>();

    for (QName featureType : featureTypes) {
      if (!featureTypesPerNamespace.containsKey(featureType.getNamespaceURI())) {
        featureTypesPerNamespace.put(featureType.getNamespaceURI(), new ArrayList<>());
      }
      featureTypesPerNamespace.get(featureType.getNamespaceURI()).add(featureType.getLocalPart());
    }

    return featureTypesPerNamespace;
  }
}
