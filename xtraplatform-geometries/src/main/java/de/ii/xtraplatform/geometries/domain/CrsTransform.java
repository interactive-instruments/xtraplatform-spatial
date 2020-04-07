package de.ii.xtraplatform.geometries.domain;

import de.ii.xtraplatform.crs.domain.CrsTransformer;
import org.immutables.value.Value;

import java.io.IOException;

@Value.Immutable
public abstract class CrsTransform implements CoordinatesTransformation {

    @Value.Parameter
    protected abstract CrsTransformer getCrsTransformer();

    @Override
    public void onCoordinates(double[] coordinates, int length, int dimension) throws IOException {

        //TODO: transform in place???
        double[] transformed;
        if (dimension == 3) {
            transformed = getCrsTransformer().transform3d(coordinates, length / dimension, /*TODO*/false);
        } else {
            transformed = getCrsTransformer().transform(coordinates, length / dimension, /*TODO*/false);
        }

        getNext().onCoordinates(transformed, length, dimension);
    }
}
