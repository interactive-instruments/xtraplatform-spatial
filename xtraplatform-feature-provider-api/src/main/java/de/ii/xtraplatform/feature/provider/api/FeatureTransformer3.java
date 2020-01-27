package de.ii.xtraplatform.feature.provider.api;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;

public interface FeatureTransformer3<T extends Property<?>, U extends Feature<T>> {

    U createFeature();

    T createProperty();

    void processFeature(U feature) throws IOException;
}
