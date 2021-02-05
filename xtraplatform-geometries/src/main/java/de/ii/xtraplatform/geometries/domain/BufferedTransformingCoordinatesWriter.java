/**
 * Copyright 2021 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.xtraplatform.geometries.domain;

import de.ii.xtraplatform.crs.domain.CrsTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author zahnen
 */
public class BufferedTransformingCoordinatesWriter extends DefaultCoordinatesWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(BufferedTransformingCoordinatesWriter.class);
    private static final int BUFFER_SIZE = 10000;
    private LazyStringCoordinateTuple tupleBuffer;
    private final double[] coordinateBuffer;
    private final List<double[]> reversePolygonBuffer;
    private final CrsTransformer transformer;
    private final boolean swap;
    private final boolean reversepolygon;
    private final int precision;

    private int zCounter = 0;
    private boolean continuing;

    public BufferedTransformingCoordinatesWriter(CoordinateFormatter formatter, int srsDimension,
                                                 CrsTransformer transformer, boolean swap, boolean reversepolygon,
                                                 int precision) {
        super(formatter, srsDimension);
        this.transformer = transformer;
        //this.tupleBuffer = new LazyStringCoordinateTuple();
        this.coordinateBuffer = new double[BUFFER_SIZE];
        this.swap = swap;
        this.reversepolygon = reversepolygon;
        this.reversePolygonBuffer = new ArrayList<>();
        this.precision = precision;
    }

    @Override
    protected void writeEnd() throws IOException {
        if (isCompleteTuple()) {
            writeCoordinates(true);
        }
    }

    @Override
    protected void writeSeparator() throws IOException {
        writeCoordinates(false);
    }

    private void writeCoordinates(boolean force) throws IOException {
        if (tupleBuffer != null) {
            if (tupleBuffer.hasX()) {
                coordinateBuffer[counter - 2 - zCounter] = tupleBuffer.getX();
            }
            if (tupleBuffer.hasY()) {
                coordinateBuffer[counter - 1 - zCounter] = tupleBuffer.getY();
            }
            tupleBuffer = null;
        }

        if (force || counter == BUFFER_SIZE) {

            double[] c = postProcessCoordinates(coordinateBuffer, (counter - zCounter) / 2);

            if (continuing) {
                super.writeSeparator();
            }

            if (this.reversepolygon) {

                if (!force) { // force == end of coordinates
                    // add c to the buffer
                    reversePolygonBuffer.add(c);
                } else {

                    // the buffer is empty, just write out the current c
                    if (reversePolygonBuffer.isEmpty()) {

                        for (int i = 0; i < c.length; i += 2) {
                            formatter.value(Double.toString(c[i]));
                            formatter.value(Double.toString(c[i + 1]));
                            if (i < c.length - 2) {
                                super.writeSeparator();
                            }
                        }
                    } else { // the buffer must be written out in reverse order                   
                        for (int i0 = reversePolygonBuffer.size() - 1; i0 >= 0; i0--) {
                            double[] c0 = reversePolygonBuffer.get(i0);
                            for (int i = 0; i < c0.length; i += 2) {
                                formatter.value(Double.toString(c0[i]));
                                formatter.value(Double.toString(c0[i + 1]));
                                if (i < c0.length - 2) {
                                    super.writeSeparator();
                                }
                            }
                        }
                        reversePolygonBuffer.clear();
                    }
                }
            } else {

                int numCoo;
                if (c.length == coordinateBuffer.length) {
                    numCoo = (counter - zCounter);
                } else {
                    numCoo = c.length;
                }
                for (int i = 0; i < numCoo; i += 2) {
                    formatter.value(Double.toString(c[i]));
                    formatter.value(Double.toString(c[i + 1]));
                    if (i < numCoo - 2) {
                        super.writeSeparator();
                    }
                }
                counter = 0;
            }

            continuing = true;
        }
    }

    protected double[] postProcessCoordinates(double[] in, int numPts) {
        double[] out;
        if (transformer != null) {
            out = transformer.transform(in, numPts, true);
        } else {
            if (this.swap) {
                out = new double[numPts * 2];
                for (int i = 0; i < numPts * 2; i += 2) {
                    out[i] = in[i + 1];
                    out[i + 1] = in[i];
                }
            } else {
                out = in;
            }
        }
        // das Array das in den Buffer geht muss eine Kopie sein Fall: out = in;
        // Revere Arrays ... [#430]
        if (this.reversepolygon) {
            double[] out2 = new double[numPts * 2];
            for (int i = 0; i < numPts * 2; i++) {
                out2[numPts * 2 - i - 1] = out[i];
            }
            for (int i = 0; i < numPts * 2; i += 2) {
                out[i] = out2[i + 1];
                out[i + 1] = out2[i];
            }
        }

        if (precision > 0) {
            double factor = Math.pow(10, precision);
            for(int i = 0; i < numPts * 2; i++){
                out[i] = (double)Math.round(out[i]* factor) / factor;
            }
        }

        // just to be shure not returning the full buffer ...
        if (out.length == numPts * 2) {
            return out;
        }
        // else: just the valid range
        return Arrays.copyOfRange(out, 0, numPts * 2);
    }

    @Override
    protected void formatValue(String val) throws IOException {
        if (tupleBuffer == null) {
            tupleBuffer = new LazyStringCoordinateTuple();
        }
        if (isXValue()) {
            tupleBuffer.setX(val);
        }
        if (isYValue()) {
            tupleBuffer.setY(val);
        }
    }

    @Override
    protected void formatValue(char[] chars, int i, int j) throws IOException {
        if (isZValue()) {
            zCounter++;
        } else {
            coordinateBuffer[counter - 1 - zCounter] = Double.parseDouble(String.copyValueOf(chars, i, j));
        }
    }

    @Override
    protected void formatRaw(char[] chars, int i, int j) throws IOException {
        if (isXValue()) {
            tupleBuffer.appendX(String.copyValueOf(chars, i, j));
        }
        if (isYValue()) {
            tupleBuffer.appendY(String.copyValueOf(chars, i, j));
        }
    }
}
