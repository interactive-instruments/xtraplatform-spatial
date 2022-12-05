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
 * </code>
 * @langDe # Tiles
 *     <p>Es werden aktuell drei Arten von Tile-Providern unterstützt:
 *     <p>- `FEATURES`: Die Kacheln werden aus einem Feature-Provider abgeleitet. - `MBTILES`: Die
 *     Kacheln eines Tileset im Kachelschema "WebMercatorQuad" liegen in einem MBTiles-Archiv vor. -
 *     `TILESERVER`: Die Kacheln werden von einer TileServer-GL-Instanz abgerufen.
 */
@DocFile(
    path = "providers/tile",
    name = "README.md",
    tables = {
      @DocTable(
          name = "properties",
          rows = {
            @DocStep(type = Step.TAG_REFS, params = "{@propertyTable}"),
            @DocStep(type = Step.JSON_PROPERTIES)
          },
          columnSet = ColumnSet.JSON_PROPERTIES),
    })
public interface TileProviderData extends ProviderData {

  String ENTITY_TYPE = "providers";
  String PROVIDER_TYPE = "TILE";

  @Override
  String getProviderType();

  @Override
  String getProviderSubType();

  LayerOptionsCommonDefault getLayerDefaults();

  // TODO: Buildable, merge defaults into layers
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
