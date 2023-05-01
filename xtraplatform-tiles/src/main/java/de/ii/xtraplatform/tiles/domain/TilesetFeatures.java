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
import de.ii.xtraplatform.docs.DocIgnore;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.Buildable;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableBuilder;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
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
@JsonDeserialize(builder = ImmutableTilesetFeatures.Builder.class)
public interface TilesetFeatures
    extends TilesetCommon, TileGenerationOptions, WithFeatureProvider, Buildable<TilesetFeatures> {
  String COMBINE_ALL = "*";

  @Override
  String getId();

  @DocIgnore
  @Override
  BuildableMap<MinMax, ImmutableMinMax.Builder> getLevels();

  @DocIgnore
  @Override
  Optional<LonLat> getCenter();

  @DocIgnore
  @Override
  Optional<String> getFeatureProvider();

  @DocIgnore
  @Nullable
  @Override
  Integer getFeatureLimit();

  @DocIgnore
  @Nullable
  @Override
  Double getMinimumSizeInPixel();

  @DocIgnore
  @Nullable
  @Override
  Boolean getIgnoreInvalidGeometries();

  @DocIgnore
  @Override
  Map<String, List<LevelTransformation>> getTransformations();

  /**
   * @langEn The name of the feature type. By default the tileset id is used.
   * @langDe Der Name des Feature-Types. Standardmäßig wird die Tileset-Id verwendet.
   * @default null
   * @since v3.4
   */
  Optional<String> getFeatureType();

  /**
   * @langEn Instead of being generated using a `featureType`, a tileset may be composed of multiple
   *     other tilesets. Takes a list of tileset ids. A list with a single entry `*` combines all
   *     tilesets.
   * @langDe Anstatt aus einem `featureType` generiert zu werden, kann ein Tileset auch aus mehreren
   *     anderen Tilesets kombiniert werden. Der Wert ist eine Liste von Tileset-Ids oder eine Liste
   *     mit einem einzelnen Eintrag `*` um alle anderen Tilesets zu kombinieren.
   * @default []
   * @since v3.4
   */
  List<String> getCombine();

  /**
   * @langEn Filters to select a subset of feature for certain zoom levels using a CQL filter
   *     expression, see example below.
   * @langDe Über Filter kann gesteuert werden, welche Features auf welchen Zoomstufen selektiert
   *     werden sollen. Dazu dient ein CQL-Filterausdruck, der in `filter` angegeben wird. Siehe das
   *     Beispiel unten.
   * @default {}
   * @since v3.4
   */
  Map<String, List<LevelFilter>> getFilters();

  @JsonIgnore
  @Value.Derived
  default boolean isCombined() {
    return !getCombine().isEmpty();
  }

  // TODO: check

  @Override
  default ImmutableTilesetFeatures.Builder getBuilder() {
    return new ImmutableTilesetFeatures.Builder().from(this);
  }

  abstract class Builder implements BuildableBuilder<TilesetFeatures> {}

  default TilesetFeatures mergeDefaults(TilesetFeaturesDefaults defaults) {
    ImmutableTilesetFeatures.Builder withDefaults = getBuilder();

    if (this.getFeatureProvider().isEmpty() && defaults.getFeatureProvider().isPresent()) {
      withDefaults.featureProvider(defaults.getFeatureProvider());
    }
    if (this.getLevels().isEmpty()) {
      withDefaults.levels(defaults.getLevels());
    }
    if (this.getCenter().isEmpty() && defaults.getCenter().isPresent()) {
      withDefaults.center(defaults.getCenter());
    }
    if (this.getTransformations().isEmpty()) {
      withDefaults.transformations(defaults.getTransformations());
    }
    if (Objects.isNull(this.getFeatureLimit()) && Objects.nonNull(defaults.getFeatureLimit())) {
      withDefaults.featureLimit(defaults.getFeatureLimit());
    }
    if (Objects.isNull(this.getMinimumSizeInPixel())
        && Objects.nonNull(defaults.getMinimumSizeInPixel())) {
      withDefaults.minimumSizeInPixel(defaults.getMinimumSizeInPixel());
    }
    if (Objects.isNull(this.getIgnoreInvalidGeometries())
        && Objects.nonNull(defaults.getIgnoreInvalidGeometries())) {
      withDefaults.ignoreInvalidGeometries(defaults.getIgnoreInvalidGeometries());
    }

    return withDefaults.build();
  }
}
