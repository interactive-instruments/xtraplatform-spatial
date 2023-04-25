/*
 * Copyright 2023 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.tiles.domain.WithCenter.LonLat;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(deepImmutablesDetection = true)
public interface TilesetMetadata {

  Set<String> getEncodings();

  Map<String, MinMax> getLevels();

  Optional<LonLat> getCenter();

  Optional<BoundingBox> getBounds();

  Map<String, Set<FeatureSchema>> getVectorSchemas();

  // Map<String, Set<VectorLayer>> getVectorLayers();

  @JsonIgnore
  @Value.Derived
  default Set<String> getTileMatrixSets() {
    return getLevels().keySet();
  }

  @JsonIgnore
  @Value.Derived
  default boolean isVector() {
    return getEncodings().contains("MVT");
  }
}
