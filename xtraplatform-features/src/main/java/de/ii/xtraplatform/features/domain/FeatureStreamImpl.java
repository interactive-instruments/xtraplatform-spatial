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
import de.ii.xtraplatform.base.domain.ETag;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.crs.domain.CrsTransformer;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.transform.ImmutablePropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformation;
import de.ii.xtraplatform.features.domain.transform.PropertyTransformations;
import de.ii.xtraplatform.streams.domain.Reactive;
import de.ii.xtraplatform.streams.domain.Reactive.Sink;
import de.ii.xtraplatform.streams.domain.Reactive.SinkReduced;
import de.ii.xtraplatform.streams.domain.Reactive.Stream;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

public class FeatureStreamImpl implements FeatureStream {

  private final Query query;
  private final FeatureProviderDataV2 data;
  private final CrsTransformerFactory crsTransformerFactory;
  private final Map<String, Codelist> codelists;
  private final QueryRunner runner;
  private final boolean doTransform;

  public FeatureStreamImpl(
      Query query,
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
      Sink<Object> sink, Map<String, PropertyTransformations> propertyTransformations) {

    Map<String, PropertyTransformations> mergedTransformations =
        getMergedTransformations(propertyTransformations);

    BiFunction<FeatureTokenSource, Map<String, String>, Stream<Result>> stream =
        (tokenSource, virtualTables) -> {
          FeatureTokenSource source =
              doTransform
                  ? getFeatureTokenSourceTransformed(tokenSource, mergedTransformations)
                  : tokenSource;
          ImmutableResult.Builder resultBuilder = ImmutableResult.builder();
          final ETag.Incremental eTag = ETag.incremental();
          final boolean strongETag =
              query instanceof FeatureQuery
                  && ((FeatureQuery) query)
                      .getETag()
                      .filter(type -> type == ETag.Type.STRONG)
                      .isPresent();

          if (query instanceof FeatureQuery
              && ((FeatureQuery) query)
                  .getETag()
                  .filter(type -> type == ETag.Type.WEAK)
                  .isPresent()) {
            source = source.via(new FeatureTokenTransformerWeakETag(resultBuilder));
          }

          source = source.via(new FeatureTokenTransformerMetadata(resultBuilder));

          source = source.via(new FeatureTokenTransformerHasFeatures(resultBuilder));

          Reactive.BasicStream<?, Void> basicStream =
              sink instanceof Reactive.SinkTransformed
                  ? source.to((Reactive.SinkTransformed<Object, ?>) sink)
                  : source.to(sink);

          return basicStream
              .withResult(resultBuilder.isEmpty(true).hasFeatures(false))
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

    return runner.runQuery(stream, query, mergedTransformations, !doTransform);
  }

  @Override
  public <X> CompletionStage<ResultReduced<X>> runWith(
      SinkReduced<Object, X> sink, Map<String, PropertyTransformations> propertyTransformations) {

    Map<String, PropertyTransformations> mergedTransformations =
        getMergedTransformations(propertyTransformations);

    BiFunction<FeatureTokenSource, Map<String, String>, Reactive.Stream<ResultReduced<X>>> stream =
        (tokenSource, virtualTables) -> {
          FeatureTokenSource source =
              doTransform
                  ? getFeatureTokenSourceTransformed(tokenSource, mergedTransformations)
                  : tokenSource;
          ImmutableResultReduced.Builder<X> resultBuilder = ImmutableResultReduced.<X>builder();
          final ETag.Incremental eTag = ETag.incremental();
          final boolean strongETag =
              query instanceof FeatureQuery
                  && ((FeatureQuery) query)
                      .getETag()
                      .filter(type -> type == ETag.Type.STRONG)
                      .isPresent();

          if (query instanceof FeatureQuery
              && ((FeatureQuery) query)
                  .getETag()
                  .filter(type -> type == ETag.Type.WEAK)
                  .isPresent()) {
            source = source.via(new FeatureTokenTransformerWeakETag(resultBuilder));
          }

          source = source.via(new FeatureTokenTransformerMetadata(resultBuilder));

          source = source.via(new FeatureTokenTransformerHasFeatures(resultBuilder));

          Reactive.BasicStream<?, X> basicStream =
              sink instanceof Reactive.SinkReducedTransformed
                  ? source.to((Reactive.SinkReducedTransformed<Object, ?, X>) sink)
                  : source.to(sink);

          return basicStream
              .withResult(resultBuilder.isEmpty(true).hasFeatures(false))
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

    return runner.runQuery(stream, query, mergedTransformations, !doTransform);
  }

  private FeatureTokenSource getFeatureTokenSourceTransformed(
      FeatureTokenSource featureTokenSource,
      Map<String, PropertyTransformations> propertyTransformations) {
    FeatureTokenTransformerMappings schemaMapper =
        new FeatureTokenTransformerMappings(
            propertyTransformations, codelists, data.getNativeTimeZone());

    Optional<CrsTransformer> crsTransformer =
        query
            .getCrs()
            .flatMap(
                targetCrs ->
                    crsTransformerFactory.getTransformer(
                        data.getNativeCrs().orElse(OgcCrs.CRS84), targetCrs));
    FeatureTokenTransformerCoordinates valueMapper =
        new FeatureTokenTransformerCoordinates(crsTransformer);

    FeatureTokenTransformerRemoveEmptyOptionals cleaner =
        new FeatureTokenTransformerRemoveEmptyOptionals();

    FeatureTokenSource tokenSourceTransformed =
        featureTokenSource.via(schemaMapper).via(valueMapper).via(cleaner);

    if (FeatureTokenValidator.LOGGER.isTraceEnabled()) {
      tokenSourceTransformed = tokenSourceTransformed.via(new FeatureTokenValidator());
    }

    return tokenSourceTransformed;
  }

  private Map<String, PropertyTransformations> getMergedTransformations(
      Map<String, PropertyTransformations> propertyTransformations) {
    if (query instanceof FeatureQuery) {
      FeatureQuery featureQuery = (FeatureQuery) query;

      return ImmutableMap.of(
          featureQuery.getType(),
          getPropertyTransformations(
              (FeatureQuery) query,
              Optional.ofNullable(propertyTransformations.get(featureQuery.getType()))));
    }

    if (query instanceof MultiFeatureQuery) {
      MultiFeatureQuery multiFeatureQuery = (MultiFeatureQuery) query;

      return multiFeatureQuery.getQueries().stream()
          .map(
              typeQuery ->
                  new SimpleImmutableEntry<>(
                      typeQuery.getType(),
                      getPropertyTransformations(
                          typeQuery,
                          Optional.ofNullable(propertyTransformations.get(typeQuery.getType())))))
          .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    return ImmutableMap.of();
  }

  private PropertyTransformations getPropertyTransformations(
      TypeQuery typeQuery, Optional<PropertyTransformations> propertyTransformations) {
    FeatureSchema featureSchema = data.getTypes().get(typeQuery.getType());

    if (typeQuery instanceof FeatureQuery
        && ((FeatureQuery) typeQuery).getSchemaScope() == SchemaBase.Scope.RECEIVABLE) {
      return () -> getProviderTransformationsMutations(featureSchema);
    }

    PropertyTransformations providerTransformations =
        () -> getProviderTransformations(featureSchema);

    return propertyTransformations
        .map(p -> p.mergeInto(providerTransformations))
        .orElse(providerTransformations);
  }

  private Map<String, List<PropertyTransformation>> getProviderTransformations(
      FeatureSchema featureSchema) {
    return featureSchema
        .accept(AbstractFeatureProvider.WITH_SCOPE_RETURNABLE)
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
                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));
  }

  private Map<String, List<PropertyTransformation>> getProviderTransformationsMutations(
      FeatureSchema featureSchema) {
    return featureSchema
        .accept(AbstractFeatureProvider.WITH_SCOPE_RECEIVABLE)
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
                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));
  }
}
