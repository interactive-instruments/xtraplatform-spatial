package de.ii.xtraplatform.crs.api;

import java.io.IOException;

public interface DoubleArrayProcessor {
    void onStart() throws IOException;

    void onCoordinates(double[] coordinates, int length, int dimension) throws IOException;

    void onEnd() throws IOException;
}
