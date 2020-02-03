package de.ii.xtraplatform.geometries.domain;

import org.immutables.value.Value;

public interface CoordinatesWriter<T> extends SeperateStringsProcessor {

    @Value.Parameter
    T getDelegate();

    @Value.Parameter
    int getDimension();

}
