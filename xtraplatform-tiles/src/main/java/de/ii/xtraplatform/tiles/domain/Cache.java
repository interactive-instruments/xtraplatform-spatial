/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Locale;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableCache.Builder.class)
public interface Cache extends WithTmsLevels, WithLayerTmsLevels {
  enum Type {
    DYNAMIC,
    IMMUTABLE;

    public String getSuffix() {
      return this.name().substring(0, 3).toLowerCase(Locale.ROOT);
    }
  }

  enum Storage {
    PLAIN,
    MBTILES
  }

  Type getType();

  Storage getStorage();

  @Value.Default
  default boolean doNotSeed() {
    return false;
  }

  @Override
  Map<String, MinMax> getLevels();

  @Override
  Map<String, Map<String, MinMax>> getLayerLevels();
}
