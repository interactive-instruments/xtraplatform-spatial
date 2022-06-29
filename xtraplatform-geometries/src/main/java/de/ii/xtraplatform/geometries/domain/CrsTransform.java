/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.geometries.domain;

import de.ii.xtraplatform.crs.domain.CrsTransformer;
import java.io.IOException;
import org.immutables.value.Value;

@Value.Immutable
public abstract class CrsTransform implements CoordinatesTransformation {

  @Value.Parameter
  protected abstract CrsTransformer getCrsTransformer();

  @Override
  public void onCoordinates(double[] coordinates, int length, int dimension) throws IOException {
    double[] transformed =
        getCrsTransformer().transform(coordinates, length / dimension, dimension);

    getNext().onCoordinates(transformed, length, dimension);
  }
}
