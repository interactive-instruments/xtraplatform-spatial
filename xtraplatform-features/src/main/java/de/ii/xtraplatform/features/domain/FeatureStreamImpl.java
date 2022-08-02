/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.domain;

import static de.ii.xtraplatform.features.domain.transform.FeaturePropertyTransformerDateFormat.DATETIME_FORMAT;
import static de.ii.xtraplatform.features.domain.transform.FeaturePropertyTransformerDateFormat.DATE_FORMAT;
import static de.ii.xtraplatform.features.domain.transform.PropertyTransformations.WILDCARD;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.streams.domain.Reactive;
import de.ii.xtraplatform.streams.domain.Reactive.Stream;
import de.ii.xtraplatform.web.domain.ETag;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

public class FeatureStreamImpl implements FeatureStream {

  private final FeatureQuery query;
  private final FeatureProviderDataV2 data;
  private final CrsTransformerFactory crsTransformerFactory;
  private final Map<String, Codelist> codelists;
  private final QueryRunner runner;
  private final boolean doTransform;

  public FeatureStreamImpl(
      FeatureQuery query,
      FeatureProviderDataV2 data,
      CrsTransformerFactory crsTransformerFactory,
      Map<String, Codelist> codelists,
      QueryRunner runner,
      boolean doTransform) {
    this.query = query;
    this.data = data;
    this.crsTransformerFactory = crsTransformerFactory;
    this.codelists = codelists;
    this.runner = runner;
    this.doTransform = doTransform;
  }

  @Override
  public CompletionStage<Result> runWith(
      Reactive.Sink<Object> sink, Optional<PropertyTransformations> propertyTransformations) {

    BiFunction<FeatureTokenSource, Map<String, String>, Stream<Result>> stream =
        (tokenSource, virtualTables) -> {
          FeatureTokenSource source =
              doTransform
                  ? getFeatureTokenSourceTransformed(tokenSource, propertyTransformations)
                  : tokenSource;
          ImmutableResult.Builder resultBuilder = ImmutableResult.builder();
          final ETag.Incremental eTag = ETag.incremental();
          final boolean strongETag =
              query.getETag().filter(type -> type == ETag.Type.STRONG).isPresent();

          if (query.getETag().filter(type -> type == ETag.Type.WEAK).isPresent()) {
            source = source.via(new FeatureTokenTransformerWeakETag(resultBuilder));
          }

          Reactive.BasicStream<?, Void> basicStream =
              sink instanceof Reactive.SinkTransformed
                  ? source.to((Reactive.SinkTransformed<Object, ?>) sink)
                  : source.to(sink);

          return basicStream
              .withResult(resultBuilder.isEmpty(true))
              .handleError(ImmutableResult.Builder::error)
              .handleItem(
                  (builder, x) -> {
                    if (strongETag && x instanceof byte[]) {
                      eTag.put((byte[]) x);
                    }
                    return builder.isEmpty(x instanceof byte[] ? ((byte[]) x).length <= 0 : false);
                  })
              .handleEnd(
                  (ImmutableResult.Builder builder1) -> {
                    if (strongETag) {
                      builder1.eTag(eTag.build(ETag.Type.STRONG));
                    }
                    return builder1.build();
                  });
        };

    return runner.runQuery(stream, query, !doTransform);
  }

  @Override
  public <X> CompletionStage<ResultReduced<X>> runWith(
      Reactive.SinkReduced<Object, X> sink,
      Optional<PropertyTransformations> propertyTransformations) {

    BiFunction<FeatureTokenSource, Map<String, String>, Reactive.Stream<ResultReduced<X>>> stream =
        (tokenSource, virtualTables) -> {
          FeatureTokenSource source =
              doTransform
                  ? getFeatureTokenSourceTransformed(tokenSource, propertyTransformations)
                  : tokenSource;
          ImmutableResultReduced.Builder<X> resultBuilder = ImmutableResultReduced.<X>builder();
          final ETag.Incremental eTag = ETag.incremental();
          final boolean strongETag =
              query.getETag().filter(type -> type == ETag.Type.STRONG).isPresent();

          if (query.getETag().filter(type -> type == ETag.Type.WEAK).isPresent()) {
            source = source.via(new FeatureTokenTransformerWeakETag(resultBuilder));
          }

          Reactive.BasicStream<?, X> basicStream =
              sink instanceof Reactive.SinkReducedTransformed
                  ? source.to((Reactive.SinkReducedTransformed<Object, ?, X>) sink)
                  : source.to(sink);

          return basicStream
              .withResult(resultBuilder.isEmpty(true))
              .handleError(ImmutableResultReduced.Builder::error)
              .handleItem(
                  (builder, x) -> {
                    if (strongETag && x instanceof byte[]) {
                      eTag.put((byte[]) x);
                    }
                    return builder
                        .reduced((X) x)
                        .isEmpty(x instanceof byte[] && ((byte[]) x).length <= 0);
                  })
              .handleEnd(
                  (ImmutableResultReduced.Builder<X> xBuilder) -> {
                    if (strongETag) {
                      xBuilder.eTag(eTag.build(ETag.Type.STRONG));
                    }
                    return xBuilder.build();
                  });
        };

    return runner.runQuery(stream, query, !doTransform);
  }

