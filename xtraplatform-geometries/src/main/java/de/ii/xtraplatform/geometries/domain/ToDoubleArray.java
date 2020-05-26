/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.geometries.domain;

import org.immutables.value.Value;

import java.io.IOException;

@Value.Immutable
public abstract class ToDoubleArray implements CoordinatesWriter<DoubleArrayProcessor> {

    protected static final int MAX_BUFFER_SIZE = 1000;
    protected int bufferCursor = 0;
    protected final double[] buffer = new double[MAX_BUFFER_SIZE];

    @Override
    public void onStart() throws IOException {
        getDelegate().onStart();
    }

    @Override
    public void onSeparator() throws IOException {

    }

    @Override
    public void onX(char[] chars, int offset, int length) throws IOException {
        addToBuffer(chars, offset, length);
    }

    @Override
    public void onY(char[] chars, int offset, int length) throws IOException {
        addToBuffer(chars, offset, length);
    }

    @Override
    public void onZ(char[] chars, int offset, int length) throws IOException {
        addToBuffer(chars, offset, length);
    }

    @Override
    public void onFlush() throws IOException {
        if (bufferCursor > 0) {
            getDelegate().onCoordinates(buffer, bufferCursor, getDimension());
            bufferCursor = 0;
        }

        getDelegate().onFlush();
    }

    @Override
    public void onEnd() throws IOException {
        onFlush();

        getDelegate().onEnd();
    }

    private void addToBuffer(char[] chars, int offset, int length) throws IOException {
        this.buffer[bufferCursor] = Double.parseDouble(String.valueOf(chars, offset,length));
        bufferCursor++;

        if (bufferCursor == MAX_BUFFER_SIZE) {
            onFlush();
        }
    }
}
