package de.ii.xtraplatform.geometries.domain;

import java.io.IOException;

public interface DoubleArrayProcessor {
    void onStart() throws IOException;

    void onCoordinates(double[] coordinates, int length, int dimension) throws IOException;

    void onEnd() throws IOException;
}
