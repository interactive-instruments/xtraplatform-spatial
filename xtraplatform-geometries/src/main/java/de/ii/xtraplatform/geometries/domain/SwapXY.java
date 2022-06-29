/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.geometries.domain;

import java.io.IOException;
import org.immutables.value.Value;

@Value.Immutable
public abstract class SwapXY implements CoordinatesTransformation {

  @Override
  public void onCoordinates(double[] coordinates, int length, int dimension) throws IOException {

    for (int i = 0; i < length; i = i + dimension) {
      double x = coordinates[i];
      coordinates[i] = coordinates[i + 1];
      coordinates[i + 1] = x;
    }

    getNext().onCoordinates(coordinates, length, dimension);
  }
}
