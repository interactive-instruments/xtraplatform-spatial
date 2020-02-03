package de.ii.xtraplatform.crs.api;

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
    default void onEnd() throws IOException {
        getNext().onEnd();
    }

}
