/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.docs.DocIgnore;
import de.ii.xtraplatform.entities.domain.maptobuilder.Buildable;
import de.ii.xtraplatform.entities.domain.maptobuilder.BuildableBuilder;
import de.ii.xtraplatform.entities.domain.maptobuilder.BuildableMap;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * @langEn ### Raster Tileset
 *     <p>To generate raster tiles [xtratiler](https://github.com/ldproxy/xtratiler) is required.
 *     Raster tiles cannot be generated on-demand, so a seeded [cache](#cache) is required.
 *     Currently only `DYNAMIC` caches are supported.
 * @langDe ### Raster-Tileset
 *     <p>Um Raster-Kacheln zu generieren, wird [xtratiler](https://github.com/ldproxy/xtratiler)
 *     benötigt. Raster-Kacheln können nicht auf Anfrage generiert werden, daher ist ein `seeded`
 *     [Cache](#cache) erforderlich. Aktuell werden nur `DYNAMIC`-Caches unterstützt.
 */
@Value.Immutable
@JsonDeserialize(builder = ImmutableTilesetRaster.Builder.class)
public interface TilesetRaster extends TilesetCommonDefaults, Buildable<TilesetRaster> {

  @DocIgnore
  Optional<String> getPrefix();

  @DocIgnore
  @Override
  BuildableMap<MinMax, ImmutableMinMax.Builder> getLevels();

  @DocIgnore
  @Override
  Optional<LonLat> getCenter();

  /**
   * @langEn List of MapLibre styles that raster tiles should be generated for. The entries are
   *     relative paths to values of type `maplibre-styles` in the store.
   * @langDe Liste der MapLibre-Styles für die Raster-Kacheln generiert werden sollen. Die Einträge
   *     sind relative Pfade zu Values vom Typ `maplibre-styles` im Store.
   * @since v4.1
   * @default []
   */
  List<String> getStyles();

  @Override
  default ImmutableTilesetRaster.Builder getBuilder() {
    return new ImmutableTilesetRaster.Builder().from(this);
  }

  abstract class Builder implements BuildableBuilder<TilesetRaster> {}
}
