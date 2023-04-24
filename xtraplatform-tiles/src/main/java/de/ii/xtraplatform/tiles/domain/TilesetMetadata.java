/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableList;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public interface TilesetMetadata {

  Set<String> getTileMatrixSets();

  Map<String, MinMax> getLevels();

  Set<String> getTileEncodings();

  Optional<LonLat> getCenter();

  Optional<BoundingBox> getBounds();

  Set<VectorLayer> getVectorLayers();

  @JsonIgnore
  @Value.Derived
  default boolean isVector() {
    return getTileEncodings().contains("MVT");
  }

  @Value.Immutable
  interface LonLat {

    static LonLat of(double lon, double lat) {
      return ImmutableLonLat.builder().lon(lon).lat(lat).build();
    }

    double getLon();

    double getLat();

    @JsonIgnore
    @Value.Derived
    default List<Double> asList() {
      return ImmutableList.of(getLon(), getLat());
    }
  }
}
