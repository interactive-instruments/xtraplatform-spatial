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
 * # HTTP
 *
 * @langEn With this tile provider, the tiles are obtained via HTTP, e.g. from a
 *     [TileServer-GL](https://github.com/maptiler/tileserver-gl) instance.
 *     <p>## Configuration
 *     <p>### Options
 *     <p>{@docTable:properties}
 *     <p>### Layer Defaults
 *     <p>{@docTable:layerDefaults}
 *     <p>### Layer
 *     <p>{@docTable:layer}
 * @langDe Bei diesem Tile-Provider werden die Kacheln über HTTP bezogen, z.B. von einer
 *     [TileServer-GL](https://github.com/maptiler/tileserver-gl) Instanz.
 *     <p>## Konfiguration
 *     <p>{@docTable:properties}
 * @ref:cfgProperties {@link de.ii.xtraplatform.tiles.domain.ImmutableTileProviderHttpData}
 * @ref:layerDefaultsTable {@link de.ii.xtraplatform.tiles.domain.ImmutableLayerOptionsHttpDefault}
 * @ref:layerTable {@link de.ii.xtraplatform.tiles.domain.ImmutableLayerOptionsHttp}
 */
@DocFile(
    path = "providers/tile",
    name = "http.md",
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
@JsonDeserialize(builder = ImmutableTileProviderHttpData.Builder.class)
public interface TileProviderHttpData extends TileProviderData {

  String PROVIDER_SUBTYPE = "HTTP";
  String ENTITY_SUBTYPE = String.format("%s/%s", PROVIDER_TYPE, PROVIDER_SUBTYPE).toLowerCase();

  @Value.Default
  @Override
  default ImmutableLayerOptionsHttpDefault getLayerDefaults() {
    return new ImmutableLayerOptionsHttpDefault.Builder().build();
  }

  @Override
  Map<String, LayerOptionsHttp> getLayers();

  @Override
  default TileProviderData mergeInto(TileProviderData source) {
    if (Objects.isNull(source) || !(source instanceof TileProviderHttpData)) return this;

    TileProviderHttpData src = (TileProviderHttpData) source;

    ImmutableTileProviderHttpData.Builder builder =
        new ImmutableTileProviderHttpData.Builder().from(src).from(this);

    /*List<String> tileEncodings =
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
    builder.tileEncodings(tileEncodings);*/

    return builder.build();
  }

  abstract class Builder extends TileProviderData.Builder<ImmutableTileProviderHttpData.Builder>
      implements EntityDataBuilder<TileProviderData> {
    @Override
    public ImmutableTileProviderHttpData.Builder fillRequiredFieldsWithPlaceholders() {
      return this.id(EntityDataDefaults.PLACEHOLDER)
          .providerType(EntityDataDefaults.PLACEHOLDER)
          .providerSubType(EntityDataDefaults.PLACEHOLDER);
    }
  }
}
