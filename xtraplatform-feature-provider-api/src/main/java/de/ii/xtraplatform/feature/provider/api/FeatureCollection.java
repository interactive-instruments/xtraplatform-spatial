package de.ii.xtraplatform.feature.provider.api;

import org.immutables.value.Value;

import java.util.OptionalLong;

@Value.Immutable
public interface FeatureCollection {

    @Value.Parameter
    OptionalLong getNumberReturned();

    @Value.Parameter
    OptionalLong getNumberMatched();
}
