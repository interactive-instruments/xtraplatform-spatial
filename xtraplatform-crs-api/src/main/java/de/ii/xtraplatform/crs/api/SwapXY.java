package de.ii.xtraplatform.crs.api;

import org.immutables.value.Value;

import java.io.IOException;

@Value.Immutable
public abstract class SwapXY implements CoordinatesTransformation {

    @Override
    public void onCoordinates(double[] coordinates, int length, int dimension) throws IOException {

        for (int i = 0; i < length; i = i + dimension) {
                double x = coordinates[i];
                coordinates[i] = coordinates[i+1];
                coordinates[i+1] = x;
        }

        getNext().onCoordinates(coordinates, length, dimension);
    }
}
