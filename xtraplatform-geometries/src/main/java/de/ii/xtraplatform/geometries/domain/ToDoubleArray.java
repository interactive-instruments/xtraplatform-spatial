/**
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
public abstract class ToDoubleArray implements CoordinatesWriter<DoubleArrayProcessor> {

    protected static final int MAX_BUFFER_SIZE = 512;

    protected int bufferCursor;

    protected ToDoubleArray() {
        this.bufferCursor = 0;
    }

    @Value.Derived
    protected int getBufferSize() {
        return MAX_BUFFER_SIZE * getDimension();
    }

    @Value.Derived
    protected double[] getBuffer() {
        return new double[getBufferSize()];
    }

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
            getDelegate().onCoordinates(getBuffer(), bufferCursor, getDimension());
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
        this.getBuffer()[bufferCursor] = Double.parseDouble(String.valueOf(chars, offset,length));
        bufferCursor++;

        if (bufferCursor == getBufferSize()) {
            onFlush();
        }
    }
}
