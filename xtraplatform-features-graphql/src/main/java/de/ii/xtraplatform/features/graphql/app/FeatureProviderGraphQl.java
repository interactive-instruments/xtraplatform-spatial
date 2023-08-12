/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.features.graphql.app;

import com.google.common.collect.ImmutableMap;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.cql.domain.Cql;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsInfo;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.EpsgCrs;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.AbstractFeatureProvider;
import de.ii.xtraplatform.features.domain.AggregateStatsReader;
import de.ii.xtraplatform.features.domain.ConnectorFactory;
import de.ii.xtraplatform.features.domain.FeatureCrs;
import de.ii.xtraplatform.features.domain.FeatureEventHandler.ModifiableContext;
import de.ii.xtraplatform.features.domain.FeatureExtents;
import de.ii.xtraplatform.features.domain.FeatureMetadata;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector;
import de.ii.xtraplatform.features.domain.FeatureProviderConnector.QueryOptions;
import de.ii.xtraplatform.features.domain.FeatureProviderDataV2;
import de.ii.xtraplatform.features.domain.FeatureQueries;
import de.ii.xtraplatform.features.domain.FeatureQuery;
import de.ii.xtraplatform.features.domain.FeatureQueryEncoder;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.FeatureTokenDecoder;
import de.ii.xtraplatform.features.domain.Metadata;
import de.ii.xtraplatform.features.domain.ProviderExtensionRegistry;
import de.ii.xtraplatform.features.domain.Query;
import de.ii.xtraplatform.features.domain.SchemaMapping;
import de.ii.xtraplatform.features.domain.transform.OnlyQueryables;
import de.ii.xtraplatform.features.domain.transform.OnlySortables;
import de.ii.xtraplatform.features.graphql.domain.FeatureProviderGraphQlData;
import de.ii.xtraplatform.features.graphql.domain.GraphQlConnector;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.streams.domain.Reactive;
import de.ii.xtraplatform.streams.domain.Reactive.Stream;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

