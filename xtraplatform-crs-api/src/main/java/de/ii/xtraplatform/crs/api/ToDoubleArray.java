package de.ii.xtraplatform.crs.api;

import org.immutables.value.Value;

import java.io.IOException;

@Value.Immutable
public abstract class ToDoubleArray implements CoordinatesWriter<DoubleArrayProcessor> {

    protected static final int MAX_BUFFER_SIZE = 1000;
    protected int bufferCursor = 0;
    protected final double[] buffer = new double[MAX_BUFFER_SIZE];

    @Override
    public void onStart() throws IOException {
        getDelegate().onStart();
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
    public void onEnd() throws IOException {
        getDelegate().onCoordinates(buffer, bufferCursor, getDimension());

        getDelegate().onEnd();
    }

    private void addToBuffer(char[] chars, int offset, int length) throws IOException {
        this.buffer[bufferCursor] = Double.parseDouble(String.valueOf(chars, offset,length));
        bufferCursor++;

        if (bufferCursor == MAX_BUFFER_SIZE) {
            getDelegate().onCoordinates(buffer, bufferCursor, getDimension());
            bufferCursor = 0;
        }
    }
}
