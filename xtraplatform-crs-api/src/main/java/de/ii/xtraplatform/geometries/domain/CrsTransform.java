package de.ii.xtraplatform.geometries.domain;

import org.immutables.value.Value;

import java.io.IOException;

@Value.Immutable
public abstract class CrsTransform implements CoordinatesTransformation {

    @Value.Parameter
    protected abstract CrsTransformer getCrsTransformer();

    @Override
    public void onCoordinates(double[] coordinates, int length, int dimension) throws IOException {

        double[] transformed = getCrsTransformer().transform(coordinates, length / dimension, /*TODO*/false);

        getNext().onCoordinates(transformed, transformed.length, dimension);
    }
}
