/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.crs.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(builder = "new", deepImmutablesDetection = true)
@JsonDeserialize(builder = ImmutableBoundingBox.Builder.class)
public interface BoundingBox {

  static BoundingBox of(double xmin, double ymin, double xmax, double ymax, EpsgCrs crs) {
    return new ImmutableBoundingBox.Builder().xmin(xmin).ymin(ymin).xmax(xmax).ymax(ymax).epsgCrs(crs).build();
  }

  double getXmin();

  double getYmin();

  double getXmax();

  double getYmax();

  @Value.Default
  default EpsgCrs getEpsgCrs() {
    return OgcCrs.CRS84;
  }

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default double[] toArray() {
    return new double[]{getXmin(), getYmin(), getXmax(), getYmax()};
  }
}
