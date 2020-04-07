package de.ii.xtraplatform.features.domain.transform;

public interface FeaturePropertyTransformer<T> {

    String getType();

    String getParameter();

    T transform(T input);

}
