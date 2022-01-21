/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.geometries.domain;

import org.immutables.value.Value;

import java.io.IOException;

public interface CoordinatesTransformation extends DoubleArrayProcessor {

    @Value.Parameter
    DoubleArrayProcessor getNext();

    @Override
    default void onStart() throws IOException {
        getNext().onStart();
    }

    @Override
    default void onCoordinates(double[] coordinates, int length, int dimension) throws IOException {
        getNext().onCoordinates(coordinates, length, dimension);
    }

    @Override
    default void onFlush() throws IOException {
        getNext().onFlush();
    }

    @Override
    default void onEnd() throws IOException {
        getNext().onEnd();
    }

}
