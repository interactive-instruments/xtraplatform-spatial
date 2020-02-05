package de.ii.xtraplatform.geometries.domain;

import org.immutables.value.Value;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Value.Immutable
public abstract class ToChars implements DoubleArrayProcessor {

    private boolean isFirst = true;

    @Value.Parameter
    protected abstract SeperateStringsProcessor getCoordinatesProcessor();

    @Value.Parameter
    protected abstract int getPrecision();

    @Override
    public void onStart() throws IOException {
        getCoordinatesProcessor().onStart();
    }

    @Override
    public void onCoordinates(double[] coordinates, int length, int dimension) throws IOException {

        for (int i = 0; i < length; i++) {
            Axis axis = Axis.fromInt[i % dimension];
            String value = String.valueOf(coordinates[i]);

            if (getPrecision() > 0) {
                BigDecimal bd = new BigDecimal(value).setScale(getPrecision(), RoundingMode.HALF_UP);
                value = bd.toPlainString();
            }

            switch (axis) {
                case X:
                    if (isFirst) {
                        this.isFirst = false;
                    } else {
                        getCoordinatesProcessor().onSeparator();
                    }
                    getCoordinatesProcessor().onX(value.toCharArray(), 0, value.length());
                    break;
                case Y:
                    getCoordinatesProcessor().onY(value.toCharArray(), 0, value.length());
                    break;
                case Z:
                    getCoordinatesProcessor().onZ(value.toCharArray(), 0, value.length());
                    break;
            }
        }
    }

    @Override
    public void onEnd() throws IOException {
        getCoordinatesProcessor().onEnd();
    }
}
