/**
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.geometries.domain;

import java.io.IOException;

public interface DoubleArrayProcessor {
    void onStart() throws IOException;

    void onCoordinates(double[] coordinates, int length, int dimension) throws IOException;

    default void onFlush() throws IOException {}

    void onEnd() throws IOException;
}
