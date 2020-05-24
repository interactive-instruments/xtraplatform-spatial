package de.ii.xtraplatform.feature.provider.sql.app;


import org.immutables.value.Value;

//TODO: move to xtraplatform-base
@Value.Immutable
public interface Tuple<T,U> {

    @Value.Parameter
    T first();

    @Value.Parameter
    U second();
}
