/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.graphql.app;

import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.features.domain.FeatureProviderCapabilities;
import de.ii.xtraplatform.features.domain.FeatureProviderCapabilities.Level;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector.QueryOptions;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureQueryEncoder;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureSchemaBase;
import de.ii.xtraplatform.features.domain.ImmutableFeatureProviderCapabilities;
import de.ii.xtraplatform.features.domain.Query;
import de.ii.xtraplatform.features.domain.TypeQuery;
import de.ii.xtraplatform.features.graphql.domain.ConnectionInfoGraphQlHttp;
import de.ii.xtraplatform.features.graphql.domain.FeatureProviderGraphQlData.QueryGeneratorSettings;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureQueryEncoderGraphQl implements FeatureQueryEncoder<String, QueryOptions> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureQueryEncoderGraphQl.class);

  private final Map<String, FeatureSchema> featureSchemas;
  private final QueryGeneratorSettings queryGeneration;
  private final EpsgCrs nativeCrs;
  private final FilterEncoderGraphQl filterEncoder;

  public FeatureQueryEncoderGraphQl(
      Map<String, FeatureSchema> featureSchemas,
      ConnectionInfoGraphQlHttp connectionInfo,
      QueryGeneratorSettings queryGeneration,
      EpsgCrs nativeCrs,
      CrsTransformerFactory crsTransformerFactory,
      Cql cql) {
    this.featureSchemas = featureSchemas;
    this.queryGeneration = queryGeneration;
    this.nativeCrs = nativeCrs;
    this.filterEncoder = new FilterEncoderGraphQl(nativeCrs, crsTransformerFactory, cql);
  }

  @Override
  public String encode(Query query, Map<String, String> additionalQueryParameters) {
    if (query instanceof FeatureQuery) {
      return encodeFeatureQuery((FeatureQuery) query, additionalQueryParameters);
    }

    throw new IllegalArgumentException();
  }

  @Override
  public QueryOptions getOptions(TypeQuery typeQuery, Query query) {
    return new QueryOptions() {};
  }

  @Override
  public FeatureProviderCapabilities getCapabilities() {
    // TODO: derive from queryGeneration
    return ImmutableFeatureProviderCapabilities.builder().level(Level.BASIC).build();
  }

  public boolean isValid(final FeatureQuery query) {
    return featureSchemas.containsKey(query.getType());
  }

  public String getTypeName(final FeatureQuery query) {
    FeatureSchema featureSchema = featureSchemas.get(query.getType());
    String name =
        featureSchema.getSourcePath().map(sourcePath -> sourcePath.substring(1)).orElse(null);

    return name;
  }

  public String getFields(FeatureSchema featureSchema, String indentation) {
    return featureSchema.getProperties().stream()
        .filter(
            s ->
                s.getScope().orElse(FeatureSchemaBase.Scope.QUERIES)
                    == FeatureSchemaBase.Scope.QUERIES)
        .map(
            prop -> {
              if (prop.isValue()) {
                return prop.getSourcePath();
              } else if (prop.isObject()) {
                return prop.getSourcePath()
                    .map(obj -> obj + " " + getFields(prop, indentation + "  "));
              }

              return Optional.<String>empty();
            })
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(
            Collectors.joining(
                "\\n  " + indentation, "{\\n  " + indentation, "\\n" + indentation + "}"));
  }

  public String encodeFeatureQuery(
      FeatureQuery query, Map<String, String> additionalQueryParameters) {
    String queryTemplate = "{\\n  %s %s\\n}";

    String fields = getFields(featureSchemas.get(query.getType()), "  ");

    String q =
        String.format(queryTemplate, queryGeneration.getCollection(getTypeName(query)), fields);

    if (query.returnsSingleFeature() && query.getFilter().isPresent()) {
      Map<String, String> filter =
          filterEncoder.encode(query.getFilter().get(), featureSchemas.get(query.getType()));
      String filter2 =
          filter.entrySet().stream()
              .map(entry -> String.format("%s: \\\"%s\\\"", entry.getKey(), entry.getValue()))
              .collect(Collectors.joining(","));

      q =
          String.format(
              queryTemplate,
              queryGeneration.getSingle(getTypeName(query)) + "(" + filter2 + ")",
              fields);
    }

    return "{\"query\":\"" + q + "\"}";
  }
}
