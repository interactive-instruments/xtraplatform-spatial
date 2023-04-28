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
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableMap;
import de.ii.xtraplatform.tiles.domain.ImmutableMinMax.Builder;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

/**
 * @langEn ### Tileset
 *     <p>All options from [Tileset Defaults](#tileset-defaults) are also available and can be
 *     overriden here.
 * @langDe ### Tileset
 *     <p>Alle Optionen aus [Tileset Defaults](#tileset-defaults) sind ebenfalls verfübgar und
 *     können hier überschrieben werden.
 */
@Value.Immutable
@JsonDeserialize(builder = ImmutableTilesetHttp.Builder.class)
public interface TilesetHttp extends TilesetCommon, TilesetHttpDefaults {

  @DocIgnore
  @Override
  BuildableMap<MinMax, Builder> getLevels();

  @DocIgnore
  @Override
  Optional<LonLat> getCenter();

  @DocIgnore
  @Override
  Map<String, String> getEncodings();

  /**
   * @langEn URL template for accessing tiles. Parameters to use are `{{tileMatrix}}`,
   *     `{{tileRow}}`, `{{tileCol}}` and `{{fileExtension}}`.
   * @langDe URL-Template für den Zugriff auf Kacheln. Zu verwenden sind die Parameter
   *     `{{tileMatrix}}`, `{{tileRow}}`, `{{tileCol}}` und `{{fileExtension}}`.
   * @since v3.4
   */
  String getUrlTemplate();
}
