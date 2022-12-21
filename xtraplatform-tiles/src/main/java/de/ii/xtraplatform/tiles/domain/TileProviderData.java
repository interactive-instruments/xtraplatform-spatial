/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import de.ii.xtraplatform.docs.DocFile;
import de.ii.xtraplatform.docs.DocStep;
import de.ii.xtraplatform.docs.DocStep.Step;
import de.ii.xtraplatform.docs.DocTable;
import de.ii.xtraplatform.docs.DocTable.ColumnSet;
import de.ii.xtraplatform.features.domain.ProviderData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.LinkedHashMap;
import java.util.Map;
import org.immutables.value.Value;

/**
 * @langEn # Tiles
 *     <p>There are currently three types of Tile providers:
 *     <p><code>
 * - [Features](features.md): The tiles are derived from a feature provider.
 * - [MbTiles](mbtiles.md): The tiles are retrieved from one or more MBTiles files.
 * - [HTTP](http.md): The tiles are retrieved via HTTP, e.g. from a TileServer GL instance.
 *     </code>
 *     <p>## Configuration
 *     <p>{@docTable:cfgProperties}
 * @langDe # Tiles
 *     <p>Es werden aktuell drei Arten von Tile-Providern unterstützt:
 *     <p><code>
 * - [Features](features.md): Die Kacheln werden aus einem Feature-Provider abgeleitet.
 * - [MbTiles](mbtiles.md): Die Kacheln liegen in einer oder mehreren MBTiles-Dateien vor.
 * - [HTTP](http.md): Die Kacheln werden via HTTP abgerufen, z.B. von einer TileServer GL Instanz.
 *     </code>
 *     <p>## Konfiguration
 *     <p>{@docTable:cfgProperties}
 * @ref:cfgProperties {@link de.ii.xtraplatform.tiles.domain.ImmutableTileProviderCommonData}
 */
@DocFile(
    path = "providers/tile",
    name = "README.md",
    tables = {
      @DocTable(
          name = "cfgProperties",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@ref:cfgProperties}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
    })
public interface TileProviderData extends ProviderData {

  String ENTITY_TYPE = "providers";
  String PROVIDER_TYPE = "TILE";

  /**
   * @langEn Always `TILE`.
   * @langDe Immer `TILE`.
   */
  @Override
  String getProviderType();

  @Override
  String getProviderSubType();

  LayerOptionsCommonDefault getLayerDefaults();

  Map<String, ? extends LayerOptionsCommon> getLayers();

  @JsonIgnore
  @Value.Lazy
  default Map<String, Map<String, Range<Integer>>> getTmsRanges() {
    return getLayers().entrySet().stream()
        .map(
            entry -> {
              LinkedHashMap<String, Range<Integer>> ranges =
                  new LinkedHashMap<>(getLayerDefaults().getTmsRanges());
              ranges.putAll(entry.getValue().getTmsRanges());

              return new SimpleImmutableEntry<>(entry.getKey(), ranges);
            })
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  TileProviderData mergeInto(TileProviderData tileProvider);

  abstract class Builder<T extends TileProviderData.Builder<T>>
      implements EntityDataBuilder<TileProviderData> {

    public abstract T id(String id);

    public abstract T providerType(String providerType);

    public abstract T providerSubType(String featureProviderType);
  }
}
