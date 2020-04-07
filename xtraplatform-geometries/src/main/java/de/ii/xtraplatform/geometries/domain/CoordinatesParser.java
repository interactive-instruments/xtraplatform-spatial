/**
 * Copyright 2020 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.geometries.domain;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.util.Objects;

/**
 *
 * @author zahnen
 */
public class CoordinatesParser {

    private final SeperateStringsProcessor coordinatesProcessor;
    private final int sourceDimension;
    private final int targetDimension;

    private boolean started;
    private boolean lastCharWasSeparator;
    private char[] chunkBoundaryBuffer;
    private int counter;
    private Axis axis;

    public CoordinatesParser(SeperateStringsProcessor coordinatesProcessor, int sourceDimension, int targetDimension) {
        this.coordinatesProcessor = coordinatesProcessor;
        this.sourceDimension = sourceDimension;
        this.targetDimension = targetDimension;
        this.started = false;
        this.lastCharWasSeparator = true;
        this.chunkBoundaryBuffer = null;
        this.counter = 0;
        this.axis = Axis.X;
    }

    public final void parse(char[] chars, int offset, int length) throws IOException {

        boolean endOfChunk = false;
        int read = 0;

        // iterate over chunk
        for (int j = offset; j < offset + length; j++) {
            endOfChunk = j == offset + length - 1;

            // skip leading, trailing and back-to-back whitespaces
            if (isSeparator(chars[j])) {
                if (!lastCharWasSeparator) {
                    // we just found the end of a coordinate
                    lastCharWasSeparator = true;

                    onValue(chars, j - read, read);

                    read = -1;
                } else {
                    read--;
                }
            } else {
                lastCharWasSeparator = false;
            }

            // if we reach the end of the provided input chunk and the last character
            // is not a whitespace, append the chars since the last whitespace to chunkBoundaryBuffer
            if (endOfChunk) {
                onChunkEnd(chars, j - read, read + 1);
            }

            read++;
        }
    }

    public void close() throws IOException {
        if (!started) {
            onStart();
        }
        if (!lastCharWasSeparator) {
            onValue(new char[0], 0, 0);
        }

        coordinatesProcessor.onEnd();
    }

    private boolean isSeparator(char chr) {
        return chr == ' ' || chr == '\n' || chr == '\t' || chr == '\r' || chr == ',';
    }

    private void onStart() throws IOException {
        this.started = true;
        coordinatesProcessor.onStart();
    }

    private void onChunkEnd(char[] chars, int offset, int length) throws IOException {
        if (length > 0) {
            if (hasChunkBoundaryBuffer()) {
                //chunkBoundaryBuffer = chunkBoundaryBuffer.concat(String.copyValueOf(chars, offset, length));
                chunkBoundaryBuffer = ArrayUtils.addAll(chunkBoundaryBuffer, ArrayUtils.subarray(chars, offset, offset + length));
            } else {
                //chunkBoundaryBuffer = String.copyValueOf(chars, offset, length);
                chunkBoundaryBuffer = ArrayUtils.subarray(chars, offset, offset + length);
            }
        }
    }

    private boolean hasChunkBoundaryBuffer() throws IOException {
        return Objects.nonNull(chunkBoundaryBuffer);
    }

    private void onValue(char[] chars, int offset, int length) throws IOException {
        this.axis = Axis.fromInt[counter % sourceDimension];
        this.counter++;

        // if chunkBoundaryBuffer is not empty, we just started with a new input chunk and need to write chunkBoundaryBuffer first
        if (hasChunkBoundaryBuffer()) {
            //formatValue(chunkBoundaryBuffer.concat(String.copyValueOf(chars, start, end)));
            char[] value = ArrayUtils.addAll(chunkBoundaryBuffer, ArrayUtils.subarray(chars, offset, offset + length));
            chunkBoundaryBuffer = null;
            formatValue(value, 0, value.length);
            //formatRaw(chars, start, end);
        } else {
            formatValue(chars, offset, length);

        }
    }

    private void formatValue(char[] chars, int offset, int length) throws IOException {
        boolean writeSeparator = true;
        if (!started) {
            onStart();
            writeSeparator = false;
        }

        switch (axis) {
            case X:
                if (writeSeparator) {
                    coordinatesProcessor.onSeparator();
                }
                coordinatesProcessor.onX(chars, offset, length);
                break;
            case Y:
                coordinatesProcessor.onY(chars, offset, length);
                break;
            case Z:
                if (targetDimension == 3) {
                    coordinatesProcessor.onZ(chars, offset, length);
                }
                break;
        }
    }
}
