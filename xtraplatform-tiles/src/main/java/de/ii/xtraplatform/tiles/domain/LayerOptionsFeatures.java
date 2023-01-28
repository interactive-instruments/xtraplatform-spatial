/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.Buildable;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(builder = ImmutableLayerOptionsFeatures.Builder.class)
public interface LayerOptionsFeatures
    extends LayerOptionsCommon, TileGenerationOptions, Buildable<LayerOptionsFeatures> {
  String COMBINE_ALL = "*";

  Optional<String> getFeatureProvider();

  Optional<String> getFeatureType();

  List<String> getCombine();

  Map<String, List<LevelFilter>> getFilters();

  @JsonIgnore
  @Value.Derived
  default boolean isCombined() {
    return !getCombine().isEmpty();
  }

  // TODO: check

  @Override
  default ImmutableLayerOptionsFeatures.Builder getBuilder() {
    return new ImmutableLayerOptionsFeatures.Builder().from(this);
  }

  abstract class Builder implements BuildableBuilder<LayerOptionsFeatures> {}
}
