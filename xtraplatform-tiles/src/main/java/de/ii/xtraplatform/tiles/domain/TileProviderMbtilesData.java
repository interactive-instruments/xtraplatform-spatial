/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.docs.DocStep;
import de.ii.xtraplatform.docs.DocStep.Step;
import de.ii.xtraplatform.docs.DocTable;
import de.ii.xtraplatform.docs.DocTable.ColumnSet;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityDataDefaults;
import java.util.Map;
import java.util.Objects;
import org.immutables.value.Value;

/**
 * # MbTiles
 *
 * @langEn With this tile provider, the tiles are provided via an [MBTiles
 *     file](https://github.com/mapbox/mbtiles-spec). The tile format and all other properties of
 *     the tileset resource are derived from the contents of the MBTiles file. Only the
 *     "WebMercatorQuad" tiling scheme is supported.
 *     <p>## Configuration
 *     <p>### Options
 *     <p>{@docTable:properties}
 *     <p>### Layer Defaults
 *     <p>{@docTable:layerDefaults}
 *     <p>### Layer
 *     <p>{@docTable:layer}
 * @langDe Bei diesem Tile-Provider werden die Kacheln über eine
 *     [MBTiles-Datei](https://github.com/mapbox/mbtiles-spec) bereitgestellt. Das Kachelformat und
 *     alle anderen Eigenschaften der Tileset-Ressource ergeben sich aus dem Inhalt der
 *     MBTiles-Datei. Unterstützt wird nur das Kachelschema "WebMercatorQuad".
 *     <p>## Konfiguration
 *     <p>### Optionen
 *     <p>{@docTable:properties}
 *     <p>### Layer Defaults
 *     <p>{@docTable:layerDefaults}
 *     <p>### Layer
 *     <p>{@docTable:layer}
 * @ref:cfgProperties {@link de.ii.xtraplatform.tiles.domain.ImmutableTileProviderMbtilesData}
 * @ref:layerDefaultsTable {@link
 *     de.ii.xtraplatform.tiles.domain.ImmutableLayerOptionsMbTilesDefault}
 * @ref:layerTable {@link de.ii.xtraplatform.tiles.domain.ImmutableLayerOptionsMbTiles}
 */
@DocFile(
    path = "providers/tile",
    name = "mbtiles.md",
    tables = {
      @DocTable(
          name = "properties",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:cfgProperties}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
      @DocTable(
          name = "layerDefaults",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:layerDefaultsTable}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
      @DocTable(
          name = "layer",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:layerTable}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
    })
@Value.Immutable
@JsonDeserialize(builder = ImmutableTileProviderMbtilesData.Builder.class)
public interface TileProviderMbtilesData extends TileProviderData {

  String PROVIDER_SUBTYPE = "MBTILES";
  String ENTITY_SUBTYPE = String.format("%s/%s", PROVIDER_TYPE, PROVIDER_SUBTYPE).toLowerCase();

  // TODO: error when using interface
  @Value.Default
  @Override
  default ImmutableLayerOptionsMbTilesDefault getLayerDefaults() {
    return new ImmutableLayerOptionsMbTilesDefault.Builder().build();
  }

  @Override
  Map<String, LayerOptionsMbTiles> getLayers();

  @Override
  default TileProviderData mergeInto(TileProviderData source) {
    if (Objects.isNull(source) || !(source instanceof TileProviderMbtilesData)) return this;

    TileProviderMbtilesData src = (TileProviderMbtilesData) source;

    ImmutableTileProviderMbtilesData.Builder builder =
        new ImmutableTileProviderMbtilesData.Builder().from(src).from(this);

    // if (!getCenter().isEmpty()) builder.center(getCenter());
    // else if (!src.getCenter().isEmpty()) builder.center(src.getCenter());

    return builder.build();
  }

  abstract class Builder extends TileProviderData.Builder<ImmutableTileProviderMbtilesData.Builder>
      implements EntityDataBuilder<TileProviderData> {
    @Override
    public ImmutableTileProviderMbtilesData.Builder fillRequiredFieldsWithPlaceholders() {
      return this.id(EntityDataDefaults.PLACEHOLDER)
          .providerType(EntityDataDefaults.PLACEHOLDER)
          .providerSubType(EntityDataDefaults.PLACEHOLDER);
    }
  }
}
