package de.ii.xtraplatform.features.domain;

import org.immutables.value.Value;

import java.util.OptionalLong;

@Value.Immutable
public interface FeatureCollection {

    @Value.Parameter
    OptionalLong getNumberReturned();

    @Value.Parameter
    OptionalLong getNumberMatched();
}