  // TODO multi
  // - QueryEncoder
  // - providerTransformations with full path, or map with type key and then matching with current
  // targetSchema?
  // - SchemaMapping with multiple schemas in decoded (maybe attach mapping to row, set in
  // onValueRow)
  // - query in GenericContext (use QueryParametersGeoAndPaging, for derived projection methods,
  // maybe get matching query using current targetSchema)
  // - QueryParameters(Presentation|Generic) + QueryParametersTyped

  private FeatureTokenSource getFeatureTokenSourceTransformed(
      FeatureTokenSource featureTokenSource,
      Optional<PropertyTransformations> propertyTransformations) {
    FeatureSchema featureSchema = data.getTypes().get(query.getType());
    Map<String, List<PropertyTransformation>> providerTransformationMap =
        featureSchema
            .accept(AbstractFeatureProvider.WITH_SCOPE_QUERIES)
            .accept(
                (schema, visitedProperties) ->
                    java.util.stream.Stream.concat(
                            schema.getTransformations().isEmpty()
                                ? schema.isTemporal()
                                    ? java.util.stream.Stream.of(
                                        new AbstractMap.SimpleImmutableEntry<
                                            String, List<PropertyTransformation>>(
                                            String.join(".", schema.getFullPath()),
                                            ImmutableList.of(
                                                new ImmutablePropertyTransformation.Builder()
                                                    .dateFormat(
                                                        schema.getType() == SchemaBase.Type.DATETIME
                                                            ? DATETIME_FORMAT
                                                            : DATE_FORMAT)
                                                    .build())))
                                    : java.util.stream.Stream.empty()
                                : java.util.stream.Stream.of(
                                    new AbstractMap.SimpleImmutableEntry<
                                        String, List<PropertyTransformation>>(
                                        schema.getFullPath().isEmpty()
                                            ? WILDCARD
                                            : String.join(".", schema.getFullPath()),
                                        schema.getTransformations())),
                            visitedProperties.stream().flatMap(m -> m.entrySet().stream()))
                        .collect(
                            ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));
    Map<String, List<PropertyTransformation>> providerTransformationMapMutations =
        featureSchema
            .accept(AbstractFeatureProvider.WITH_SCOPE_MUTATIONS)
            .accept(
                (schema, visitedProperties) ->
                    java.util.stream.Stream.concat(
                            schema.isTemporal()
                                ? java.util.stream.Stream.of(
                                    new AbstractMap.SimpleImmutableEntry<
                                        String, List<PropertyTransformation>>(
                                        String.join(".", schema.getFullPath()),
                                        ImmutableList.of(
                                            new ImmutablePropertyTransformation.Builder()
                                                .dateFormat(
                                                    schema.getType() == SchemaBase.Type.DATETIME
                                                        ? DATETIME_FORMAT
                                                        : DATE_FORMAT)
                                                .build())))
                                : java.util.stream.Stream.empty(),
                            visitedProperties.stream().flatMap(m -> m.entrySet().stream()))
                        .collect(
                            ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));
    PropertyTransformations providerTransformations = () -> providerTransformationMap;

    PropertyTransformations mergedTransformations =
        query.getSchemaScope() == FeatureSchema.Scope.QUERIES
            ? propertyTransformations
                .map(p -> p.mergeInto(providerTransformations))
                .orElse(providerTransformations)
            : () -> providerTransformationMapMutations;

    FeatureTokenTransformerSchemaMappings schemaMapper =
        new FeatureTokenTransformerSchemaMappings(mergedTransformations);

    Optional<CrsTransformer> crsTransformer =
        query
            .getCrs()
            .flatMap(
                targetCrs ->
                    crsTransformerFactory.getTransformer(
                        data.getNativeCrs().orElse(OgcCrs.CRS84), targetCrs));
    FeatureTokenTransformerValueMappings valueMapper =
        new FeatureTokenTransformerValueMappings(
            mergedTransformations, codelists, data.getNativeTimeZone(), crsTransformer);

    FeatureTokenTransformerRemoveEmptyOptionals cleaner =
        new FeatureTokenTransformerRemoveEmptyOptionals();

    FeatureTokenTransformerLogger logger = new FeatureTokenTransformerLogger();

    return featureTokenSource.via(schemaMapper).via(valueMapper).via(cleaner);
    // .via(logger);
  }
}