public class FeatureProviderGraphQl
    extends AbstractFeatureProvider<
        byte[], String, FeatureProviderConnector.QueryOptions, FeatureSchema>
    implements FeatureProvider2, FeatureQueries, FeatureCrs, FeatureExtents, FeatureMetadata {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureProviderGraphQl.class);

  static final String ENTITY_SUB_TYPE = "feature/graphql";
  public static final String PROVIDER_TYPE = "GRAPHQL";
  private static final MediaType MEDIA_TYPE = new MediaType("application", "geo+json");

  private final CrsTransformerFactory crsTransformerFactory;
  private final Cql cql;
  private final EntityRegistry entityRegistry;

  private FeatureQueryEncoderGraphQl queryTransformer;
  private AggregateStatsReader<FeatureSchema> aggregateStatsReader;

  @AssistedInject
  public FeatureProviderGraphQl(
      CrsTransformerFactory crsTransformerFactory,
      Cql cql,
      ConnectorFactory connectorFactory,
      Reactive reactive,
      EntityRegistry entityRegistry,
      ProviderExtensionRegistry extensionRegistry,
      @Assisted FeatureProviderDataV2 data) {
    super(connectorFactory, reactive, crsTransformerFactory, extensionRegistry, data);

    this.crsTransformerFactory = crsTransformerFactory;
    this.cql = cql;
    this.entityRegistry = entityRegistry;
  }

  @Override
  protected boolean onStartup() throws InterruptedException {
    boolean success = super.onStartup();

    if (!success) {
      return false;
    }

    this.queryTransformer =
        new FeatureQueryEncoderGraphQl(
            getData().getTypes(),
            getSourceSchemas(),
            getData().getConnectionInfo(),
            getData().getQueries(),
            getData().getNativeCrs().orElse(OgcCrs.CRS84),
            crsTransformerFactory,
            cql);
    this.aggregateStatsReader =
        new AggregateStatsReaderGraphQl(
            this, crsTransformerFactory, getData().getNativeCrs().orElse(OgcCrs.CRS84));

    return true;
  }

  @Override
  protected Map<String, List<FeatureSchema>> getSourceSchemas() {
    Map<String, List<FeatureSchema>> types =
        getData().getTypes().entrySet().stream()
            .map(
                entry ->
                    new SimpleImmutableEntry<>(
                        entry.getKey(),
                        List.of(
                            entry
                                .getValue()
                                .accept(
                                    new SchemaDeriverGraphQl(
                                        getData().getTypes(), getData().getQueries())))))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return types;
  }

  @Override
  public FeatureProviderGraphQlData getData() {
    return (FeatureProviderGraphQlData) super.getData();
  }

  @Override
  protected GraphQlConnector getConnector() {
    return (GraphQlConnector) super.getConnector();
  }

  @Override
  protected FeatureQueryEncoder<String, QueryOptions> getQueryEncoder() {
    return queryTransformer;
  }

  @Override
  protected FeatureTokenDecoder<
          byte[], FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>>
      getDecoder(Query query) {
    return getDecoder(query, false);
  }

  private FeatureTokenDecoder<
          byte[], FeatureSchema, SchemaMapping, ModifiableContext<FeatureSchema, SchemaMapping>>
      getDecoder(Query query, boolean passThrough) {
    if (!(query instanceof FeatureQuery)) {
      throw new IllegalArgumentException();
    }
    FeatureQuery featureQuery = (FeatureQuery) query;
    FeatureSchema featureSchema = getSourceSchemas().get(featureQuery.getType()).get(0);
    String name =
        featureSchema.getSourcePath().map(sourcePath -> sourcePath.substring(1)).orElse(null);

    String wrapper =
        featureQuery.returnsSingleFeature() && getData().getQueries().getSingle().isPresent()
            ? getData().getQueries().getSingle().get().getName(name)
            : getData().getQueries().getCollection().getName(name);

    return new FeatureTokenDecoderGraphQlJson2(featureSchema, featureQuery, name, wrapper);
  }

  @Override
  protected Map<String, Codelist> getCodelists() {
    // TODO
    getData().getCodelists();

    return entityRegistry.getEntitiesForType(Codelist.class).stream()
        .map(codelist -> new SimpleImmutableEntry<>(codelist.getId(), codelist))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @Override
  public boolean supportsCrs() {
    return super.supportsCrs() && getData().getNativeCrs().isPresent();
  }

  @Override
  public EpsgCrs getNativeCrs() {
    return getData().getNativeCrs().get();
  }

  @Override
  public boolean isCrsSupported(EpsgCrs crs) {
    return Objects.equals(getNativeCrs(), crs) || crsTransformerFactory.isSupported(crs);
  }

  @Override
  public boolean is3dSupported() {
    return ((CrsInfo) crsTransformerFactory).is3d(getNativeCrs());
  }

  @Override
  public long getFeatureCount(String typeName) {
    if (getData().getTypes().containsKey(typeName)) {
      return -1;
    }

    try {
      Stream<Long> countGraph =
          aggregateStatsReader.getCount(List.of(getData().getTypes().get(typeName)));

      return countGraph
          .on(getStreamRunner())
          .run()
          .exceptionally(throwable -> -1L)
          .toCompletableFuture()
          .join();
    } catch (Throwable e) {
      // continue
    }

    return -1;
  }

  @Override
  public FeatureSchema getQueryablesSchema(
      FeatureSchema schema, List<String> included, List<String> excluded, String pathSeparator) {
    OnlyQueryables queryablesSelector =
        new OnlyQueryables(included, excluded, pathSeparator, (path) -> false);

    return schema.accept(queryablesSelector);
  }

  @Override
  public FeatureSchema getSortablesSchema(
      FeatureSchema schema, List<String> included, List<String> excluded, String pathSeparator) {
    OnlySortables sortablesSelector =
        new OnlySortables(included, excluded, pathSeparator, (path) -> false);

    return schema.accept(sortablesSelector);
  }

  @Override
  public Optional<BoundingBox> getSpatialExtent(String typeName) {
    if (getData().getTypes().containsKey(typeName)) {
      return Optional.empty();
    }

    try {
      Stream<Optional<BoundingBox>> extentGraph =
          aggregateStatsReader.getSpatialExtent(
              List.of(getData().getTypes().get(typeName)), is3dSupported());

      return extentGraph
          .on(getStreamRunner())
          .run()
          .exceptionally(throwable -> Optional.empty())
          .toCompletableFuture()
          .join();
    } catch (Throwable e) {
      // continue
      boolean br = true;
    }

    return Optional.empty();
  }

  @Override
  public Optional<BoundingBox> getSpatialExtent(String typeName, EpsgCrs crs) {
    return getSpatialExtent(typeName)
        .flatMap(
            boundingBox ->
                crsTransformerFactory
                    .getTransformer(getNativeCrs(), crs, false)
                    .flatMap(
                        crsTransformer -> {
                          try {
                            return Optional.of(crsTransformer.transformBoundingBox(boundingBox));
                          } catch (CrsTransformationException e) {
                            return Optional.empty();
                          }
                        }));
  }

  @Override
  public Optional<Interval> getTemporalExtent(String typeName) {
    return Optional.empty();
  }

  @Override
  public Optional<Metadata> getMetadata() {
    return Optional.empty();
  }
}
