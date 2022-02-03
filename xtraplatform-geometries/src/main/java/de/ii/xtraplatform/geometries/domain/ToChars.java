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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Value.Immutable
public abstract class ToChars implements DoubleArrayProcessor {

    private boolean isFirst = true;

    @Value.Parameter
    protected abstract SeperateStringsProcessor getCoordinatesProcessor();

    @Value.Parameter
    protected abstract List<Integer> getPrecision();

    @Override
    public void onStart() throws IOException {
        getCoordinatesProcessor().onStart();
    }

    @Override
    public void onCoordinates(double[] coordinates, int length, int dimension) throws IOException {
        Integer[] precision = getPrecision().toArray(getPrecision().toArray(new Integer[0]));
        for (int i = 0; i < length; i++) {
            int axisIndex = i % dimension;
            Axis axis = Axis.fromInt[axisIndex];
            String value = String.valueOf(coordinates[i]);

            /*
            TODO: will not be applied when no transformations are given
             move to separate step
            */
            if (precision[axisIndex] > 0) {
                BigDecimal bd = new BigDecimal(value).setScale(precision[axisIndex], RoundingMode.HALF_UP);
                value = bd.toPlainString();
            }

            switch (axis) {
                case X:
                    if (isFirst) {
                        this.isFirst = false;
                    } else {
                        getCoordinatesProcessor().onSeparator();
                    }
                    getCoordinatesProcessor().onX(value.toCharArray(), 0, value.length());
                    break;
                case Y:
                    getCoordinatesProcessor().onY(value.toCharArray(), 0, value.length());
                    break;
                case Z:
                    getCoordinatesProcessor().onZ(value.toCharArray(), 0, value.length());
                    break;
            }
        }
    }

    @Override
    public void onFlush() throws IOException {

        getCoordinatesProcessor().onFlush();
    }

    @Override
    public void onEnd() throws IOException {
        getCoordinatesProcessor().onEnd();
    }
}
