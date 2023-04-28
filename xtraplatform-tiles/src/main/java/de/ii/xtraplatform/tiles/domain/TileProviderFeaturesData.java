/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.docs.DocStep;
import de.ii.xtraplatform.docs.DocStep.Step;
import de.ii.xtraplatform.docs.DocTable;
import de.ii.xtraplatform.docs.DocTable.ColumnSet;
import de.ii.xtraplatform.docs.DocVar;
import de.ii.xtraplatform.store.domain.entities.AutoEntity;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaults;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableMap;
import de.ii.xtraplatform.tiles.domain.ImmutableTilesetFeatures.Builder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * # Features
 *
 * @langEn In this tile provider, the tiles in Mapbox Vector Tiles format are derived from a
 *     [Feature Provider](../feature/README.md).
 *     <p>## Configuration
 *     <p>{@docTable:properties}
 *     <p>{@docVar:tilesetDefaults}
 *     <p>{@docTable:tilesetDefaults}
 *     <p>{@docVar:tileset}
 *     <p>{@docTable:tileset}
 *     <p>{@docVar:cache}
 *     <p>{@docTable:cache}
 *     <p>{@docVar:seeding}
 *     <p>{@docTable:seeding}
 *     <p>## Example
 *     <p>{@docVar:examples}
 * @langDe Bei diesem Tile-Provider werden die Kacheln im Format Mapbox Vector Tiles aus einem
 *     [Feature Provider](../feature/README.md) abgeleitet.
 *     <p>## Konfiguration
 *     <p>{@docTable:properties}
 *     <p>{@docVar:tilesetDefaults}
 *     <p>{@docTable:tilesetDefaults}
 *     <p>{@docVar:tileset}
 *     <p>{@docTable:tileset}
 *     <p>{@docVar:cache}
 *     <p>{@docTable:cache}
 *     <p>{@docVar:seeding}
 *     <p>{@docTable:seeding}
 *     <p>## Beispiel
 *     <p>{@docVar:examples}
 * @ref:cfgProperties {@link de.ii.xtraplatform.tiles.domain.ImmutableTileProviderFeaturesData}
 * @ref:tilesetDefaults {@link de.ii.xtraplatform.tiles.domain.TilesetFeaturesDefaults}
 * @ref:tilesetDefaultsTable {@link
 *     de.ii.xtraplatform.tiles.domain.ImmutableTilesetFeaturesDefaults}
 * @ref:tileset {@link de.ii.xtraplatform.tiles.domain.TilesetFeatures}
 * @ref:tilesetTable {@link de.ii.xtraplatform.tiles.domain.ImmutableTilesetFeatures}
 * @ref:seeding {@link de.ii.xtraplatform.tiles.domain.SeedingOptions}
 * @ref:seedingTable {@link de.ii.xtraplatform.tiles.domain.ImmutableSeedingOptions}
 * @ref:cache {@link de.ii.xtraplatform.tiles.domain.Cache}
 * @ref:cacheTable {@link de.ii.xtraplatform.tiles.domain.ImmutableCache}
 * @examplesAll <code>
 * ```yaml
 * id: vineyards-tiles
 * providerType: TILE
 * providerSubType: FEATURES
 * caches:
 * - type: IMMUTABLE
 *   storage: MBTILES
 *   levels:
 *     WebMercatorQuad:
 *       min: 5
 *       max: 12
 * - type: DYNAMIC
 *   storage: MBTILES
 *   seeded: false
 *   levels:
 *     WebMercatorQuad:
 *       min: 13
 *       max: 18
 * tilesetDefaults:
 *   levels:
 *     WebMercatorQuad:
 *       min: 5
 *       max: 18
 * tilesets:
 *   __all__:
 *     id: __all__
 *     combine: ['*']
 *   vineyards:
 *     id: vineyards
 * ```
 * </code>
 */
@DocFile(
    path = "providers/tile",
    name = "features.md",
    tables = {
      @DocTable(
          name = "properties",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:cfgProperties}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
      @DocTable(
          name = "tilesetDefaults",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:tilesetDefaultsTable}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
      @DocTable(
          name = "tileset",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:tilesetTable}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
      @DocTable(
          name = "seeding",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:seedingTable}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
      @DocTable(
          name = "cache",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:cacheTable}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
    },
    vars = {
      @DocVar(
          name = "tilesetDefaults",
          value = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:tilesetDefaults}"),
            @DocStep(type = Step.TAG, params = "{@bodyBlock}")
          }),
      @DocVar(
          name = "tileset",
          value = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:tileset}"),
            @DocStep(type = Step.TAG, params = "{@bodyBlock}")
          }),
      @DocVar(
          name = "seeding",
          value = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:seeding}"),
            @DocStep(type = Step.TAG, params = "{@bodyBlock}")
          }),
      @DocVar(
          name = "cache",
          value = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:cache}"),
            @DocStep(type = Step.TAG, params = "{@bodyBlock}")
          }),
      @DocVar(
          name = "examples",
          value = {
            // @DocStep(type = Step.TAG_REFS, params = "{@ref:cfg}"),
            @DocStep(type = Step.TAG, params = "{@examples}")
          }),
    })
@Value.Immutable
@JsonDeserialize(builder = ImmutableTileProviderFeaturesData.Builder.class)
public interface TileProviderFeaturesData extends TileProviderData, WithCaches, AutoEntity {

