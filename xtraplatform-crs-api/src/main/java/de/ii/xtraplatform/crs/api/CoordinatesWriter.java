package de.ii.xtraplatform.crs.api;

import org.immutables.value.Value;

public interface CoordinatesWriter<T> extends SeperateStringsProcessor {

    @Value.Parameter
    T getDelegate();

    @Value.Parameter
    int getDimension();

}
