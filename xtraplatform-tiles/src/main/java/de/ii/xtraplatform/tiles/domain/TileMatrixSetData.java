/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.tiles.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

/** This class specifies the data structure of a tile matrix set. */
@Value.Immutable
// TODO: remove when migrated to entity (???)
@Value.Style(deepImmutablesDetection = true, builder = "new")
@JsonDeserialize(builder = ImmutableTileMatrixSetData.Builder.class)
public interface TileMatrixSetData {

  String getId();

  Optional<String> getTitle();

  Optional<String> getDescription();

  List<String> getKeywords();

  String getCrs();

  Optional<URI> getWellKnownScaleSet();

  Optional<URI> getUri();

  Optional<TilesBoundingBox> getBoundingBox();

  List<TileMatrix> getTileMatrices();

  List<String> getOrderedAxes();
}