  String PROVIDER_SUBTYPE = "FEATURES";
  String ENTITY_SUBTYPE = String.format("%s/%s", PROVIDER_TYPE, PROVIDER_SUBTYPE).toLowerCase();

  /**
   * @langEn Always `FEATURES`.
   * @langDe Immer `FEATURES`.
   */
  @Override
  String getProviderSubType();

  @Override
  Optional<Boolean> getAuto();

  @Override
  Optional<Boolean> getAutoPersist();

  /**
   * @langEn Defaults for all `tilesets`, see [Tileset Defaults](#tileset-defaults).
   * @langDe Defaults für alle `tilesets`, siehe [Tileset Defaults](#tileset-defaults).
   * @since v3.4
   */
  @Override
  TilesetFeaturesDefaults getTilesetDefaults();

  /**
   * @langEn Definition of tilesets, see [Tileset](#tileset).
   * @langDe Definition von Tilesets, see [Tileset](#tileset).
   * @since v3.4
   * @default {}
   */
  @Override
  BuildableMap<TilesetFeatures, ImmutableTilesetFeatures.Builder> getTilesets();

  /**
   * @langEn List of cache definitions, see [Cache](#cache).
   * @langDe Liste von Cache-Definitionen, siehe [Cache](#cache).
   * @since v3.4
   * @default []
   */
  @Override
  List<Cache> getCaches();

  /**
   * @langEn Controls how and when tiles are precomputed, see [Seeding](#seeding).
   * @langDe Steuert wie und wann Kacheln vorberechnet werden, siehe [Seeding](#seeding).
   * @since v3.4
   * @default {}
   */
  @Override
  Optional<SeedingOptions> getSeeding();

  @Override
  default TileProviderData mergeInto(TileProviderData source) {
    if (Objects.isNull(source) || !(source instanceof TileProviderFeaturesData)) return this;

    Builder builder =
        new ImmutableTileProviderFeaturesData.Builder()
            .from((TileProviderFeaturesData) source)
            .from(this);

    TileProviderFeaturesData src = (TileProviderFeaturesData) source;
    /*
        List<String> tileEncodings =
            Objects.nonNull(src.getTileEncodings())
                ? Lists.newArrayList(src.getTileEncodings())
                : Lists.newArrayList();
        getTileEncodings()
            .forEach(
                tileEncoding -> {
                  if (!tileEncodings.contains(tileEncoding)) {
                    tileEncodings.add(tileEncoding);
                  }
                });
        builder.tileEncodings(tileEncodings);

        Map<String, MinMax> mergedSeeding =
            Objects.nonNull(src.getSeeding())
                ? Maps.newLinkedHashMap(src.getSeeding())
                : Maps.newLinkedHashMap();
        if (Objects.nonNull(getSeeding())) getSeeding().forEach(mergedSeeding::put);
        builder.seeding(mergedSeeding);

        Map<String, MinMax> mergedZoomLevels =
            Objects.nonNull(src.getZoomLevels())
                ? Maps.newLinkedHashMap(src.getZoomLevels())
                : Maps.newLinkedHashMap();
        if (Objects.nonNull(getZoomLevels())) getZoomLevels().forEach(mergedZoomLevels::put);
        builder.zoomLevels(mergedZoomLevels);

        if (!getCenter().isEmpty()) builder.center(getCenter());
        else if (!src.getCenter().isEmpty()) builder.center(src.getCenter());

        Map<String, MinMax> mergedZoomLevelsCache =
            Objects.nonNull(src.getZoomLevelsCache())
                ? Maps.newLinkedHashMap(src.getZoomLevelsCache())
                : Maps.newLinkedHashMap();
        if (Objects.nonNull(getZoomLevelsCache()))
          getZoomLevelsCache().forEach(mergedZoomLevelsCache::put);
        builder.zoomLevelsCache(mergedZoomLevelsCache);

        Map<String, List<Rule>> mergedRules =
            Objects.nonNull(src.getRules())
                ? Maps.newLinkedHashMap(src.getRules())
                : Maps.newLinkedHashMap();
        if (Objects.nonNull(getRules())) getRules().forEach(mergedRules::put);
        builder.rules(mergedRules);

        Map<String, List<PredefinedFilter>> mergedFilters =
            Objects.nonNull(src.getFilters())
                ? Maps.newLinkedHashMap(src.getFilters())
                : Maps.newLinkedHashMap();
        if (Objects.nonNull(getFilters())) getFilters().forEach(mergedFilters::put);
        builder.filters(mergedFilters);

        if (Objects.nonNull(getCenter())) builder.center(getCenter());
        else if (Objects.nonNull(src.getCenter())) builder.center(src.getCenter());
    */
    return builder.build();
  }

  abstract class Builder extends TileProviderData.Builder<ImmutableTileProviderFeaturesData.Builder>
      implements EntityDataBuilder<TileProviderData> {
    @Override
    public ImmutableTileProviderFeaturesData.Builder fillRequiredFieldsWithPlaceholders() {
      return this.id(EntityDataDefaults.PLACEHOLDER)
          .providerType(EntityDataDefaults.PLACEHOLDER)
          .providerSubType(EntityDataDefaults.PLACEHOLDER);
    }

    @JsonAlias("layers")
    public abstract Map<String, ImmutableTilesetFeatures.Builder> getTilesets();
  }
}
