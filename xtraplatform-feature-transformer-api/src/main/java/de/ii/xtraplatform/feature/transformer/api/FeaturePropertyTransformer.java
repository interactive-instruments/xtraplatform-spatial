package de.ii.xtraplatform.feature.transformer.api;

public interface FeaturePropertyTransformer<T> {

    String getType();

    String getParameter();

    T transform(T input);

}
